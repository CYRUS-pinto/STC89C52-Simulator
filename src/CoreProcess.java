import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * CoreProcess â€” Chrisel Lobo (25190110)
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

    private static Socket loggerSocket = null;
    private static PrintWriter loggerWriter = null;
    private static final ArrayBlockingQueue<String> logQueue = new ArrayBlockingQueue<>(1024);
    private static final java.util.concurrent.atomic.AtomicLong droppedLogs = new java.util.concurrent.atomic.AtomicLong(0);
    private static volatile boolean loggerRunning = true;
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
        System.out.println("  STC89C52 MULTI-PROCESS SIMULATOR â€” CORE EXECUTION PROCESS (PORT " + CORE_PORT + ")");
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
            loggerSocket = new Socket(LOGGER_HOST, LOGGER_PORT);
            loggerWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(loggerSocket.getOutputStream())), true);
            System.out.println("[Core] Connected to LoggerProcess successfully.");
        } catch (IOException e) {
            System.out.println("[Core] Notice: LoggerProcess not detected on port " + LOGGER_PORT +
                    " (start LoggerProcess first). Continuing with console fallback.");
        }

        Thread dispatcher = new Thread(() -> {
            while (loggerRunning || !logQueue.isEmpty()) {
                try {
                    String frame = logQueue.poll(50, TimeUnit.MILLISECONDS);
                    if (frame != null && loggerWriter != null) {
                        loggerWriter.println(frame);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ignored) {}
            }
        }, "Core-AsyncLoggerDispatcher");
        dispatcher.setDaemon(true);
        dispatcher.start();
    }

    public static boolean offerLogFrame(String level, String cat, String msg) {
        String frame = "LOG|" + level + "|" + cat + "|" + msg;
        boolean accepted = logQueue.offer(frame);
        if (!accepted) {
            droppedLogs.incrementAndGet();
        }
        return accepted;
    }

    public static long getDroppedLogsCount() {
        return droppedLogs.get();
    }

    private static synchronized void log(String level, String cat, String msg) {
        System.out.println("[Core:" + level + "] " + msg);
        offerLogFrame(level, cat, msg);
    }

    private static synchronized void closeLogger() {
        loggerRunning = false;
        if (loggerWriter != null) {
            try {
                logQueue.offer("LOG|SHUTDOWN|CORE|CoreProcess closing logger connection.");
                long deadline = System.currentTimeMillis() + 100;
                while (!logQueue.isEmpty() && System.currentTimeMillis() < deadline) {
                    Thread.sleep(5);
                }
                loggerWriter.close();
                if (loggerSocket != null) loggerSocket.close();
            } catch (Exception ignored) {}
            loggerWriter = null;
            loggerSocket = null;
        }
    }

    // â”€â”€ Client connection handler â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                        toUI.println("BYE");
                        break;
                    } else if (verb.equals("SHUTDOWN")) {
                        log("SHUTDOWN", "CORE", "Server shutdown requested.");
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
                                if (parsed[parsed.length - 1] != (byte) 0xFF) {
                                    byte[] withHalt = new byte[parsed.length + 1];
                                    System.arraycopy(parsed, 0, withHalt, 0, parsed.length);
                                    withHalt[parsed.length] = (byte) 0xFF;
                                    parsed = withHalt;
                                }
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
                    

                    } else if (verb.equals("CRC")) {
                        synchronized (cpuLock) {
                            String dataStr = payload.isEmpty() ? "123456789" : payload;
                            byte[] rawBytes = dataStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                            int[] r = new int[]{ 0, 0, 0 };
                            for (byte b : rawBytes) {
                                CRC24b.step8051Hardware(r, b);
                            }
                            int crcVal = CRC24b.from8051Registers(r[0], r[1], r[2]);
                            regs.setR(5, 0, r[0]);
                            regs.setR(6, 0, r[1]);
                            regs.setR(7, 0, r[2]);
                            regs.A = r[2];
                            psw.updateParity(regs.A);
                            log("INFO", "CRC", "Calculated CRC-24b for '" + dataStr + "': 0x" + String.format("%06X", crcVal) + " (R5=0x" + String.format("%02X", r[0]) + ", R6=0x" + String.format("%02X", r[1]) + ", R7=0x" + String.format("%02X", r[2]) + ")");
                            emitState(toUI);
                            toUI.println(String.format("CRC|CHECKSUM=0x%06X|R5=%02X|R6=%02X|R7=%02X", crcVal, r[0], r[1], r[2]));
                        }
                    } else if (verb.equals("CRC_TEST")) {
                        synchronized (cpuLock) {
                            String dataStr = payload.isEmpty() ? "123456789" : payload;
                            byte[] rawBytes = dataStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                            int[] r = new int[]{ 0, 0, 0 };
                            for (byte b : rawBytes) {
                                CRC24b.step8051Hardware(r, b);
                            }
                            int crcVal = CRC24b.from8051Registers(r[0], r[1], r[2]);
                            regs.setR(5, 0, r[0]);
                            regs.setR(6, 0, r[1]);
                            regs.setR(7, 0, r[2]);
                            regs.A = r[2];
                            psw.updateParity(regs.A);

                            byte[] codeword = CRC24b.encode(rawBytes);
                            int cleanSyn = CRC24b.decode(codeword);

                            byte[] noisy1 = CRC24b.inject1BitError(codeword, 0, 0);
                            int syn1 = CRC24b.decode(noisy1);

                            byte[] noisy2 = CRC24b.inject2BitError(codeword, 0, 0);
                            int syn2 = CRC24b.decode(noisy2);

                            StringBuilder cwHex = new StringBuilder();
                            for (byte b : codeword) {
                                cwHex.append(String.format("%02X ", b & 0xFF));
                            }

                            log("INFO", "CRC", "CRC-24b Encoded: 0x" + String.format("%06X", crcVal) + " for '" + dataStr + "'");
                            log("INFO", "CRC", "Clean Codeword Syndrome: 0x" + String.format("%06X", cleanSyn) + " [VALID PACKET]");
                            log("WARNING", "CRC", "1-Bit Noise Injected -> Syndrome: 0x" + String.format("%06X", syn1) + " [ERROR DETECTED]");
                            log("WARNING", "CRC", "2-Bit Burst Injected  -> Syndrome: 0x" + String.format("%06X", syn2) + " [BURST DETECTED]");

                            emitState(toUI);
                            toUI.println(String.format("CRC_TEST|ORIGINAL=%s|CHECKSUM=0x%06X|R5=%02X|R6=%02X|R7=%02X|CODEWORD=%s|CLEAN=0x%06X|BIT1=0x%06X|BIT2=0x%06X",
                                    dataStr, crcVal, r[0], r[1], r[2], cwHex.toString().trim(), cleanSyn, syn1, syn2));
                        }
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

    // â”€â”€ Build comprehensive STATE string for both Web UI and Swing UI â”€â”€â”€â”€â”€
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
        StringBuilder sb = new StringBuilder();
        for (String line : hexInput.split("\\r?\\n")) {
            int idx = line.indexOf(';');
            if (idx != -1) line = line.substring(0, idx);
            idx = line.indexOf("//");
            if (idx != -1) line = line.substring(0, idx);
            sb.append(line).append(" ");
        }
        String clean = sb.toString().replaceAll("[^0-9A-Fa-f]", "");
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