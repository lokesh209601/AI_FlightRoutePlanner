import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FlightDataLoader {

    public FlightGraph loadDefaultGraph() throws IOException {
        Path airportsPath = resolveDataFile("airports.dat.txt");
        Path routesPath = resolveDataFile("routes.dat.txt");
        return load(airportsPath, routesPath);
    }

    public FlightGraph load(Path airportsPath, Path routesPath) throws IOException {
        Map<Integer, Airport> airportsById = new HashMap<>();
        Map<String, Airport> airportsByCode = new HashMap<>();
        int rawAirportRecords = loadAirports(airportsPath, airportsById, airportsByCode);

        Map<Integer, Map<Integer, FlightConnection>> adjacencyBuilder = new HashMap<>();
        RouteCounts routeCounts = loadRoutes(routesPath, airportsById, airportsByCode, adjacencyBuilder);

        ArrayList<Airport> airports = new ArrayList<>(airportsById.values());
        Collections.sort(airports);

        Map<Integer, List<FlightConnection>> outgoingConnections = new HashMap<>();
        int uniqueConnections = 0;
        for (Map.Entry<Integer, Map<Integer, FlightConnection>> entry: adjacencyBuilder.entrySet()) {
            ArrayList<FlightConnection> connections = new ArrayList<>(entry.getValue().values());
            connections.sort((left, right) -> left.getDestinationAirport().getDisplayName().compareToIgnoreCase(right.getDestinationAirport().getDisplayName()));
            outgoingConnections.put(entry.getKey(), Collections.unmodifiableList(connections));
            uniqueConnections += connections.size();
        }

        return new FlightGraph(
                airportsPath.toAbsolutePath().normalize(),
                routesPath.toAbsolutePath().normalize(),
                airports,
                airportsById,
                airportsByCode,
                outgoingConnections,
                rawAirportRecords,
                routeCounts.rawRouteRecords,
                routeCounts.usableRouteRecords,
                uniqueConnections
        );
    }

    private int loadAirports(Path airportsPath, Map<Integer, Airport> airportsById, Map<String, Airport> airportsByCode) throws IOException {
        int rawAirportRecords = 0;

        try (BufferedReader reader = Files.newBufferedReader(airportsPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                rawAirportRecords++;
                List<String> fields = CsvUtils.parseCsvLine(line);
                if (fields.size() < 8) {
                    continue;
                }

                Integer id = FlightSearchUtils.parseInteger(fields.get(0));
                Double latitude = FlightSearchUtils.parseDouble(fields.get(6));
                Double longitude = FlightSearchUtils.parseDouble(fields.get(7));
                String iataCode = FlightSearchUtils.normalizeDataValue(fields.get(4)).toUpperCase();
                String icaoCode = FlightSearchUtils.normalizeDataValue(fields.get(5)).toUpperCase();

                if (id == null || latitude == null || longitude == null) {
                    continue;
                }
                if (iataCode.isEmpty() && icaoCode.isEmpty()) {
                    continue;
                }

                Airport airport = new Airport(
                        id,
                        FlightSearchUtils.normalizeDataValue(fields.get(1)),
                        FlightSearchUtils.normalizeDataValue(fields.get(2)),
                        FlightSearchUtils.normalizeDataValue(fields.get(3)),
                        iataCode,
                        icaoCode,
                        latitude,
                        longitude
                );

                airportsById.put(id, airport);
                registerAirportCode(airportsByCode, airport.getIataCode(), airport);
                registerAirportCode(airportsByCode, airport.getIcaoCode(), airport);
            }
        }

        return rawAirportRecords;
    }

    private RouteCounts loadRoutes(
            Path routesPath,
            Map<Integer, Airport> airportsById,
            Map<String, Airport> airportsByCode,
            Map<Integer, Map<Integer, FlightConnection>> adjacencyBuilder
    ) throws IOException {
        int rawRouteRecords = 0;
        int usableRouteRecords = 0;

        try (BufferedReader reader = Files.newBufferedReader(routesPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                rawRouteRecords++;
                List<String> fields = CsvUtils.parseCsvLine(line);
                if (fields.size() < 8) {
                    continue;
                }

                Integer stops = FlightSearchUtils.parseInteger(fields.get(7));
                if (stops != null && stops > 0) {
                    continue;
                }

                Airport sourceAirport = resolveAirport(fields.get(3), fields.get(2), airportsById, airportsByCode);
                Airport destinationAirport = resolveAirport(fields.get(5), fields.get(4), airportsById, airportsByCode);
                if (sourceAirport == null || destinationAirport == null) {
                    continue;
                }
                if (sourceAirport.getId() == destinationAirport.getId()) {
                    continue;
                }

                Map<Integer, FlightConnection> outgoingConnections = adjacencyBuilder.computeIfAbsent(sourceAirport.getId(), ignored -> new HashMap<>());
                FlightConnection connection = outgoingConnections.get(destinationAirport.getId());
                if (connection == null) {
                    double distanceKm = FlightSearchUtils.haversineDistanceKm(sourceAirport, destinationAirport);
                    connection = new FlightConnection(sourceAirport, destinationAirport, distanceKm);
                    outgoingConnections.put(destinationAirport.getId(), connection);
                }

                connection.addAirline(FlightSearchUtils.normalizeDataValue(fields.get(0)));
                usableRouteRecords++;
            }
        }

        return new RouteCounts(rawRouteRecords, usableRouteRecords);
    }

    private void registerAirportCode(Map<String, Airport> airportsByCode, String code, Airport airport) {
        if (!code.isEmpty() && !airportsByCode.containsKey(code)) {
            airportsByCode.put(code, airport);
        }
    }

    private Airport resolveAirport(String idField, String codeField, Map<Integer, Airport> airportsById, Map<String, Airport> airportsByCode) {
        Integer airportId = FlightSearchUtils.parseInteger(idField);
        if (airportId != null && airportsById.containsKey(airportId)) {
            return airportsById.get(airportId);
        }

        String code = FlightSearchUtils.normalizeDataValue(codeField).toUpperCase();
        if (!code.isEmpty()) {
            return airportsByCode.get(code);
        }

        return null;
    }

    private Path resolveDataFile(String fileName) throws IOException {
        Path[] candidates = new Path[] {
                Paths.get(fileName),
                Paths.get("..", fileName),
                Paths.get(System.getProperty("user.dir"), fileName),
                Paths.get(System.getProperty("user.dir"), "..", fileName)
        };

        for (Path candidate: candidates) {
            Path normalizedCandidate = candidate.toAbsolutePath().normalize();
            if (Files.exists(normalizedCandidate)) {
                return normalizedCandidate;
            }
        }

        throw new IOException("Could not find '" + fileName + "' in the current directory or its parent.");
    }

    private static class RouteCounts {
        private final int rawRouteRecords;
        private final int usableRouteRecords;

        private RouteCounts(int rawRouteRecords, int usableRouteRecords) {
            this.rawRouteRecords = rawRouteRecords;
            this.usableRouteRecords = usableRouteRecords;
        }
    }

}
