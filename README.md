# Flight Route Planner  @lokesh209601

git remote add origin https://github.com/lokesh209601/AI_FlightRoutePlanner.git

## What It Does

- Loads airport data from `airports.dat.txt`
- Loads route data from `routes.dat.txt`
- Builds a directed graph of direct flight connections
- Lets you choose source and destination airports in a Swing UI
- Runs `BFS`, `DFS`, `BestF`, `AStar`, `Dijkstra`, or `BellmanFord` to find a route
- Shows the airport path, leg-by-leg itinerary, total distance, fuel consumption (in KL), fuel cost (in lakhs), runtime, and search stats

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
java -cp src A1Main <DFS|BFS|BestF|AStar|Dijkstra|BellmanFord> <source airport code> <destination airport code>
```

Example:

```powershell
java -cp src A1Main Dijkstra DEL LHR
```

## Search Algorithms

The app supports six search algorithms for finding flight routes:

- **DFS** (Depth-First Search): Explores paths deeply before backtracking; explores all airports
- **BFS** (Breadth-First Search): Explores airports level-by-level; explores all airports  
- **BestF** (Greedy Best-First Search): Uses straight-line distance heuristic to prioritize promising paths; may not find shortest path
- **AStar** (A* Search): Combines actual distance with heuristic estimates for optimal pathfinding
- **Dijkstra** (Dijkstra's Algorithm): Guarantees shortest path by relaxing all edges; uses priority queue ordering by cumulative distance
- **BellmanFord** (Bellman-Ford Algorithm): Finds shortest paths using edge relaxation; handles negative weights (if any)

## Fuel Metrics

Route results now display:
- **Fuel Consumption**: Calculated in kiloliters (KL) based on distance and aircraft efficiency (3.5 L/km at 700 km/h cruise speed)
- **Fuel Cost**: Estimated cost in lakhs (1 lakh = 100,000 INR) at ₹150,000 per kiloliter

## Notes

- The app searches over direct routes from the dataset
- Multi-stop journeys are built by chaining direct connections
- Routes with intermediate stops in a single record are skipped so each graph edge represents one direct leg
- Airport selection in the UI is sorted alphabetically by display label, starting with airport code
- The GUI now includes a route map panel; it uses embedded JavaFX WebView when JavaFX is available and falls back to opening the same generated map in your browser when it is not
- The embedded map page is served from `http://127.0.0.1` inside the app so OpenStreetMap tile requests include a valid HTTP referer
- You can override the detected JavaFX SDK location by setting `JAVAFX_HOME`
