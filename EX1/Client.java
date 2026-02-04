import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            System.out.println("Connecté au serveur " + SERVER_ADDRESS + ":" + SERVER_PORT);
            
            String heure = in.readLine();
            System.out.println("Heure reçue du serveur: " + heure);
            
        } catch (UnknownHostException e) {
            System.err.println("Serveur introuvable: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erreur I/O: " + e.getMessage());
        }
        
        System.out.println("Connexion fermée.");
    }
}
