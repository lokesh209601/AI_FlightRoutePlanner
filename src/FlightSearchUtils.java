public class FlightSearchUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public static String normalizeDataValue(String value) {
        if (value == null) {
            return "";
        }

        String normalizedValue = value.trim();
        if (normalizedValue.equals("\\N")) {
            return "";
        }

        return normalizedValue;
    }

    public static Integer parseInteger(String value) {
        String normalizedValue = normalizeDataValue(value);
        if (normalizedValue.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(normalizedValue);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double parseDouble(String value) {
        String normalizedValue = normalizeDataValue(value);
        if (normalizedValue.isEmpty()) {
            return null;
        }

        try {
            return Double.parseDouble(normalizedValue);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public static double haversineDistanceKm(Airport sourceAirport, Airport destinationAirport) {
        double latitudeDelta = Math.toRadians(destinationAirport.getLatitude() - sourceAirport.getLatitude());
        double longitudeDelta = Math.toRadians(destinationAirport.getLongitude() - sourceAirport.getLongitude());
        double sourceLatitude = Math.toRadians(sourceAirport.getLatitude());
        double destinationLatitude = Math.toRadians(destinationAirport.getLatitude());

        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(sourceLatitude) * Math.cos(destinationLatitude)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public static String formatDouble(double value) {
        return String.format("%.3f", value);
    }

    public static double estimateFlightHours(double distanceKm) {
        final double averageCruiseSpeedKmph = 700.0;
        return distanceKm / averageCruiseSpeedKmph;
    }

    public static double estimateFuelLiters(double distanceKm) {
        final double fuelBurnLitersPerKm = 3.5;
        double fuelLiters = distanceKm * fuelBurnLitersPerKm;
        return fuelLiters / 1000.0;
    }

    public static double calculateFuelCostINR(double fuelKL) {
        final double costPerKL = 150000.0;
        return fuelKL * costPerKL;
    }

    public static String formatCurrency(double amountINR) {
        double amountInLakhs = amountINR / 100000.0;
        return String.format("%.2f", amountInLakhs);
    }

    public static String formatFlightDuration(double hours) {
        int totalMinutes = (int) Math.round(hours * 60);
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        return h + "h " + m + "m";
    }

}
