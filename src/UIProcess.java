import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.sun.net.httpserver.*;

/**
 * STC89C52 Microcontroller Multi-Process Simulator - UI Process
 * Owner & Developer: Bondas (UI Module)
 *
 * No hardcoded/demo data: every value shown comes from CoreProcess (port 5000).
 * Until Core replies, panels show "--". If Core is down, the UI says so.
 *
 * Core -> UI protocol (one line per state):
 *   STATE|PC=0004|A=01|B=00|CY=true|OV=false|SP=07|Q_SIZE=2      (required fields)
 * Optional extra keys, shown only if Core sends them:
 *   AC, P (flags), R0..R7, INSTR, OPCODE, FETCH, DECODE, EXECUTE,
 *   Q_CAP, Q_FRONT, Q_REAR, Q_ELEMS (comma separated), RAM (512 hex chars = 256 bytes)
 * Other lines starting with LOG| are shown as "Last Event". "HALTED" ends a RUN.
 *
 * Requires Java 15+ (uses a text block for the web page).
 */
public class UIProcess {
    private static final String CORE_HOST = "localhost";
    private static final int CORE_PORT = 5000;
    private static final int WEB_PORT = 8080;
    private static final String OFFLINE = "ERROR|CORE_OFFLINE";
    private static final Set<String> WEB_COMMANDS = Set.of("STEP", "RUN", "STOP", "RESET", "LOAD", "STATE");

    // Last real STATE line received from Core (null until the first one arrives)
    private static volatile String lastState = null;

    public static void main(String[] args) throws IOException {
        printConsoleBanner();

        HttpServer webServer = HttpServer.create(new InetSocketAddress(WEB_PORT), 0);
        webServer.createContext("/", new WebHandler());
        webServer.createContext("/api/cmd", new ApiHandler());
        webServer.start();

        HttpServer altServer = null;
        try {
            altServer = HttpServer.create(new InetSocketAddress(8000), 0);
            altServer.createContext("/", new WebHandler());
            altServer.createContext("/api/cmd", new ApiHandler());
            altServer.start();
        } catch (Exception ignored) {}

        System.out.println("[JAVA WEB SERVER] Running at: http://localhost:" + WEB_PORT + " (also http://localhost:8000)");
        System.out.println("Open http://localhost:" + WEB_PORT + " or http://localhost:8000 in Chrome/Edge for the 6-panel dashboard.\n");

        lastState = sendCommandToCore("STATE");
        printConsoleDashboard(lastState);

        Scanner scanner = new Scanner(System.in);
        System.out.println("\nCommands: STEP | RUN | STOP | RESET | HELP | QUIT");
        while (true) {
            System.out.print("STC89C52-UI> ");
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String cmd = input.toUpperCase();
            if (cmd.equals("QUIT") || cmd.equals("EXIT")) {
                sendCommandToCore("QUIT");
                System.out.println("[UI System] Disconnecting from CoreProcess. Goodbye!");
                break;
            }
            if (cmd.equals("HELP")) {
                printHelpMenu();
                continue;
            }

            String resp = sendCommandToCore(cmd);
            if (resp.startsWith("ERROR|")) {
                System.out.println("[UI System] " + resp + " (is CoreProcess running on port " + CORE_PORT + "?)");
            }
            printConsoleDashboard(lastState);
        }

        webServer.stop(0);
    }

    // ---------------------------------------------------------------- web server

