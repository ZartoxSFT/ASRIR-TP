import java.io.*;
import java.net.*;

public class Joueur extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Jeu jeu;
    private Joueur adversaire;
    private char symbole; // 'X' ou 'O'
    private boolean monTour;
    private boolean actif;
    
    public Joueur(Socket socket, Jeu jeu, char symbole) throws IOException {
        this.socket = socket;
        this.jeu = jeu;
        this.symbole = symbole;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.monTour = false;
        this.actif = true;
    }
    
    /**
     * Définit l'adversaire et qui commence
     */
    public void setAdversaire(Joueur adversaire, boolean commence) {
        this.adversaire = adversaire;
        this.monTour = commence;
    }
    
    /**
     * Envoie un message au client
     */
    private void envoyerMessage(String message) {
        out.println(message);
    }
    
    /**
     * Reçoit un message du client
     */
    private String recevoirMessage() throws IOException {
        return in.readLine();
    }
    
    @Override
    public void run() {
        try {
            if (monTour) {
                envoyerMessage("START:" + symbole);
            } else {
                envoyerMessage("WAIT:" + symbole);
            }
            
            while (actif) {
                if (monTour) {
                    String message = recevoirMessage();
                    
                    if (message == null) {
                        break;
                    }
                    
                    if (message.startsWith("PLAY:")) {
                        int position = Integer.parseInt(message.substring(5));
                        
                        if (jeu.placerCoup(position, symbole)) {
                            envoyerMessage("VALID:" + jeu.getPlateau());
                            
                            if (jeu.victoire(symbole)) {
                                envoyerMessage("VICTORY:" + jeu.getPlateau());
                                adversaire.envoyerMessage("DEFEAT:" + jeu.getPlateau());
                                actif = false;
                                adversaire.arreter();
                            } else if (jeu.plateauPlein()) {
                                envoyerMessage("DRAW:" + jeu.getPlateau());
                                adversaire.envoyerMessage("DRAW:" + jeu.getPlateau());
                                actif = false;
                                adversaire.arreter();
                            } else {
                                monTour = false;
                                adversaire.monTour = true;
                                adversaire.envoyerMessage("OPPONENT_PLAY:" + jeu.getPlateau() + ":" + symbole + ":" + position);
                            }
                        } else {
                            envoyerMessage("INVALID:" + jeu.getPlateau());
                        }
                    } else if (message.startsWith("QUIT")) {
                        actif = false;
                        adversaire.arreter();
                    }
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
        } catch (IOException e) {
            System.out.println("Erreur communication avec client: " + e.getMessage());
            // Informer l'adversaire et l'arrêter
            if (adversaire != null && adversaire.estActif()) {
                adversaire.envoyerMessage("OPPONENT_DISCONNECTED");
                adversaire.arreter();
            }
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erreur fermeture socket: " + e.getMessage());
            }
            actif = false;
        }
    }
    
    /**
     * Teste si ce joueur est actif
     */
    public boolean estActif() {
        return actif;
    }
    
    /**
     * Arrête ce joueur
     */
    public void arreter() {
        actif = false;
    }
}
