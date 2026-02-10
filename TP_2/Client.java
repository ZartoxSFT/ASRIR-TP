import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int CONTROL_PORT = 21;
    
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, CONTROL_PORT);
             Scanner scan = new Scanner(System.in);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            System.out.println("Connecté au serveur " + SERVER_ADDRESS + ":" + CONTROL_PORT);
            
             String command;
            while (true) {
                System.out.print("Entrez une commande: ");
                command = scan.nextLine().trim();
                
                out.write(command);

            }
            
        } catch (UnknownHostException e) {
            System.err.println("Serveur introuvable: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
        
        System.out.println("Connexion fermée.");
    }
}
