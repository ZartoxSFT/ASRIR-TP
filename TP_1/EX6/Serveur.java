import java.io.*;
import java.net.*;

public class Serveur {
    private static final int PORT = 8080;
    private static Jeu jeu;
    private static Joueur joueur1;
    private static Joueur joueur2;
    
    public static void main(String[] args) {
        System.out.println("Serveur tic-tac-toe démarré sur le port " + PORT);
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            jeu = new Jeu();
            
            System.out.println("En attente de 2 clients...");
            
            Socket socket1 = serverSocket.accept();
            System.out.println("Client 1 connecté: " + socket1.getInetAddress());
            
            Socket socket2 = serverSocket.accept();
            System.out.println("Client 2 connecté: " + socket2.getInetAddress());
            
            joueur1 = new Joueur(socket1, jeu, 'X');
            joueur2 = new Joueur(socket2, jeu, 'O');

            boolean joueur1Commence = Math.random() < 0.5;
            joueur1.setAdversaire(joueur2, joueur1Commence);
            joueur2.setAdversaire(joueur1, !joueur1Commence);
            
            System.out.println("Joueur " + (joueur1Commence ? "1 (X)" : "2 (O)") + " commence");
            

            joueur1.start();
            joueur2.start();
            
            joueur1.join();
            joueur2.join();
            
            System.out.println("Partie terminée");
            
        } catch (IOException e) {
            System.err.println("Erreur serveur: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Erreur d'interruption: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
