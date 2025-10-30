import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*; // ✅ Import added for Files.readAllLines() and Files.write()

public class HealthServer {
    private static final String FILE_PATH = "records.txt";

    public static void main(String[] args) throws IOException {
        int port = 8080;
        ServerSocket server = new ServerSocket(port);
        System.out.println("Server running on http://localhost:" + port);

        while (true) {
            Socket socket = server.accept();
            new Thread(() -> handleClient(socket)).start();
        }
    }

    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String line = in.readLine();
            if (line == null) return;
            // Parse request line: e.g. "GET /index.html HTTP/1.1"
            String[] requestParts = line.split(" ");
            String method = requestParts.length > 0 ? requestParts[0] : "";
            String path = requestParts.length > 1 ? requestParts[1] : "/";

            if (method.equals("GET")) {
                // Serve static files (/, /index.html, /style.css, /script.js, etc.)
                if (path.equals("/") || path.endsWith(".html") || path.endsWith(".css") || path.endsWith(".js")) {
                    String fileName = path.equals("/") ? "index.html" : path.substring(1);
                    Path filePath = Paths.get(fileName);
                    if (Files.exists(filePath)) {
                        String content = new String(Files.readAllBytes(filePath));
                        String contentType = getContentType(fileName);
                        sendResponse(out, "200 OK", contentType, content);
                    } else {
                        sendResponse(out, "404 Not Found", "text/plain", fileName + " not found");
                    }
                } else if (path.equals("/favicon.ico")) {
                    sendResponse(out, "204 No Content", "", "");
                } else if (path.equals("/records")) {
                    handleGetRecords(out);
                } else {
                    sendResponse(out, "404 Not Found", "text/plain", "Invalid endpoint");
                }
            } else if (method.equals("POST")) {
                if (path.equals("/add")) {
                    handleAddRecord(in, out);
                } else if (path.equals("/delete")) {
                    handleDeleteRecord(in, out);
                } else {
                    sendResponse(out, "404 Not Found", "text/plain", "Invalid endpoint");
                }
            } else {
                sendResponse(out, "400 Bad Request", "text/plain", "Unsupported method");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🟢 Add Record
    private static void handleAddRecord(BufferedReader in, BufferedWriter out) throws IOException {
        int contentLength = 0;
        String line;
        while (!(line = in.readLine()).isEmpty()) {
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }

        char[] body = new char[contentLength];
        in.read(body);
        String requestBody = new String(body);

        String[] parts = requestBody.split("&");
        String name = parts[0].split("=")[1];
        String age = parts[1].split("=")[1];
        String gender = parts[2].split("=")[1];
        String disease = parts[3].split("=")[1];

        try (FileWriter fw = new FileWriter(FILE_PATH, true)) {
            fw.write(name + "," + age + "," + gender + "," + disease + "\n");
        }

        sendResponse(out, "200 OK", "text/plain", "Record added successfully");
    }

    // 🟢 Get All Records
    private static void handleGetRecords(BufferedWriter out) throws IOException {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            sendResponse(out, "200 OK", "application/json", "[]");
            return;
        }

        List<String> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int id = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4)
                    records.add(String.format("{\"id\":%d,\"name\":\"%s\",\"age\":\"%s\",\"gender\":\"%s\",\"disease\":\"%s\"}",
                            id++, parts[0], parts[1], parts[2], parts[3]));
            }
        }

        String json = "[" + String.join(",", records) + "]";
        sendResponse(out, "200 OK", "application/json", json);
    }

    // 🟠 Delete Record
    private static void handleDeleteRecord(BufferedReader in, BufferedWriter out) throws IOException {
        int contentLength = 0;
        String line;
        while (!(line = in.readLine()).isEmpty()) {
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }

        char[] body = new char[contentLength];
        in.read(body);
        String requestBody = new String(body);
        int id = Integer.parseInt(requestBody.split("=")[1]);

        File file = new File(FILE_PATH);
        if (!file.exists()) {
            sendResponse(out, "200 OK", "text/plain", "No records to delete");
            return;
        }

        List<String> lines = new ArrayList<>(Files.readAllLines(file.toPath())); // ✅ Works now
        if (id >= 0 && id < lines.size()) {
            lines.remove(id);
            Files.write(file.toPath(), lines); // ✅ Works now
            sendResponse(out, "200 OK", "text/plain", "Record deleted successfully");
        } else {
            sendResponse(out, "400 Bad Request", "text/plain", "Invalid ID");
        }
    }

    // 🟣 Common Response Sender
    private static void sendResponse(BufferedWriter out, String status, String type, String body) throws IOException {
        out.write("HTTP/1.1 " + status + "\r\n");
        out.write("Content-Type: " + type + "\r\n");
        out.write("Access-Control-Allow-Origin: *\r\n");
        out.write("Content-Length: " + body.length() + "\r\n");
        out.write("\r\n");
        out.write(body);
        out.flush();
    }

    // Helper to map filename to Content-Type
    private static String getContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "text/plain";
    }
}