import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             Scanner scan = new Scanner(System.in);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            System.out.println("Connecté au serveur " + SERVER_ADDRESS + ":" + SERVER_PORT);
            System.out.println("Commandes disponibles: DATE, HOUR, FULL, CLOSE");
            
            String command;
            while (true) {
                System.out.print("Entrez une commande: ");
                command = scan.nextLine().trim();
                
                if (command.isEmpty()) {
                    continue;
                }
                
                out.println(command);
                
                if (command.toUpperCase().equals("CLOSE")) {
                    String response = in.readLine();
                    System.out.println("Réponse du serveur: " + response);
                    break;
                }
                
                String response = in.readLine();
                if (response != null) {
                    System.out.println("Réponse du serveur: " + response);
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
