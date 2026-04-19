import java.util.ArrayList;
import java.util.List;

public class MapHtmlBuilder {

    public static String buildRouteHtml(SearchResult result) {
        List<Airport> airportsToPlot = new ArrayList<>();
        if (result.hasSolution()) {
            airportsToPlot.addAll(result.getPathAirports());
        }
        else {
            airportsToPlot.add(result.getRequest().getSourceAirport());
            airportsToPlot.add(result.getRequest().getDestinationAirport());
        }

        StringBuilder markerScript = new StringBuilder();
        StringBuilder coordinateScript = new StringBuilder();

        for (int index = 0; index < airportsToPlot.size(); index++) {
            Airport airport = airportsToPlot.get(index);
            if (index > 0) {
                coordinateScript.append(",\n");
            }

            coordinateScript.append("[")
                    .append(airport.getLatitude())
                    .append(", ")
                    .append(airport.getLongitude())
                    .append("]");

            String labelPrefix;
            if (index == 0) {
                labelPrefix = "Start";
            }
            else if (index == airportsToPlot.size() - 1) {
                labelPrefix = "Destination";
            }
            else {
                labelPrefix = "Stop " + index;
            }

            markerScript.append("L.marker([")
                    .append(airport.getLatitude())
                    .append(", ")
                    .append(airport.getLongitude())
                    .append("]).addTo(map)")
                    .append(".bindPopup(")
                    .append(toJavaScriptString(labelPrefix + ": " + airport.getDisplayName()))
                    .append(");\n");
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
                .append("<html><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("<meta name=\"referrer\" content=\"strict-origin-when-cross-origin\">")
                .append("<title>Flight Route Map</title>")
                .append("<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />")
                .append("<style>")
                .append("html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; box-sizing: border-box; }")
                .append("body { font-family: sans-serif; background: #eef3f7; overflow: hidden; }")
                .append("#map { position: absolute; inset: 0; }")
                .append("</style></head><body>")
                .append("<div id=\"map\"></div>")
                .append("<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>")
                .append("<script>")
                .append("const route = [")
                .append(coordinateScript)
                .append("];")
                .append("let map = null;")
                .append("let tileLayer = null;")
                .append("let routeLine = null;")
                .append("function setMapSize() {")
                .append("  const mapElement = document.getElementById('map');")
                .append("  mapElement.style.width = '100%';")
                .append("  mapElement.style.height = '100%';")
                .append("  document.documentElement.style.height = '100%';")
                .append("  document.body.style.height = '100%';")
                .append("}")
                .append("function fitRouteToView() {");

        if (airportsToPlot.size() > 1) {
            html.append("  if (routeLine) {")
                    .append("    map.fitBounds(routeLine.getBounds().pad(0.25), { animate: false });")
                    .append("  }");
        }
        else if (airportsToPlot.size() == 1) {
            html.append("  if (route.length === 1) {")
                    .append("    map.setView(route[0], 5, { animate: false });")
                    .append("  }");
        }
        else {
            html.append("  map.setView([20, 0], 2, { animate: false });");
        }

        html.append("}")
                .append("function redrawMap() {")
                .append("  if (!map) { return; }")
                .append("  setMapSize();")
                .append("  map.invalidateSize(true);")
                .append("  fitRouteToView();")
                .append("}")
                .append("function initMap() {")
                .append("  if (map) {")
                .append("    redrawMap();")
                .append("    return;")
                .append("  }")
                .append("  setMapSize();")
                .append("  map = L.map('map', {")
                .append("    preferCanvas: true,")
                .append("    zoomAnimation: false,")
                .append("    fadeAnimation: false,")
                .append("    markerZoomAnimation: false,")
                .append("    inertia: false")
                .append("  });");

        html.append(markerScript);

        if (airportsToPlot.size() > 1) {
            html.append("  routeLine = L.polyline(route, {color: 'blue', weight: 4}).addTo(map);");
        }
        else {
            html.append("  routeLine = null;");
        }

        html.append("  fitRouteToView();")
                .append("  tileLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {")
                .append("    attribution: '&copy; OpenStreetMap contributors',")
                .append("    maxZoom: 19,")
                .append("    subdomains: 'abcd',")
                .append("    updateWhenIdle: true,")
                .append("    keepBuffer: 8")
                .append("  }).addTo(map);")
                .append("  window.flightRouteMap = map;")
                .append("  window.flightRouteLine = routeLine;")
                .append("  window.refreshFlightRouteMap = redrawMap;")
                .append("  setTimeout(redrawMap, 250);")
                .append("}")
                .append("window.addEventListener('load', function () {")
                .append("  requestAnimationFrame(function () {")
                .append("    setTimeout(initMap, 250);")
                .append("  });")
                .append("});")
                .append("window.addEventListener('resize', function () {")
                .append("  setTimeout(redrawMap, 150);")
                .append("});");

        html.append("</script></body></html>");
        return html.toString();
    }

    public static String buildPlaceholderHtml(String message) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>" +
                "html,body{height:100%;margin:0;font-family:sans-serif;background:#eef3f7;color:#234;display:flex;" +
                "align-items:center;justify-content:center;text-align:center;padding:24px;box-sizing:border-box;}" +
                "</style></head><body><div>" + escapeHtml(message) + "</div></body></html>";
    }

    private static String toJavaScriptString(String value) {
        return "'" + value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "") + "'";
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

}
