import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Server {
    private static final int PORT = 21;
    private static String realUser ="foo";
    private static String realPWD ="bar";
    private static String userAno ="anonymous";

    public static void main(String[] args) {
        System.out.println("Serveur démarré sur le port " + PORT);
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                System.out.println("En attente de connexion...");
                
                
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecté: " + clientSocket.getInetAddress());
                
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur: " + e.getMessage());
        }
    }
    private static void handleClient(Socket clientSocket) {
        boolean isAuthenticated = false;
        String username = null;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.trim().split("\\s+", 2);
                String cmd = parts[0].toUpperCase();
                String arg = (parts.length > 1) ? parts[1].trim() : null;

                if (!isAuthenticated && (!cmd.equals("USER") || !cmd.equals("PASS"))){
                    out.println("530 Not logged in. Only USER is allowed.");
                    continue;
                }
                switch (cmd){
                    case "USER":
                        username = arg;
                        if(realUser.equals(arg) || userAno.equals(arg))
                        {
                            out.println("Rentrez un mdp");
                        }
                        break;
                    case "PASS":
                        if(username == null){
                            out.println("Rentrez un utilisateur");
                        } else if ((realUser.equals(username) && realPWD.equals(arg)) || userAno.equals(username) ) {
                            isAuthenticated = true;
                        }


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


