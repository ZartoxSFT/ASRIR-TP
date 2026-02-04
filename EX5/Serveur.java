import java.io.*;
import java.net.*;

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
            
            String line;
            while ((line = in.readLine()) != null) {
                String transformedLine = line.toUpperCase();
                System.out.println("Ligne reçue: " + line);
                
                out.println(transformedLine);
                System.out.println("Ligne transformée envoyée au client: " + transformedLine);
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
