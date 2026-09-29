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
    private static final Set<String> WEB_COMMANDS = Set.of("STEP", "RUN", "STOP", "RESET", "LOAD");

    // Last real STATE line received from Core (null until the first one arrives)
    private static volatile String lastState = null;

    public static void main(String[] args) throws IOException {
        printConsoleBanner();

        HttpServer webServer = HttpServer.create(new InetSocketAddress(WEB_PORT), 0);
        webServer.createContext("/", new WebHandler());
        webServer.createContext("/api/cmd", new ApiHandler());
        webServer.start();

        System.out.println("[JAVA WEB SERVER] Running at: http://localhost:" + WEB_PORT);
        System.out.println("Open http://localhost:" + WEB_PORT + " in Chrome/Edge for the 6-panel dashboard.\n");

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
  <title>STC89C52 Microcontroller Simulator - 6 Panel Web UI</title>
  <script src='https://cdn.tailwindcss.com'></script>
  <style>@import url('https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;500;600;700&display=swap'); body { font-family: 'Fira Code', monospace; }</style>
</head>
<body class='bg-slate-950 text-slate-100 p-4 md:p-6 min-h-screen text-xs'>
  <div class='max-w-7xl mx-auto space-y-4'>
    <div class='bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-wrap justify-between items-center gap-4 shadow-xl'>
      <div>
        <h1 class='text-lg font-bold text-emerald-400 flex items-center gap-2'>
          <span id='dot' class='w-3 h-3 rounded-full bg-slate-500'></span>
          STC89C52 MICROCONTROLLER SIMULATOR (6-PANEL DASHBOARD)
        </h1>
        <p class='text-[11px] text-slate-400'>Owner: Bondas (UI Module) | Core Socket: localhost:5000 | Web HTTP: localhost:8080</p>
      </div>
      <div class='flex flex-wrap items-center gap-2'>
        <button onclick='loadCode()' class='px-3 py-1.5 bg-slate-800 hover:bg-slate-700 font-bold rounded text-slate-200 border border-slate-700'>[ Load Hex / Code ]</button>
        <button onclick='send("STEP")' class='px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-500 font-bold rounded text-white'>[ Step (F->D->E) ]</button>
        <button onclick='send("RUN")' class='px-3.5 py-1.5 bg-sky-600 hover:bg-sky-500 font-bold rounded text-white'>[ Run Continuous ]</button>
        <button onclick='send("STOP")' class='px-3.5 py-1.5 bg-rose-600 hover:bg-rose-500 font-bold rounded text-white'>[ Stop / Halt ]</button>
        <button onclick='send("RESET")' class='px-3.5 py-1.5 bg-amber-600 hover:bg-amber-500 font-bold rounded text-white'>[ Reset ]</button>
      </div>
    </div>

    <div class='grid grid-cols-1 md:grid-cols-3 gap-4'>

      <!-- 1. Assembly / Opcode Editor -->
      <div class='bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-2'>
        <h2 class='font-bold text-sky-400 border-b border-slate-800 pb-2 uppercase tracking-wide'>1. Assembly / Opcode Editor</h2>
        <textarea id='code' placeholder='Paste hex bytes here, e.g. 74 FE 24 03 ...' class='w-full bg-slate-950 p-3 rounded-lg border border-slate-800/80 text-slate-300 min-h-[190px] outline-none'>74 FE 24 03 04 54 0F E0 74 09 E0 D0 FF</textarea>
      </div>

      <!-- 2. Execution Trace -->
      <div class='bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-2'>
        <h2 class='font-bold text-emerald-400 border-b border-slate-800 pb-2 uppercase tracking-wide'>2. Execution Trace (Week 2)</h2>
        <div class='bg-slate-950 p-3 rounded-lg border border-slate-800/80 space-y-1.5 min-h-[190px]'>
          <div class='text-slate-400'>Current PC: <span class='text-sky-300 font-bold' id='trace-pc'>--</span></div>
          <div class='text-slate-400'>Instruction: <span class='text-emerald-300 font-bold' id='trace-instr'>--</span></div>
          <div class='text-emerald-400' id='trace-fetch'></div>
          <div class='text-emerald-400' id='trace-decode'></div>
          <div class='text-emerald-400' id='trace-exec'></div>
          <div class='border-t border-slate-800 pt-1 text-slate-400'>
            State Transition:<br>
            &nbsp;&nbsp;ACC: <span id='trace-acc'>--</span><br>
            &nbsp;&nbsp;CY : <span id='trace-cy'>--</span>
          </div>
        </div>
      </div>

      <!-- 3. Registers & PSW -->
      <div class='bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-2'>
        <h2 class='font-bold text-amber-400 border-b border-slate-800 pb-2 uppercase tracking-wide'>3. Registers &amp; PSW (Weeks 1 &amp; 2)</h2>
        <div class='bg-slate-950 p-3 rounded-lg border border-slate-800/80 space-y-1.5 min-h-[190px]'>
          <div class='grid grid-cols-2 gap-2 text-slate-300'>
            <div>PC : <span class='text-sky-300 font-bold' id='reg-pc'>--</span></div>
            <div>SP : <span class='text-sky-300 font-bold' id='reg-sp'>--</span></div>
            <div>ACC: <span class='text-emerald-400 font-bold' id='reg-acc'>--</span></div>
            <div>B  : <span class='text-emerald-400 font-bold' id='reg-b'>--</span></div>
            <div>R0 : <span id='reg-r0'>--</span></div><div>R1 : <span id='reg-r1'>--</span></div>
            <div>R2 : <span id='reg-r2'>--</span></div><div>R3 : <span id='reg-r3'>--</span></div>
            <div>R4 : <span id='reg-r4'>--</span></div><div>R5 : <span id='reg-r5'>--</span></div>
            <div>R6 : <span id='reg-r6'>--</span></div><div>R7 : <span id='reg-r7'>--</span></div>
          </div>
          <div class='border-t border-slate-800 pt-2 space-y-1'>
            <div class='text-slate-400'>FLAGS (PSW):</div>
            <div class='flex gap-2' id='psw-flags'></div>
          </div>
        </div>
      </div>

      <!-- 4. Hardware Stack -->
      <div class='bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-2'>
        <h2 class='font-bold text-sky-400 border-b border-slate-800 pb-2 uppercase tracking-wide'>4. Hardware Stack (SP: <span id='stack-sp'>--</span>)</h2>
        <div class='bg-slate-950 p-3 rounded-lg border border-slate-800/80 space-y-1.5 min-h-[160px] text-slate-300'>
          <div>Top of Stack: <span class='text-emerald-400 font-bold' id='stack-top'>--</span></div>
          <div>Stack Depth : <span id='stack-depth'>--</span></div>
        </div>
      </div>

      <!-- 5. Circular FIFO Queue -->
      <div class='bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-2'>
        <h2 class='font-bold text-emerald-400 border-b border-slate-800 pb-2 uppercase tracking-wide'>5. Circular FIFO Queue (Week 3)</h2>
        <div class='bg-slate-950 p-3 rounded-lg border border-slate-800/80 space-y-1.5 min-h-[160px] text-slate-300'>
          <div>Size: <span class='text-emerald-400 font-bold' id='q-size'>--</span></div>
          <div>Capacity: <span id='q-cap'>--</span></div>
          <div>Front: <span id='q-front'>--</span></div>
          <div>Rear : <span id='q-rear'>--</span></div>
          <div>Elements: <span class='text-emerald-300 font-bold' id='q-elems'>--</span></div>
        </div>
      </div>

      <!-- 6. Memory RAM Inspector -->
      <div class='bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-2'>
        <h2 class='font-bold text-amber-400 border-b border-slate-800 pb-2 uppercase tracking-wide'>6. Memory RAM Inspector (256 Bytes)</h2>
        <pre id='ram' class='bg-slate-950 p-3 rounded-lg border border-slate-800/80 min-h-[160px] text-[11px] text-slate-400 overflow-x-auto'>--</pre>
      </div>
    </div>

    <div class='bg-slate-900 border border-slate-800 rounded-xl p-3 flex flex-wrap justify-between items-center text-slate-400 text-[11px]'>
      <div>STATUS BAR: <span id='status' class='font-bold text-slate-400'>Waiting for first command</span></div>
      <div id='status-last-event'>Last Event: --</div>
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
      const cls = v === undefined ? 'bg-slate-800 text-slate-600'
                : on ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 font-bold'
                     : 'bg-slate-800 text-slate-500';
      return "<span class='px-1.5 py-0.5 rounded " + cls + "'>[" + name + ": " + (v === undefined ? '-' : (on ? 1 : 0)) + "]</span>";
    }

    function render(s) {
      set('trace-pc', hex(s.PC)); set('reg-pc', hex(s.PC));
      set('reg-sp', hex(s.SP)); set('stack-sp', hex(s.SP));
      const accTxt = s.A === undefined ? undefined : '0x' + s.A + ' (' + parseInt(s.A, 16) + ')';
      set('reg-acc', accTxt); set('reg-b', hex(s.B));
      for (let i = 0; i < 8; i++) set('reg-r' + i, hex(s['R' + i]));
      set('trace-instr', s.INSTR);
      $('trace-fetch').innerText  = s.FETCH   ? 'Phase 1: FETCH   [ ' + s.FETCH + ' ]'   : '';
      $('trace-decode').innerText = s.DECODE  ? 'Phase 2: DECODE  [ ' + s.DECODE + ' ]'  : '';
      $('trace-exec').innerText   = s.EXECUTE ? 'Phase 3: EXECUTE [ ' + s.EXECUTE + ' ]' : '';

      // state transition = previous real state -> current real state
      set('trace-acc', s.A === undefined ? undefined : (prev && prev.A !== undefined ? '0x' + prev.A + ' -> ' : '') + '0x' + s.A);
      set('trace-cy',  s.CY === undefined ? undefined : (prev && prev.CY !== undefined ? prev.CY + ' -> ' : '') + s.CY);

      $('psw-flags').innerHTML = flagChip('CY', s.CY) + flagChip('AC', s.AC) + flagChip('OV', s.OV) + flagChip('P', s.P);

      // stack depth is derived from SP (8051 reset value is 0x07)
      if (s.SP !== undefined) set('stack-depth', Math.max(0, parseInt(s.SP, 16) - 7) + ' items pushed');
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
          $('dot').className = 'w-3 h-3 rounded-full bg-rose-500';
          $('status').className = 'font-bold text-rose-400';
          $('status').innerText = 'Core offline - start CoreProcess on port 5000';
          return;
        }
        $('dot').className = 'w-3 h-3 rounded-full bg-emerald-500 animate-pulse';
        $('status').className = 'font-bold text-emerald-400';
        $('status').innerText = text.includes('HALTED') ? 'Halted' : 'Connected (port 5000)';

        const log = text.split('\\n').filter(l => l.startsWith('LOG|')).pop();
        if (log) set('status-last-event', 'Last Event: ' + log);

        const s = parseState(text);
        if (s) { render(s); prev = s; }
        if (cmd === 'RESET') prev = null;
      } catch (e) {
        $('status').className = 'font-bold text-rose-400';
        $('status').innerText = 'UI server unreachable';
      }
    }

    function loadCode() {
      const code = $('code').value.trim();
      if (code) send('LOAD ' + code);
    }
  </script>
</body>
</html>
""";
}