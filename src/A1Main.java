
import java.awt.GraphicsEnvironment;
import java.io.IOException;

public class A1Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            if (GraphicsEnvironment.isHeadless()) {
                System.err.println("GUI mode is not available in this environment.");
                System.err.println(Helper.getUsageMessage());
                System.exit(1);
            }

            if (JavaFxRuntimeBootstrap.relaunchWithJavaFxIfNeeded(A1Main.class, args)) {
                return;
            }

            FlightRoutePlannerUI.launch();
            return;
        }

        try {
            FlightGraph graph = new FlightDataLoader().loadDefaultGraph();
            SearchRequest request = SearchRequest.fromArgs(args, graph);
            SearchRunner searchRunner = new SearchRunner(graph);

            System.out.println(graph.getDatasetSummary());
            System.out.println("Search type: " + request.getSearchType());
            System.out.println("Source airport: " + request.getSourceAirport());
            System.out.println("Destination airport: " + request.getDestinationAirport());
            System.out.println("Starting " + SearchRunner.getSearchLabel(request.getSearchType()) + "...\n");

            SearchResult result = searchRunner.run(request, true);
            System.out.print(result.toConsoleSummary());

            if (!result.hasSolution()) {
                System.exit(1);
            }
        }
        catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            if (!Helper.getUsageMessage().equals(e.getMessage())) {
                System.err.println(Helper.getUsageMessage());
            }
            System.exit(1);
        }
        catch (IOException e) {
            System.err.println("Failed to load airport data: " + e.getMessage());
            System.exit(1);
        }
    }

}
