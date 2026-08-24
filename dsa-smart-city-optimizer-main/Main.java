import java.util.*;
import java.io.*;

// ============================== CLASS: AccountManager ==============================
class AccountManager {
    private static final String FILE_NAME = "accounts.txt";
    private static Map<String, String> accounts = new HashMap<>();
    private static Map<String, String> roles = new HashMap<>();

    static {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    accounts.put(parts[0], parts[1]);
                    roles.put(parts[0], parts[2]);
                }
            }
        } catch (IOException e) {
            // File may not exist initially
        }
    }

    private static void saveAccount(String id, String password, String role) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            bw.write(id + "," + password + "," + role);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error saving account.");
        }
    }

    public static String signIn(Scanner sc) {
        System.out.print("Enter ID: ");
        String id = sc.next();
        System.out.print("Enter Password: ");
        String pw = sc.next();
        if (accounts.containsKey(id) && accounts.get(id).equals(pw)) {
            System.out.println("Login successful.");
            return id;
        } else {
            System.out.println("Invalid credentials.");
            return null;
        }
    }

    public static String createAccount(Scanner sc) {
        System.out.print("Enter new ID: ");
        String id = sc.next();
        if (accounts.containsKey(id)) {
            System.out.println("ID already exists.");
            return null;
        }
        System.out.print("Enter new Password: ");
        String pw = sc.next();
        System.out.print("Enter role (municipal/citizen): ");
        String role = sc.next().toLowerCase();

        if (!role.equals("municipal") && !role.equals("citizen")) {
            System.out.println("Invalid role.");
            return null;
        }

        accounts.put(id, pw);
        roles.put(id, role);
        saveAccount(id, pw, role);
        System.out.println("Account created successfully.");
        return id;
    }

    public static String getRole(String id) {
        return roles.get(id);
    }
}

// ============================== CLASS: Resource ==============================
class Resource {
    String type, id, driverName;
    boolean available = true;

    Resource(String type, String id, String driverName) {
        this.type = type;
        this.id = id;
        this.driverName = driverName;
    }
}

// ============================== CLASS: Request ==============================
class Request {
    String requesterID, type, location, status, allocatedResource;
    int priority;

    Request(String requesterID, String type, String location, int priority) {
        this.requesterID = requesterID;
        this.type = type;
        this.location = location;
        this.priority = priority;
        this.status = "Pending";
        this.allocatedResource = "None";
    }
}

// ============================== CLASS: LRUCache ==============================
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    LRUCache(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}

// ============================== CLASS: CityGraph (WEIGHTED) ==============================
class CityGraph {
    // Modified: adjacency map now stores Map<neighbor, distance>
    Map<String, Map<String, Integer>> adj = new HashMap<>();
    Map<String, List<Resource>> resources = new HashMap<>();
    
    // LRU Cache for shortest routes
    LRUCache<String, List<String>> routeCache = new LRUCache<>(10);
    
    // Double-ended queue for priority emergency dispatches
    Deque<Request> requestQueue = new ArrayDeque<>();
    
    // Decoupled listener/callback triggered when a queued request is auto-assigned
    java.util.function.Consumer<Request> onQueueDispatchListener = null;

    void addArea(String area) {
        adj.putIfAbsent(area, new HashMap<>());
        routeCache.clear();
    }

    // Modified: Add weighted road
    void addRoad(String a, String b, int distance) {
        if (!adj.containsKey(a) || !adj.containsKey(b)) {
            System.out.println("One or both areas not found.");
            return;
        }
        adj.get(a).put(b, distance);
        adj.get(b).put(a, distance);
        routeCache.clear();
        System.out.println("Road added between " + a + " and " + b + " with distance " + distance);
    }

    void addResourceCenter(String area) {
        resources.putIfAbsent(area, new ArrayList<>());
        System.out.println("Resource center added at " + area);
    }

