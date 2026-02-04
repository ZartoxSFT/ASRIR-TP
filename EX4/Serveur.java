import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Serveur {
    private static final int PORT = 8080;
    
    public static void main(String[] args) {
        System.out.println("Serveur démarré sur le port " + PORT);
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                System.out.println("En attente de connexion...");
                
                
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecté: " + clientSocket.getInetAddress());
                
                
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur: " + e.getMessage());
        }
    }
    
    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            String command;
            while ((command = in.readLine()) != null) {
                command = command.trim().toUpperCase();
                System.out.println("Commande reçue: " + command);
                
                if (command.equals("CLOSE")) {
                    System.out.println("Client demande la fermeture de la connexion.");
                    break;
                } else if (command.equals("DATE")) {
                    String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    out.println(date);
                    System.out.println("Date envoyée au client: " + date);
                } else if (command.equals("HOUR")) {
                    String heure = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    out.println(heure);
                    System.out.println("Heure envoyée au client: " + heure);
                } else if (command.equals("FULL")) {
                    String full = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                    out.println(full);
                    System.out.println("Date et heure envoyées au client: " + full);
                } else {
                    out.println("ERREUR: Commande non reconnue. Utilisez DATE, HOUR, FULL ou CLOSE.");
                    System.out.println("Commande non valide reçue: " + command);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Erreur lors de la communication avec le client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Connexion fermée avec le client.");
            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture de la socket: " + e.getMessage());
            }
        }
    }
}
