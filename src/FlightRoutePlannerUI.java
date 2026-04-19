import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

public class FlightRoutePlannerUI extends JFrame {

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font BODY_FONT = new Font("Consolas", Font.PLAIN, 13);

    private final JComboBox<String> algorithmCombo = new JComboBox<>(SearchRequest.getSupportedAlgorithms());
    private final JComboBox<Airport> sourceCombo = new JComboBox<>();
    private final JComboBox<Airport> destinationCombo = new JComboBox<>();
    private final JButton runButton = new JButton("Find Route");
    private final JButton swapButton = new JButton("Swap");
    private final JLabel statusLabel = new JLabel("Loading airport data...");
    private final JLabel datasetLabel = new JLabel("Dataset: loading...");
    private final JTextArea summaryArea = new JTextArea();
    private final JTextArea routeMetricsArea = new JTextArea();
    private final JTextArea itineraryArea = new JTextArea();
    private final EmbeddedMapPanel mapPanel = new EmbeddedMapPanel();

    private FlightGraph graph;

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeel();
            FlightRoutePlannerUI ui = new FlightRoutePlannerUI();
            ui.setVisible(true);
        });
    }

    public FlightRoutePlannerUI() {
        super("Flight Route Planner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);

        summaryArea.setEditable(false);
        summaryArea.setFont(BODY_FONT);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setText("Loading airport and route data...");

        itineraryArea.setEditable(false);
        itineraryArea.setFont(BODY_FONT);
        itineraryArea.setLineWrap(true);
        itineraryArea.setWrapStyleWord(true);
        itineraryArea.setText("Itinerary details will appear here.");

        routeMetricsArea.setEditable(false);
        routeMetricsArea.setFont(BODY_FONT);
        routeMetricsArea.setLineWrap(true);
        routeMetricsArea.setWrapStyleWord(true);
        routeMetricsArea.setText("Route time and fuel metrics will appear here after a search.");

        sourceCombo.setEnabled(false);
        destinationCombo.setEnabled(false);
        algorithmCombo.setEnabled(false);
        runButton.setEnabled(false);
        swapButton.setEnabled(false);
        sourceCombo.setMaximumRowCount(20);
        destinationCombo.setMaximumRowCount(20);

        AirportComboRenderer airportRenderer = new AirportComboRenderer();
        sourceCombo.setRenderer(airportRenderer);
        destinationCombo.setRenderer(airportRenderer);

        setLayout(new BorderLayout(12, 12));
        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildMainPanel(), BorderLayout.CENTER);
        add(buildFooterPanel(), BorderLayout.SOUTH);

        wireEvents();
        loadDataAsync();
    }

    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));

        JLabel titleLabel = new JLabel("Flight Route Planner");
        titleLabel.setFont(TITLE_FONT);

        JLabel subtitleLabel = new JLabel("Search real airport routes loaded from airports.dat.txt and routes.dat.txt.");

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(subtitleLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildMainPanel() {
        JPanel controlsPanel = buildControlsPanel();

        JScrollPane summaryScrollPane = new JScrollPane(summaryArea);
        summaryScrollPane.setBorder(BorderFactory.createTitledBorder("Summary"));

        JScrollPane metricsScrollPane = new JScrollPane(routeMetricsArea);
        metricsScrollPane.setBorder(BorderFactory.createTitledBorder("Route Metrics"));

        JScrollPane itineraryScrollPane = new JScrollPane(itineraryArea);
        itineraryScrollPane.setBorder(BorderFactory.createTitledBorder("Itinerary"));

        JPanel leftPanel = new JPanel(new BorderLayout(12, 12));
        leftPanel.add(controlsPanel, BorderLayout.NORTH);
        leftPanel.add(summaryScrollPane, BorderLayout.CENTER);

        JSplitPane textSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, metricsScrollPane, itineraryScrollPane);
        textSplitPane.setResizeWeight(0.45);

        JSplitPane contentSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mapPanel, textSplitPane);
        contentSplitPane.setResizeWeight(0.55);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(contentSplitPane, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel buildControlsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Search Controls"));
        panel.setPreferredSize(new Dimension(370, 420));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        panel.add(new JLabel("Dataset"), gbc);
        gbc.gridy++;
        panel.add(datasetLabel, gbc);

        gbc.gridy++;
        panel.add(new JLabel("Algorithm"), gbc);
        gbc.gridy++;
        panel.add(algorithmCombo, gbc);

        gbc.gridy++;
        panel.add(new JLabel("Source Airport"), gbc);
        gbc.gridy++;
        panel.add(sourceCombo, gbc);

        gbc.gridy++;
        panel.add(new JLabel("Destination Airport"), gbc);
        gbc.gridy++;
        panel.add(destinationCombo, gbc);

        gbc.gridy++;
        panel.add(runButton, gbc);

        gbc.gridy++;
        panel.add(swapButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    private JPanel buildFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 16, 10, 16));
        panel.add(statusLabel, BorderLayout.WEST);
        return panel;
    }

    private void wireEvents() {
        runButton.addActionListener(e -> runSearchAsync());
        swapButton.addActionListener(e -> {
            Object sourceSelection = sourceCombo.getSelectedItem();
            sourceCombo.setSelectedItem(destinationCombo.getSelectedItem());
            destinationCombo.setSelectedItem(sourceSelection);
        });
    }

    private void loadDataAsync() {
        SwingWorker<FlightGraph, Void> worker = new SwingWorker<FlightGraph, Void>() {
            @Override
            protected FlightGraph doInBackground() throws Exception {
                return new FlightDataLoader().loadDefaultGraph();
            }

            @Override
            protected void done() {
                try {
                    graph = get();
                    populateAirportSelectors(graph.getAirports());
                    datasetLabel.setText(graph.getDatasetSummary());
                    statusLabel.setText("Dataset loaded. Select airports and run a search.");
                    summaryArea.setText(graph.getDetailedDatasetSummary());
                    routeMetricsArea.setText("Route time and fuel metrics will appear here after a search.");
                    itineraryArea.setText("Choose a source and destination airport, then click Find Route.");
                    mapPanel.showPlaceholder("Run a search to draw the route on the map.");
                    setControlsEnabled(true);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Dataset loading was interrupted.");
                }
                catch (ExecutionException e) {
                    String message = e.getCause() == null ? "Failed to load dataset." : e.getCause().getMessage();
                    summaryArea.setText("Could not load airport data.\n\n" + message);
                    itineraryArea.setText("Search is unavailable until the dataset loads successfully.");
                    mapPanel.showPlaceholder("Map unavailable until the dataset loads successfully.");
                    statusLabel.setText("Dataset load failed.");
                }
            }
        };

        worker.execute();
    }

    private void populateAirportSelectors(List<Airport> airports) {
        DefaultComboBoxModel<Airport> sourceModel = new DefaultComboBoxModel<>();
        DefaultComboBoxModel<Airport> destinationModel = new DefaultComboBoxModel<>();

        for (Airport airport: airports) {
            sourceModel.addElement(airport);
            destinationModel.addElement(airport);
        }

        sourceCombo.setModel(sourceModel);
        destinationCombo.setModel(destinationModel);

        selectAirportOrFallback(sourceCombo, "DEL", 0);
        selectAirportOrFallback(destinationCombo, "LHR", Math.min(1, airports.size() - 1));
    }

    private void selectAirportOrFallback(JComboBox<Airport> comboBox, String code, int fallbackIndex) {
        Airport airport = graph.findAirportByCode(code);
        if (airport != null) {
            comboBox.setSelectedItem(airport);
            return;
        }

        if (comboBox.getItemCount() > 0) {
            comboBox.setSelectedIndex(Math.max(0, fallbackIndex));
        }
    }

    private void runSearchAsync() {
        if (graph == null) {
            return;
        }

        SearchRequest request;
        try {
            request = new SearchRequest(
                    (String) algorithmCombo.getSelectedItem(),
                    (Airport) sourceCombo.getSelectedItem(),
                    (Airport) destinationCombo.getSelectedItem()
            );
        }
        catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid Search Request", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setControlsEnabled(false);
        statusLabel.setText("Running " + SearchRunner.getSearchLabel(request.getSearchType()) + "...");
        summaryArea.setText("Searching...");
        itineraryArea.setText("Working on the route...");
        mapPanel.showPlaceholder("Searching for a route...");

        SwingWorker<SearchResult, Void> worker = new SwingWorker<SearchResult, Void>() {
            @Override
            protected SearchResult doInBackground() {
                return new SearchRunner(graph).run(request, false);
            }

            @Override
            protected void done() {
                try {
                    SearchResult result = get();
                    summaryArea.setText(result.toDisplayText());
                    routeMetricsArea.setText(result.toRouteMetricsText());
                    itineraryArea.setText(result.toItineraryText());
                    mapPanel.showSearchResult(result);
                    statusLabel.setText(result.hasSolution() ? "Route found." : "No route found.");
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Search interrupted.");
                }
                catch (ExecutionException e) {
                    String message = e.getCause() == null ? "Search failed." : e.getCause().getMessage();
                    statusLabel.setText("Search failed.");
                    JOptionPane.showMessageDialog(FlightRoutePlannerUI.this, message, "Search Error", JOptionPane.ERROR_MESSAGE);
                }
                finally {
                    setControlsEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void setControlsEnabled(boolean enabled) {
        sourceCombo.setEnabled(enabled);
        destinationCombo.setEnabled(enabled);
        algorithmCombo.setEnabled(enabled);
        runButton.setEnabled(enabled);
        swapButton.setEnabled(enabled);
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ignored) {
        }
    }

    private static class AirportComboRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(
                javax.swing.JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Airport) {
                setText(((Airport) value).getDisplayName());
            }
            return this;
        }
    }

}
