public class SearchRequest {

    private static final String[] SUPPORTED_ALGORITHMS = {"BFS", "DFS", "BestF", "AStar", "Dijkstra", "BellmanFord"};

    private final String searchType;
    private final Airport sourceAirport;
    private final Airport destinationAirport;

    public SearchRequest(String searchType, Airport sourceAirport, Airport destinationAirport) {
        this.searchType = searchType;
        this.sourceAirport = sourceAirport;
        this.destinationAirport = destinationAirport;
        validate();
    }

    public static SearchRequest fromArgs(String[] args, FlightGraph graph) {
        if (args.length < 3) {
            throw new IllegalArgumentException(Helper.getUsageMessage());
        }

        Airport sourceAirport = graph.findAirportByCode(args[1]);
        if (sourceAirport == null) {
            throw new IllegalArgumentException("Unknown source airport code '" + args[1] + "'.");
        }

        Airport destinationAirport = graph.findAirportByCode(args[2]);
        if (destinationAirport == null) {
            throw new IllegalArgumentException("Unknown destination airport code '" + args[2] + "'.");
        }

        return new SearchRequest(args[0], sourceAirport, destinationAirport);
    }

    public static String[] getSupportedAlgorithms() {
        return SUPPORTED_ALGORITHMS.clone();
    }

    private void validate() {
        boolean supported = false;
        for (String algorithm: SUPPORTED_ALGORITHMS) {
            if (algorithm.equals(searchType)) {
                supported = true;
                break;
            }
        }

        if (!supported) {
            throw new IllegalArgumentException("Unsupported search type '" + searchType + "'.");
        }

        if (sourceAirport == null || destinationAirport == null) {
            throw new IllegalArgumentException("Source and destination airports must be selected.");
        }

        if (sourceAirport.getId() == destinationAirport.getId()) {
            throw new IllegalArgumentException("Source and destination airports must be different.");
        }
    }

    public String getSearchType() {
        return searchType;
    }

    public Airport getSourceAirport() {
        return sourceAirport;
    }

    public Airport getDestinationAirport() {
        return destinationAirport;
    }

}
