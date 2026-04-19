# Flight Route Planner

A Java flight-route planner that loads real airport and route data from `airports.dat.txt` and `routes.dat.txt`, then runs `BFS`, `DFS`, `BestF`, and `AStar` on the resulting flight graph.

## What It Does

- Loads airport data from `airports.dat.txt`
- Loads route data from `routes.dat.txt`
- Builds a directed graph of direct flight connections
- Lets you choose source and destination airports in a Swing UI
- Runs `BFS`, `DFS`, `BestF`, or `AStar` to find a route
- Shows the airport path, leg-by-leg itinerary, total distance, runtime, and search stats

## Run The UI

From the project root:

```powershell
javac src\*.java
java -cp src A1Main
```

This opens the desktop UI and loads the dataset automatically. On this machine the app also auto-detects the installed JavaFX SDK at `C:\Program Files\javafx-sdk-24.0.1` and relaunches itself with the correct module path so the map can render inside the window.

The launcher now also enables the JavaFX native-access flags that newer JDKs warn about, so you should not see the `System::load`, `WebPage::twkInitWebCore`, or `sun.misc.Unsafe::allocateMemory` startup warnings during a normal UI launch.

If you want to force the embedded JavaFX WebView launch explicitly, use:

```powershell
.\run-ui.ps1
```

or on Command Prompt:

```bat
run-ui.bat
```

## Run From The Command Line

Use airport codes from the dataset, such as IATA or ICAO codes:

```powershell
java -cp src A1Main <DFS|BFS|BestF|AStar> <source airport code> <destination airport code>
```

Example:

```powershell
java -cp src A1Main AStar DEL LHR
```

## Notes

- The app searches over direct routes from the dataset
- Multi-stop journeys are built by chaining direct connections
- Routes with intermediate stops in a single record are skipped so each graph edge represents one direct leg
- Airport selection in the UI is sorted alphabetically by display label, starting with airport code
- The GUI now includes a route map panel; it uses embedded JavaFX WebView when JavaFX is available and falls back to opening the same generated map in your browser when it is not
- The embedded map page is served from `http://127.0.0.1` inside the app so OpenStreetMap tile requests include a valid HTTP referer
- You can override the detected JavaFX SDK location by setting `JAVAFX_HOME`
