
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LogHandler {

    private final String fileName;

    public LogHandler(String fileName) {
        this.fileName = fileName;
    }

    public synchronized void writeLog(
            String level, String category, String message) {

        LogMessage log = new LogMessage(level, category, message);

        String output = log.getFormattedMessage();

        System.out.println("[Logger] " + output);

        try (BufferedWriter writer =
                     new BufferedWriter(
                             new FileWriter(fileName, true))) {

            writer.write(output);
            writer.newLine();

        } catch (IOException e) {

            System.err.println(
                    "Error writing log file: " + e.getMessage());
        }
    }
}
