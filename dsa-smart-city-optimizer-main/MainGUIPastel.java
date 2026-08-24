import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * MainGUIPastel.java
 * Attractive pastel-themed Swing frontend with drawn city map.
 *
 * Put this file in the same directory as your backend classes:
 * AccountManager, CityGraph, Resource, Request, HistoryManager, etc.
 *
 * Compile:
 *   javac *.java
 * Run:
 *   java MainGUIPastel
 */
public class MainGUIPastel {

    // backend objects (assumed available in same package)
    private static CityGraph city = new CityGraph();
    private static String currentUser = null;
    private static String currentRole = null;
    private static final String REQUESTS_FILE = "requests.txt";

    // Pastel palette
    private static final Color BG = Color.decode("#FFF8E7");        // cream
    private static final Color PANEL1 = Color.decode("#E8EAF6");     // lavender
    private static final Color PANEL2 = Color.decode("#E0F7FA");     // mint
    private static final Color BTN1 = Color.decode("#FFCDD2");       // coral
    private static final Color BTN2 = Color.decode("#C8E6C9");       // mint green
    private static final Color BTN3 = Color.decode("#BBDEFB");       // sky blue
    private static final Color TEXT = Color.decode("#37474F");      // dark gray
    private static final Color NODE_FILL = Color.decode("#F3E5F5"); // soft purple
    private static final Color EDGE_COLOR = Color.decode("#B2DFDB"); // pale teal

    // UI components that need to be refreshed
    private static JTextArea outputArea;
    private static GraphPanel graphPanel;

    public static void main(String[] args) {
        // Load persisted city configuration before the GUI opens.
        // This populates the shared 'city' CityGraph so areas, roads, and resources
        // are immediately visible on the dashboard without manual re-entry.
        CityPersistenceManager.loadConfiguration(city);
        // Use Swing thread
        SwingUtilities.invokeLater(MainGUIPastel::createAndShowLogin);
    }

