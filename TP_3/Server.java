import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("Serveur HTTP démarré sur le port " + PORT);
        System.out.println("Supporte 2 sites via Host:");
        System.out.println(" - Host: site1  -> dossier site1/");
        System.out.println(" - Host: site2  -> dossier site2/");
        System.out.println("===========================================\n");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("En attente de connexions...\n");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    Thread clientThread = new Thread(new ClientHandler(clientSocket));
                    clientThread.start();

                    System.out.println("[NOUVELLE CONNEXION] Client: " +
                            clientSocket.getInetAddress().getHostAddress() +
                            ":" + clientSocket.getPort());

                } catch (IOException e) {
                    System.err.println("[ERREUR] Erreur lors de l'acceptation du client: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("[ERREUR FATALE] Impossible de démarrer le serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket clientSocket;

    private static final String SERVER_NAME = "MonServeurTP3";

    private static final String SITE1_ROOT = "site1";
    private static final String SITE2_ROOT = "site2";

    private static final Map<String, String> HOST_TO_ROOT = new HashMap<>();
    static {
        HOST_TO_ROOT.put("site1", SITE1_ROOT);
        HOST_TO_ROOT.put("site1.local", SITE1_ROOT);
        HOST_TO_ROOT.put("localhost", SITE1_ROOT);

        HOST_TO_ROOT.put("site2", SITE2_ROOT);
        HOST_TO_ROOT.put("site2.local", SITE2_ROOT);
    }

    private static final Map<Integer, String> STATUS_MESSAGES = new HashMap<>();
    static {
        STATUS_MESSAGES.put(200, "OK");
        STATUS_MESSAGES.put(400, "Bad Request");
        STATUS_MESSAGES.put(404, "Not Found");
        STATUS_MESSAGES.put(405, "Method Not Allowed");
        STATUS_MESSAGES.put(500, "Internal Server Error");
    }

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        String clientInfo = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
                OutputStream dataOut = clientSocket.getOutputStream()
        ) {
            System.out.println("\n--- [" + Thread.currentThread().getName() + "] Traitement client: " + clientInfo + " ---");

            String request = receiveHttpRequest(in);

            if (request == null || request.isBlank()) {
                System.out.println("[" + clientInfo + "] Requête vide reçue");
                return;
            }

            System.out.println("[" + clientInfo + "] Requête complète reçue:\n" + request);

            int code = validateHttpRequest(request);
            if (code != 200) {
                if (code == 400) sendErrorResponse(out, dataOut, 400, clientInfo);
                else if (code == 405) sendErrorResponse(out, dataOut, 405, clientInfo);
                else sendErrorResponse(out, dataOut, 500, clientInfo);
                return;
            }

            String host = extractHost(request);
            if (host == null || host.isEmpty()) {

                sendErrorResponse(out, dataOut, 400, clientInfo);
                return;
            }

            String webRoot = HOST_TO_ROOT.get(host);
            if (webRoot == null) {
                sendErrorResponse(out, dataOut, 404, clientInfo);
                return;
            }

            String requestLine = request.split("\r\n")[0];
            parseAndRespond(requestLine, out, dataOut, clientInfo, webRoot);

        } catch (IOException e) {
            System.err.println("[ERREUR] [" + clientInfo + "] Erreur lors du traitement: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("[FERMETURE] Client: " + clientInfo);
            } catch (IOException e) {
                System.err.println("[ERREUR] Erreur lors de la fermeture: " + e.getMessage());
            }
        }
    }


    private String receiveHttpRequest(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();

        String line = in.readLine();
        if (line == null) return "";
        sb.append(line).append("\r\n");

        while ((line = in.readLine()) != null) {
            sb.append(line).append("\r\n");
            if (line.isEmpty()) break;
        }

        return sb.toString();
    }

    private int validateHttpRequest(String request) {
        if (request == null || request.isBlank()) return 400;

        String[] lines = request.split("\r\n");
        if (lines.length == 0 || lines[0].isBlank()) return 400;

        Pattern p = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+HTTP/(\\d\\.\\d)\\s*$");
        Matcher m = p.matcher(lines[0].trim());
        if (!m.matches()) return 400;

        String method = m.group(1);
        if (!"GET".equals(method)) return 405;

        boolean hasHost = false;
        for (int i = 1; i < lines.length; i++) {
            String l = lines[i];
            if (l.isEmpty()) break;
            if (l.toLowerCase().startsWith("host:")) {
                hasHost = true;
                break;
            }
        }
        if (!hasHost) return 400;

        return 200;
    }

    private String extractHost(String request) {
        String[] lines = request.split("\r\n");
        for (int i = 1; i < lines.length; i++) {
            String l = lines[i];
            if (l.isEmpty()) break;

            if (l.toLowerCase().startsWith("host:")) {
                String hostValue = l.substring(5).trim();
                int idx = hostValue.indexOf(':');
                if (idx >= 0) hostValue = hostValue.substring(0, idx);
                return hostValue.trim().toLowerCase();
            }
        }
        return null;
    }


    private String buildHttpHeader(int code) {
        return buildHttpHeader(code, null);
    }

    private String buildHttpHeader(int code, Integer contentLength) {
        int finalCode = STATUS_MESSAGES.containsKey(code) ? code : 500;
        String message = STATUS_MESSAGES.get(finalCode);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
        String dateHeader = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.FRENCH).format(now);

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(finalCode).append(" ").append(message).append("\r\n");
        sb.append("Date: ").append(dateHeader).append("\r\n");
        sb.append("Server: ").append(SERVER_NAME).append("\r\n");
        sb.append("Connection: close\r\n");
        if (contentLength != null) {
            sb.append("Content-Length: ").append(contentLength).append("\r\n");
        }
        sb.append("Content-Type: text/html\r\n");
        sb.append("\r\n");
        return sb.toString();
    }

    private void parseAndRespond(String requestLine, PrintWriter out, OutputStream dataOut, String clientInfo, String webRoot) {
        try {
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendErrorResponse(out, dataOut, 400, clientInfo);
                return;
            }

            String method = parts[0];
            String path = parts[1];

            if (!"GET".equals(method)) {
                sendErrorResponse(out, dataOut, 405, clientInfo);
                return;
            }

            if (path.startsWith("http://") || path.startsWith("https://")) {
                try {
                    URI uri = URI.create(path);
                    path = uri.getRawPath();
                    if (path == null || path.isEmpty()) path = "/";
                } catch (IllegalArgumentException e) {
                    sendErrorResponse(out, dataOut, 400, clientInfo);
                    return;
                }
            }

            int q = path.indexOf("?");
            if (q >= 0) path = path.substring(0, q);

            try {
                path = java.net.URLDecoder.decode(path, "UTF-8");
            } catch (Exception e) {
                sendErrorResponse(out, dataOut, 400, clientInfo);
                return;
            }

            if (path.contains("..")) {
                sendErrorResponse(out, dataOut, 400, clientInfo);
                return;
            }

            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            String filePath = path.startsWith("/")
                    ? webRoot + path
                    : webRoot + "/" + path;

            File file = new File(filePath);

            if (file.exists() && file.isDirectory()) {
                file = new File(file, "index.html");
            }

            if (!file.exists() || !file.isFile()) {
                sendErrorResponse(out, dataOut, 404, clientInfo);
                System.out.println("[" + clientInfo + "] 404 Not Found: " + file.getPath());
                return;
            }

            byte[] fileContent = Files.readAllBytes(file.toPath());

            String header = buildHttpHeader(200, fileContent.length);
            out.print(header);
            out.flush();

            dataOut.write(fileContent);
            dataOut.flush();

            System.out.println("[" + clientInfo + "] 200 OK (" + webRoot + "): " + file.getPath());

        } catch (Exception e) {
            System.err.println("[ERREUR] [" + clientInfo + "] " + e.getMessage());
            sendErrorResponse(out, dataOut, 500, clientInfo);
        }
    }

    private void sendErrorResponse(PrintWriter out, OutputStream dataOut, int code, String clientInfo) {
        try {
            String message = STATUS_MESSAGES.containsKey(code) ? STATUS_MESSAGES.get(code) : STATUS_MESSAGES.get(500);
            int finalCode = STATUS_MESSAGES.containsKey(code) ? code : 500;

            String errorPage =
                    "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Erreur " + finalCode + "</title>" +
                            "<style>body{font-family:Arial;text-align:center;padding:50px;}" +
                            "h1{color:#d32f2f;}</style></head><body>" +
                            "<h1>Erreur " + finalCode + "</h1>" +
                            "<p>" + message + "</p>" +
                            "<hr><p>" + SERVER_NAME + "</p></body></html>";

            byte[] content = errorPage.getBytes(StandardCharsets.UTF_8);

            String header = buildHttpHeader(finalCode, content.length);
            out.print(header);
            out.flush();

            dataOut.write(content);
            dataOut.flush();

            System.out.println("[" + clientInfo + "] Erreur envoyée: " + finalCode + " " + message);

        } catch (IOException e) {
            System.err.println("[ERREUR] Impossible d'envoyer la réponse d'erreur: " + e.getMessage());
        }
    }
}