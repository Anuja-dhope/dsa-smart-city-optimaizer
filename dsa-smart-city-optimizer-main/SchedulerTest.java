import java.util.*;

/**
 * SchedulerTest.java — standalone test (no JUnit required).
 *
 * Tests all 6 scheduling scenarios specified in the task brief.
 * Compile alongside Main.java:
 *   javac *.java
 * Run:
 *   java SchedulerTest
 *
 * Each test constructs a fresh CityGraph, populates requestQueue directly
 * (bypassing allocateResource so we control queue order precisely), then
 * calls a helper that wraps findBestRequestForResource() via markComplete().
 */
public class SchedulerTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        test1_FIFO();
        test2_Priority();
        test3_Compatibility();
        test4_PriorityAndCompatibility();
        test5_EqualPriorityFIFO_MixedResources();
        test6_NoCompatibleResource();

        System.out.println("\n============================");
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println("SOME TESTS FAILED");
        }
    }

    // ---------------------------------------------------------------
    // Helper: build a minimal graph with one area and one resource
    // ---------------------------------------------------------------
    private static CityGraph buildGraph(Resource res) {
        CityGraph g = new CityGraph();
        g.adj.put("Area1", new HashMap<>());
        g.resources.put("Area1", new ArrayList<>(Collections.singletonList(res)));
        return g;
    }

    // ---------------------------------------------------------------
    // Helper: directly enqueue requests in order (simulates arrival order)
    // ---------------------------------------------------------------
    private static void enqueue(CityGraph g, Request... reqs) {
        for (Request r : reqs) {
            r.status = "Queued";
            g.requestQueue.addLast(r); // exactly as allocateResource() now does
        }
    }

    // ---------------------------------------------------------------
    // Helper: simulate "resource freed", call markComplete() which internally
    // calls findBestRequestForResource() and dispatches.
    // Returns the request that was dispatched (null if none).
    // ---------------------------------------------------------------
    private static Request dispatch(CityGraph g, Resource res) {
        List<Request> before = new ArrayList<>(g.requestQueue);
        res.available = true; // mark as freed so markComplete finds it
        g.markComplete(res.id);
        // Find which request was removed from the queue
        for (Request r : before) {
            if (!g.requestQueue.contains(r)) {
                return r;
            }
        }
        return null; // nothing dispatched
    }

    // ---------------------------------------------------------------
    // TEST 1 - FIFO: same type, same priority
    // A, B, C (all Ambulance, P0, arrival order A then B then C)
    // Expected dispatch order: A -> B -> C
    // ---------------------------------------------------------------
    static void test1_FIFO() {
        System.out.println("\n--- TEST 1: FIFO ---");
        Resource amb = new Resource("Ambulance", "AMB-1", "Driver1");
        CityGraph g = buildGraph(amb);

        Request a = new Request("u1", "Ambulance", "Area1", 0);
        Request b = new Request("u2", "Ambulance", "Area1", 0);
        Request c = new Request("u3", "Ambulance", "Area1", 0);

        enqueue(g, a, b, c);

        Request d1 = dispatch(g, amb);
        amb.available = true;
        Request d2 = dispatch(g, amb);
        amb.available = true;
        Request d3 = dispatch(g, amb);

        boolean ok = d1 == a && d2 == b && d3 == c;
        printResult("TEST 1 - FIFO", ok,
            "Expected: A->B->C, Got: " + label(d1) + "->" + label(d2) + "->" + label(d3));
    }

    // ---------------------------------------------------------------
    // TEST 2 - Priority ordering
    // A(P1), B(P0), C(P2) - all Ambulance
    // Expected: B(P0) -> A(P1) -> C(P2)
    // ---------------------------------------------------------------
    static void test2_Priority() {
        System.out.println("\n--- TEST 2: Priority ---");
        Resource amb = new Resource("Ambulance", "AMB-2", "Driver2");
        CityGraph g = buildGraph(amb);

        Request a = new Request("u1", "Ambulance", "Area1", 1);
        Request b = new Request("u2", "Ambulance", "Area1", 0);
        Request c = new Request("u3", "Ambulance", "Area1", 2);

        enqueue(g, a, b, c); // queue: [A(P1), B(P0), C(P2)]

        Request d1 = dispatch(g, amb);
        amb.available = true;
        Request d2 = dispatch(g, amb);
        amb.available = true;
        Request d3 = dispatch(g, amb);

        boolean ok = d1 == b && d2 == a && d3 == c;
        printResult("TEST 2 - Priority", ok,
            "Expected: B(P0)->A(P1)->C(P2), Got: " + label(d1) + "->" + label(d2) + "->" + label(d3));
    }

    // ---------------------------------------------------------------
    // TEST 3 - Resource compatibility
    // Queue: A(Ambulance,P0), F(Fire Brigade,P0), B(Ambulance,P0)
    // Fire Brigade becomes available -> expects F dispatched; A,B remain
    // ---------------------------------------------------------------
    static void test3_Compatibility() {
        System.out.println("\n--- TEST 3: Resource Compatibility ---");
        Resource fire = new Resource("Fire Brigade", "FB-3", "Driver3");
        CityGraph g = buildGraph(fire);

        Request a = new Request("u1", "Ambulance", "Area1", 0);
        Request f = new Request("u2", "Fire Brigade", "Area1", 0);
        Request b = new Request("u3", "Ambulance", "Area1", 0);

        enqueue(g, a, f, b); // queue: [A, F, B]

        Request d1 = dispatch(g, fire);

        List<Request> remaining = new ArrayList<>(g.requestQueue);
        boolean ok = d1 == f && remaining.size() == 2
                     && remaining.get(0) == a && remaining.get(1) == b;
        printResult("TEST 3 - Compatibility", ok,
            "Expected F dispatched; remaining [A,B]. Got: dispatched=" + label(d1)
            + " remaining.size=" + remaining.size());
    }

    // ---------------------------------------------------------------
    // TEST 4 - Priority + Compatibility
    // Queue: A(Ambulance,P0), F(Fire Brigade,P0), B(Ambulance,P1), C(Fire Brigade,P1)
    // Fire Brigade available -> expects F (P0) dispatched, NOT A (wrong type)
    // ---------------------------------------------------------------
    static void test4_PriorityAndCompatibility() {
        System.out.println("\n--- TEST 4: Priority + Compatibility ---");
        Resource fire = new Resource("Fire Brigade", "FB-4", "Driver4");
        CityGraph g = buildGraph(fire);

        Request a = new Request("u1", "Ambulance",    "Area1", 0);
        Request f = new Request("u2", "Fire Brigade", "Area1", 0);
        Request b = new Request("u3", "Ambulance",    "Area1", 1);
        Request c = new Request("u4", "Fire Brigade", "Area1", 1);

        enqueue(g, a, f, b, c);

        Request d1 = dispatch(g, fire);

        boolean ok = d1 == f;
        printResult("TEST 4 - Priority+Compatibility", ok,
            "Expected F(Fire,P0). Got: " + label(d1));
    }

    // ---------------------------------------------------------------
    // TEST 5 - Equal priority FIFO with mixed resources
    // Queue: A(Ambulance,P0), F1(Fire Brigade,P0), F2(Fire Brigade,P0), B(Ambulance,P0)
    // Fire Brigade available -> F1 first, then F2
    // ---------------------------------------------------------------
    static void test5_EqualPriorityFIFO_MixedResources() {
        System.out.println("\n--- TEST 5: Equal-priority FIFO, mixed resources ---");
        Resource fire = new Resource("Fire Brigade", "FB-5", "Driver5");
        CityGraph g = buildGraph(fire);

        Request a  = new Request("u1", "Ambulance",    "Area1", 0);
        Request f1 = new Request("u2", "Fire Brigade", "Area1", 0);
        Request f2 = new Request("u3", "Fire Brigade", "Area1", 0);
        Request b  = new Request("u4", "Ambulance",    "Area1", 0);

        enqueue(g, a, f1, f2, b);

        Request d1 = dispatch(g, fire);
        fire.available = true;
        Request d2 = dispatch(g, fire);

        boolean ok = d1 == f1 && d2 == f2;
        printResult("TEST 5 - Equal-priority FIFO mixed", ok,
            "Expected F1 then F2. Got: " + label(d1) + " then " + label(d2));
    }

    // ---------------------------------------------------------------
    // TEST 6 - No compatible resource in queue
    // Queue: A(Ambulance,P0), B(Ambulance,P0)
    // Fire Brigade becomes available -> nothing dispatched, queue unchanged
    // ---------------------------------------------------------------
    static void test6_NoCompatibleResource() {
        System.out.println("\n--- TEST 6: No compatible resource ---");
        Resource fire = new Resource("Fire Brigade", "FB-6", "Driver6");
        CityGraph g = buildGraph(fire);

        Request a = new Request("u1", "Ambulance", "Area1", 0);
        Request b = new Request("u2", "Ambulance", "Area1", 0);

        enqueue(g, a, b);
        int sizeBefore = g.requestQueue.size();

        fire.available = true;
        g.markComplete(fire.id);

        int sizeAfter = g.requestQueue.size();
        boolean ok = sizeAfter == sizeBefore && fire.available;
        printResult("TEST 6 - No compatible resource", ok,
            "Expected queue size=" + sizeBefore + ", fire still available. "
            + "Got: size=" + sizeAfter + ", fire.available=" + fire.available);
    }

    // ---------------------------------------------------------------
    // Utility helpers
    // ---------------------------------------------------------------
    private static String label(Request r) {
        if (r == null) return "null";
        return r.requesterID + "(" + r.type + ",P" + r.priority + ")";
    }

    private static void printResult(String label, boolean ok, String detail) {
        if (ok) {
            System.out.println("  PASS  " + label);
            passed++;
        } else {
            System.out.println("  FAIL  " + label);
            System.out.println("        " + detail);
            failed++;
        }
    }
}