    void addResource(String area, Resource r) {
        if (!resources.containsKey(area)) {
            System.out.println("Resource center not found for area.");
            return;
        }
        // Guard against duplicate IDs (e.g. if loadConfiguration is inadvertently
        // called more than once on the same CityGraph instance).
        for (Resource existing : resources.get(area)) {
            if (existing.id.equals(r.id)) {
                System.out.println("[Persistence] Skipping duplicate resource ID: " + r.id + " at " + area);
                return;
            }
        }
        resources.get(area).add(r);
        System.out.println("Resource added successfully at " + area);
    }

    void displayMap() {
        System.out.println("\n--- City Map ---");
        for (String a : adj.keySet()) {
            System.out.print(a + " -> ");
            for (Map.Entry<String, Integer> e : adj.get(a).entrySet()) {
                System.out.print(e.getKey() + "(" + e.getValue() + " km) ");
            }
            System.out.println();
        }
    }

    void showAllResources() {
        System.out.println("\n--- All Resources ---");
        for (String area : resources.keySet()) {
            System.out.println(area + ":");
            for (Resource r : resources.get(area)) {
                System.out.println("   " + r.type + " | ID: " + r.id + " | Driver: " + r.driverName +
                        " | Available: " + r.available);
            }
        }
    }

    // Modified: Dijkstra's algorithm for weighted shortest path with LRU caching
    List<String> shortestPath(String start, String end) {
        if (!adj.containsKey(start) || !adj.containsKey(end)) {
            return new ArrayList<>();
        }

        String cacheKey = start + "->" + end;
        if (routeCache.containsKey(cacheKey)) {
            System.out.println("[Cache Hit] Retrieved route from LRU cache: " + cacheKey);
            return new ArrayList<>(routeCache.get(cacheKey));
        }

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));

        for (String node : adj.keySet()) {
            dist.put(node, Integer.MAX_VALUE);
        }

        dist.put(start, 0);
        pq.add(start);

        while (!pq.isEmpty()) {
            String node = pq.poll();

            for (Map.Entry<String, Integer> e : adj.get(node).entrySet()) {
                String nei = e.getKey();
                int w = e.getValue();
                if (dist.get(node) + w < dist.get(nei)) {
                    dist.put(nei, dist.get(node) + w);
                    parent.put(nei, node);
                    pq.add(nei);
                }
            }
        }

        if (dist.get(end) == Integer.MAX_VALUE)
            return new ArrayList<>();

        List<String> path = new ArrayList<>();
        for (String at = end; at != null; at = parent.get(at))
            path.add(at);
        Collections.reverse(path);

        routeCache.put(cacheKey, path);
        return path;
    }

    /**
     * Returns true if the given request is compatible with the given resource.
     * Compatibility is defined as matching resource types (case-insensitive).
     * Keeping this isolated makes the dispatch logic easy to extend later.
     */
    private boolean isCompatible(Request req, Resource res) {
        return req.type.equalsIgnoreCase(res.type);
    }

    /**
     * Scans the pending requestQueue (in arrival order) and returns the best
     * request to dispatch to the given available resource, according to:
     *
     *   PRIMARY   : highest priority (lowest numeric value — 0 > 1 > 2)
     *   SECONDARY : resource compatibility (req.type must match res.type)
     *   TERTIARY  : FIFO / oldest arrival order wins ties
     *
     * The queue is maintained in pure arrival order (addLast only), so the
     * first compatible request encountered at a given priority level is
     * automatically the oldest one at that level — no secondary sort needed.
     *
     * Returns null if no compatible request exists (resource stays available).
     */
    private Request findBestRequestForResource(Resource res) {
        Request best = null;
        for (Request r : requestQueue) {
            if (!isCompatible(r, res)) {
                continue; // skip incompatible resource types
            }
            if (best == null) {
                best = r; // first compatible candidate
            } else if (r.priority < best.priority) {
                // lower numeric value = higher urgency; prefer this one
                best = r;
            }
            // equal priority: keep 'best' because it arrived earlier
            // (queue is in arrival order, so the earlier element was seen first)
        }
        return best;
    }

    Resource allocateResource(Request req) {
        String emergencyArea = req.location;
        String type = req.type;
        for (String area : resources.keySet()) {
            for (Resource r : resources.get(area)) {
                if (r.type.equalsIgnoreCase(type) && r.available) {
                    r.available = false;
                    List<String> path = shortestPath(area, emergencyArea);
                    System.out.println("\nAllocated " + r.type + " (" + r.id + ") from " + area);
                    System.out.println("Driver: " + r.driverName);
                    if (path.isEmpty()) {
                        System.out.println("No direct path found.");
                    } else {
                        System.out.println("Shortest path: " + path);
                    }
                    return r;
                }
            }
        }
        // No resource available — enqueue in arrival order.
        // IMPORTANT: always use addLast() so the queue reflects true arrival
        // order. Priority is resolved at dispatch time by findBestRequestForResource(),
        // NOT by reordering the queue here. Using addFirst() for high-priority
        // requests was the root cause of the FIFO-violation bug.
        req.status = "Queued";
        requestQueue.addLast(req);
        System.out.println("No available resource of type " + type + ". Request queued (Priority: " + req.priority + ").");
        return null;
    }

    void markComplete(String id) {
        Resource freedResource = null;
        String freedArea = null;
        for (String area : resources.keySet()) {
            for (Resource r : resources.get(area)) {
                if (r.id.equals(id)) {
                    r.available = true;
                    freedResource = r;
                    freedArea = area;
                    System.out.println("Task completed for " + r.id);
                    break;
                }
            }
            if (freedResource != null) break;
        }

        if (freedResource == null) {
            System.out.println("No resource found with given ID.");
            return;
        }

        // Use findBestRequestForResource() to select the optimal pending request.
        // This enforces: highest priority → compatible type → FIFO arrival order.
        // The old code used a simple forward-iterator first-match, which ignored
        // priority entirely and could dispatch a lower-priority request over a
        // higher-priority one that appeared later in the deque.
        Request pending = findBestRequestForResource(freedResource);

        if (pending == null) {
            // No compatible queued request — resource remains available.
            return;
        }

        // Dispatch: mark resource busy and update request state.
        freedResource.available = false;
        pending.allocatedResource = freedResource.id;
        pending.status = "Assigned";

        System.out.println("\n[Queue Dispatch] Automatically assigning freed resource " + freedResource.id + " to queued request from " + pending.requesterID);
        List<String> path = shortestPath(freedArea, pending.location);
        System.out.println("Driver: " + freedResource.driverName);
        if (path.isEmpty()) {
            System.out.println("No direct path found.");
        } else {
            System.out.println("Shortest path: " + path);
        }

        // Remove only the dispatched request; preserve all others in arrival order.
        // Iterator.remove() is the clean O(n) way to remove a specific element
        // from an ArrayDeque without disturbing the rest of the queue.
        Iterator<Request> it = requestQueue.iterator();
        while (it.hasNext()) {
            if (it.next() == pending) { // identity check — same object reference
                it.remove();
                break;
            }
        }

        // Trigger listener callback for persistence sync in GUI.
        if (onQueueDispatchListener != null) {
            onQueueDispatchListener.accept(pending);
        }
    }
}

