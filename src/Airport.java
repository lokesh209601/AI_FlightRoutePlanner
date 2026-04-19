public class Airport implements Comparable<Airport> {

    private final int id;
    private final String name;
    private final String city;
    private final String country;
    private final String iataCode;
    private final String icaoCode;
    private final double latitude;
    private final double longitude;

    public Airport(int id, String name, String city, String country, String iataCode, String icaoCode, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.country = country;
        this.iataCode = iataCode;
        this.icaoCode = icaoCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getPrimaryCode() {
        if (!iataCode.isEmpty()) {
            return iataCode;
        }
        if (!icaoCode.isEmpty()) {
            return icaoCode;
        }
        return "ID" + id;
    }

    public String getDisplayName() {
        return getPrimaryCode() + " - " + name + " (" + city + ", " + country + ")";
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public int compareTo(Airport other) {
        return this.getDisplayName().compareToIgnoreCase(other.getDisplayName());
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getIataCode() {
        return iataCode;
    }

    public String getIcaoCode() {
        return icaoCode;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

}
