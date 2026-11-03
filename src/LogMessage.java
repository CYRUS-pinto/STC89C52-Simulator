
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogMessage {

    private String level;
    private String category;
    private String message;
    private LocalDateTime time;

    public LogMessage(String level, String category, String message) {
        this.level = level;
        this.category = category;
        this.message = message;
        this.time = LocalDateTime.now();
    }

    public String getLevel() {
        return level;
    }

    public String getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public String getFormattedMessage() {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        return "[" + time.format(formatter) + "] "
                + "[" + level + "] "
                + "[" + category + "] "
                + message;
    }
}