    static class WebHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] response = WEB_PAGE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }

    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getRawQuery();
            String result;
            if (query != null && query.startsWith("cmd=")) {
                String raw = URLDecoder.decode(query.substring(4), StandardCharsets.UTF_8).trim();
                String verb = raw.split("\\s+", 2)[0].toUpperCase();
                if (WEB_COMMANDS.contains(verb)) {
                    // keep any payload (e.g. hex for LOAD) untouched
                    String payload = raw.length() > verb.length() ? raw.substring(verb.length()) : "";
                    result = sendCommandToCore(verb + payload);
                } else {
                    result = "ERROR|UNKNOWN_COMMAND";
                }
            } else {
                result = "ERROR|MISSING_CMD";
            }

            byte[] response = result.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }

    // ---------------------------------------------------------------- core client

    private static synchronized String sendCommandToCore(String cmd) {
        boolean isRun = cmd.equals("RUN");
        StringBuilder sb = new StringBuilder();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(CORE_HOST, CORE_PORT), 2000);
            socket.setSoTimeout(isRun ? 30000 : 3000);
            PrintWriter toCore = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader fromCore = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            toCore.println(cmd);

            if (cmd.equals("QUIT")) return "";

            String resp;
            try {
                while ((resp = fromCore.readLine()) != null) {
                    sb.append(resp).append("\n");
                    if (resp.startsWith("STATE|")) lastState = resp;
                    if (resp.equals("HALTED") || resp.equals("RESET_DONE")) break;
                    if (cmd.equals("STEP") || cmd.equals("RESET") || cmd.startsWith("LOAD")) break; // single-reply commands
                }
            } catch (SocketTimeoutException timeout) {
                // Core did not send more; return whatever we already got
            }
            return sb.toString();

        } catch (IOException e) {
            return OFFLINE;
        }
    }

    // ---------------------------------------------------------------- console UI

    private static Map<String, String> parseState(String stateLine) {
        Map<String, String> m = new HashMap<>();
        if (stateLine == null || !stateLine.startsWith("STATE|")) return m;
        String[] parts = stateLine.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq > 0) m.put(parts[i].substring(0, eq).trim().toUpperCase(), parts[i].substring(eq + 1).trim());
        }
        return m;
    }

    private static String v(Map<String, String> m, String key) {
        return m.getOrDefault(key, "--");
    }

    private static String flag(Map<String, String> m, String key) {
        String s = m.get(key);
        if (s == null) return "-";
        return (s.equalsIgnoreCase("true") || s.equals("1")) ? "1" : "0";
    }

    private static void printConsoleDashboard(String stateLine) {
        Map<String, String> s = parseState(stateLine);
        boolean have = !s.isEmpty();

        String acc = v(s, "A");
        String accDec = have && s.containsKey("A") ? String.valueOf(parseHexOrDec(acc)) : "--";
        String depth = "--";
        if (s.containsKey("SP")) depth = String.valueOf(Math.max(0, parseHexOrDec(s.get("SP")) - 0x07));

        String line = "+" + "-".repeat(70) + "+";
        System.out.println(line);
        System.out.println("|" + center("STC89C52 MICROCONTROLLER SIMULATOR", 70) + "|");
        System.out.println(line);
        System.out.println("| " + pad(have ? "Core state received" : "No state from Core yet (press STEP / RUN)", 68) + " |");
        System.out.println(line);
        System.out.println("| " + pad("PC : 0x" + v(s, "PC") + "     SP : 0x" + v(s, "SP") + "     Stack depth: " + depth, 68) + " |");
        System.out.println("| " + pad("ACC: 0x" + acc + " (" + accDec + ")     B : 0x" + v(s, "B"), 68) + " |");
        System.out.println("| " + pad("FLAGS: [CY:" + flag(s, "CY") + "] [AC:" + flag(s, "AC") + "] [OV:" + flag(s, "OV") + "] [P:" + flag(s, "P") + "]", 68) + " |");
        if (s.containsKey("INSTR")) {
            System.out.println("| " + pad("Instruction: " + s.get("INSTR"), 68) + " |");
        }
        System.out.println("| " + pad("FIFO Queue size: " + v(s, "Q_SIZE") + (s.containsKey("Q_CAP") ? " / " + s.get("Q_CAP") : ""), 68) + " |");
        System.out.println(line);
    }

    private static String pad(String t, int w) {
        return t.length() >= w ? t.substring(0, w) : t + " ".repeat(w - t.length());
    }

    private static String center(String t, int w) {
        int left = Math.max(0, (w - t.length()) / 2);
        return pad(" ".repeat(left) + t, w);
    }

    private static int parseHexOrDec(String val) {
        try { return Integer.parseInt(val, 16); } catch (Exception e) { return 0; }
    }

    private static void printConsoleBanner() {
        System.out.println("=====================================================================");
        System.out.println("  STC89C52 8051 MICROCONTROLLER MULTI-PROCESS SIMULATOR - UI");
        System.out.println("  Developer & Owner: Bondas (UI Module)");
        System.out.println("=====================================================================");
    }

    private static void printHelpMenu() {
        System.out.println("\n[SIMULATOR INSTRUCTION REFERENCE MANUAL]");
        System.out.println("----------------------------------------------------------------");
        System.out.println(" STEP  : Executes 1 instruction (Fetch -> Decode -> Execute)");
        System.out.println(" RUN   : Executes continuously until HALT (opcode 0xFF)");
        System.out.println(" STOP  : Asks Core to stop a running program");
        System.out.println(" RESET : Restores PC=0, Registers, Stack & Queue to defaults");
        System.out.println(" QUIT  : Closes TCP connection and terminates session");
        System.out.println("----------------------------------------------------------------\n");
    }

    // ---------------------------------------------------------------- web page

    private static final String WEB_PAGE = """
<!DOCTYPE html>
<html lang='en'>
<head>
  <meta charset='UTF-8'>
  <meta name='viewport' content='width=device-width, initial-scale=1.0'>
  <title>STC89C52 Microcontroller Simulator - 6 Panel Dashboard</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, 'Roboto', 'Courier New', monospace; background: #0f172a; color: #f1f5f9; padding: 16px; font-size: 13px; line-height: 1.5; }
    .container { max-width: 1200px; margin: 0 auto; display: flex; flex-direction: column; gap: 14px; }
    .header-bar { background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 14px 18px; display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 12px; }
    .title { font-size: 16px; font-weight: 700; color: #38bdf8; display: flex; align-items: center; gap: 8px; }
    .dot { width: 10px; height: 10px; border-radius: 50%; background: #64748b; display: inline-block; }
    .dot-green { background: #10b981; }
    .dot-red { background: #ef4444; }
    .subtext { font-size: 11px; color: #94a3b8; margin-top: 2px; }
    .btn-group { display: flex; flex-wrap: wrap; gap: 8px; }
    button { background: #334155; color: #fff; border: 1px solid #475569; padding: 6px 14px; border-radius: 6px; font-size: 12px; font-weight: 600; cursor: pointer; transition: background 0.15s; }
    button:hover { background: #475569; }
    button.btn-step { background: #059669; border-color: #10b981; }
    button.btn-step:hover { background: #10b981; }
    button.btn-run { background: #0284c7; border-color: #38bdf8; }
    button.btn-run:hover { background: #0369a1; }
    button.btn-stop { background: #dc2626; border-color: #ef4444; }
    button.btn-stop:hover { background: #b91c1c; }
    button.btn-reset { background: #d97706; border-color: #f59e0b; }
    button.btn-reset:hover { background: #b45309; }
    button.btn-refresh { background: #4f46e5; border-color: #6366f1; }
    button.btn-refresh:hover { background: #4338ca; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 14px; }
    .card { background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 14px; display: flex; flex-direction: column; gap: 10px; }
    .card-title { font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid #334155; padding-bottom: 6px; }
    .title-blue { color: #38bdf8; }
    .title-green { color: #34d399; }
    .title-amber { color: #fbbf24; }
    .inner-box { background: #0b1120; border: 1px solid #1e293b; border-radius: 6px; padding: 12px; min-height: 160px; font-family: 'Courier New', monospace; font-size: 12px; color: #cbd5e1; }
    textarea.inner-box { width: 100%; resize: vertical; outline: none; border-color: #334155; color: #f1f5f9; }
    .trace-phase { color: #34d399; margin: 4px 0; font-weight: 600; }
    .val-highlight { font-weight: 700; color: #38bdf8; }
    .reg-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px 14px; }
    .flag-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
    .chip { padding: 2px 8px; border-radius: 4px; font-size: 11px; background: #0f172a; border: 1px solid #334155; color: #94a3b8; }
    .chip-on { background: #064e3b; border-color: #10b981; color: #34d399; font-weight: 700; }
    .status-bar { background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 10px 16px; display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; font-size: 11px; color: #94a3b8; }
    .status-text { font-weight: 700; color: #34d399; }
    pre { white-space: pre; overflow-x: auto; }
  </style>
</head>
<body>
  <div class='container'>
    <div class='header-bar'>
      <div>
        <div class='title'>
          <span id='dot' class='dot'></span>
          STC89C52 MICROCONTROLLER SIMULATOR (6-PANEL DASHBOARD)
        </div>
        <div class='subtext'>System Architecture: Multi-Process POSIX IPC | Core Port: 5000 | Web: 8080</div>
      </div>
      <div class='btn-group'>
        <button onclick='loadCode()'>[ Load Hex ]</button>
        <button class='btn-step' onclick='send("STEP")'>[ Step (F-D-E) ]</button>
        <button class='btn-run' onclick='send("RUN")'>[ Run Continuous ]</button>
        <button class='btn-stop' onclick='send("STOP")'>[ Stop / Halt ]</button>
        <button class='btn-reset' onclick='send("RESET")'>[ Reset ]</button>
        <button class='btn-refresh' onclick='send("STATE")'>[ Refresh ]</button>
      </div>
    </div>

    <div class='grid'>
      <!-- 1. Assembly / Opcode Editor -->
      <div class='card'>
        <div class='card-title title-blue'>1. Assembly / Hex Opcode Editor</div>
        <div style='display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 4px;'>
          <button style='font-size: 10.5px; padding: 4px 8px; background: #1e3a8a; border-color: #3b82f6;' onclick='setPreset(1)'>[ Preset 1: Math &amp; Flags ]</button>
          <button style='font-size: 10.5px; padding: 4px 8px; background: #065f46; border-color: #10b981;' onclick='setPreset(2)'>[ Preset 2: FIFO Queue ]</button>
          <button style='font-size: 10.5px; padding: 4px 8px; background: #78350f; border-color: #f59e0b;' onclick='setPreset(3)'>[ Preset 3: Logic (ANL) ]</button>
          <button style='font-size: 10.5px; padding: 4px 8px; background: #4c1d95; border-color: #8b5cf6;' onclick='setPreset(4)'>[ Preset 4: Jump (SJMP) ]</button>
        </div>
        <textarea id='code' placeholder='Enter hex bytes, e.g.: 74 FE 24 03 04 54 0F E0 74 09 E0 D0 FF' class='inner-box'>74 FE 24 03 04 54 0F E0 74 09 E0 D0 FF</textarea>
        <div style='font-size: 10.5px; color: #94a3b8; line-height: 1.4; border-top: 1px solid #334155; padding-top: 6px;'>
          <b>Opcode Reference:</b> <code>74 d</code>: MOV A,#data | <code>24 d</code>: ADD A,#data | <code>04</code>: INC A | <code>54 d</code>: ANL A,#data | <code>80 r</code>: SJMP | <code>E0</code>: ENQUEUE | <code>D0</code>: DEQUEUE | <code>FF</code>: HALT
        </div>
      </div>

      <!-- 2. Execution Trace -->
      <div class='card'>
        <div class='card-title title-green'>2. Execution Trace (Fetch - Decode - Execute)</div>
        <div class='inner-box'>
          <div>Current PC: <span class='val-highlight' id='trace-pc'>--</span></div>
          <div>Instruction: <span class='val-highlight' id='trace-instr'>--</span></div>
          <div class='trace-phase' id='trace-fetch'></div>
          <div class='trace-phase' id='trace-decode'></div>
          <div class='trace-phase' id='trace-exec'></div>
          <div style='margin-top: 6px; border-top: 1px solid #1e293b; padding-top: 4px; color: #94a3b8;'>
            State Transition:<br>
            &nbsp;&nbsp;ACC: <span id='trace-acc'>--</span><br>
            &nbsp;&nbsp;CY : <span id='trace-cy'>--</span>
          </div>
        </div>
      </div>

      <!-- 3. Registers & PSW -->
      <div class='card'>
        <div class='card-title title-amber'>3. Registers &amp; PSW Flags</div>
        <div class='inner-box'>
          <div class='reg-grid'>
            <div>PC : <span class='val-highlight' id='reg-pc'>--</span></div>
            <div>SP : <span class='val-highlight' id='reg-sp'>--</span></div>
            <div>ACC: <span class='val-highlight' id='reg-acc'>--</span></div>
            <div>B  : <span class='val-highlight' id='reg-b'>--</span></div>
            <div>R0 : <span id='reg-r0'>--</span></div><div>R1 : <span id='reg-r1'>--</span></div>
            <div>R2 : <span id='reg-r2'>--</span></div><div>R3 : <span id='reg-r3'>--</span></div>
            <div>R4 : <span id='reg-r4'>--</span></div><div>R5 : <span id='reg-r5'>--</span></div>
            <div>R6 : <span id='reg-r6'>--</span></div><div>R7 : <span id='reg-r7'>--</span></div>
          </div>
          <div style='margin-top: 8px; border-top: 1px solid #1e293b; padding-top: 6px;'>
            <div style='color: #94a3b8;'>PSW Flags:</div>
            <div class='flag-chips' id='psw-flags'></div>
          </div>
        </div>
      </div>

      <!-- 4. Hardware Stack -->
      <div class='card'>
        <div class='card-title title-blue'>4. Hardware Stack (SP: <span id='stack-sp'>--</span>)</div>
        <div class='inner-box'>
          <div>Top of Stack: <span class='val-highlight' id='stack-top'>--</span></div>
          <div style='margin-top: 6px;'>Stack Depth : <span id='stack-depth'>--</span></div>
          <div style='margin-top: 6px; color: #94a3b8; font-size: 11px;'>8051 Hardware Stack resets to SP=0x07 (pre-increment ++SP to 0x08 on PUSH).</div>
        </div>
      </div>

      <!-- 5. Circular FIFO Queue -->
      <div class='card'>
        <div class='card-title title-green'>5. Circular FIFO Queue (Buffer)</div>
        <div class='inner-box'>
          <div>Size    : <span class='val-highlight' id='q-size'>--</span></div>
          <div>Capacity: <span id='q-cap'>--</span></div>
          <div>Front   : <span id='q-front'>--</span></div>
          <div>Rear    : <span id='q-rear'>--</span></div>
          <div style='margin-top: 6px;'>Elements: <span class='val-highlight' id='q-elems'>--</span></div>
        </div>
      </div>

      <!-- 6. Memory RAM Inspector -->
      <div class='card'>
        <div class='card-title title-amber'>6. Memory RAM Inspector (256 Bytes)</div>
        <div class='inner-box' style='overflow-x: auto;'>
          <pre id='ram' style='font-size: 11px; color: #94a3b8;'>--</pre>
        </div>
      </div>
    </div>

    <div class='status-bar'>
      <div>STATUS: <span id='status' class='status-text'>Connecting...</span></div>
      <div id='status-last-event'>Last Event: None</div>
    </div>
  </div>

  <script>
    const $ = id => document.getElementById(id);
    const set = (id, val) => { $(id).innerText = (val === undefined || val === '') ? '--' : val; };
    let prev = null;

    function parseState(text) {
      const line = text.split('\\n').reverse().find(l => l.startsWith('STATE|'));
      if (!line) return null;
      const m = {};
      line.trim().split('|').slice(1).forEach(p => {
        const i = p.indexOf('=');
        if (i > 0) m[p.slice(0, i).trim().toUpperCase()] = p.slice(i + 1).trim();
      });
      return m;
    }

    const isOn = v => v === 'true' || v === '1';
    const hex = v => v === undefined ? undefined : '0x' + v;

    function flagChip(name, v) {
      const on = isOn(v);
      const cls = on ? 'chip chip-on' : 'chip';
      return "<span class='" + cls + "'>[" + name + ": " + (v === undefined ? '-' : (on ? 1 : 0)) + "]</span>";
    }

    function render(s) {
      set('trace-pc', hex(s.PC)); set('reg-pc', hex(s.PC));
      set('reg-sp', hex(s.SP)); set('stack-sp', hex(s.SP));
      const accTxt = s.A === undefined ? undefined : '0x' + s.A + ' (' + parseInt(s.A, 16) + ')';
      set('reg-acc', accTxt); set('reg-b', hex(s.B));
      for (let i = 0; i < 8; i++) set('reg-r' + i, hex(s['R' + i]));
      set('trace-instr', s.INSTR);
      $('trace-fetch').innerText  = s.FETCH   ? 'Fetch  : ' + s.FETCH   : '';
      $('trace-decode').innerText = s.DECODE  ? 'Decode : ' + s.DECODE  : '';
      $('trace-exec').innerText   = s.EXECUTE ? 'Execute: ' + s.EXECUTE : '';

      set('trace-acc', s.A === undefined ? undefined : (prev && prev.A !== undefined ? '0x' + prev.A + ' -> ' : '') + '0x' + s.A);
      set('trace-cy',  s.CY === undefined ? undefined : (prev && prev.CY !== undefined ? prev.CY + ' -> ' : '') + s.CY);

      $('psw-flags').innerHTML = flagChip('CY', s.CY) + flagChip('AC', s.AC) + flagChip('OV', s.OV) + flagChip('P', s.P);

      if (s.SP !== undefined) set('stack-depth', Math.max(0, parseInt(s.SP, 16) - 7) + ' items');
      else set('stack-depth', undefined);

      if (s.RAM && s.SP !== undefined) {
        const sp = parseInt(s.SP, 16);
        set('stack-top', 'RAM[0x' + s.SP + '] = 0x' + s.RAM.substr(sp * 2, 2));
      } else set('stack-top', undefined);

      set('q-size', s.Q_SIZE); set('q-cap', s.Q_CAP);
      set('q-front', s.Q_FRONT); set('q-rear', s.Q_REAR);
      set('q-elems', s.Q_ELEMS ? '[ ' + s.Q_ELEMS.split(',').join(', ') + ' ]' : undefined);

      if (s.RAM && s.RAM.length >= 512) {
        let out = 'Addr  ' + Array.from({length: 16}, (_, i) => '+' + i.toString(16).toUpperCase()).join(' ') + '\\n';
        for (let r = 0; r < 16; r++) {
          out += '0x' + (r * 16).toString(16).toUpperCase().padStart(2, '0') + ': ' +
                 s.RAM.substr(r * 32, 32).match(/.{2}/g).join(' ') + '\\n';
        }
        $('ram').innerText = out;
      } else $('ram').innerText = '--';
    }

    async function send(cmd) {
      try {
        const res = await fetch('/api/cmd?cmd=' + encodeURIComponent(cmd));
        const text = await res.text();
        if (text.includes('ERROR|CORE_OFFLINE')) {
          $('dot').className = 'dot dot-red';
          $('status').style.color = '#ef4444';
          $('status').innerText = 'Core offline - start CoreProcess on port 5000';
          return;
        }
        $('dot').className = 'dot dot-green';
        $('status').style.color = '#34d399';
        $('status').innerText = text.includes('HALTED') ? 'Halted' : 'Connected to Core (port 5000)';

        const log = text.split('\\n').filter(l => l.startsWith('LOG|')).pop();
        if (log) set('status-last-event', 'Last Event: ' + log);

        const s = parseState(text);
        if (s) { render(s); prev = s; }
        if (cmd === 'RESET') prev = null;
      } catch (e) {
        $('status').style.color = '#ef4444';
        $('status').innerText = 'UI server unreachable';
      }
    }

    const PRESETS = {
      1: "74 FE ; MOV A, #254\\n24 03 ; ADD A, #3 (CY=1, AC=1)\\n04    ; INC A\\n54 0F ; ANL A, #0x0F\\nE0    ; ENQUEUE A\\n74 09 ; MOV A, #9\\nE0    ; ENQUEUE A\\nD0    ; DEQUEUE into A\\nFF    ; HALT",
      2: "74 0A ; MOV A, #10\\nE0    ; ENQUEUE A (item 1)\\n74 1E ; MOV A, #30\\nE0    ; ENQUEUE A (item 2)\\n74 05 ; MOV A, #5\\nE0    ; ENQUEUE A (item 3)\\nD0    ; DEQUEUE -> A=10\\nD0    ; DEQUEUE -> A=30\\nD0    ; DEQUEUE -> A=5\\nFF    ; HALT",
      3: "74 AB ; MOV A, #0xAB\\n54 0F ; ANL A, #0x0F (Mask high nibble -> A=0x0B)\\nFF    ; HALT",
      4: "74 00 ; MOV A, #0\\n04    ; INC A -> A=1\\n80 02 ; SJMP +2 (skips next MOV)\\n74 99 ; MOV A, #0x99 (SKIPPED)\\n04    ; INC A -> A=2\\nFF    ; HALT"
    };

    function setPreset(n) {
      if (PRESETS[n]) {
        $('code').value = PRESETS[n];
        loadCode();
      }
    }

    function loadCode() {
      const code = $('code').value.trim();
      if (code) send('LOAD ' + code);
    }

    window.addEventListener('DOMContentLoaded', () => {
      send('STATE');
    });
  </script>
</body>
</html>
""";
}