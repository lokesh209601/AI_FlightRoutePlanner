public class Helper {

    public static String getUsageMessage() {
        return "usage: java -cp src A1Main <DFS|BFS|BestF|AStar> <source airport code> <destination airport code>";
    }

    public static void errorMessage() {
        System.err.println(getUsageMessage());
        System.exit(1);
    }

}
