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
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String heure = now.format(formatter);
            
            out.println(heure);
            System.out.println("Heure envoyée au client: " + heure);
            
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
