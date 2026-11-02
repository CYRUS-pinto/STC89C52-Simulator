import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Executors;

public class UIServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", exchange -> {
            byte[] page = Files.readAllBytes(Paths.get("index.html"));
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, page.length);
            OutputStream out = exchange.getResponseBody();
            out.write(page);
            out.close();
        });

        server.createContext("/api/cmd", exchange -> {
        String query = exchange.getRequestURI().getQuery();
        String cmd = query.substring(query.indexOf("=") + 1);

        String reply;
try {
    Socket core = new Socket("127.0.0.1", 5000);
    PrintWriter toCore = new PrintWriter(core.getOutputStream(), true);
    BufferedReader fromCore = new BufferedReader(new InputStreamReader(core.getInputStream()));
    toCore.println(cmd);
    reply = fromCore.readLine();
    core.close();
     } catch (Exception e) {
      reply = "OFFLINE";
      }
        byte[] bytes = reply.getBytes("UTF-8");

        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
         OutputStream out = exchange.getResponseBody();
        out.write(bytes);
        out.close();
        });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("UI running at http://localhost:8080");
    }
}
