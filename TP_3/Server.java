import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    // Mets 6666 si ton TP l’exige, sinon garde 5000
    private static final int PORT = 5000;
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

    // Exo 5: nom du serveur
    private static final String SERVER_NAME = "MonServeurTP3";

    // Exo 5: table code -> message
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
                // Exo 5: réponses d’erreur avec en-tête conforme
                if (code == 400) sendErrorResponse(out, dataOut, 400, clientInfo);
                else if (code == 405) sendErrorResponse(out, dataOut, 405, clientInfo);
                else sendErrorResponse(out, dataOut, 500, clientInfo);
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

    // Exo 4: réception requête complète jusqu'à la ligne vide
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

    // Exo 4: validation -> 400 / 405 / 200
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

    // Exo 5: méthode surchargée pour générer l’en-tête
    private String buildHttpHeader(int code) {
        return buildHttpHeader(code, null);
    }

    // Exo 5: surcharge code + taille (Content-Length seulement si fourni)
    private String buildHttpHeader(int code, Integer contentLength) {
        int finalCode = STATUS_MESSAGES.containsKey(code) ? code : 500;
        String message = STATUS_MESSAGES.get(finalCode);

        // Format demandé: "lun., 24 nov. 2025 09:45:39 CET"
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
        String day = DateTimeFormatter.ofPattern("EEE", Locale.FRENCH).format(now); // ex: "lun."
        String rest = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss z", Locale.FRENCH).format(now);
        String dateHeader = day + ", " + rest;

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(finalCode).append(" ").append(message).append("\r\n");
        sb.append("Date: ").append(dateHeader).append("\r\n");
        sb.append("Server: ").append(SERVER_NAME).append("\r\n");
        sb.append("Connection: close\r\n");
        if (contentLength != null) {
            sb.append("Content-Length: ").append(contentLength).append("\r\n");
        }
        // Exo 5: au début, systématiquement text/html
        sb.append("Content-Type: text/html\r\n");
        sb.append("\r\n"); // ligne vide
        return sb.toString();
    }

    private void parseAndRespond(String requestLine, PrintWriter out, OutputStream dataOut, String clientInfo) {
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

            // Cas TP: GET http://localhost:5000/foo.html HTTP/1.1
            if (path.startsWith("http://") || path.startsWith("https://")) {
                try {
                    URI uri = URI.create(path);
                    path = uri.getPath();
                    if (path == null || path.isEmpty()) path = "/";
                } catch (IllegalArgumentException e) {
                    sendErrorResponse(out, dataOut, 400, clientInfo);
                    return;
                }
            }

            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            String filePath = path.startsWith("/")
                    ? Server.WEB_ROOT + path
                    : Server.WEB_ROOT + "/" + path;

            File file = new File(filePath);

            // (Ton choix actuel) fichier absent => 400
            // Attention: l'exo 6 demandera plutôt 404
            if (!file.exists() || !file.isFile()) {
                sendErrorResponse(out, dataOut, 400, clientInfo);
                System.out.println("[" + clientInfo + "] Fichier absent (renvoyé 400): " + filePath);
                return;
            }

            byte[] fileContent = Files.readAllBytes(file.toPath());

            // Exo 5: en-tête conforme
            String header = buildHttpHeader(200, fileContent.length);
            out.print(header);
            out.flush();

            dataOut.write(fileContent);
            dataOut.flush();

            System.out.println("[" + clientInfo + "] 200 OK: " + filePath);

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

            byte[] content = errorPage.getBytes("UTF-8");

            // Exo 5: en-tête conforme (avec Content-Length)
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
