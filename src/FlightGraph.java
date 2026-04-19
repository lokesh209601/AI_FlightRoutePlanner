import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FlightGraph {

    private final Path airportsPath;
    private final Path routesPath;
    private final List<Airport> airports;
    private final Map<Integer, Airport> airportsById;
    private final Map<String, Airport> airportsByCode;
    private final Map<Integer, List<FlightConnection>> outgoingConnections;
    private final int rawAirportRecords;
    private final int rawRouteRecords;
    private final int usableRouteRecords;
    private final int uniqueConnections;

    public FlightGraph(
            Path airportsPath,
            Path routesPath,
            List<Airport> airports,
            Map<Integer, Airport> airportsById,
            Map<String, Airport> airportsByCode,
            Map<Integer, List<FlightConnection>> outgoingConnections,
            int rawAirportRecords,
            int rawRouteRecords,
            int usableRouteRecords,
            int uniqueConnections
    ) {
        this.airportsPath = airportsPath;
        this.routesPath = routesPath;
        this.airports = new ArrayList<>(airports);
        this.airportsById = airportsById;
        this.airportsByCode = airportsByCode;
        this.outgoingConnections = outgoingConnections;
        this.rawAirportRecords = rawAirportRecords;
        this.rawRouteRecords = rawRouteRecords;
        this.usableRouteRecords = usableRouteRecords;
        this.uniqueConnections = uniqueConnections;
    }

    public Airport findAirportByCode(String code) {
        if (code == null) {
            return null;
        }
        return airportsByCode.get(code.trim().toUpperCase());
    }

    public List<FlightConnection> getOutgoingConnections(Airport airport) {
        List<FlightConnection> connections = outgoingConnections.get(airport.getId());
        if (connections == null) {
            return Collections.emptyList();
        }
        return connections;
    }

    public List<Airport> getAirports() {
        return Collections.unmodifiableList(airports);
    }

    public String getDatasetSummary() {
        return airports.size() + " airports loaded, " + uniqueConnections + " unique direct connections";
    }

    public String getDetailedDatasetSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("Airport file: ").append(airportsPath).append('\n');
        builder.append("Route file: ").append(routesPath).append('\n');
        builder.append("Airport records read: ").append(rawAirportRecords).append('\n');
        builder.append("Usable airports loaded: ").append(airports.size()).append('\n');
        builder.append("Route records read: ").append(rawRouteRecords).append('\n');
        builder.append("Direct usable route records: ").append(usableRouteRecords).append('\n');
        builder.append("Unique directed connections: ").append(uniqueConnections).append('\n');
        builder.append("\nType an airport code by pressing the first letters while a combo box is focused.");
        return builder.toString();
    }

    public Map<Integer, Airport> getAirportsById() {
        return airportsById;
    }

}
