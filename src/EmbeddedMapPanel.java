import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


public class EmbeddedMapPanel extends JPanel {

    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private final JLabel messageLabel = new JLabel("Map view will appear here.");
    private final JTextArea fallbackTextArea = new JTextArea();
    private final JButton openMapButton = new JButton("Open Map In Browser");

    private JComponent javaFxPanel;
    private Object webView;
    private Object webEngine;
    private boolean javaFxEmbedded;
    private String currentHtml;
    private String pendingHtml;
    private LocalMapHttpServer localMapHttpServer;

    public EmbeddedMapPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Route Map"));
        setPreferredSize(new Dimension(700, 320));

        fallbackTextArea.setEditable(false);
        fallbackTextArea.setLineWrap(true);
        fallbackTextArea.setWrapStyleWord(true);

        openMapButton.setEnabled(false);
        openMapButton.addActionListener(e -> openCurrentMapInBrowser());

        add(contentPanel, BorderLayout.CENTER);

        try {
            localMapHttpServer = new LocalMapHttpServer();
        }
        catch (IOException e) {
            showFallbackMessage(
                    "Local map server could not be started.",
                    "Embedded map pages need a small localhost server so tile requests include a valid HTTP Referer.\n\n"
                            + e.getMessage()
            );
        }

        if (!initializeJavaFxWebView()) {
            showFallbackMessage(
                    "JavaFX WebView is not available in this runtime.",
                    "Run a search to generate a browser-based route map. " +
                            "If you want the map embedded directly in the app, launch the program with JavaFX modules available."
            );
        }

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refreshEmbeddedMapLayout();
                scheduleEmbeddedMapRefresh(500);
            }

            @Override
            public void componentShown(ComponentEvent e) {
                refreshEmbeddedMapLayout();
                scheduleEmbeddedMapRefresh(500);
            }
        });
    }

    public void showPlaceholder(String message) {
        String placeholderHtml = MapHtmlBuilder.buildPlaceholderHtml(message);
        currentHtml = null;
        pendingHtml = placeholderHtml;

        if (javaFxEmbedded) {
            loadHtmlIntoWebView(placeholderHtml);
            messageLabel.setText(message);
            openMapButton.setEnabled(false);
            refreshEmbeddedMapLayout();
        }
        else {
            showFallbackMessage(
                    message,
                    "Embedded JavaFX WebView is not available in this runtime.\n\n" +
                            "Once a route is found, you can still open the map in your system browser."
            );
        }
    }

    
    public void showSearchResult(SearchResult result) {
        String routeHtml = MapHtmlBuilder.buildRouteHtml(result);
        currentHtml = routeHtml;
        pendingHtml = routeHtml;

        if (javaFxEmbedded) {
            loadHtmlIntoWebView(routeHtml);
            messageLabel.setText(result.hasSolution()
                    ? "Embedded route map using JavaFX WebView."
                    : "No route found. Showing source and destination on the map.");
            openMapButton.setEnabled(true);
            refreshEmbeddedMapLayout();
        }
        else {
            showFallbackMessage(
                    "Browser fallback is active because JavaFX WebView is unavailable.",
                    buildCoordinateSummary(result)
            );
            openMapButton.setEnabled(true);
        }
    }

    
    private boolean initializeJavaFxWebView() {
        try {
            Class<?> jfxPanelClass = Class.forName("javafx.embed.swing.JFXPanel");
            Class<?> platformClass = Class.forName("javafx.application.Platform");
            Class<?> webViewClass = Class.forName("javafx.scene.web.WebView");
            Class<?> sceneClass = Class.forName("javafx.scene.Scene");
            Class<?> parentClass = Class.forName("javafx.scene.Parent");

            JComponent jfxPanel = (JComponent) jfxPanelClass.getConstructor().newInstance();
            javaFxPanel = jfxPanel;

            contentPanel.removeAll();
            contentPanel.add(jfxPanel, BorderLayout.CENTER);
            contentPanel.add(buildFooterStrip(), BorderLayout.SOUTH);
            contentPanel.revalidate();
            contentPanel.repaint();

            Method setImplicitExit = platformClass.getMethod("setImplicitExit", boolean.class);
            setImplicitExit.invoke(null, false);

            Method runLater = platformClass.getMethod("runLater", Runnable.class);
            runLater.invoke(null, new Runnable() {
                @Override
                public void run() {
                    try {
                        webView = webViewClass.getConstructor().newInstance();
                        webViewClass.getMethod("setMinSize", double.class, double.class).invoke(webView, 0.0, 0.0);
                        webViewClass.getMethod("setPrefSize", double.class, double.class).invoke(webView, 800.0, 600.0);
                        webViewClass.getMethod("setMaxSize", double.class, double.class).invoke(webView, Double.MAX_VALUE, Double.MAX_VALUE);
                        webEngine = webViewClass.getMethod("getEngine").invoke(webView);
                        Object scene = sceneClass.getConstructor(parentClass).newInstance(webView);
                        jfxPanelClass.getMethod("setScene", sceneClass).invoke(jfxPanel, scene);

                        if (pendingHtml != null) {
                            loadHtmlIntoWebView(pendingHtml);
                        }
                    }
                    catch (Exception e) {
                        SwingUtilities.invokeLater(() -> showFallbackMessage(
                                "JavaFX WebView failed to initialize.",
                                "The map can still be opened in your browser.\n\n" + e.getMessage()
                        ));
                    }
                }
            });

            javaFxEmbedded = true;
            return true;
        }
        catch (Exception e) {
            javaFxEmbedded = false;
            return false;
        }
    }

   
    private JPanel buildFooterStrip() {
        JPanel footerPanel = new JPanel(new BorderLayout(8, 8));
        footerPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        footerPanel.add(messageLabel, BorderLayout.CENTER);
        footerPanel.add(openMapButton, BorderLayout.EAST);
        return footerPanel;
    }

    
    private void showFallbackMessage(String headline, String details) {
        javaFxEmbedded = false;
        contentPanel.removeAll();
        messageLabel.setText(headline);
        fallbackTextArea.setText(details);
        contentPanel.add(new JScrollPane(fallbackTextArea), BorderLayout.CENTER);
        contentPanel.add(buildFooterStrip(), BorderLayout.SOUTH);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    
    private void loadHtmlIntoWebView(String html) {
        pendingHtml = html;
        if (webEngine == null || localMapHttpServer == null || javaFxPanel == null || !javaFxPanel.isDisplayable()) {
            return;
        }

        localMapHttpServer.setHtmlDocument(html);
        String pageUrl = localMapHttpServer.getCurrentMapUrl().toString();

        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform");
            Method runLater = platformClass.getMethod("runLater", Runnable.class);
            runLater.invoke(null, new Runnable() {
                @Override
                public void run() {
                    try {
                        webEngine.getClass().getMethod("load", String.class).invoke(webEngine, pageUrl);
                        SwingUtilities.invokeLater(() -> {
                            if (javaFxPanel != null && javaFxPanel.isDisplayable()) {
                                refreshEmbeddedMapLayout();
                                scheduleEmbeddedMapRefresh(500);
                            }
                        });
                    }
                    catch (Exception ignored) {
                    }
                }
            });
        }
        catch (Exception ignored) {
        }
    }

  
    private void scheduleEmbeddedMapRefresh(int delayMillis) {
        Timer timer = new Timer(delayMillis, e -> refreshEmbeddedMapLayout());
        timer.setRepeats(false);
        timer.start();
    }

    
    private void refreshEmbeddedMapLayout() {
        if (!javaFxEmbedded || webEngine == null || webView == null || javaFxPanel == null || !javaFxPanel.isDisplayable()) {
            return;
        }

        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform");
            Method runLater = platformClass.getMethod("runLater", Runnable.class);
            runLater.invoke(null, new Runnable() {
                @Override
                public void run() {
                    try {
                        double width = Math.max(1, javaFxPanel.getWidth());
                        double height = Math.max(1, javaFxPanel.getHeight());

                        webView.getClass().getMethod("setPrefSize", double.class, double.class).invoke(webView, width, height);
                        webView.getClass().getMethod("setMaxSize", double.class, double.class).invoke(webView, width, height);
                        webView.getClass().getMethod("resize", double.class, double.class).invoke(webView, width, height);
                        webEngine.getClass()
                                .getMethod("executeScript", String.class)
                                .invoke(webEngine, "if (window.refreshFlightRouteMap) { window.refreshFlightRouteMap(); }");
                    }
                    catch (Exception ignored) {
                    }
                }
            });
        }
        catch (Exception ignored) {
        }
    }

    
    private void openCurrentMapInBrowser() {
        if (currentHtml == null || currentHtml.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Run a search first so the route map can be generated.", "No Map Available", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(this, "Desktop browser integration is not supported in this environment.", "Browser Unavailable", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (localMapHttpServer == null) {
                Path htmlFile = Files.createTempFile("flight-route-map-", ".html");
                Files.writeString(htmlFile, currentHtml, StandardCharsets.UTF_8);
                Desktop.getDesktop().browse(htmlFile.toUri());
                return;
            }

            localMapHttpServer.setHtmlDocument(currentHtml);
            Desktop.getDesktop().browse(localMapHttpServer.getCurrentMapUrl());
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not open the map in the browser: " + e.getMessage(), "Browser Error", JOptionPane.ERROR_MESSAGE);
        }
    }

   
    private String buildCoordinateSummary(SearchResult result) {
        StringBuilder builder = new StringBuilder();

        builder.append("Route coordinates:\n\n");

        if (result.hasSolution()) {
            int index = 1;
            for (Airport airport: result.getPathAirports()) {
                builder.append(index++)
                        .append(". ")
                        .append(airport.getPrimaryCode())
                        .append(" - ")
                        .append(airport.getName())
                        .append(" | lat ")
                        .append(FlightSearchUtils.formatDouble(airport.getLatitude()))
                        .append(", lon ")
                        .append(FlightSearchUtils.formatDouble(airport.getLongitude()))
                        .append('\n');
            }
        }
        else {
            Airport sourceAirport = result.getRequest().getSourceAirport();
            Airport destinationAirport = result.getRequest().getDestinationAirport();
            builder.append("Source: ")
                    .append(sourceAirport.getPrimaryCode())
                    .append(" | lat ")
                    .append(FlightSearchUtils.formatDouble(sourceAirport.getLatitude()))
                    .append(", lon ")
                    .append(FlightSearchUtils.formatDouble(sourceAirport.getLongitude()))
                    .append('\n');
            builder.append("Destination: ")
                    .append(destinationAirport.getPrimaryCode())
                    .append(" | lat ")
                    .append(FlightSearchUtils.formatDouble(destinationAirport.getLatitude()))
                    .append(", lon ")
                    .append(FlightSearchUtils.formatDouble(destinationAirport.getLongitude()))
                    .append('\n');
        }

        builder.append("\nClick \"Open Map In Browser\" to view the plotted route using the generated HTML map.");
        return builder.toString();
    }

}
