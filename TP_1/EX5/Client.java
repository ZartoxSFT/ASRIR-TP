import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
             BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            System.out.println("Connecté au serveur " + SERVER_ADDRESS + ":" + SERVER_PORT);
            System.out.println("Entrez des lignes de texte (Ctrl+Z pour quitter):");
            
            String line;
            while ((line = in.readLine()) != null) {
                out.println(line);
                
                String response = serverInput.readLine();
                if (response != null) {
                    System.out.println(response);
                } else {
                    System.out.println("Connexion fermée par le serveur.");
                    break;
                }
            }
            
        } catch (UnknownHostException e) {
            System.err.println("Serveur introuvable: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
        
        System.out.println("Connexion fermée.");
    }
}
