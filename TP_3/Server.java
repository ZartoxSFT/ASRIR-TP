import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    private static final int PORT = 6666;
    public static final String WEB_ROOT = "www";

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("Serveur HTTP démarré sur le port " + PORT);
        System.out.println("Racine web: " + WEB_ROOT);
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

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        String clientInfo = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
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
                if (code == 400) sendErrorResponse(out, dataOut, 400, "Bad Request", clientInfo);
                else if (code == 405) sendErrorResponse(out, dataOut, 405, "Method Not Allowed", clientInfo);
                else sendErrorResponse(out, dataOut, 500, "Internal Server Error", clientInfo);
                return;
            }

            // OK -> on traite GET
            String requestLine = request.split("\r\n")[0];
            parseAndRespond(requestLine, out, dataOut, clientInfo);

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

    private void parseAndRespond(String requestLine, PrintWriter out, OutputStream dataOut, String clientInfo) {
        try {
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendErrorResponse(out, dataOut, 400, "Bad Request", clientInfo);
                return;
            }

            String method = parts[0];
            String path = parts[1];

            // Sécurité: si jamais on arrive ici avec autre chose que GET
            if (!"GET".equals(method)) {
                sendErrorResponse(out, dataOut, 405, "Method Not Allowed", clientInfo);
                return;
            }

            // IMPORTANT: gérer le format TP "GET http://localhost:6666/foo.html HTTP/1.1"
            if (path.startsWith("http://") || path.startsWith("https://")) {
                try {
                    URI uri = URI.create(path);
                    path = uri.getPath(); // ex: /index.html
                    if (path == null || path.isEmpty()) path = "/";
                } catch (IllegalArgumentException e) {
                    sendErrorResponse(out, dataOut, 400, "Bad Request", clientInfo);
                    return;
                }
            }

            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            String filePath;
            if (path.startsWith("/")) {
                filePath = Server.WEB_ROOT + path;
            } else {
                filePath = Server.WEB_ROOT + "/" + path;
            }

            File file = new File(filePath);

            // Tu as demandé: PAS de 404 -> donc fichier absent = 400
            if (!file.exists() || !file.isFile()) {
                sendErrorResponse(out, dataOut, 400, "Bad Request", clientInfo);
                System.out.println("[" + clientInfo + "] Fichier absent (renvoyé 400): " + filePath);
                return;
            }

            byte[] fileContent = Files.readAllBytes(file.toPath());
            String contentType = getContentType(filePath);

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: " + contentType);
            out.println("Content-Length: " + fileContent.length);
            out.println("Connection: close");
            out.println();
            out.flush();

            dataOut.write(fileContent);
            dataOut.flush();

            System.out.println("[" + clientInfo + "] 200 OK: " + filePath);

        } catch (Exception e) {
            System.err.println("[ERREUR] [" + clientInfo + "] " + e.getMessage());
            sendErrorResponse(out, dataOut, 500, "Internal Server Error", clientInfo);
        }
    }

    private String getContentType(String filePath) {
        if (filePath.endsWith(".html") || filePath.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css";
        if (filePath.endsWith(".js")) return "application/javascript";
        if (filePath.endsWith(".json")) return "application/json";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
        if (filePath.endsWith(".gif")) return "image/gif";
        if (filePath.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    private void sendErrorResponse(PrintWriter out, OutputStream dataOut, int code, String message, String clientInfo) {
        try {
            String errorPage = "<!DOCTYPE html><html><head><title>Erreur " + code + "</title>" +
                    "<style>body{font-family:Arial;text-align:center;padding:50px;}" +
                    "h1{color:#d32f2f;}</style></head><body>" +
                    "<h1>Erreur " + code + "</h1>" +
                    "<p>" + message + "</p>" +
                    "<hr><p>Serveur HTTP/1.1</p></body></html>";

            byte[] content = errorPage.getBytes("UTF-8");

            out.println("HTTP/1.1 " + code + " " + message);
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println("Content-Length: " + content.length);
            out.println("Connection: close");
            out.println();
            out.flush();

            dataOut.write(content);
            dataOut.flush();

            System.out.println("[" + clientInfo + "] Erreur envoyée: " + code + " " + message);
        } catch (IOException e) {
            System.err.println("[ERREUR] Impossible d'envoyer la réponse d'erreur: " + e.getMessage());
        }
    }
}
