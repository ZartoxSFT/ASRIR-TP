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
        Socket dataSocket = null;
        ServerSocket passiveServer = null;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.trim().split("\\s+", 2);
                String cmd = parts[0].toUpperCase();
                String arg = (parts.length > 1) ? parts[1].trim() : null;

                if (!isAuthenticated && !cmd.equals("USER") && !cmd.equals("PASS")) {
                    out.println("501 Pas encore authentifié. Veuillez d'abord vous authentifier.");
                    continue;
                }

                if (isAuthenticated && (cmd.equals("USER") || cmd.equals("PASS"))) {
                    out.println("503 Deja authentifie. USER/PASS interdits.");
                    continue;
                }

                switch (cmd){
                    
                    case "USER":
                        username = arg;
                        if (realUser.equals(arg) || userAno.equals(arg)) {
                            out.println("Saissez votre mot de passe");
                        } else {
                            username = null;
                            out.println("530 Utilisateur inconnu. Recommencez.");
                        }
                        break;
                    case "PASS":
                        if (username == null) {
                            out.println("Rentrez un utilisateur");
                        } else if ((realUser.equals(username) && realPWD.equals(arg)) || userAno.equals(username) ) {
                            isAuthenticated = true;
                            out.println("230 Login successful.");
                        } else {
                            username = null;
                            isAuthenticated = false;
                            out.println("530 Login incorrect. Recommencez.");
                        }
                        break;
                    case "PORT":
                        if (arg == null) {
                            out.println("501 Syntaxe: PORT a,b,c,d,e,f");
                            break;
                        }
                        String[] nums = arg.split(",");
                        if (nums.length != 6) {
                            out.println("501 Syntaxe: PORT a,b,c,d,e,f");
                            break;
                        }
                        try {
                            int a = Integer.parseInt(nums[0]);
                            int b = Integer.parseInt(nums[1]);
                            int c = Integer.parseInt(nums[2]);
                            int d = Integer.parseInt(nums[3]);
                            int e = Integer.parseInt(nums[4]);
                            int f = Integer.parseInt(nums[5]);
                            if (a < 0 || a > 255 || b < 0 || b > 255 || c < 0 || c > 255 || d < 0 || d > 255 || e < 0 || e > 255 || f < 0 || f > 255) {
                                out.println("501 Parametres PORT invalides.");
                                break;
                            }
                            String host = a + "." + b + "." + c + "." + d;
                            int port = (e * 256) + f;
                            if (dataSocket != null && !dataSocket.isClosed()) {
                                dataSocket.close();
                            }
                            dataSocket = new Socket(host, port);
                            out.println("200 PORT OK.");
                        } catch (NumberFormatException e) {
                            out.println("501 Parametres PORT invalides.");
                        } catch (IOException e) {
                            out.println("425 Impossible d'ouvrir la connexion de donnees.");
                        }
                        break;
                    case "PASV":
                        try {
                            if (passiveServer != null && !passiveServer.isClosed()) {
                                passiveServer.close();
                            }
                            passiveServer = new ServerSocket(0);
                            passiveServer.setSoTimeout(5000);

                            InetAddress localAddr = clientSocket.getLocalAddress();
                            if (!(localAddr instanceof Inet4Address)) {
                                localAddr = InetAddress.getByName("127.0.0.1");
                            }
                            String ip = localAddr.getHostAddress();
                            String[] octets = ip.split("\\.");
                            if (octets.length != 4) {
                                out.println("425 Adresse IP locale invalide.");
                                break;
                            }

                            int port = passiveServer.getLocalPort();
                            int e = port / 256;
                            int f = port % 256;
                            out.println("227 " + octets[0] + "," + octets[1] + "," + octets[2] + "," + octets[3] + "," + e + "," + f);

                            if (dataSocket != null && !dataSocket.isClosed()) {
                                dataSocket.close();
                            }
                            try {
                                dataSocket = passiveServer.accept();
                            } catch (SocketTimeoutException eTimeout) {
                                out.println("425 Timeout connexion de donnees.");
                            }
                        } catch (IOException e) {
                            out.println("425 Impossible d'ouvrir la connexion de donnees.");
                        }
                        break;
                    case "LIST":
                        if (dataSocket == null || dataSocket.isClosed()) {
                            out.println("425 Pas de connexion de donnees.");
                            break;
                        }
                        out.println("150 Ouverture de la connexion de donnees.");
                        try (PrintWriter dataOut = new PrintWriter(dataSocket.getOutputStream(), true)) {
                            File dir = new File(".");
                            File[] files = dir.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    dataOut.println(file.getName());
                                }
                            }
                            out.println("226 Transfert termine.");
                        } catch (IOException e) {
                            out.println("426 Connexion de donnees fermee.");
                        } finally {
                            try {
                                dataSocket.close();
                            } catch (IOException e) {
                                // Ignore close errors on data socket.
                            }
                        }
                        break;
                    default:
                        out.println("500 Commande non reconnue. Recommencez.");
                        break;


                }

            }

        } catch (IOException e) {
            System.err.println("Erreur lors de la communication avec le client: " + e.getMessage());
        } finally {
            try {
                if (dataSocket != null && !dataSocket.isClosed()) {
                    dataSocket.close();
                }
                if (passiveServer != null && !passiveServer.isClosed()) {
                    passiveServer.close();
                }
                clientSocket.close();
                System.out.println("Connexion fermée avec le client.");
            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture de la socket: " + e.getMessage());
            }
        }
    }
}


