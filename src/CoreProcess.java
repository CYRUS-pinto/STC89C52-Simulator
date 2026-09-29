import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * CoreProcess — Chrisel Lobo (25190110)
 *
 * Week 4 requirement: Core Execution Engine running as an INDEPENDENT process.
 * - Manages CPU execution, Memory, Stack, and Circular Queue.
 * - Listens on TCP port 5000 for UI commands (compatible with both UIProcess web/CLI and SimulatorUI Swing).
 * - Connects as an IPC client to LoggerProcess on TCP port 5001.
 * - Handles concurrent connections from UI clients using multi-threading.
 * - Supports commands: STEP, RUN, STOP, RESET, LOAD [hex], STATE, QUIT.
 *
 * IPC Mechanism: TCP Sockets (POSIX-compatible, cross-platform).
 */
public class CoreProcess {
    private static final int CORE_PORT   = 5000;
    private static final String LOGGER_HOST = "127.0.0.1";
    private static final int LOGGER_PORT = 5001;

    private static Registers regs;
    private static Memory mem;
    private static DataStructures ds;
    private static PSW psw;
    private static CPU cpu;
    private static byte[] currentProgram;

    private static PrintWriter loggerWriter = null;
    private static final Object cpuLock = new Object();
    private static volatile boolean serverRunning = true;
    private static ServerSocket serverSocket;

    // Standard verification bytecode test program (Week 2 & 4 demo)
    private static final byte[] DEFAULT_PROGRAM = new byte[] {
        0x74, (byte) 0xFE, // MOV A, #254
        0x24, 0x03,        // ADD A, #3  (Causes Carry: 257 -> 01, CY=1)
        0x04,              // INC A       (A = 02)
        0x54, 0x0F,        // ANL A, #0x0F(A = 02)
        (byte) 0xE0,       // ENQUEUE A   (Q = [02])
        0x74, 0x09,        // MOV A, #9   (A = 09)
        (byte) 0xE0,       // ENQUEUE A   (Q = [02, 09])
        (byte) 0xD0,       // DEQUEUE A   (A = 02, FIFO order)
        (byte) 0xFF        // HALT
    };

    private static void initialize(byte[] programToLoad) {
        synchronized (cpuLock) {
            regs = new Registers();
            mem = new Memory();
            ds = new DataStructures(256, 16);
            psw = new PSW();
            cpu = new CPU(regs, mem, ds, psw);

            currentProgram = (programToLoad != null && programToLoad.length > 0)
                    ? programToLoad.clone()
                    : DEFAULT_PROGRAM.clone();

            mem.loadProgram(currentProgram);
        }
    }

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("  STC89C52 MULTI-PROCESS SIMULATOR — CORE EXECUTION PROCESS (PORT " + CORE_PORT + ")");
        System.out.println("  Owner: Chrisel Lobo (25190110) | Module: CPU, Memory, Stack, Queue");
        System.out.println("=====================================================================");

        initialize(DEFAULT_PROGRAM);

        // Connect to LoggerProcess on port 5001
        connectLogger();

        log("INFO", "CORE", "CoreProcess initialized with default demo bytecode (" + DEFAULT_PROGRAM.length + " bytes).");

        // Start ServerSocket on port 5000
        try {
            serverSocket = new ServerSocket(CORE_PORT);
            System.out.println("[Core] Server listening for UIProcess/SimulatorUI on port " + CORE_PORT + "...");

            ExecutorService clientPool = Executors.newCachedThreadPool();

            while (serverRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientPool.submit(new ClientHandler(clientSocket));
                } catch (SocketException se) {
                    if (!serverRunning) break;
                }
            }