    // ---------------- LOGIN / SIGNUP ----------------
    private static void createAndShowLogin() {
        JFrame frame = new JFrame("Smart City — Pastel Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(480, 380);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("<html><center><span style='font-size:16pt'>Smart City<br>Emergency Resource Optimizer</span></center></html>", SwingConstants.CENTER);
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        root.add(title, BorderLayout.NORTH);

        JPanel center = new RoundedPanel(PANEL1, 12);
        center.setLayout(new GridBagLayout());
        center.setBorder(new EmptyBorder(16,16,16,16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField idField = new JTextField();
        JPasswordField pwField = new JPasswordField();

        gbc.gridx = 0; gbc.gridy = 0;
        center.add(new JLabel("User ID:"), gbc);
        gbc.gridx = 1;
        center.add(idField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        center.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        center.add(pwField, gbc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setOpaque(false);
        JButton loginBtn = styledButton("Login", BTN2);
        JButton signupBtn = styledButton("Sign Up", BTN3);
        JButton exitBtn = styledButton("Exit", BTN1);
        btnRow.add(loginBtn); btnRow.add(signupBtn); btnRow.add(exitBtn);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        center.add(btnRow, gbc);

        root.add(center, BorderLayout.CENTER);

        // footer
        JLabel foot = new JLabel("Pastel UI • Click Sign Up to create an account", SwingConstants.CENTER);
        foot.setForeground(TEXT);
        root.add(foot, BorderLayout.SOUTH);

        // Actions
        loginBtn.addActionListener(e -> {
            String id = idField.getText().trim();
            String pw = new String(pwField.getPassword()).trim();
            if (id.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Enter both ID and password.", "Missing fields", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // simulate Scanner input for existing AccountManager.signIn(Scanner)
            Scanner sc = new Scanner(id + "\n" + pw + "\n");
            String res = AccountManager.signIn(sc);
            if (res != null) {
                currentUser = res;
                currentRole = AccountManager.getRole(currentUser);
                frame.dispose();
                createAndShowMainWindow();
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid credentials.", "Login failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        signupBtn.addActionListener(e -> {
            String id = idField.getText().trim();
            String pw = new String(pwField.getPassword()).trim();
            if (id.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Enter desired ID and password to sign up.", "Missing fields", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String[] opts = {"municipal", "citizen"};
            String role = (String) JOptionPane.showInputDialog(frame, "Choose role:", "Sign Up", JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);
            if (role == null) return;
            Scanner sc = new Scanner(id + "\n" + pw + "\n" + role + "\n");
            String created = AccountManager.createAccount(sc);
            if (created != null) {
                JOptionPane.showMessageDialog(frame, "Account created! Please login.");
            } else {
                JOptionPane.showMessageDialog(frame, "Account creation failed (ID exists or invalid role).", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        exitBtn.addActionListener(e -> System.exit(0));

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    // ---------------- MAIN WINDOW ----------------
    private static void createAndShowMainWindow() {
        JFrame frame = new JFrame("Smart Resource Manager Dashboard — " + currentUser);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 1000);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12,12));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(12,12,12,12));

        // Top bar
        JPanel topBar = new RoundedPanel(PANEL2, 10);
        topBar.setLayout(new BorderLayout());
        topBar.setPreferredSize(new Dimension(0, 62));
        JLabel appLabel = new JLabel("Smart City Resource Optimizer ");
        appLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        appLabel.setForeground(TEXT);
        topBar.add(appLabel, BorderLayout.WEST);

        JLabel roleLabel = new JLabel("Logged in: " + currentUser + " (" + currentRole + ")");
        roleLabel.setForeground(TEXT);
        topBar.add(roleLabel, BorderLayout.EAST);

        root.add(topBar, BorderLayout.NORTH);

        // Tabs: Dashboard & Map View
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabs.setBackground(BG);

        // Dashboard split: left controls, right output + map miniature
        JPanel dashboard = new JPanel(new BorderLayout(10,10));
        dashboard.setOpaque(false);

        JPanel controls = new RoundedPanel(PANEL1, 14);
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(new EmptyBorder(12,12,12,12));
        controls.setPreferredSize(new Dimension(320, 0));

        JLabel ctrlHeader = new JLabel("Actions");
        ctrlHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ctrlHeader.setForeground(TEXT);
        ctrlHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        controls.add(ctrlHeader);
        controls.add(Box.createVerticalStrut(10));

        // Buttons (shared but enabled/disabled by role)
        JButton btnAddArea = styledButton("Add Area", BTN3);
        JButton btnAddRoad = styledButton("Add Road (weighted)", BTN3);
        JButton btnAddCenter = styledButton("Add Resource Center", BTN3);
        JButton btnAddResource = styledButton("Add Resource", BTN2);
        JButton btnShowResources = styledButton("Show All Resources", BTN1);
        JButton btnMarkComplete = styledButton("Mark Task Complete", BTN2);
        JButton btnViewHistory = styledButton("View History", BTN1);
        JButton btnCreateRequest = styledButton("Create Emergency Request", BTN2);
        JButton btnLogout = styledButton("Logout", BTN1);

        // role-based enable/disable
        boolean isMunicipal = "municipal".equalsIgnoreCase(currentRole);
        btnAddArea.setEnabled(isMunicipal);
        btnAddRoad.setEnabled(isMunicipal);
        btnAddCenter.setEnabled(isMunicipal);
        btnAddResource.setEnabled(isMunicipal);
        btnMarkComplete.setEnabled(isMunicipal);
        btnViewHistory.setEnabled(true);
        btnShowResources.setEnabled(true);
        btnCreateRequest.setEnabled(!isMunicipal);

        // Add buttons with spacing
        for (JButton b : new JButton[]{btnAddArea, btnAddRoad, btnAddCenter, btnAddResource, btnShowResources, btnCreateRequest, btnMarkComplete, btnViewHistory, btnLogout}) {
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            controls.add(b);
            controls.add(Box.createVerticalStrut(8));
        }

        dashboard.add(controls, BorderLayout.WEST);

        // Right: output area + small map preview
        JPanel rightPanel = new RoundedPanel(PANEL2, 12);
        rightPanel.setLayout(new BorderLayout(8,8));
        rightPanel.setBorder(new EmptyBorder(10,10,10,10));

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputArea.setBackground(Color.white);
        outputArea.setForeground(TEXT);
        JScrollPane outScroll = new JScrollPane(outputArea);
        outScroll.setPreferredSize(new Dimension(600, 180));
        outScroll.setMinimumSize(new Dimension(100, 100));

        // Graph preview panel
        graphPanel = new GraphPanel(city);
        graphPanel.setPreferredSize(new Dimension(600, 700));
        graphPanel.setMinimumSize(new Dimension(100, 200));
        graphPanel.setBorder(new LineBorder(Color.decode("#E0E0E0"), 1, true));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outScroll, graphPanel);
        splitPane.setResizeWeight(0.2);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setBackground(PANEL2);

        rightPanel.add(splitPane, BorderLayout.CENTER);

        dashboard.add(rightPanel, BorderLayout.CENTER);

        // Map view tab (bigger)
        JPanel mapTab = new JPanel(new BorderLayout(10,10));
        mapTab.setBackground(BG);
        GraphPanel largeMap = new GraphPanel(city);
        largeMap.setPreferredSize(new Dimension(800, 780));
        JScrollPane mapScroll = new JScrollPane(largeMap);
        mapTab.add(mapScroll, BorderLayout.CENTER);

        // Add tabs
        tabs.addTab("Dashboard", dashboard);
        tabs.addTab("Map View", mapTab);

        root.add(tabs, BorderLayout.CENTER);

        // Events for buttons
        btnAddArea.addActionListener(e -> {
            String area = JOptionPane.showInputDialog(null, "Enter new area name:");
            if (area != null && !area.trim().isEmpty()) {
                city.addArea(area.trim());
                CityPersistenceManager.saveCities(city); // persist new area immediately
                appendOutput("Area added: " + area.trim());
                graphPanel.repaint();
            }
        });

        btnAddRoad.addActionListener(e -> {
            Object[] areas = city.adj.keySet().toArray();
            if (areas.length < 2) {
                JOptionPane.showMessageDialog(null, "Please add at least 2 areas first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JPanel p = new JPanel(new GridLayout(3,2,8,8));
            JComboBox<Object> cbA = new JComboBox<>(areas);
            JComboBox<Object> cbB = new JComboBox<>(areas);
            JTextField d = new JTextField();
            p.add(new JLabel("Area A:")); p.add(cbA);
            p.add(new JLabel("Area B:")); p.add(cbB);
            p.add(new JLabel("Distance (km):")); p.add(d);
            int r = JOptionPane.showConfirmDialog(null, p, "Add weighted road", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r == JOptionPane.OK_OPTION) {
                try {
                    String aa = (String) cbA.getSelectedItem();
                    String bb = (String) cbB.getSelectedItem();
                    if (aa.equals(bb)) {
                        JOptionPane.showMessageDialog(null, "Area A and Area B must be different.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    int dist = Integer.parseInt(d.getText().trim());
                    city.addRoad(aa, bb, dist);
                    CityPersistenceManager.saveEdges(city); // persist updated road network immediately
                    appendOutput("Road added: " + aa + " <-> " + bb + " (" + dist + " km)");
                    graphPanel.repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Invalid input for distance.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnAddCenter.addActionListener(e -> {
            Object[] areas = city.adj.keySet().toArray();
            if (areas.length == 0) {
                JOptionPane.showMessageDialog(null, "No areas available. Please add areas first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String area = (String) JOptionPane.showInputDialog(null, "Select center area:", "Add Resource Center",
                    JOptionPane.QUESTION_MESSAGE, null, areas, areas[0]);
            if (area != null) {
                city.addResourceCenter(area);
                appendOutput("Resource center added at " + area);
                graphPanel.repaint();
            }
        });

        btnAddResource.addActionListener(e -> {
            Object[] centers = city.resources.keySet().toArray();
            if (centers.length == 0) {
                JOptionPane.showMessageDialog(null, "No resource centers available. Please add resource centers first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JPanel p = new JPanel(new GridLayout(4,2,8,8));
            JComboBox<Object> cbCenter = new JComboBox<>(centers);
            String[] types = {"Ambulance", "Fire Brigade", "Police"};
            JComboBox<String> cbType = new JComboBox<>(types);
            JTextField id = new JTextField();
            JTextField driver = new JTextField();
            p.add(new JLabel("Center area:")); p.add(cbCenter);
            p.add(new JLabel("Resource type:")); p.add(cbType);
            p.add(new JLabel("Vehicle ID:")); p.add(id);
            p.add(new JLabel("Driver name:")); p.add(driver);
            int r = JOptionPane.showConfirmDialog(null, p, "Add Resource", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r == JOptionPane.OK_OPTION) {
                String centerArea = (String) cbCenter.getSelectedItem();
                String resType = (String) cbType.getSelectedItem();
                String vehId = id.getText().trim();
                String drvName = driver.getText().trim();
                if (vehId.isEmpty() || drvName.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Vehicle ID and Driver name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                city.addResource(centerArea, new Resource(resType, vehId, drvName));
                CityPersistenceManager.saveResources(city); // persist new resource immediately
                appendOutput("Resource " + vehId + " added at " + centerArea);
                graphPanel.repaint();
            }
        });

        btnShowResources.addActionListener(e -> outputArea.setText(getResourceString()));

        btnMarkComplete.addActionListener(e -> {
            List<String> busyIds = new ArrayList<>();
            for (List<Resource> list : city.resources.values()) {
                for (Resource res : list) {
                    if (!res.available) {
                        busyIds.add(res.id);
                    }
                }
            }
            if (busyIds.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No resources currently assigned to tasks.", "Information", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Object[] options = busyIds.toArray();
            String rid = (String) JOptionPane.showInputDialog(null, "Select resource ID to mark complete:", "Mark Task Complete",
                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (rid != null) {
                city.markComplete(rid);
                HistoryManager.updateStatus(rid);
                updateRequestsFileStatus(rid, "Completed");
                appendOutput("Resource " + rid + " marked complete.");
                graphPanel.repaint();
            }
        });

        btnViewHistory.addActionListener(e -> outputArea.setText(captureConsoleOutput(() -> HistoryManager.showMunicipalHistory())));

        btnCreateRequest.addActionListener(e -> {
            Object[] areas = city.adj.keySet().toArray();
            if (areas.length == 0) {
                JOptionPane.showMessageDialog(null, "No areas available in the city graph.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JPanel p = new JPanel(new GridLayout(3,2,8,8));
            JComboBox<Object> cbArea = new JComboBox<>(areas);
            String[] types = {"Ambulance", "Fire Brigade", "Police"};
            JComboBox<String> cbType = new JComboBox<>(types);
            String[] opts = {"0 - High", "1 - Medium", "2 - Low"};
            JComboBox<String> pri = new JComboBox<>(opts);
            p.add(new JLabel("Emergency area:")); p.add(cbArea);
            p.add(new JLabel("Resource type:")); p.add(cbType);
            p.add(new JLabel("Priority:")); p.add(pri);
            int r = JOptionPane.showConfirmDialog(null, p, "Create Emergency Request", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r == JOptionPane.OK_OPTION) {
                String emergencyArea = (String) cbArea.getSelectedItem();
                String resourceType = (String) cbType.getSelectedItem();
                int priority = pri.getSelectedIndex();
                Request req = new Request(currentUser, resourceType, emergencyArea, priority);
                Resource allocated = city.allocateResource(req);
                if (allocated != null) {
                    req.allocatedResource = allocated.id;
                    req.status = "Assigned";
                    JOptionPane.showMessageDialog(null, "Allocated: " + allocated.id + " (Driver: " + allocated.driverName + ")");
                } else {
                    JOptionPane.showMessageDialog(null, "No available resource of that type. Request has been queued.");
                }
                HistoryManager.addRequest(req);
                appendRequestToFile(req);
                appendOutput("Request created: " + req.type + " at " + req.location + " (status: " + req.status + ")");
                graphPanel.repaint();
            }
        });

        btnLogout.addActionListener(e -> {
            frame.dispose();
            currentUser = null;
            currentRole = null;
            createAndShowLogin();
        });

        // Finish
        frame.setContentPane(root);
        frame.setVisible(true);
        splitPane.setDividerLocation(0.2);

        // initial refresh
        graphPanel.repaint();
        outputArea.setText("--- Welcome to the Dashboard ---\n");

        // Sync queue dispatches with the GUI and database files
        city.onQueueDispatchListener = pending -> {
            appendOutput("[Queue Dispatch] Automatically assigned freed resource " + pending.allocatedResource + " to queued request from " + pending.requesterID);
            updateRequestsFileStatusForQueued(pending);
            graphPanel.repaint();
        };
    }

    // ---------------- Graph panel (drawn map) ----------------
    static class GraphPanel extends JPanel {
        private CityGraph cityGraph;
        private Map<String, Point> coords = new HashMap<>();

        GraphPanel(CityGraph cityGraph) {
            this.cityGraph = cityGraph;
            setBackground(BG);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(800, 500);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawGraph((Graphics2D)g);
        }

        private void drawGraph(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // build nodes list
            List<String> nodes = new ArrayList<>(cityGraph.adj.keySet());
            int n = nodes.size();
            int w = getWidth(), h = Math.max(getHeight(), 360);

            // place nodes on circles if not previously placed
            coords.clear();
            if (n == 0) return;

            int radius = Math.min(w, h) / 3;
            int cx = w/2, cy = h/2;
            for (int i=0;i<n;i++) {
                double angle = 2*Math.PI*i/n;
                int x = cx + (int)(radius*Math.cos(angle));
                int y = cy + (int)(radius*Math.sin(angle));
                coords.put(nodes.get(i), new Point(x,y));
            }

            // draw edges
            g2.setStroke(new BasicStroke(2f));
            for (String a : nodes) {
                Map<String,Integer> neighbors = cityGraph.adj.getOrDefault(a, Collections.emptyMap());
                Point pa = coords.get(a);
                if (pa == null) continue;
                for (Map.Entry<String,Integer> e : neighbors.entrySet()) {
                    String b = e.getKey();
                    Point pb = coords.get(b);
                    if (pb == null) continue;
                    // draw line (half alpha)
                    g2.setColor(new Color(EDGE_COLOR.getRed(), EDGE_COLOR.getGreen(), EDGE_COLOR.getBlue(), 180));
                    g2.drawLine(pa.x, pa.y, pb.x, pb.y);
                    // draw distance label near midpoint
                    int mx = (pa.x + pb.x)/2;
                    int my = (pa.y + pb.y)/2;
                    String dist = e.getValue() + " km";
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    g2.setColor(TEXT);
                    g2.drawString(dist, mx+4, my-4);
                }
            }

            // draw nodes
            int nodeSize = 48;
            for (String a : nodes) {
                Point p = coords.get(a);
                if (p == null) continue;
                Ellipse2D circle = new Ellipse2D.Double(p.x - nodeSize/2.0, p.y - nodeSize/2.0, nodeSize, nodeSize);
                g2.setColor(NODE_FILL);
                g2.fill(circle);
                g2.setColor(PANEL1.darker());
                g2.setStroke(new BasicStroke(2f));
                g2.draw(circle);
                // draw label
                g2.setColor(TEXT);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int labelW = fm.stringWidth(a);
                g2.drawString(a, p.x - labelW/2, p.y + nodeSize/2 + 14);
                // small resource dot count
                int count = cityGraph.resources.getOrDefault(a, Collections.emptyList()).size();
                if (count > 0) {
                    String s = String.valueOf(count);
                    int dotR = 14;
                    int dx = p.x + nodeSize/2 - dotR/2;
                    int dy = p.y - nodeSize/2 - dotR/2;
                    g2.setColor(new Color(255,255,255,220));
                    g2.fillOval(dx, dy, dotR, dotR);
                    g2.setColor(TEXT);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    int sw = g2.getFontMetrics().stringWidth(s);
                    g2.drawString(s, dx + (dotR - sw)/2, dy + dotR - 4);
                }
            }
        }
    }

    // ---------------- Utility & persistence ----------------
    private static JButton styledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(TEXT);
        b.setFocusPainted(false);
        b.setBorder(new RoundedBorder(10));
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return b;
    }

    private static void appendOutput(String s) {
        outputArea.append(s + "\n");
    }

    private static String getResourceString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Resource Centers & Resources ---\n");
        for (String area : city.resources.keySet()) {
            sb.append(area).append(":\n");
            for (Resource r : city.resources.get(area)) {
                sb.append("  • ").append(r.type).append(" | ID: ").append(r.id)
                  .append(" | Driver: ").append(r.driverName).append(" | Available: ").append(r.available).append("\n");
            }
        }
        if (city.resources.isEmpty()) sb.append("(No resource centers yet)\n");
        return sb.toString();
    }

    // capture System.out from methods that print to console (like HistoryManager)
    private static String captureConsoleOutput(Runnable r) {
        PrintStream old = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        try {
            r.run();
        } finally {
            System.out.flush();
            System.setOut(old);
        }
        return baos.toString();
    }

    // Append request to requests.txt for persistence
    private static void appendRequestToFile(Request req) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(REQUESTS_FILE, true))) {
            // use pipe-separated format
            String line = escape(req.requesterID) + "|" + escape(req.type) + "|" + escape(req.location) + "|" + req.priority + "|" + escape(req.status) + "|" + escape(req.allocatedResource);
            bw.write(line);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Update queued request in requests.txt to Assigned
    private static void updateRequestsFileStatusForQueued(Request req) {
        File infile = new File(REQUESTS_FILE);
        if (!infile.exists()) return;
        File temp = new File(REQUESTS_FILE + ".tmp");
        try (BufferedReader br = new BufferedReader(new FileReader(infile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(temp))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 6 && parts[0].equals(req.requesterID) &&
                    parts[1].equals(req.type) && parts[2].equals(req.location) &&
                    parts[4].equals("Queued")) {
                    parts[4] = "Assigned";
                    parts[5] = req.allocatedResource;
                    bw.write(String.join("|", parts));
                } else {
                    bw.write(line);
                }
                bw.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        if (!infile.delete()) {
            System.err.println("Could not delete original requests file.");
            return;
        }
        if (!temp.renameTo(infile)) {
            System.err.println("Could not rename temp requests file.");
        }
    }

    // Update status in requests.txt when marking complete
    private static void updateRequestsFileStatus(String resourceId, String newStatus) {
        File infile = new File(REQUESTS_FILE);
        if (!infile.exists()) return;
        File temp = new File(REQUESTS_FILE + ".tmp");
        try (BufferedReader br = new BufferedReader(new FileReader(infile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(temp))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 6 && parts[5].equals(resourceId)) {
                    parts[4] = newStatus;
                    bw.write(String.join("|", parts));
                } else {
                    bw.write(line);
                }
                bw.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        if (!infile.delete()) {
            System.err.println("Could not delete original requests file.");
            return;
        }
        if (!temp.renameTo(infile)) {
            System.err.println("Could not rename temp requests file.");
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("|", "/");
    }

    // ---------------- Small UI helper classes ----------------
    // Rounded panel
    static class RoundedPanel extends JPanel {
        private Color bg;
        private int arc;
        RoundedPanel(Color bg, int arc) {
            super();
            this.bg = bg;
            this.arc = arc;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0,0,getWidth(),getHeight(),arc,arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Rounded border for buttons
    static class RoundedBorder implements Border {
        private int radius;
        RoundedBorder(int radius) { this.radius = radius; }
        public Insets getBorderInsets(Component c) { return new Insets(this.radius+1, this.radius+1, this.radius+2, this.radius); }
        public boolean isBorderOpaque() { return false; }
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(new Color(220,220,220));
            g.drawRoundRect(x, y, width-1, height-1, radius, radius);
        }
    }
}
