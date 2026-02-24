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
        //serveur qui écoute en continu et accepte les connexions
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("En attente de connexions...\n");

            while (true) {
                try {
                    // On accepte un client (socket TCP)
                    Socket clientSocket = serverSocket.accept();
                    // multi-clients => un thread par client
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
/**
 * Classe qui gère 1 client (dans un thread).
 * Elle reçoit une requête, la valide, choisit le site via Host, puis renvoie le fichier / l'erreur.
 */
class ClientHandler implements Runnable {
    private final Socket clientSocket;

    // nom du serveur affiché dans l'en-tête HTTP
    private static final String SERVER_NAME = "MonServeurTP3";

    // deux sites = deux dossiers racines
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

    /**
     * table code -> message.
     * Permet de construire la status line : "HTTP/1.1 404 Not Found"
     */
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

        // On récupère les flux (entrée / sortie)
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
                OutputStream dataOut = clientSocket.getOutputStream()
        ) {
            System.out.println("\n--- [" + Thread.currentThread().getName() + "] Traitement client: " + clientInfo + " ---");

            // lire une requête HTTP complète (jusqu'à ligne vide)
            String request = receiveHttpRequest(in);

            if (request == null || request.isBlank()) {
                System.out.println("[" + clientInfo + "] Requête vide reçue");
                return;
            }

            System.out.println("[" + clientInfo + "] Requête complète reçue:\n" + request);

            // validation (format + GET + Host)
            int code = validateHttpRequest(request);
            if (code != 200) {
                if (code == 400) sendErrorResponse(out, dataOut, 400, clientInfo);
                else if (code == 405) sendErrorResponse(out, dataOut, 405, clientInfo);
                else sendErrorResponse(out, dataOut, 500, clientInfo);
                return;
            }

            // récupérer Host pour choisir le site
            String host = extractHost(request);
            if (host == null || host.isEmpty()) {

                sendErrorResponse(out, dataOut, 400, clientInfo);
                return;
            }

            // webRoot = dossier racine du site choisi
            String webRoot = HOST_TO_ROOT.get(host);
            if (webRoot == null) {
                // Host non reconnu => erreur (ici: 404)
                sendErrorResponse(out, dataOut, 404, clientInfo);
                return;
            }

            // On prend la première ligne (request-line) : "GET /path HTTP/1.1"
            String requestLine = request.split("\r\n")[0];
            // répondre en servant un fichier selon l'URL dans le bon site
            parseAndRespond(requestLine, out, dataOut, clientInfo, webRoot);

        } catch (IOException e) {
            System.err.println("[ERREUR] [" + clientInfo + "] Erreur lors du traitement: " + e.getMessage());
        } finally {
            // Toujours fermer la socket client
            try {
                clientSocket.close();
                System.out.println("[FERMETURE] Client: " + clientInfo);
            } catch (IOException e) {
                System.err.println("[ERREUR] Erreur lors de la fermeture: " + e.getMessage());
            }
        }
    }


    /**
     * Lit la requête HTTP jusqu'à la ligne vide.
     * Exemple :
     * GET / HTTP/1.1\r\n
     * Host: ...\r\n
     * \r\n
     */private String receiveHttpRequest(BufferedReader in) throws IOException {
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

    /**
     * Vérifie :
     * - request-line correcte (regex)
     * - méthode GET seulement
     * - présence du header Host:
     *
     * Retour :
     * - 200 si OK
     * - 400 si mal formée
     * - 405 si méthode != GET
     */
    private int validateHttpRequest(String request) {
        if (request == null || request.isBlank()) return 400;

        String[] lines = request.split("\r\n");
        if (lines.length == 0 || lines[0].isBlank()) return 400;

        // request-line: METHOD PATH HTTP/x.x
        Pattern p = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+HTTP/(\\d\\.\\d)\\s*$");
        Matcher m = p.matcher(lines[0].trim());
        if (!m.matches()) return 400;

        String method = m.group(1);
        if (!"GET".equals(method)) return 405;

        // HTTP/1.1 => Host obligatoire
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

    /**
     * Récupère le header Host: ...
     * Gère aussi "Host: localhost:5000" en retirant le port.
     */
    private String extractHost(String request) {
        String[] lines = request.split("\r\n");
        for (int i = 1; i < lines.length; i++) {
            String l = lines[i];
            if (l.isEmpty()) break;

            if (l.toLowerCase().startsWith("host:")) {
                String hostValue = l.substring(5).trim(); // après "Host:"
                int idx = hostValue.indexOf(':'); // après "Host:"
                if (idx >= 0) hostValue = hostValue.substring(0, idx);
                return hostValue.trim().toLowerCase();
            }
        }
        return null;
    }

    private String buildHttpHeader(int code, Integer contentLength) {
        // Si code inconnu -> 500
        int finalCode = STATUS_MESSAGES.containsKey(code) ? code : 500;
        String message = STATUS_MESSAGES.get(finalCode);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
        String dateHeader = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.FRENCH).format(now);

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(finalCode).append(" ").append(message).append("\r\n");
        // headers obligatoires exo 5
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

    /**
     * Traite un GET :
     * - enlève ?query
     * - décode %xx (URLDecoder)
     * - refuse ".." (anti traversal)
     * - "/" => "/index.html"
     * - si dossier => ajoute "index.html"
     * - absent => 404
     * - sinon 200 + contenu fichier
     *
     * webRoot : dossier racine choisi via Host (exo 7)
     */
    private void parseAndRespond(String requestLine, PrintWriter out, OutputStream dataOut, String clientInfo, String webRoot) {
         try {
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendErrorResponse(out, dataOut, 400, clientInfo);
                return;
            }

            String method = parts[0];
            String path = parts[1];

             // sécurité : si on arrive ici avec autre chose que GET
             if (!"GET".equals(method)) {
                sendErrorResponse(out, dataOut, 405, clientInfo);
                return;
            }

             // Cas "GET http://.../page.html HTTP/1.1"
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

             // enlever la partie ?query
             int q = path.indexOf("?");
            if (q >= 0) path = path.substring(0, q);

             // décoder %xx (version simple)
             try {
                path = java.net.URLDecoder.decode(path, "UTF-8");
            } catch (Exception e) {
                sendErrorResponse(out, dataOut, 400, clientInfo);
                return;
            }
             // éviter de sortir du dossier du site
            if (path.contains("..")) {
                sendErrorResponse(out, dataOut, 400, clientInfo);
                return;
            }

            // "/" => "/index.html"
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }
            // construire le chemin local selon le site choisi
            String filePath = path.startsWith("/")
                    ? webRoot + path
                    : webRoot + "/" + path;

            File file = new File(filePath);

             // si c'est un dossier => index.html dedans
             if (file.exists() && file.isDirectory()) {
                file = new File(file, "index.html");
            }
             // absent => 404
            if (!file.exists() || !file.isFile()) {
                sendErrorResponse(out, dataOut, 404, clientInfo);
                System.out.println("[" + clientInfo + "] 404 Not Found: " + file.getPath());
                return;
            }

             // lecture + envoi du contenu
             byte[] fileContent = Files.readAllBytes(file.toPath());

            String header = buildHttpHeader(200, fileContent.length);
            out.print(header);
            out.flush();

             // Corps de la réponse = fichier
             dataOut.write(fileContent);
             dataOut.flush();

            System.out.println("[" + clientInfo + "] 200 OK (" + webRoot + "): " + file.getPath());

        } catch (Exception e) {
            System.err.println("[ERREUR] [" + clientInfo + "] " + e.getMessage());
            sendErrorResponse(out, dataOut, 500, clientInfo);
        }
    }
    /**
     * Envoie une page HTML d'erreur (400/404/405/500) avec un header conforme exo 5.
     */
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