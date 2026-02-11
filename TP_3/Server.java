import java.io.*;
import java.net.*;
import java.nio.file.*;

/**
 * Serveur HTTP multi-thread
 * Exercice 3 - ASRIR TP3
 * 
 * Le serveur écoute sur le port 6666 et peut traiter plusieurs clients en parallèle.
 * Chaque connexion client est gérée dans un thread séparé.
 */
public class Server {
    private static final int PORT = 6666;
    private static final String WEB_ROOT = "www";
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("Serveur HTTP démarré sur le port " + PORT);
        System.out.println("Racine web: " + WEB_ROOT);
        System.out.println("===========================================\n");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("En attente de connexions...\n");
            
            while (true) {
                try {
                    // Accepter une nouvelle connexion client
                    Socket clientSocket = serverSocket.accept();
                    
                    // Créer un nouveau thread pour gérer ce client
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
 * Classe interne pour gérer chaque client dans un thread séparé
 */
class ClientHandler implements Runnable {
    private Socket clientSocket;
    
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }
    
    @Override
    public void run() {
        String clientInfo = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
        
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
            OutputStream dataOut = clientSocket.getOutputStream()
        ) {
            
            System.out.println("\n--- [" + Thread.currentThread().getName() + "] Traitement client: " + clientInfo + " ---");
            
            // Lire la première ligne de la requête HTTP
            String requestLine = in.readLine();
            
            if (requestLine == null || requestLine.isEmpty()) {
                System.out.println("[" + clientInfo + "] Requête vide reçue");
                return;
            }
            
            // Afficher la ligne de requête
            System.out.println("[" + clientInfo + "] " + requestLine);
            
            // Lire et afficher tous les en-têtes HTTP
            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                System.out.println("[" + clientInfo + "] " + headerLine);
            }
            
            System.out.println("[" + clientInfo + "] --- Fin de la requête ---\n");
            
            // Parser la requête HTTP
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
    
    /**
     * Parse la requête HTTP et envoie la réponse appropriée
     */
    private void parseAndRespond(String requestLine, PrintWriter out, OutputStream dataOut, String clientInfo) {
        try {
            // Parser la ligne de requête: "GET /path HTTP/1.1"
            String[] parts = requestLine.split(" ");
            
            if (parts.length < 2) {
                sendErrorResponse(out, dataOut, 400, "Bad Request", clientInfo);
                return;
            }
            
            String method = parts[0];
            String path = parts[1];
            
            System.out.println("[" + clientInfo + "] Méthode: " + method + ", Chemin: " + path);
            
            // Pour l'instant, on ne gère que GET
            if (!method.equals("GET")) {
                sendErrorResponse(out, dataOut, 405, "Method Not Allowed", clientInfo);
                return;
            }
            
            // Gérer la racine "/"
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }
            
            // Construire le chemin du fichier
            // Retirer le "/" initial et construire le chemin complet
            String filePath;
            if (path.startsWith("/www/")) {
                filePath = path.substring(1); // Enlever le "/" initial
            } else if (path.startsWith("/")) {
                filePath = Server.WEB_ROOT + path;
            } else {
                filePath = Server.WEB_ROOT + "/" + path;
            }
            
            File file = new File(filePath);
            
            // Vérifier si le fichier existe et est lisible
            if (!file.exists() || !file.isFile()) {
                sendErrorResponse(out, dataOut, 404, "Not Found", clientInfo);
                System.out.println("[" + clientInfo + "] Fichier non trouvé: " + filePath);
                return;
            }
            
            // Lire le contenu du fichier
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String contentType = getContentType(filePath);
            
            // Envoyer la réponse HTTP
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: " + contentType);
            out.println("Content-Length: " + fileContent.length);
            out.println("Connection: close");
            out.println(); // Ligne vide entre en-têtes et corps
            out.flush();
            
            // Envoyer le contenu du fichier
            dataOut.write(fileContent);
            dataOut.flush();
            
            System.out.println("[" + clientInfo + "] Fichier envoyé: " + filePath + " (" + fileContent.length + " bytes)");
            
        } catch (Exception e) {
            System.err.println("[ERREUR] [" + clientInfo + "] Erreur lors du traitement: " + e.getMessage());
            sendErrorResponse(out, dataOut, 500, "Internal Server Error", clientInfo);
        }
    }
    
    /**
     * Détermine le type MIME du fichier en fonction de son extension
     */
    private String getContentType(String filePath) {
        if (filePath.endsWith(".html") || filePath.endsWith(".htm")) {
            return "text/html; charset=UTF-8";
        } else if (filePath.endsWith(".css")) {
            return "text/css";
        } else if (filePath.endsWith(".js")) {
            return "application/javascript";
        } else if (filePath.endsWith(".json")) {
            return "application/json";
        } else if (filePath.endsWith(".png")) {
            return "image/png";
        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filePath.endsWith(".gif")) {
            return "image/gif";
        } else if (filePath.endsWith(".txt")) {
            return "text/plain";
        } else {
            return "application/octet-stream";
        }
    }
    
    /**
     * Envoie une réponse d'erreur HTTP
     */
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


