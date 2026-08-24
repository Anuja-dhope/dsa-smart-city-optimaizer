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
        // If not allocated, add to queue+
        req.status = "Queued";
        if (req.priority == 0) {
            requestQueue.addFirst(req);
        } else {
            requestQueue.addLast(req);
        }
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

        // Check Deque requestQueue for any matching queued requests
        Iterator<Request> it = requestQueue.iterator();
        while (it.hasNext()) {
            Request pending = it.next();
            if (pending.type.equalsIgnoreCase(freedResource.type)) {
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

                // Trigger listener callback for persistence sync in GUI
                if (onQueueDispatchListener != null) {
                    onQueueDispatchListener.accept(pending);
                }

                it.remove();
                break;
            }
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

// ============================== MAIN CLASS ==============================
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        CityGraph city = new CityGraph();

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
                        }
                        case 2 -> {
                            System.out.print("Enter area1: ");
                            String a = sc.next();
                            System.out.print("Enter area2: ");
                            String b = sc.next();
                            System.out.print("Enter distance (km): ");
                            int d = sc.nextInt();
                            city.addRoad(a, b, d);
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