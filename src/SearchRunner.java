import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SearchRunner {

    private final FlightGraph graph;

    public SearchRunner(FlightGraph graph) {
        this.graph = graph;
    }

    public SearchResult run(SearchRequest request, boolean verbose) {
        long startTime = System.nanoTime();
        SearchOutcome outcome;

        switch (request.getSearchType()) {
            case "BFS":
                outcome = runBreadthFirstSearch(request, verbose);
                break;
            case "DFS":
                outcome = runDepthFirstSearch(request, verbose);
                break;
            case "BestF":
                outcome = runGreedyBestFirstSearch(request, verbose);
                break;
            case "AStar":
                outcome = runAStarSearch(request, verbose);
                break;
            default:
                throw new IllegalArgumentException("Unsupported search type '" + request.getSearchType() + "'.");
        }

        long endTime = System.nanoTime();
        double runtimeMs = (double) TimeUnit.NANOSECONDS.toMicros(endTime - startTime) / 1000.0;

        return new SearchResult(
                request,
                outcome.pathAirports,
                outcome.pathConnections,
                outcome.exploredAirports,
                outcome.totalDistanceKm,
                runtimeMs,
                outcome.nodesCreated,
                outcome.nodesExpanded,
                outcome.iterations
        );
    }

    public static String getSearchLabel(String searchType) {
        switch (searchType) {
            case "BFS":
                return "Breadth-First Search";
            case "DFS":
                return "Depth-First Search";
            case "BestF":
                return "Greedy Best-First Search";
            case "AStar":
                return "A* Search";
            default:
                return searchType;
        }
    }

    private SearchOutcome runBreadthFirstSearch(SearchRequest request, boolean verbose) {
        Deque<AirportSearchNode> frontier = new ArrayDeque<>();
        Set<Integer> frontierAirportIds = new HashSet<>();
        Set<Integer> exploredAirportIds = new HashSet<>();
        ArrayList<Airport> exploredAirports = new ArrayList<>();

        AirportSearchNode root = AirportSearchNode.root(request.getSourceAirport(), priorityForVerbose(request.getSourceAirport(), request.getDestinationAirport()));
        frontier.addLast(root);
        frontierAirportIds.add(root.getAirport().getId());

        int nodesCreated = 1;
        int iterations = 0;

        while (!frontier.isEmpty()) {
            iterations++;
            AirportSearchNode currentNode = frontier.removeFirst();
            frontierAirportIds.remove(currentNode.getAirport().getId());

            if (exploredAirportIds.contains(currentNode.getAirport().getId())) {
                continue;
            }

            exploredAirportIds.add(currentNode.getAirport().getId());
            exploredAirports.add(currentNode.getAirport());

            if (currentNode.getAirport().getId() == request.getDestinationAirport().getId()) {
                if (verbose) {
                    printTrace(iterations, currentNode, frontier.size(), exploredAirports.size());
                }
                return SearchOutcome.success(currentNode, exploredAirports, nodesCreated, iterations);
            }

            for (FlightConnection connection: graph.getOutgoingConnections(currentNode.getAirport())) {
                Airport nextAirport = connection.getDestinationAirport();
                if (exploredAirportIds.contains(nextAirport.getId()) || frontierAirportIds.contains(nextAirport.getId())) {
                    continue;
                }

                AirportSearchNode childNode = currentNode.createChild(connection, 0.0);
                frontier.addLast(childNode);
                frontierAirportIds.add(nextAirport.getId());
                nodesCreated++;
            }

            if (verbose) {
                printTrace(iterations, currentNode, frontier.size(), exploredAirports.size());
            }
        }

        return SearchOutcome.failure(exploredAirports, nodesCreated, iterations);
    }

    private SearchOutcome runDepthFirstSearch(SearchRequest request, boolean verbose) {
        Deque<AirportSearchNode> frontier = new ArrayDeque<>();
        Set<Integer> frontierAirportIds = new HashSet<>();
        Set<Integer> exploredAirportIds = new HashSet<>();
        ArrayList<Airport> exploredAirports = new ArrayList<>();

        AirportSearchNode root = AirportSearchNode.root(request.getSourceAirport(), priorityForVerbose(request.getSourceAirport(), request.getDestinationAirport()));
        frontier.addFirst(root);
        frontierAirportIds.add(root.getAirport().getId());

        int nodesCreated = 1;
        int iterations = 0;

        while (!frontier.isEmpty()) {
            iterations++;
            AirportSearchNode currentNode = frontier.removeFirst();
            frontierAirportIds.remove(currentNode.getAirport().getId());

            if (exploredAirportIds.contains(currentNode.getAirport().getId())) {
                continue;
            }

            exploredAirportIds.add(currentNode.getAirport().getId());
            exploredAirports.add(currentNode.getAirport());

            if (currentNode.getAirport().getId() == request.getDestinationAirport().getId()) {
                if (verbose) {
                    printTrace(iterations, currentNode, frontier.size(), exploredAirports.size());
                }
                return SearchOutcome.success(currentNode, exploredAirports, nodesCreated, iterations);
            }

            List<FlightConnection> outgoingConnections = graph.getOutgoingConnections(currentNode.getAirport());
            for (int i = outgoingConnections.size() - 1; i >= 0; i--) {
                FlightConnection connection = outgoingConnections.get(i);
                Airport nextAirport = connection.getDestinationAirport();
                if (exploredAirportIds.contains(nextAirport.getId()) || frontierAirportIds.contains(nextAirport.getId())) {
                    continue;
                }

                AirportSearchNode childNode = currentNode.createChild(connection, 0.0);
                frontier.addFirst(childNode);
                frontierAirportIds.add(nextAirport.getId());
                nodesCreated++;
            }

            if (verbose) {
                printTrace(iterations, currentNode, frontier.size(), exploredAirports.size());
            }
        }

        return SearchOutcome.failure(exploredAirports, nodesCreated, iterations);
    }

    private SearchOutcome runGreedyBestFirstSearch(SearchRequest request, boolean verbose) {
        PriorityQueue<AirportSearchNode> frontier = createPriorityQueue();
        Set<Integer> exploredAirportIds = new HashSet<>();
        Set<Integer> frontierAirportIds = new HashSet<>();
        ArrayList<Airport> exploredAirports = new ArrayList<>();

        AirportSearchNode root = AirportSearchNode.root(
                request.getSourceAirport(),
                FlightSearchUtils.haversineDistanceKm(request.getSourceAirport(), request.getDestinationAirport())
        );
        frontier.add(root);
        frontierAirportIds.add(root.getAirport().getId());

        int nodesCreated = 1;
        int iterations = 0;

        while (!frontier.isEmpty()) {
            iterations++;
            AirportSearchNode currentNode = frontier.poll();
            frontierAirportIds.remove(currentNode.getAirport().getId());

            if (exploredAirportIds.contains(currentNode.getAirport().getId())) {
                continue;
            }

            exploredAirportIds.add(currentNode.getAirport().getId());
            exploredAirports.add(currentNode.getAirport());

            if (currentNode.getAirport().getId() == request.getDestinationAirport().getId()) {
                if (verbose) {
                    printTrace(iterations, currentNode, frontier.size(), exploredAirports.size());
                }
                return SearchOutcome.success(currentNode, exploredAirports, nodesCreated, iterations);
            }

            for (FlightConnection connection: graph.getOutgoingConnections(currentNode.getAirport())) {
                Airport nextAirport = connection.getDestinationAirport();
                if (exploredAirportIds.contains(nextAirport.getId()) || frontierAirportIds.contains(nextAirport.getId())) {
                    continue;
                }

                double heuristic = FlightSearchUtils.haversineDistanceKm(nextAirport, request.getDestinationAirport());
                AirportSearchNode childNode = currentNode.createChild(connection, heuristic);
                frontier.add(childNode);
                frontierAirportIds.add(nextAirport.getId());
                nodesCreated++;
            }

            if (verbose) {
                printTrace(iterations, currentNode, frontier.size(), exploredAirports.size());
            }
        }

        return SearchOutcome.failure(exploredAirports, nodesCreated, iterations);
    }

    private SearchOutcome runAStarSearch(SearchRequest request, boolean verbose) {
        PriorityQueue<AirportSearchNode> frontier = createPriorityQueue();
        Map<Integer, Double> bestDistanceByAirport = new HashMap<>();
        Set<Integer> expandedAirportIds = new HashSet<>();
        ArrayList<Airport> exploredAirports = new ArrayList<>();

        double rootPriority = FlightSearchUtils.haversineDistanceKm(request.getSourceAirport(), request.getDestinationAirport());
        AirportSearchNode root = AirportSearchNode.root(request.getSourceAirport(), rootPriority);
        frontier.add(root);
        bestDistanceByAirport.put(root.getAirport().getId(), 0.0);

        int nodesCreated = 1;
        int iterations = 0;

        while (!frontier.isEmpty()) {
            iterations++;
            AirportSearchNode currentNode = frontier.poll();
            double bestDistance = bestDistanceByAirport.getOrDefault(currentNode.getAirport().getId(), Double.POSITIVE_INFINITY);
            if (currentNode.getCostSoFar() > bestDistance + 0.0001) {
                continue;
            }

            if (expandedAirportIds.contains(currentNode.getAirport().getId())) {
                continue;
            }

            expandedAirportIds.add(currentNode.getAirport().getId());
            exploredAirports.add(currentNode.getAirport());

            if (currentNode.getAirport().getId() == request.getDestinationAirport().getId()) {
                if (verbose) {
                    printTrace(iterations, currentNode, frontier.size(), exploredAirports.size());
                }
                return SearchOutcome.success(currentNode, exploredAirports, nodesCreated, iterations);
            }

            for (FlightConnection connection: graph.getOutgoingConnections(currentNode.getAirport())) {
                Airport nextAirport = connection.getDestinationAirport();
                if (expandedAirportIds.contains(nextAirport.getId())) {
                    continue;
                }

                double newDistance = currentNode.getCostSoFar() + connection.getDistanceKm();
                double knownDistance = bestDistanceByAirport.getOrDefault(nextAirport.getId(), Double.POSITIVE_INFINITY);
                if (newDistance + 0.0001 >= knownDistance) {
                    continue;
                }

                bestDistanceByAirport.put(nextAirport.getId(), newDistance);
                double heuristic = FlightSearchUtils.haversineDistanceKm(nextAirport, request.getDestinationAirport());
                AirportSearchNode childNode = currentNode.createChild(connection, newDistance + heuristic);
                frontier.add(childNode);
                nodesCreated++;
            }

            if (verbose) {
                printTrace(iterations, currentNode, frontier.size(), exploredAirports.size());
            }
        }

        return SearchOutcome.failure(exploredAirports, nodesCreated, iterations);
    }

    private PriorityQueue<AirportSearchNode> createPriorityQueue() {
        return new PriorityQueue<>(
                Comparator.comparingDouble(AirportSearchNode::getPriority)
                        .thenComparing(node -> node.getAirport().getDisplayName())
        );
    }

    private void printTrace(int iteration, AirportSearchNode currentNode, int frontierSize, int exploredCount) {
        System.out.println("Iteration #" + iteration + " -------------------------");
        System.out.println("Current airport: " + currentNode.getAirport());
        System.out.println("Current cost: " + FlightSearchUtils.formatDouble(currentNode.getCostSoFar()) + " km");
        System.out.println("Frontier size: " + frontierSize);
        System.out.println("Airports expanded: " + exploredCount);
    }

    private double priorityForVerbose(Airport sourceAirport, Airport destinationAirport) {
        return FlightSearchUtils.haversineDistanceKm(sourceAirport, destinationAirport);
    }

    private static class SearchOutcome {
        private final List<Airport> pathAirports;
        private final List<FlightConnection> pathConnections;
        private final List<Airport> exploredAirports;
        private final double totalDistanceKm;
        private final int nodesCreated;
        private final int nodesExpanded;
        private final int iterations;

        private SearchOutcome(
                List<Airport> pathAirports,
                List<FlightConnection> pathConnections,
                List<Airport> exploredAirports,
                double totalDistanceKm,
                int nodesCreated,
                int nodesExpanded,
                int iterations
        ) {
            this.pathAirports = pathAirports;
            this.pathConnections = pathConnections;
            this.exploredAirports = exploredAirports;
            this.totalDistanceKm = totalDistanceKm;
            this.nodesCreated = nodesCreated;
            this.nodesExpanded = nodesExpanded;
            this.iterations = iterations;
        }

        private static SearchOutcome success(AirportSearchNode goalNode, List<Airport> exploredAirports, int nodesCreated, int iterations) {
            ArrayList<Airport> airportPath = new ArrayList<>();
            ArrayList<FlightConnection> connectionPath = new ArrayList<>();
            double totalDistanceKm = 0.0;

            AirportSearchNode currentNode = goalNode;
            while (currentNode != null) {
                airportPath.add(0, currentNode.getAirport());
                if (currentNode.getIncomingConnection() != null) {
                    connectionPath.add(0, currentNode.getIncomingConnection());
                    totalDistanceKm += currentNode.getIncomingConnection().getDistanceKm();
                }
                currentNode = currentNode.getParent();
            }

            return new SearchOutcome(
                    airportPath,
                    connectionPath,
                    new ArrayList<>(exploredAirports),
                    totalDistanceKm,
                    nodesCreated,
                    exploredAirports.size(),
                    iterations
            );
        }

        private static SearchOutcome failure(List<Airport> exploredAirports, int nodesCreated, int iterations) {
            return new SearchOutcome(
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(exploredAirports),
                    0.0,
                    nodesCreated,
                    exploredAirports.size(),
                    iterations
            );
        }
    }

}
