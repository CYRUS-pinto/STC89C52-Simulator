import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class UIServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", exchange -> {
            // Look for index.html next to where Java was started, then in src/
            Path p = Paths.get("index.html");
            if (!Files.exists(p)) {
                p = Paths.get("src/index.html");
            }
            byte[] page = Files.exists(p)
                    ? Files.readAllBytes(p)
                    : "<h1>STC89C52 Simulator</h1><p>index.html not found in working directory</p>"
                            .getBytes(StandardCharsets.UTF_8);

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
