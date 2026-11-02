
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class LoggerProcess {

    private static final int PORT = 5001;
    private static boolean running = true;

    private static LogHandler handler =
            new LogHandler("simulator.log");

    public static void main(String[] args) {

        int port = PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number.");
                return;
            }
        }

        System.out.println("Logger Process started on port " + port);

        startServer(port);
    }

    private static void startServer(int port) {

        try (ServerSocket server = new ServerSocket(port)) {

            handler.writeLog("INFO", "LOGGER",
                    "Logger process started");

            while (running) {

                System.out.println("Waiting for CoreProcess...");

                try (Socket socket = server.accept();
                     BufferedReader input = new BufferedReader(
                             new InputStreamReader(socket.getInputStream()))) {

                    System.out.println("CoreProcess connected.");

                    readMessages(input);

                } catch (IOException e) {
                    System.out.println("Connection error: " + e.getMessage());
                }
            }

            handler.writeLog("INFO", "LOGGER",
                    "Logger process stopped");

        } catch (IOException e) {
            System.out.println("Could not start server: " + e.getMessage());
        }
    }

    private static void readMessages(BufferedReader input)
            throws IOException {

        String message;

        while (running && (message = input.readLine()) != null) {

            if (message.trim().isEmpty()) {
                continue;
            }

            if (message.equals("LOG|SHUTDOWN")
                    || message.startsWith("LOG|SHUTDOWN|")) {

                handler.writeLog("INFO", "LOGGER",
                        "Shutdown request received");

                running = false;
                break;
            }

            processMessage(message);
        }

        System.out.println("CoreProcess disconnected.");
    }

    private static void processMessage(String message) {

        String[] parts = message.split("\\|", 4);

        if (parts.length != 4 || !parts[0].equals("LOG")) {
            handler.writeLog("WARNING", "INVALID",
                    "Invalid log message: " + message);
            return;
        }

        String level = parts[1].trim().toUpperCase();
        String category = parts[2].trim();
        String details = parts[3].trim();

        if (category.isEmpty() || details.isEmpty()) {
            handler.writeLog("WARNING", "INVALID",
                    "Incomplete log message");
            return;
        }

        if (!level.equals("INFO") &&
            !level.equals("ERROR") &&
            !level.equals("WARNING") &&
            !level.equals("DEBUG")) {

            handler.writeLog("WARNING", "INVALID",
                    "Unknown log level: " + level);
            return;
        }

        handler.writeLog(level, category, details);
    }
}