// ============================== CLASS: HistoryManager ==============================
class HistoryManager {
    private static Map<Integer, Request> allRequests = new TreeMap<>();
    private static int requestCounter = 1;

    static void addRequest(Request req) {
        allRequests.put(requestCounter++, req);
    }

    static void updateStatus(String resourceID) {
        for (Request r : allRequests.values()) {
            if (r.allocatedResource.equals(resourceID)) {
                r.status = "Completed";
            }
        }
    }

    static void showMunicipalHistory() {
        System.out.println("\n--- All Requests History ---");
        if (allRequests.isEmpty()) {
            System.out.println("No requests recorded yet.");
            return;
        }
        for (Map.Entry<Integer, Request> e : allRequests.entrySet()) {
            Request r = e.getValue();
            System.out.println("Request ID: " + e.getKey() + " | Type: " + r.type + " | Location: " + r.location
                    + " | Status: " + r.status + " | Resource: " + r.allocatedResource + " | Requested by: " + r.requesterID);
        }
    }

    static void showUserHistory(String userID) {
        System.out.println("\n--- Your Requests ---");
        boolean found = false;
        for (Map.Entry<Integer, Request> e : allRequests.entrySet()) {
            Request r = e.getValue();
            if (r.requesterID.equals(userID)) {
                System.out.println("Request ID: " + e.getKey() + " | Type: " + r.type + " | Location: " + r.location
                        + " | Status: " + r.status + " | Resource: " + r.allocatedResource);
                found = true;
            }
        }
        if (!found) System.out.println("You have no requests yet.");
    }
}

