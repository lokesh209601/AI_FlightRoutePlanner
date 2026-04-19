public class AirportSearchNode {

    private final Airport airport;
    private final AirportSearchNode parent;
    private final FlightConnection incomingConnection;
    private final double costSoFar;
    private final double priority;

    public AirportSearchNode(Airport airport, AirportSearchNode parent, FlightConnection incomingConnection, double costSoFar, double priority) {
        this.airport = airport;
        this.parent = parent;
        this.incomingConnection = incomingConnection;
        this.costSoFar = costSoFar;
        this.priority = priority;
    }

    public static AirportSearchNode root(Airport airport, double priority) {
        return new AirportSearchNode(airport, null, null, 0.0, priority);
    }

    public AirportSearchNode createChild(FlightConnection connection, double priority) {
        return new AirportSearchNode(
                connection.getDestinationAirport(),
                this,
                connection,
                this.costSoFar + connection.getDistanceKm(),
                priority
        );
    }

    public Airport getAirport() {
        return airport;
    }

    public AirportSearchNode getParent() {
        return parent;
    }

    public FlightConnection getIncomingConnection() {
        return incomingConnection;
    }

    public double getCostSoFar() {
        return costSoFar;
    }

    public double getPriority() {
        return priority;
    }

}
