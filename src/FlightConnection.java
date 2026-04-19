import java.util.LinkedHashSet;
import java.util.Set;


public class FlightConnection {

    private final Airport sourceAirport;
    private final Airport destinationAirport;
    private final double distanceKm;
    private final Set<String> airlineCodes = new LinkedHashSet<>();
    private int routeCount = 0;

   
    public FlightConnection(Airport sourceAirport, Airport destinationAirport, double distanceKm) {
        this.sourceAirport = sourceAirport;
        this.destinationAirport = destinationAirport;
        this.distanceKm = distanceKm;
    }

   
    public void addAirline(String airlineCode) {
        routeCount++;
        if (airlineCode != null && !airlineCode.trim().isEmpty()) {
            airlineCodes.add(airlineCode.trim().toUpperCase());
        }
    }

   
    public String getAirlineSummary(int limit) {
        if (airlineCodes.isEmpty()) {
            return routeCount + " route(s), airline code unavailable";
        }

        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String airlineCode: airlineCodes) {
            if (count > 0) {
                builder.append(", ");
            }
            builder.append(airlineCode);
            count++;
            if (count == limit) {
                break;
            }
        }

        if (airlineCodes.size() > limit) {
            builder.append(" ... +").append(airlineCodes.size() - limit).append(" more");
        }

        builder.append(" (").append(routeCount).append(" route(s))");
        return builder.toString();
    }

    
    public Airport getSourceAirport() {
        return sourceAirport;
    }

   
    public Airport getDestinationAirport() {
        return destinationAirport;
    }

   
    public double getDistanceKm() {
        return distanceKm;
    }

}