// ============================== CLASS: CityPersistenceManager ==============================
/**
 * Responsible for reading and writing the city configuration (areas, roads,
 * resources) to/from text files in the data/ directory.
 *
 * Separation of concerns:
 *   CityGraph           → graph data + algorithms (unchanged)
 *   CityPersistenceManager → file I/O only
 *   GUI / Main          → user interaction only
 *
 * File layout (relative paths — works on any machine):
 *   data/cities.txt    — one area name per line
 *   data/edges.txt     — source,destination,distance  (undirected; stored once)
 *   data/resources.txt — area|type|id|driverName
 */
class CityPersistenceManager {

    static final String DATA_DIR      = "data";
    static final String CITIES_FILE   = DATA_DIR + File.separator + "cities.txt";
    static final String EDGES_FILE    = DATA_DIR + File.separator + "edges.txt";
    static final String RESOURCES_FILE = DATA_DIR + File.separator + "resources.txt";

    /** Creates the data/ directory if it does not already exist. */
    private static void ensureDataDir() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Loads cities, edges, and resources from the data/ files into the given
     * CityGraph. Must be called ONCE on a fresh CityGraph at application startup.
     * Missing files are silently skipped — empty configuration is the default.
     */
    static void loadConfiguration(CityGraph city) {
        ensureDataDir();
        loadCities(city);
        loadEdges(city);
        loadResources(city);
    }

    // -------- SAVE --------

