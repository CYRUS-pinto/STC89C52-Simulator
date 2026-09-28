package src;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggerProcess {
    private static final int PORT = 5001;
    private static final String LOG_FILE = "simulation.log";
    private static final SimpleDateFormat TS_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void main(String[] args) {
        System.out.println("LoggerProcess starting...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Listening on port " + PORT + "...");
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter fileWriter = new PrintWriter(
                         new BufferedWriter(new FileWriter(LOG_FILE, true)))) {

                // Write session header
                String sessionHeader = "================================================================================\n"
                        + "               NEW SIMULATION SESSION INITIALIZED: "
                        + TS_FORMAT.format(new Date()) + "\n"
                        + "================================================================================";
                System.out.println(sessionHeader);
                fileWriter.println(sessionHeader);
                fileWriter.flush();

                String line;
                while ((line = in.readLine()) != null) {
                    String formatted = formatLogLine(line);
                    System.out.println(formatted);
                    fileWriter.println(formatted);
                    fileWriter.flush();

                    // graceful shutdown if QUIT received
                    if (line.contains("QUIT")) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("LoggerProcess terminated.");
    }

    private static String formatLogLine(String raw) {
        String[] tokens = raw.split("\\|", 4);
        String timestamp = TS_FORMAT.format(new Date());

        if (tokens.length < 4 || !tokens[0].equals("LOG")) {
            return "[" + timestamp + "] [INFO ] [MISC       ] " + raw;
        }

        String level = padRight(tokens[1], 5);
        String category = padRight(tokens[2], 10);
        String details = tokens[3];

        return "[" + timestamp + "] [" + level + "] [" + category + "] " + details;
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
