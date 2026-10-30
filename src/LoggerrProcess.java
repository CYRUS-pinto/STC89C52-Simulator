package logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class LoggerProcess {

    private static final int PORT = 5001;

    private static final String LOG_FILE = "simulator.log";

    private static boolean running = true;

    private static LogHandler handler =
            new LogHandler(LOG_FILE);

    public static void main(String[] args) {

        int port = PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number.");
                return;
            }
        }

        System.out.println("==================================");
        System.out.println("     STC89C52 LOGGER PROCESS");
        System.out.println("==================================");
        System.out.println("Starting Logger Process...");
        System.out.println("Port: " + port);

        startServer(port);
    }

    private static void startServer(int port) {

        try (ServerSocket server = new ServerSocket(port)) {

            handler.writeLog(
                    "INFO", "LOGGER",
                    "Logger process started.");

            while (running) {

                System.out.println(
                        "\nWaiting for CoreProcess connection...");

                try (Socket socket = server.accept();
                     BufferedReader reader =
                             new BufferedReader(
                                     new InputStreamReader(
                                             socket.getInputStream(),
                                             StandardCharsets.UTF_8))) {

                    System.out.println(
                            "CoreProcess connected: "
                                    + socket.getInetAddress());

                    handleConnection(reader);

                } catch (IOException e) {

                    System.err.println(
                            "Connection error: " + e.getMessage());
                }
            }

            handler.writeLog(
                    "INFO", "LOGGER",
                    "Logger process stopped.");

        } catch (IOException e) {

            System.err.println(
                    "Unable to start Logger Server: "
                            + e.getMessage());
        }
    }

    private static void handleConnection(
            BufferedReader reader) throws IOException {

        String message;

        while (running && (message = reader.readLine()) != null) {

            if (message.trim().isEmpty()) {
                continue;
            }

            if (isShutdownMessage(message)) {

                handler.writeLog(
                        "INFO", "LOGGER",
                        "Shutdown request received.");

                running = false;
                break;
            }

            processLogMessage(message);
        }

        System.out.println("CoreProcess disconnected.");
    }

    private static void processLogMessage(String message) {

        String[] parts = message.split("\\|", 4);

        if (parts.length != 4 || !parts[0].equals("LOG")) {

            handler.writeLog(
                    "WARNING", "INVALID",
                    "Invalid log message: " + message);

            return;
        }

        String level = parts[1].trim().toUpperCase();
        String category = parts[2].trim();
        String details = parts[3].trim();

        if (category.isEmpty() || details.isEmpty()) {

            handler.writeLog(
                    "WARNING", "INVALID",
                    "Incomplete log message.");

            return;
        }

        if (!level.equals("INFO")
                && !level.equals("ERROR")
                && !level.equals("WARNING")
                && !level.equals("DEBUG")) {

            handler.writeLog(
                    "WARNING", "INVALID",
                    "Unknown log level: " + level);

            return;
        }

        handler.writeLog(level, category, details);
    }

    private static boolean isShutdownMessage(String message) {

        String[] parts = message.split("\\|", 4);

        return parts.length >= 3
                && parts[0].equals("LOG")
                && parts[1].equals("SHUTDOWN");
    }
}