    /**
     * Overwrites data/cities.txt with the current set of areas (adj.keySet()).
     * Called immediately after every successful addArea().
     */
    static void saveCities(CityGraph city) {
        ensureDataDir();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CITIES_FILE, false))) {
            for (String area : city.adj.keySet()) {
                bw.write(area);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("[Persistence] Error saving cities: " + e.getMessage());
        }
    }

    /**
     * Overwrites data/edges.txt with the current road network.
     * Each undirected edge is stored once (canonical order: A,B where A <= B).
     * CityGraph.addRoad() stores both directions, so calling addRoad(A,B,d) on
     * load correctly reconstructs A->B and B->A without duplication.
     * Called immediately after every successful addRoad().
     */
    static void saveEdges(CityGraph city) {
        ensureDataDir();
        Set<String> written = new HashSet<>();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(EDGES_FILE, false))) {
            for (Map.Entry<String, Map<String, Integer>> entry : city.adj.entrySet()) {
                String a = entry.getKey();
                for (Map.Entry<String, Integer> edge : entry.getValue().entrySet()) {
                    String b = edge.getKey();
                    int dist = edge.getValue();
                    // Use canonical key (smaller string first) to write each edge once.
                    String key = (a.compareTo(b) <= 0) ? a + "|" + b : b + "|" + a;
                    if (written.add(key)) {
                        bw.write(a + "," + b + "," + dist);
                        bw.newLine();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Persistence] Error saving edges: " + e.getMessage());
        }
    }

    /**
     * Overwrites data/resources.txt with all resources currently registered.
     * Format per line: area|type|id|driverName
     * Resources are always restored as AVAILABLE on the next startup (see loadResources).
     * Called immediately after every successful addResource().
     */
    static void saveResources(CityGraph city) {
        ensureDataDir();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(RESOURCES_FILE, false))) {
            for (Map.Entry<String, List<Resource>> entry : city.resources.entrySet()) {
                String area = entry.getKey();
                for (Resource r : entry.getValue()) {
                    bw.write(area + "|" + r.type + "|" + r.id + "|" + r.driverName);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("[Persistence] Error saving resources: " + e.getMessage());
        }
    }

    // -------- LOAD --------

    private static void loadCities(CityGraph city) {
        File f = new File(CITIES_FILE);
        if (!f.exists()) return; // no saved config yet — start empty
        int lineNum = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;
                city.addArea(line);
            }
        } catch (IOException e) {
            System.err.println("[Persistence] Error loading cities at line " + lineNum + ": " + e.getMessage());
        }
    }

    private static void loadEdges(CityGraph city) {
        File f = new File(EDGES_FILE);
        if (!f.exists()) return;
        int lineNum = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length != 3) {
                    System.err.println("[Persistence] Skipping invalid edge at line " + lineNum + ": " + line);
                    continue;
                }
                try {
                    String a = parts[0].trim();
                    String b = parts[1].trim();
                    int dist = Integer.parseInt(parts[2].trim());
                    city.addRoad(a, b, dist);
                } catch (NumberFormatException e) {
                    System.err.println("[Persistence] Skipping edge with non-integer distance at line " + lineNum + ": " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("[Persistence] Error loading edges at line " + lineNum + ": " + e.getMessage());
        }
    }

    /**
     * Restores resources from data/resources.txt.
     *
     * IMPORTANT — resources are always restored as AVAILABLE regardless of
     * their state when the application was last closed. This is intentional:
     * the application has no mechanism to persist active request assignments,
     * so restoring a resource as BUSY would create a phantom task with no
     * matching request record. AVAILABLE is the safe default; a municipal
     * operator can re-assign if needed.
     */
    private static void loadResources(CityGraph city) {
        File f = new File(RESOURCES_FILE);
        if (!f.exists()) return;
        int lineNum = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length != 4) {
                    System.err.println("[Persistence] Skipping invalid resource at line " + lineNum + ": " + line);
                    continue;
                }
                String area       = parts[0].trim();
                String type       = parts[1].trim();
                String id         = parts[2].trim();
                String driverName = parts[3].trim();
                if (area.isEmpty() || type.isEmpty() || id.isEmpty()) {
                    System.err.println("[Persistence] Skipping resource with empty fields at line " + lineNum);
                    continue;
                }
                // Ensure the area exists as a resource center.
                // addResourceCenter uses putIfAbsent, so safe to call even if already loaded.
                city.addResourceCenter(area);
                Resource r = new Resource(type, id, driverName);
                r.available = true; // always AVAILABLE on restart (see Javadoc above)
                city.addResource(area, r);
            }
        } catch (IOException e) {
            System.err.println("[Persistence] Error loading resources at line " + lineNum + ": " + e.getMessage());
        }
    }
}

