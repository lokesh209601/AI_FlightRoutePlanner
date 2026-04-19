import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class LocalMapHttpServer {

    private final HttpServer httpServer;
    private final AtomicLong versionCounter = new AtomicLong(0);
    private volatile String htmlDocument = MapHtmlBuilder.buildPlaceholderHtml("Map is loading...");

    public LocalMapHttpServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/", new MapPageHandler());
        httpServer.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "flight-route-map-http");
            thread.setDaemon(true);
            return thread;
        }));
        httpServer.start();
    }

    public void setHtmlDocument(String htmlDocument) {
        this.htmlDocument = htmlDocument;
        versionCounter.incrementAndGet();
    }

    public URI getCurrentMapUrl() {
        long version = versionCounter.get();
        return URI.create("http://127.0.0.1:" + httpServer.getAddress().getPort() + "/map?v=" + version);
    }

    private class MapPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] responseBytes = htmlDocument.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store, max-age=0");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBytes);
            }
        }
    }

}
