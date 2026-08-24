import java.util.*;

/**
 * PersistenceTest.java -- validates the full persistence lifecycle.
 *
 * Compile and run from the project directory:
 *   javac *.java && java PersistenceTest
 *
 * Verifies:
 *   1. Cities A,B,C,D load correctly from data/cities.txt
 *   2. Edges load correctly and are bidirectional with correct weights
 *   3. Resources are restored as AVAILABLE at correct areas
 *   4. Dijkstra A->D produces [A,B,C,D] with distance 15 after load
 *   5. Double-load into the same graph does not create duplicate nodes
 */
public class PersistenceTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testLoadCities();
        testLoadEdges();
        testLoadResources();
        testDijkstraAfterLoad();
        testNoDuplicatesOnDoubleLoad();

        System.out.println("============================");
        System.out.println("Persistence Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL PERSISTENCE TESTS PASSED");
        else             System.out.println("SOME PERSISTENCE TESTS FAILED");
    }

    // TEST 1: cities.txt has A,B,C,D -- all must appear after load
    static void testLoadCities() {
        CityGraph g = new CityGraph();
        CityPersistenceManager.loadConfiguration(g);
        boolean ok = g.adj.containsKey("A") && g.adj.containsKey("B")
                  && g.adj.containsKey("C") && g.adj.containsKey("D");
        printResult("Load cities A,B,C,D present", ok);
        if (!ok) System.out.println("  Loaded keys: " + g.adj.keySet());
    }

    // TEST 2: edges.txt has A-B=5, B-C=3, C-D=7 -- stored bidirectionally
    static void testLoadEdges() {
        CityGraph g = new CityGraph();
        CityPersistenceManager.loadConfiguration(g);
        Integer ab = g.adj.get("A").get("B");
        Integer ba = g.adj.get("B").get("A");
        Integer bc = g.adj.get("B").get("C");
        Integer cd = g.adj.get("C").get("D");
        boolean ok = ab != null && ab == 5
                  && ba != null && ba == 5
                  && bc != null && bc == 3
                  && cd != null && cd == 7;
        printResult("Load edges bidirectional correct weights", ok);
        if (!ok) System.out.println("  A-B=" + ab + " B-A=" + ba + " B-C=" + bc + " C-D=" + cd);
    }

    // TEST 3: resources.txt -- AMB-01 at A and POL-01 at D, both AVAILABLE
    static void testLoadResources() {
        CityGraph g = new CityGraph();
        CityPersistenceManager.loadConfiguration(g);
        List<Resource> atA = g.resources.getOrDefault("A", Collections.emptyList());
        List<Resource> atD = g.resources.getOrDefault("D", Collections.emptyList());
        boolean ambAtA = false, polAtD = false;
        for (Resource r : atA) { if ("AMB-01".equals(r.id) && r.available) ambAtA = true; }
        for (Resource r : atD) { if ("POL-01".equals(r.id) && r.available) polAtD = true; }
        printResult("Resources AVAILABLE at correct areas AMB-01@A POL-01@D", ambAtA && polAtD);
        if (!ambAtA) System.out.println("  AMB-01 not found AVAILABLE at A. atA=" + atA.size() + " resources");
        if (!polAtD) System.out.println("  POL-01 not found AVAILABLE at D. atD=" + atD.size() + " resources");
    }

    // TEST 4: Dijkstra A->D after load should give [A, B, C, D] with total distance 15
    static void testDijkstraAfterLoad() {
        CityGraph g = new CityGraph();
        CityPersistenceManager.loadConfiguration(g);
        List<String> path = g.shortestPath("A", "D");
        int dist = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Map<String, Integer> nbrs = g.adj.get(path.get(i));
            if (nbrs != null) {
                Integer w = nbrs.get(path.get(i + 1));
                dist += (w != null) ? w : 0;
            }
        }
        List<String> expected = Arrays.asList("A", "B", "C", "D");
        boolean correctPath = path.equals(expected);
        boolean correctDist = dist == 15; // 5 + 3 + 7
        printResult("Dijkstra A-D path=[A,B,C,D] dist=15", correctPath && correctDist);
        if (!correctPath || !correctDist) System.out.println("  Got path=" + path + " dist=" + dist);
    }

    // TEST 5: calling loadConfiguration on same graph twice must not duplicate
    //         (addArea uses putIfAbsent, so re-adding the same city is a no-op)
    static void testNoDuplicatesOnDoubleLoad() {
        CityGraph g = new CityGraph();
        CityPersistenceManager.loadConfiguration(g);
        int sizeFirst = g.adj.size();
        int resFirst  = totalResources(g);

        // Second load on same instance -- putIfAbsent guards prevent duplication
        CityPersistenceManager.loadConfiguration(g);
        int sizeSecond = g.adj.size();
        int resSecond  = totalResources(g);

        boolean ok = sizeFirst == 4 && sizeSecond == 4
                  && resFirst == resSecond; // no extra resources added
        printResult("No duplicates on double load adj.size=4 resources unchanged", ok);
        if (!ok) System.out.println("  adj: " + sizeFirst + "->" + sizeSecond
                                  + "  res: " + resFirst + "->" + resSecond);
    }

    private static int totalResources(CityGraph g) {
        int n = 0;
        for (List<Resource> list : g.resources.values()) n += list.size();
        return n;
    }

    private static void printResult(String label, boolean ok) {
        if (ok) { System.out.println("  PASS  " + label); passed++; }
        else    { System.out.println("  FAIL  " + label); failed++; }
    }
}
