# Smart City Emergency Resource Optimizer 🌆🚒🚑

An interactive, premium pastel-themed Java Swing desktop application designed to optimize and manage emergency resource allocation across a city using graph-based data structures and **Dijkstra's Weighted Shortest Path Algorithm**.

---

## Key Features 🌟

### 🗺️ Visual Graph Map & Routing
- **Dynamic City Graph Drawing**: Visualizes city areas (nodes) and connecting roads (weighted edges) on a custom-drawn graphical map.
- **Dijkstra's Algorithm Implementation**: Dynamically routes dispatch vehicles (Ambulances, Fire Brigades, Police) from the nearest resource center to the emergency scene using the absolute shortest weighted route.
- **Live Dispatch Path Logs**: Prints step-by-step route navigation (e.g. `[pune, karvenagar, sihgad]`) upon allocation.

### 👥 Dual-Role Security (Municipal & Citizen)
- **Municipal Dashboard (Admins)**:
  - Add new city areas.
  - Construct weighted roads between areas with specific distances.
  - Establish resource centers.
  - Register emergency resource vehicles with IDs and driver details.
  - Mark active emergency tasks as completed.
  - View full global audit history.
- **Citizen Dashboard (Users)**:
  - Request emergency resources (Ambulance, Fire Brigade, Police) with high/medium/low priority levels.
  - View personal request history and live vehicle assignment statuses.

### 🎨 State-of-the-Art Pastel UI/UX
- **User-Adjustable Divider (JSplitPane)**: Dynamically resize the vertical split between the log console and the graphical map. Built-in minimum height bounds prevent either panel from collapsing completely.
- **Input Error Prevention**: Replaced manual text typing with dropdown selectors (`JComboBox`) for selecting areas, resource centers, active task IDs, and resource types.
- **Responsive Layout**: Designed for high-DPI displays with a default window size of `1200 x 1000`.

---

## Technical Stack 🛠️

- **Core Logic**: Java 17+ (OOP, Collection framework, custom adjacency-map Graph representation, PriorityQueue for Dijkstra's search)
- **Frontend GUI**: Java Swing & AWT (custom graphics rendering, JSplitPane, layout management, anti-aliasing)
- **Storage**: Lightweight local file-based database (`accounts.txt` and `requests.txt`) for persistent credentials and request histories.

---

## Getting Started 🚀

### Prerequisites
Make sure you have **Java Development Kit (JDK) 17** or higher installed.

### Compilation
Compile the source code in your terminal:
```bash
javac Main.java MainGUIPastel.java
```

### Run the Application
Start the interactive GUI application:
```bash
java MainGUIPastel
```

---

## Demo Accounts 🔑
To quickly test the dashboards, you can use these default credentials (or create your own using the **Sign Up** button):

- **Municipal (Admin)**: 
  - User ID: `123` | Password: `hello`
  - User ID: `12` | Password: `21`
- **Citizen (User)**: 
  - User ID: `77` | Password: `hello`
  - User ID: `2332` | Password: `32`