// ============================== MAIN CLASS ==============================
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        CityGraph city = new CityGraph();
        // Load persisted city configuration (areas, roads, resources).
        // Runs once on a fresh CityGraph — no risk of duplicate data.
        CityPersistenceManager.loadConfiguration(city);

        while (true) {
            System.out.println("\n===== SMART CITY EMERGENCY RESOURCE OPTIMIZER =====");
            System.out.println("1. Login");
            System.out.println("2. Sign Up");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");
            int opt = sc.nextInt();

            String userID = null;
            if (opt == 1) {
                userID = AccountManager.signIn(sc);
            } else if (opt == 2) {
                userID = AccountManager.createAccount(sc);
            } else if (opt == 3) {
                System.out.println("Exiting system. Goodbye!");
                return;
            } else {
                System.out.println("Invalid choice.");
                continue;
            }

            if (userID == null)
                continue;

            String role = AccountManager.getRole(userID);
            if (role.equals("municipal")) {
                while (true) {
                    System.out.println("\n--- MUNICIPAL DASHBOARD ---");
                    System.out.println("1. Add Area");
                    System.out.println("2. Add Road");
                    System.out.println("3. Display City Map");
                    System.out.println("4. Add Resource Center");
                    System.out.println("5. Add Resource");
                    System.out.println("6. Show All Resources");
                    System.out.println("7. Mark Task Complete");
                    System.out.println("8. View All History");
                    System.out.println("9. Logout");
                    System.out.print("Enter choice: ");
                    int ch = sc.nextInt();
                    switch (ch) {
                        case 1 -> {
                            System.out.print("Enter area: ");
                            city.addArea(sc.next());
                            CityPersistenceManager.saveCities(city); // persist new area
                        }
                        case 2 -> {
                            System.out.print("Enter area1: ");
                            String a = sc.next();
                            System.out.print("Enter area2: ");
                            String b = sc.next();
                            System.out.print("Enter distance (km): ");
                            int d = sc.nextInt();
                            city.addRoad(a, b, d);
                            CityPersistenceManager.saveEdges(city); // persist updated road network
                        }
                        case 3 -> city.displayMap();
                        case 4 -> {
                            System.out.print("Enter center area: ");
                            city.addResourceCenter(sc.next());
                        }
                        case 5 -> {
                            System.out.print("Enter center: ");
                            String area = sc.next();
                            System.out.print("Enter type: ");
                            String type = sc.next();
                            System.out.print("Enter vehicle ID: ");
                            String id = sc.next();
                            System.out.print("Enter driver name: ");
                            String dn = sc.next();
                            city.addResource(area, new Resource(type, id, dn));
                            CityPersistenceManager.saveResources(city); // persist new resource
                        }
                        case 6 -> city.showAllResources();
                        case 7 -> {
                            System.out.print("Enter resource ID: ");
                            String id = sc.next();
                            city.markComplete(id);
                            HistoryManager.updateStatus(id);
                        }
                        case 8 -> HistoryManager.showMunicipalHistory();
                        case 9 -> {
                            System.out.println("Returning to Main Menu...");
                            break;
                        }
                        default -> System.out.println("Invalid choice");
                    }
                    if (ch == 9) break;
                }
            } else if (role.equals("citizen")) {
                while (true) {
                    System.out.println("\n--- CITIZEN DASHBOARD ---");
                    System.out.println("1. Show All Available Resources");
                    System.out.println("2. Create Emergency Request");
                    System.out.println("3. View My History");
                    System.out.println("4. Logout");
                    System.out.print("Enter choice: ");
                    int ch = sc.nextInt();
                    switch (ch) {
                        case 1 -> city.showAllResources();
                        case 2 -> {
                            System.out.print("Enter emergency area: ");
                            String ea = sc.next();
                            System.out.print("Enter resource type: ");
                            String type = sc.next();
                            System.out.print("Enter priority (0=High, 1=Medium, 2=Low): ");
                            int pri = sc.nextInt();
                            Request req = new Request(userID, type, ea, pri);
                            Resource allocated = city.allocateResource(req);
                            if (allocated != null) {
                                req.allocatedResource = allocated.id;
                                req.status = "Assigned";
                            }
                            HistoryManager.addRequest(req);
                        }
                        case 3 -> HistoryManager.showUserHistory(userID);
                        case 4 -> {
                            System.out.println("Returning to Main Menu...");
                            break;
                        }
                        default -> System.out.println("Invalid choice");
                    }
                    if (ch == 4) break;
                }
            }
        }
    }
}