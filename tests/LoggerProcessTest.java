import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class LoggerProcessTest {

    public static void main(String[] args) {

        try (Socket socket = new Socket("localhost", 5001);
             PrintWriter writer = new PrintWriter(
                     socket.getOutputStream(),
                     true,
                     StandardCharsets.UTF_8)) {

            System.out.println("Connected to LoggerProcess.");

            writer.println(
                    "LOG|INFO|CPU|Instruction executed");

            writer.println(
                    "LOG|INFO|MEMORY|Memory read completed");

            writer.println(
                    "LOG|ERROR|MEMORY|Invalid memory address");

            writer.println(
                    "LOG|INFO|STACK|PUSH operation completed");

            writer.println(
                    "LOG|DEBUG|QUEUE|Element inserted");

            writer.println(
                    "INVALID MESSAGE");

            System.out.println("Test messages sent successfully.");

        } catch (Exception e) {

            System.err.println(
                    "Unable to communicate with LoggerProcess: "
                            + e.getMessage());
        }
    }
}
