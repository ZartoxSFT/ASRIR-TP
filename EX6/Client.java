import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    private static char[][] plateau;
    private char monSymbole;
    private boolean monTour;
    
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
             BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            Client client = new Client();
            client.monSymbole = ' ';
            client.monTour = false;
            client.plateau = new char[3][3];
            
            System.out.println("Connecté au serveur " + SERVER_ADDRESS + ":" + SERVER_PORT);
            System.out.println("En attente du message d'initialisation du serveur...\n");
            
            Thread recepteur = new Thread(() -> {
                try {
                    String message;
                    while ((message = serverInput.readLine()) != null) {
                        client.traiterMessage(message);
                    }
                } catch (IOException e) {
                    System.out.println("Connexion fermée par le serveur.");
                }
            });
            recepteur.setDaemon(true);
            recepteur.start();
            
            String ligne;
            while ((ligne = in.readLine()) != null) {
                if (client.monTour) {
                    try {
                        int position = Integer.parseInt(ligne.trim());
                        if (position >= 1 && position <= 9) {
                            out.println("PLAY:" + position);
                        } else {
                            System.out.println("Entrée invalide. Entrez une position entre 1 et 9.");
                        }
                    } catch (NumberFormatException e) {
                        if (ligne.equalsIgnoreCase("QUIT")) {
                            out.println("QUIT");
                            break;
                        } else {
                            System.out.println("Veuillez entrer un nombre entre 1 et 9.");
                        }
                    }
                } else {
                    System.out.println("Ce n'est pas votre tour. Veuillez attendre...");
                }
            }
            
        } catch (UnknownHostException e) {
            System.err.println("Serveur introuvable: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
        
        System.out.println("Déconnecté du serveur.");
    }
    
    /**
     * Affiche le plateau de jeu
     */
    private void afficherPlateau() {
        System.out.println("\n   0   1   2");
        System.out.println("  -----------");
        
        for (int i = 0; i < 3; i++) {
            System.out.print(i + " | ");
            for (int j = 0; j < 3; j++) {
                char c = plateau[i][j];
                System.out.print(c == ' ' ? " " : c);
                System.out.print(" | ");
            }
            System.out.println("\n  -----------");
        }
    }
    
    /**
     * Met à jour le plateau à partir d'une chaîne
     */
    private void mettreAJourPlateau(String plateauStr) {
        int idx = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (idx < plateauStr.length()) {
                    plateau[i][j] = plateauStr.charAt(idx++);
                }
            }
        }
    }
    
    /**
     * Traite les messages reçus du serveur
     */
    private synchronized void traiterMessage(String message) {
        if (message.startsWith("START:")) {
            String sym = message.substring(6);
            monSymbole = sym.charAt(0);
            monTour = true;
            System.out.println("\n=== PARTIE DÉMARRÉE ===");
            System.out.println("Vous jouez avec le symbole: " + monSymbole);
            System.out.println("C'est votre tour! Entrez une position (1-9):");
            afficherPlateau();
            
        } else if (message.startsWith("WAIT:")) {
            String sym = message.substring(5);
            monSymbole = sym.charAt(0);
            monTour = false;
            System.out.println("\n=== PARTIE DÉMARRÉE ===");
            System.out.println("Vous jouez avec le symbole: " + monSymbole);
            System.out.println("L'adversaire commence. Veuillez attendre...");
            
        } else if (message.startsWith("VALID:")) {
            String plateauStr = message.substring(6);
            mettreAJourPlateau(plateauStr);
            System.out.println("\nCoup accepté!");
            afficherPlateau();
            System.out.println("En attente de l'adversaire...");
            monTour = false;
            
        } else if (message.startsWith("INVALID:")) {
            String plateauStr = message.substring(8);
            mettreAJourPlateau(plateauStr);
            System.out.println("\nCoup invalide! Cette case est déjà occupée.");
            System.out.println("Entrez une autre position:");
            afficherPlateau();
            
        } else if (message.startsWith("OPPONENT_PLAY:")) {
            String[] parts = message.substring(14).split(":");
            if (parts.length >= 3) {
                String plateauStr = parts[0];
                String symbolAdversaire = parts[1];
                int position = Integer.parseInt(parts[2]);
                
                mettreAJourPlateau(plateauStr);
                System.out.println("\nL'adversaire (" + symbolAdversaire + ") a joué en position " + position);
                afficherPlateau();
                System.out.println("\nC'est votre tour! Entrez une position (1-9):");
                monTour = true;
            }
            
        } else if (message.startsWith("VICTORY:")) {
            String plateauStr = message.substring(8);
            mettreAJourPlateau(plateauStr);
            afficherPlateau();
            System.out.println("\n*** VOUS AVEZ GAGNÉ! ***");
            monTour = false;
            System.out.println("Déconnexion du serveur...");
            System.exit(0);
            
        } else if (message.startsWith("DEFEAT:")) {
            String plateauStr = message.substring(7);
            mettreAJourPlateau(plateauStr);
            afficherPlateau();
            System.out.println("\n*** VOUS AVEZ PERDU! ***");
            monTour = false;
            System.out.println("Déconnexion du serveur...");
            System.exit(0);
            
        } else if (message.startsWith("DRAW:")) {
            String plateauStr = message.substring(5);
            mettreAJourPlateau(plateauStr);
            afficherPlateau();
            System.out.println("\n*** ÉGALITÉ! ***");
            monTour = false;
            System.out.println("Déconnexion du serveur...");
            System.exit(0);
            
        } else if (message.startsWith("OPPONENT_DISCONNECTED")) {
            System.out.println("\n*** L'ADVERSAIRE S'EST DÉCONNECTÉ ***");
            System.out.println("Partie terminée.");
            monTour = false;
            System.out.println("Déconnexion du serveur...");
            System.exit(0);
        }
    }
}
