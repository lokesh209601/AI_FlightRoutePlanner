import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchResult {

    private final SearchRequest request;
    private final List<Airport> pathAirports;
    private final List<FlightConnection> pathConnections;
    private final List<Airport> exploredAirports;
    private final double totalDistanceKm;
    private final double runtimeMs;
    private final int nodesCreated;
    private final int nodesExpanded;
    private final int iterations;

    public SearchResult(
            SearchRequest request,
            List<Airport> pathAirports,
            List<FlightConnection> pathConnections,
            List<Airport> exploredAirports,
            double totalDistanceKm,
            double runtimeMs,
            int nodesCreated,
            int nodesExpanded,
            int iterations
    ) {
        this.request = request;
        this.pathAirports = new ArrayList<>(pathAirports);
        this.pathConnections = new ArrayList<>(pathConnections);
        this.exploredAirports = new ArrayList<>(exploredAirports);
        this.totalDistanceKm = totalDistanceKm;
        this.runtimeMs = runtimeMs;
        this.nodesCreated = nodesCreated;
        this.nodesExpanded = nodesExpanded;
        this.iterations = iterations;
    }

    public boolean hasSolution() {
        return !pathAirports.isEmpty();
    }

    public String toConsoleSummary() {
        StringBuilder builder = new StringBuilder();

        if (hasSolution()) {
            builder.append("Route found using ")
                    .append(request.getSearchType())
                    .append(" in ")
                    .append(FlightSearchUtils.formatDouble(runtimeMs))
                    .append(" ms\n");
            builder.append("Flights: ").append(pathConnections.size()).append('\n');
            builder.append("Total distance: ").append(FlightSearchUtils.formatDouble(totalDistanceKm)).append(" km\n");
            builder.append("Airport path: ").append(buildAirportPathLine()).append("\n\n");
            builder.append("Itinerary:\n").append(toItineraryText()).append('\n');
        }
        else {
            builder.append("No route found using ")
                    .append(request.getSearchType())
                    .append(" in ")
                    .append(FlightSearchUtils.formatDouble(runtimeMs))
                    .append(" ms\n");
        }

        builder.append("Nodes created: ").append(nodesCreated).append('\n');
        builder.append("Airports expanded: ").append(nodesExpanded).append('\n');
        builder.append("Iterations: ").append(iterations).append('\n');

        if (!exploredAirports.isEmpty()) {
            builder.append("Expanded airports: ").append(buildExploredAirportLine()).append('\n');
        }

        return builder.toString();
    }

    public String toDisplayText() {
        StringBuilder builder = new StringBuilder();
        builder.append("Algorithm: ").append(request.getSearchType()).append('\n');
        builder.append("Source: ").append(request.getSourceAirport()).append('\n');
        builder.append("Destination: ").append(request.getDestinationAirport()).append("\n\n");

        if (hasSolution()) {
            builder.append("Status: Route found\n");
            builder.append("Flights: ").append(pathConnections.size()).append('\n');
            builder.append("Total distance: ").append(FlightSearchUtils.formatDouble(totalDistanceKm)).append(" km\n");
            builder.append("Runtime: ").append(FlightSearchUtils.formatDouble(runtimeMs)).append(" ms\n");
            builder.append("Nodes created: ").append(nodesCreated).append('\n');
            builder.append("Airports expanded: ").append(nodesExpanded).append('\n');
            builder.append("Iterations: ").append(iterations).append('\n');
            builder.append("Airport path: ").append(buildAirportPathLine()).append('\n');
        }
        else {
            builder.append("Status: No route found\n");
            builder.append("Runtime: ").append(FlightSearchUtils.formatDouble(runtimeMs)).append(" ms\n");
            builder.append("Nodes created: ").append(nodesCreated).append('\n');
            builder.append("Airports expanded: ").append(nodesExpanded).append('\n');
            builder.append("Iterations: ").append(iterations).append('\n');
        }

        return builder.toString();
    }

    public String toItineraryText() {
        if (!hasSolution()) {
            return "No direct route sequence could be found between the selected airports.";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pathConnections.size(); i++) {
            FlightConnection connection = pathConnections.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(connection.getSourceAirport().getPrimaryCode())
                    .append(" -> ")
                    .append(connection.getDestinationAirport().getPrimaryCode())
                    .append(" | ")
                    .append(FlightSearchUtils.formatDouble(connection.getDistanceKm()))
                    .append(" km | ")
                    .append(connection.getAirlineSummary(5))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    public String toRouteMetricsText() {
        if (!hasSolution()) {
            return "No route metrics are available because no route was found.";
        }

        double totalFuelKL = FlightSearchUtils.estimateFuelLiters(totalDistanceKm);
        double totalFuelCost = FlightSearchUtils.calculateFuelCostINR(totalFuelKL);

        StringBuilder builder = new StringBuilder();
        builder.append("Route distance: ")
                .append(FlightSearchUtils.formatDouble(totalDistanceKm))
                .append(" km\n");
        builder.append("Estimated flight time: ")
                .append(FlightSearchUtils.formatFlightDuration(FlightSearchUtils.estimateFlightHours(totalDistanceKm)))
                .append("\n");
        builder.append("Estimated fuel consumption: ")
                .append(FlightSearchUtils.formatDouble(totalFuelKL))
                .append(" KL\n");
        builder.append("Fuel consumption cost: ")
                .append(FlightSearchUtils.formatCurrency(totalFuelCost))
                .append(" Lakhs\n\n");
        builder.append("Leg-by-leg time and fuel:\n");

        for (int i = 0; i < pathConnections.size(); i++) {
            FlightConnection connection = pathConnections.get(i);
            double segmentDistance = connection.getDistanceKm();
            double segmentHours = FlightSearchUtils.estimateFlightHours(segmentDistance);
            double segmentFuelKL = FlightSearchUtils.estimateFuelLiters(segmentDistance);

            builder.append(i + 1)
                    .append(". ")
                    .append(connection.getSourceAirport().getPrimaryCode())
                    .append(" -> ")
                    .append(connection.getDestinationAirport().getPrimaryCode())
                    .append(" | ")
                    .append(FlightSearchUtils.formatDouble(segmentDistance))
                    .append(" km | ")
                    .append(FlightSearchUtils.formatFlightDuration(segmentHours))
                    .append(" | ")
                    .append(FlightSearchUtils.formatDouble(segmentFuelKL))
                    .append(" KL\n");
        }

        return builder.toString().trim();
    }

    private String buildAirportPathLine() {
        List<String> codes = new ArrayList<>();
        for (Airport airport: pathAirports) {
            codes.add(airport.getPrimaryCode());
        }
        return String.join(" -> ", codes);
    }

    private String buildExploredAirportLine() {
        List<String> codes = new ArrayList<>();
        int limit = Math.min(25, exploredAirports.size());
        for (int i = 0; i < limit; i++) {
            codes.add(exploredAirports.get(i).getPrimaryCode());
        }

        if (exploredAirports.size() > limit) {
            codes.add("... +" + (exploredAirports.size() - limit) + " more");
        }

        return String.join(", ", codes);
    }

    public List<Airport> getPathAirports() {
        return Collections.unmodifiableList(pathAirports);
    }

    public List<FlightConnection> getPathConnections() {
        return Collections.unmodifiableList(pathConnections);
    }

    public List<Airport> getExploredAirports() {
        return Collections.unmodifiableList(exploredAirports);
    }

    public SearchRequest getRequest() {
        return request;
    }

}
