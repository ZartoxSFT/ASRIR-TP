import java.io.*;
import java.net.*;
import java.util.Scanner;

public class    Client {
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

                if (command.equalsIgnoreCase("PORT")) {
                    runActiveList(socket, in, out);
                    continue;
                }

                if (command.equalsIgnoreCase("PASV")) {
                    runPassiveList(in, out);
                    continue;
                }

                out.println(command);
                String response = in.readLine();
                if (response == null) {
                    break;
                }
                System.out.println(response);

            }
            
        } catch (UnknownHostException e) {
            System.err.println("Serveur introuvable: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
        
        System.out.println("Connexion fermée.");
    }

    private static void runActiveList(Socket controlSocket, BufferedReader in, PrintWriter out) throws IOException {
        try (ServerSocket dataServer = new ServerSocket(0)) {
            dataServer.setSoTimeout(5000);

            InetAddress localAddr = controlSocket.getLocalAddress();
            if (!(localAddr instanceof Inet4Address)) {
                localAddr = InetAddress.getByName("127.0.0.1");
            }

            String ip = localAddr.getHostAddress();
            String[] octets = ip.split("\\.");
            if (octets.length != 4) {
                System.out.println("Adresse IP locale invalide pour PORT: " + ip);
                return;
            }

            int port = dataServer.getLocalPort();
            int e = port / 256;
            int f = port % 256;

            String portCmd = "PORT " + octets[0] + "," + octets[1] + "," + octets[2] + "," + octets[3] + "," + e + "," + f;
            out.println(portCmd);

            String response = in.readLine();
            if (response == null) {
                System.out.println("Aucune reponse du serveur.");
                return;
            }
            System.out.println(response);

            try (Socket dataSocket = dataServer.accept()) {
                System.out.println("Connexion de donnees etablie avec " + dataSocket.getRemoteSocketAddress());
            } catch (SocketTimeoutException eTimeout) {
                System.out.println("Timeout: pas de connexion de donnees.");
            }
        }
    }

    private static void runPassiveList(BufferedReader in, PrintWriter out) throws IOException {
        out.println("PASV");

        String response = in.readLine();
        if (response == null) {
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

        try (Socket dataSocket = new Socket(host, port)) {
            System.out.println("Connexion de donnees etablie avec " + dataSocket.getRemoteSocketAddress());
        }
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
