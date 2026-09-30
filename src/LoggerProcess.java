import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * LoggerProcess — Arnold Noah (25190106)
 *
 * Week 4 requirement: Logging process running as a SEPARATE process.
 * - Listens on port 5001 for connections from CoreProcess.
 * - Parses messages in the format:  LOG|TYPE|message or LOG|LEVEL|CATEGORY|details
 * - Writes every log entry to:
 *      (a) the console (stdout) with a timestamp
 *      (b) log file called simulator.log in the working directory
 * - Handles graceful shutdown when it receives a LOG|SHUTDOWN|... message.
 *
 * IPC Mechanism: TCP Sockets (POSIX-compatible, cross-platform).
 * Reason for TCP sockets: Simple, reliable, ordered byte stream.
 *   Fits the one-directional Core→Logger communication perfectly.
 *   No shared memory issues, works across processes without a shared heap.
 */
public class LoggerProcess {

    // ── Configuration ──────────────────────────────────────────────────────
    private static final int PORT       = 5001;
    private static final String LOGFILE = "simulator.log";

    // ── Timestamp formatter ────────────────────────────────────────────────
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("  STC89C52 MULTI-PROCESS SIMULATOR — LOGGING PROCESS (PORT " + PORT + ")");
        System.out.println("  Owner: Arnold Noah (25190106) | Module: Logging & Audit");
        System.out.println("  Log file: " + new File(LOGFILE).getAbsolutePath());
        System.out.println("=====================================================================");

        try (ServerSocket server = new ServerSocket(PORT)) {
            boolean running = true;

            while (running) {
                System.out.println("\n[Logger] Listening on port " + PORT + " for CoreProcess...");
                try (Socket coreSocket = server.accept();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(coreSocket.getInputStream()));
                     PrintWriter fileWriter = new PrintWriter(
                             new BufferedWriter(new FileWriter(LOGFILE, true)))) {

                    System.out.println("[Logger] CoreProcess connected from " + coreSocket.getRemoteSocketAddress());

                    // Write session header
                    String header = "=== Session started " + timestamp() + " ===";
                    fileWriter.println(header);
                    System.out.println("[Logger] " + header);
                    fileWriter.flush();

                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        handleMessage(line, fileWriter);

                        // Shutdown request from Core
                        if (line.startsWith("LOG|SHUTDOWN")) {
                            running = false;
                            break;
                        }
                    }

                    String footer = "=== Session ended   " + timestamp() + " ===";
                    fileWriter.println(footer);
                    System.out.println("[Logger] " + footer);
                    fileWriter.flush();

                } catch (IOException e) {
                    System.err.println("[Logger] Connection notice: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("[Logger] Server socket error on port " + PORT + ": " + e.getMessage());
        }

        System.out.println("[Logger] LoggerProcess terminated.");
    }

    // ── Parse and log one message ──────────────────────────────────────────
    private static void handleMessage(String raw, PrintWriter fileWriter) {
        String ts = timestamp();
        String formatted;

        String[] parts = raw.split("\\|", 4);
        if (parts.length >= 4 && parts[0].equals("LOG")) {
            // 4-part: LOG|LEVEL|CATEGORY|details
            String level = padRight(parts[1], 5);
            String cat   = padRight(parts[2], 10);
            formatted = String.format("[%s] [%s] [%s] %s", ts, level, cat, parts[3]);
        } else if (parts.length >= 3 && parts[0].equals("LOG")) {
            // 3-part: LOG|TYPE|detail
            String type   = padRight(parts[1].toUpperCase(), 8);
            String detail = parts[2];
            formatted = String.format("[%s] [%s] %s", ts, type, detail);
        } else {
            // Unexpected format — log as raw
            formatted = String.format("[%s] [RAW     ] %s", ts, raw);
        }

        // Console output
        System.out.println("[Logger] " + formatted);

        // File output
        fileWriter.println(formatted);
        fileWriter.flush();
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static String timestamp() {
        return DATE_FMT.format(new Date());
    }
}
