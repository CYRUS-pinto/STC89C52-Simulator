import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * LoggerProcess — Arnold Noah (25190106)
 *
 * Week 4 requirement: Logging process running as a SEPARATE process.
 * - Listens on port 5001 for a connection from CoreProcess.
 * - Parses messages in the format:  LOG|TYPE|message
 * - Writes every log entry to:
 *      (a) the console (stdout) with a timestamp
 *      (b) a log file called simulator.log in the working directory
 * - Handles graceful shutdown when it receives a LOG|SHUTDOWN|... message.
 *
 * IPC Mechanism: TCP Sockets (POSIX-compatible, cross-platform).
 * Reason for TCP sockets: Simple, reliable, ordered byte stream.
 *   Fits the one-directional Core→Logger communication perfectly.
 *   No shared memory issues, works across processes without a shared heap.
 */
public class LoggerProcess {

    // ── Configuration ──────────────────────────────────────────────────────
    private static final int PORT      = 5001;
    private static final String LOGFILE = "simulator.log";

    // ── Timestamp formatter ────────────────────────────────────────────────
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void main(String[] args) {
        System.out.println("[Logger] Starting on port " + PORT + " ...");
        System.out.println("[Logger] Log file: " + new File(LOGFILE).getAbsolutePath());

        try (ServerSocket server = new ServerSocket(PORT)) {

            // Wait for CoreProcess to connect
            System.out.println("[Logger] Waiting for CoreProcess to connect...");
            Socket coreSocket = server.accept();
            System.out.println("[Logger] CoreProcess connected from " + coreSocket.getInetAddress());

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(coreSocket.getInputStream()));

            // Open log file in APPEND mode so multiple runs accumulate
            try (PrintWriter fileWriter = new PrintWriter(
                         new BufferedWriter(new FileWriter(LOGFILE, true)))) {

                // Write a session header
                String header = "=== Session started " + timestamp() + " ===";
                fileWriter.println(header);
                System.out.println("[Logger] " + header);

                String line;
                while ((line = reader.readLine()) != null) {
                    handleMessage(line, fileWriter);

                    // Shutdown request from Core
                    if (line.startsWith("LOG|SHUTDOWN")) {
                        break;
                    }
                }

                String footer = "=== Session ended   " + timestamp() + " ===";
                fileWriter.println(footer);
                System.out.println("[Logger] " + footer);
            }

            coreSocket.close();

        } catch (IOException e) {
            System.err.println("[Logger] ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[Logger] Shutting down.");
    }

    // ── Parse and log one message ──────────────────────────────────────────
    // Expected format:  LOG|TYPE|detail
    //   TYPE examples:  EXEC, RESET, SHUTDOWN, ERROR, INFO
    private static void handleMessage(String raw, PrintWriter fileWriter) {
        String ts = timestamp();
        String formatted;

        String[] parts = raw.split("\\|", 3); // split into at most 3 parts
        if (parts.length >= 3 && parts[0].equals("LOG")) {
            String type   = parts[1].toUpperCase();
            String detail = parts[2];
            formatted = String.format("[%s] [%s] %s", ts, type, detail);
        } else {
            // Unexpected format — log it as-is
            formatted = String.format("[%s] [RAW] %s", ts, raw);
        }

        // Console output
        System.out.println("[Logger] " + formatted);

        // File output
        fileWriter.println(formatted);
        fileWriter.flush();   // flush after every line so log is always up-to-date
    }

    // ── Timestamp helper ───────────────────────────────────────────────────
    private static String timestamp() {
        return DATE_FMT.format(new Date());
    }
}