            clientPool.shutdown();
        } catch (IOException e) {
            System.err.println("[Core] Server socket error: " + e.getMessage());
        } finally {
            closeLogger();
        }

        System.out.println("[Core] CoreProcess terminated.");
    }

    private static void connectLogger() {
        try {
            System.out.println("[Core] Connecting to LoggerProcess on " + LOGGER_HOST + ":" + LOGGER_PORT + "...");
            Socket socket = new Socket(LOGGER_HOST, LOGGER_PORT);
            loggerWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            System.out.println("[Core] Connected to LoggerProcess successfully.");
        } catch (IOException e) {
            System.out.println("[Core] Notice: LoggerProcess not detected on port " + LOGGER_PORT +
                    " (start LoggerProcess first). Continuing with console fallback.");
        }
    }

    private static synchronized void log(String level, String cat, String msg) {
        System.out.println("[Core:" + level + "] " + msg);
        if (loggerWriter != null) {
            loggerWriter.println("LOG|" + level + "|" + cat + "|" + msg);
        }
    }

    private static synchronized void closeLogger() {
        if (loggerWriter != null) {
            try {
                loggerWriter.println("LOG|SHUTDOWN|CORE|CoreProcess closing logger connection.");
                loggerWriter.close();
            } catch (Exception ignored) {}
            loggerWriter = null;
        }
    }

    // ── Client connection handler ──────────────────────────────────────────
    static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader fromUI = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter toUI = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true)) {

                String raw;
                while ((raw = fromUI.readLine()) != null) {
                    raw = raw.trim();
                    if (raw.isEmpty()) continue;

                    String verb = raw.split("\\s+", 2)[0].toUpperCase();
                    String payload = raw.length() > verb.length() ? raw.substring(verb.length()).trim() : "";

                    if (verb.equals("QUIT") || verb.equals("EXIT")) {
                        log("SHUTDOWN", "CORE", "UI requested termination.");
                        toUI.println("BYE");
                        serverRunning = false;
                        try { serverSocket.close(); } catch (Exception ignored) {}
                        break;
                    } else if (verb.equals("RESET")) {
                        initialize(currentProgram);
                        emitState(toUI);
                        toUI.println("RESET_DONE");
                        log("RESET", "CORE", "CPU and data structures reset.");
                    } else if (verb.equals("LOAD")) {
                        if (!payload.isEmpty()) {
                            byte[] parsed = parseHex(payload);
                            if (parsed.length > 0) {
                                initialize(parsed);
                                log("LOAD", "CORE", "Loaded " + parsed.length + " bytes of custom bytecode.");
                            } else {
                                initialize(DEFAULT_PROGRAM);
                                log("LOAD", "CORE", "Invalid bytecode; reloaded default program.");
                            }
                        } else {
                            initialize(DEFAULT_PROGRAM);
                            log("LOAD", "CORE", "Reloaded default demo program.");
                        }
                        emitState(toUI);
                        toUI.println("RESET_DONE");
                    } else if (verb.equals("STEP")) {
                        synchronized (cpuLock) {
                            if (!cpu.isHalt) {
                                cpu.step();
                            }
                            emitState(toUI);
                            if (cpu.isHalt) {
                                toUI.println("HALTED");
                            }
                        }
                    } else if (verb.equals("RUN")) {
                        synchronized (cpuLock) {
                            while (!cpu.isHalt) {
                                cpu.step();
                                emitState(toUI);
                            }
                            toUI.println("HALTED");
                        }
                    } else if (verb.equals("STOP")) {
                        synchronized (cpuLock) {
                            cpu.isHalt = true;
                            emitState(toUI);
                            toUI.println("HALTED");
                        }
                        log("STOP", "CORE", "Execution halted by UI.");
                    } else if (verb.equals("STATE") || verb.equals("GET_STATE")) {
                        synchronized (cpuLock) {
                            emitState(toUI);
                        }
                    } else {
                        synchronized (cpuLock) {
                            emitState(toUI);
                        }
                    }
                }

            } catch (IOException e) {
                // Client closed socket (normal for request-response clients like UIProcess)
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    // ── Build comprehensive STATE string for both Web UI and Swing UI ─────
    private static void emitState(PrintWriter toUI) {
        StringBuilder sb = new StringBuilder();
        sb.append("STATE");
        sb.append(String.format("|PC=%04X", cpu.pc));
        sb.append(String.format("|A=%02X", regs.A));
        sb.append(String.format("|B=%02X", regs.B));
        sb.append(String.format("|CY=%b", psw.isCy()));
        sb.append(String.format("|AC=%b", psw.isAc()));
        sb.append(String.format("|OV=%b", psw.isOv()));
        sb.append(String.format("|P=%b", psw.isP()));
        sb.append(String.format("|SP=%02X", ds.sp));

        int activeBank = psw.getRs0() | (psw.getRs1() << 1);
        for (int i = 0; i < 8; i++) {
            sb.append(String.format("|R%d=%02X", i, regs.getR(i, activeBank)));
        }

        String instrName = cpu.getCurrInstrcName();
        sb.append("|INSTR=").append(instrName != null ? instrName : "READY");
        sb.append(String.format("|OPCODE=%02X", cpu.getCurrOpcode()));
        sb.append("|FETCH=").append(cpu.fetchLog != null && !cpu.fetchLog.isEmpty() ? cpu.fetchLog : "PC=0x" + String.format("%04X", cpu.pc));
        sb.append("|DECODE=").append(cpu.decodeLog != null && !cpu.decodeLog.isEmpty() ? cpu.decodeLog : (instrName != null ? instrName : "None"));
        sb.append("|EXECUTE=").append(cpu.getStatechangelog() != null && !cpu.getStatechangelog().isEmpty() ? cpu.getStatechangelog() : "Idle");

        sb.append(String.format("|Q_SIZE=%d", ds.qSize));
        sb.append(String.format("|Q_CAP=%d", ds.qCapacity));
        sb.append(String.format("|Q_FRONT=%d", ds.qFront));
        sb.append(String.format("|Q_REAR=%d", ds.qRear));

        StringBuilder qElems = new StringBuilder();
        for (int i = 0; i < ds.qSize; i++) {
            int idx = (ds.qFront + i) % ds.qCapacity;
            if (i > 0) qElems.append(",");
            qElems.append(String.format("0x%02X", ds.queue[idx]));
        }
        sb.append("|Q_ELEMS=").append(qElems.toString());

        StringBuilder ramHex = new StringBuilder(512);
        for (int i = 0; i < 256; i++) {
            ramHex.append(String.format("%02X", mem.readRAM(i)));
        }
        sb.append("|RAM=").append(ramHex.toString());

        String stateStr = sb.toString();
        if (toUI != null) {
            toUI.println(stateStr);
        }
        log("EXEC", "CPU", "PC=" + String.format("%04X", cpu.pc) + "|A=" + String.format("%02X", regs.A) + "|CY=" + psw.isCy());
    }

    private static byte[] parseHex(String hexInput) {
        String clean = hexInput.replaceAll("[^0-9A-Fa-f]", "");
        if (clean.length() % 2 != 0) {
            clean = clean.substring(0, clean.length() - 1);
        }
        byte[] bytes = new byte[clean.length() / 2];
        for (int i = 0; i < clean.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(clean.substring(i, i + 2), 16);
        }
        return bytes;
    }
}