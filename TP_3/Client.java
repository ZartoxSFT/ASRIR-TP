import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Client HTTP simple pour tester le serveur
 * Exercice 3 - ASRIR TP3
 */
public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 6666;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("===========================================");
        System.out.println("Client HTTP - Test du serveur");
        System.out.println("Serveur: " + SERVER_ADDRESS + ":" + SERVER_PORT);
        System.out.println("===========================================\n");
        
        while (true) {
            System.out.println("\nOptions:");
            System.out.println("1. Envoyer une requête GET");
            System.out.println("2. Envoyer une requête personnalisée");
            System.out.println("3. Quitter");
            System.out.print("\nChoix: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    System.out.print("Chemin de la ressource (ex: /index.html): ");
                    String path = scanner.nextLine().trim();
                    if (path.isEmpty()) {
                        path = "/";
                    }
                    sendGetRequest(path);
                    break;
                    
                case "2":
                    System.out.print("Requête complète (ex: GET / HTTP/1.1): ");
                    String customRequest = scanner.nextLine();
                    sendCustomRequest(customRequest);
                    break;
                    
                case "3":
                    System.out.println("Au revoir!");
                    scanner.close();
                    return;
                    
                default:
                    System.out.println("Choix invalide!");
            }
        }
    }
    
    /**
     * Envoie une requête GET HTTP au serveur
     */
    private static void sendGetRequest(String path) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            System.out.println("\n--- Envoi de la requête ---");
            
            // Construire et envoyer la requête HTTP
            String request = "GET " + path + " HTTP/1.1";
            out.println(request);
            System.out.println(request);
            
            out.println("Host: " + SERVER_ADDRESS);
            System.out.println("Host: " + SERVER_ADDRESS);
            
            out.println("User-Agent: SimpleHTTPClient/1.0");
            System.out.println("User-Agent: SimpleHTTPClient/1.0");
            
            out.println("Accept: */*");
            System.out.println("Accept: */*");
            
            out.println("Connection: close");
            System.out.println("Connection: close");
            
            out.println(); // Ligne vide pour terminer les en-têtes
            out.flush();
            
            System.out.println("\n--- Réponse du serveur ---");
            
            // Lire et afficher la réponse
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            
        } catch (UnknownHostException e) {
            System.err.println("Erreur: Serveur introuvable - " + e.getMessage());
        } catch (ConnectException e) {
            System.err.println("Erreur: Impossible de se connecter au serveur. Est-il démarré?");
        } catch (IOException e) {
            System.err.println("Erreur I/O: " + e.getMessage());
        }
    }
    
    /**
     * Envoie une requête personnalisée au serveur
     */
    private static void sendCustomRequest(String request) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            System.out.println("\n--- Envoi de la requête ---");
            out.println(request);
            System.out.println(request);
            
            out.println("Host: " + SERVER_ADDRESS);
            System.out.println("Host: " + SERVER_ADDRESS);
            
            out.println(); // Ligne vide
            out.flush();
            
            System.out.println("\n--- Réponse du serveur ---");
            
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            
        } catch (UnknownHostException e) {
            System.err.println("Erreur: Serveur introuvable - " + e.getMessage());
        } catch (ConnectException e) {
            System.err.println("Erreur: Impossible de se connecter au serveur. Est-il démarré?");
        } catch (IOException e) {
            System.err.println("Erreur I/O: " + e.getMessage());
        }
    }
}
            System.out.println("Aucune reponse du serveur.");
            return;
        }
        System.out.println(response);

        String[] parts = extractPasvParts(response);
        if (parts == null || parts.length != 6) {
            System.out.println("Reponse PASV invalide.");
            return;
        }

        int e = Integer.parseInt(parts[4]);
        int f = Integer.parseInt(parts[5]);
        String host = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
        int port = (e * 256) + f;

        dataSocket = new Socket(host, port);
    }

    private static void handleList(BufferedReader in, PrintWriter out) throws IOException {
        if (dataServer == null && dataSocket == null) {
            System.out.println("Erreur: Aucune connexion de donnees. Utilisez PORT ou PASV d'abord.");
            return;
        }

        // Envoyer la commande LIST
        out.println("LIST");
        
        String response = in.readLine();
        if (response != null) {
            System.out.println(response); // 150 Ouverture...
        }

        try {
            // Mode actif: attendre la connexion du serveur
            if (dataServer != null && dataSocket == null) {
                dataSocket = dataServer.accept();
            }

            // Lire et afficher les données
            if (dataSocket != null) {
                try (BufferedReader dataIn = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()))) {
                    String line;
                    while ((line = dataIn.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            }

            // Lire la réponse finale (226)
            response = in.readLine();
            if (response != null) {
                System.out.println(response);
            }
        } catch (SocketTimeoutException eTimeout) {
            System.out.println("Timeout: pas de connexion de donnees.");
        } finally {
            closeDataConnection(); // Fermer après LIST
        }
    }

    private static void closeDataConnection() {
        try {
            if (dataSocket != null && !dataSocket.isClosed()) {
                dataSocket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        dataSocket = null;

        try {
            if (dataServer != null && !dataServer.isClosed()) {
                dataServer.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        dataServer = null;
    }

    private static String[] extractPasvParts(String response) {
        String[] numbers = new String[7];
        int count = 0;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < response.length(); i++) {
            char ch = response.charAt(i);
            if (Character.isDigit(ch)) {
                current.append(ch);
            } else if (current.length() > 0) {
                if (count < numbers.length) {
                    numbers[count++] = current.toString();
                }
                current.setLength(0);
            }
        }

        if (current.length() > 0 && count < numbers.length) {
            numbers[count++] = current.toString();
        }

        if (count < 7) {
            return null;
        }

        String[] result = new String[6];
        System.arraycopy(numbers, 1, result, 0, 6);
        return result;
    }
}
