import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class POP {

     private static final int DEFAULT_PORT = 1100;
     private static final Path MAIL_ROOT = Paths.get("mailboxes");

     private static final Map<String, String> USERS = new HashMap<>();

     static {
          USERS.put("amin", "amin123");
          USERS.put("cedric", "cedric123");
     }

     public static void main(String[] args) {
          int port = DEFAULT_PORT;
          if (args.length > 0) {
               try {
                    port = Integer.parseInt(args[0]);
               } catch (NumberFormatException ignored) {
                    System.out.println("Port invalide, utilisation du port par défaut " + DEFAULT_PORT);
               }
          }

          try {
               initializeMailboxes();
          } catch (IOException e) {
               System.err.println("Impossible d'initialiser les boîtes mail: " + e.getMessage());
               return;
          }

          try (ServerSocket serverSocket = new ServerSocket(port)) {
               System.out.println("Serveur POP démarré sur le port " + port);

               while (true) {
                    Socket client = serverSocket.accept();
                    Thread worker = new Thread(() -> handleClient(client));
                    worker.start();
               }
          } catch (IOException e) {
               System.err.println("Erreur serveur: " + e.getMessage());
          }
     }

     private static void initializeMailboxes() throws IOException {
          if (!Files.exists(MAIL_ROOT)) {
               Files.createDirectories(MAIL_ROOT);
          }

          for (String user : USERS.keySet()) {
               Path userDir = MAIL_ROOT.resolve(user);
               if (!Files.exists(userDir)) {
                    Files.createDirectories(userDir);
               }
          }
     }

     private static void handleClient(Socket client) {
          try (Socket socket = client;
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

               Session session = new Session(output);
               session.ok("Serveur POP prêt");

               String line;
               while ((line = input.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                         session.err("commande vide");
                         continue;
                    }

                    if (session.handle(line)) {
                         break;
                    }
               }
          } catch (IOException ignored) {
          }
     }

     private enum State {
          AUTHORIZATION,
          TRANSACTION
     }

     private static class MessageInfo {
          private final Path path;
          private final long size;
          private boolean deleted;

          MessageInfo(Path path, long size) {
               this.path = path;
               this.size = size;
               this.deleted = false;
          }
     }

     private static class Session {
          private final BufferedWriter output;
          private State state = State.AUTHORIZATION;
          private String pendingUser;
          private String authenticatedUser;
          private List<MessageInfo> messages = new ArrayList<>();

          Session(BufferedWriter output) {
               this.output = output;
          }

          boolean handle(String rawCommand) throws IOException {
               String[] parts = rawCommand.trim().split("\\s+");
               String command = parts[0].toUpperCase(Locale.ROOT);

               switch (command) {
                    case "USER":
                         cmdUser(parts);
                         break;
                    case "PASS":
                         cmdPass(parts);
                         break;
                    case "STAT":
                         ensureTransaction();
                         cmdStat();
                         break;
                    case "LIST":
                         ensureTransaction();
                         cmdList(parts);
                         break;
                    case "RETR":
                         ensureTransaction();
                         cmdRetr(parts);
                         break;
                    case "TOP":
                         ensureTransaction();
                         cmdTop(parts);
                         break;
                    case "DELE":
                         ensureTransaction();
                         cmdDele(parts);
                         break;
                    case "RSET":
                         ensureTransaction();
                         cmdRset();
                         break;
                    case "NOOP":
                         cmdNoop();
                         break;
                    case "QUIT":
                         cmdQuit();
                         return true;
                    default:
                         err("commande inconnue");
               }

               return false;
          }

          private void ensureTransaction() throws IOException {
               if (state != State.TRANSACTION) {
                    throw new PopException("authentification requise");
               }
          }

          private void cmdUser(String[] parts) throws IOException {
               if (parts.length != 2) {
                    err("syntaxe: USER <user>");
                    return;
               }

               String user = parts[1];
               if (!USERS.containsKey(user)) {
                    err("utilisateur inconnu");
                    return;
               }

               pendingUser = user;
               ok("utilisateur accepté");
          }

          private void cmdPass(String[] parts) throws IOException {
               if (parts.length != 2) {
                    err("syntaxe: PASS <mdp>");
                    return;
               }

               if (pendingUser == null) {
                    err("USER requis avant PASS");
                    return;
               }

               String expected = USERS.get(pendingUser);
               if (!expected.equals(parts[1])) {
                    err("mot de passe invalide");
                    return;
               }

               authenticatedUser = pendingUser;
               pendingUser = null;
               loadMessages();
               state = State.TRANSACTION;
               ok("authentification réussie");
          }

          private void cmdStat() throws IOException {
               int count = 0;
               long total = 0;

               for (MessageInfo msg : messages) {
                    if (!msg.deleted) {
                         count++;
                         total += msg.size;
                    }
               }

               ok(count + " " + total);
          }

          private void cmdList(String[] parts) throws IOException {
               if (parts.length == 1) {
                    int count = 0;
                    long total = 0;
                    for (MessageInfo msg : messages) {
                         if (!msg.deleted) {
                              count++;
                              total += msg.size;
                         }
                    }

                    ok(count + " messages (" + total + " octets)");
                    for (int i = 0; i < messages.size(); i++) {
                         MessageInfo msg = messages.get(i);
                         if (!msg.deleted) {
                              writeLine((i + 1) + " " + msg.size);
                         }
                    }
                    writeLine(".");
                    flush();
                    return;
               }

               if (parts.length == 2) {
                    MessageInfo msg = getMessage(parts[1]);
                    ok(messageIndex(msg) + " " + msg.size);
                    return;
               }

               err("syntaxe: LIST ou LIST <n>");
          }

          private void cmdRetr(String[] parts) throws IOException {
               if (parts.length != 2) {
                    err("syntaxe: RETR <n>");
                    return;
               }

               MessageInfo msg = getMessage(parts[1]);
               List<String> lines = Files.readAllLines(msg.path, StandardCharsets.UTF_8);

               ok(msg.size + " octets");
               for (String line : lines) {
                    writeLine(line);
               }
               writeLine(".");
               flush();
          }

          private void cmdTop(String[] parts) throws IOException {
               if (parts.length != 3) {
                    err("syntaxe: TOP <n> <x>");
                    return;
               }

               MessageInfo msg = getMessage(parts[1]);
               int limit;
               try {
                    limit = Integer.parseInt(parts[2]);
               } catch (NumberFormatException e) {
                    err("<x> doit être un entier");
                    return;
               }

               if (limit < 0) {
                    err("<x> doit être positif");
                    return;
               }

               List<String> lines = Files.readAllLines(msg.path, StandardCharsets.UTF_8);
               ok("top du message " + messageIndex(msg));
               for (int i = 0; i < Math.min(limit, lines.size()); i++) {
                    writeLine(lines.get(i));
               }
               writeLine(".");
               flush();
          }

          private void cmdDele(String[] parts) throws IOException {
               if (parts.length != 2) {
                    err("syntaxe: DELE <n>");
                    return;
               }

               MessageInfo msg = getMessage(parts[1]);
               msg.deleted = true;
               ok("message " + messageIndex(msg) + " marqué pour suppression");
          }

          private void cmdRset() throws IOException {
               int restored = 0;
               for (MessageInfo msg : messages) {
                    if (msg.deleted) {
                         msg.deleted = false;
                         restored++;
                    }
               }

               ok(restored + " messages restaurés");
          }

          private void cmdNoop() throws IOException {
               ok("noop");
          }

          private void cmdQuit() throws IOException {
               if (state == State.TRANSACTION) {
                    int deletedCount = 0;
                    for (MessageInfo msg : messages) {
                         if (msg.deleted) {
                              Files.deleteIfExists(msg.path);
                              deletedCount++;
                         }
                    }
                    ok("déconnexion, " + deletedCount + " messages supprimés");
               } else {
                    ok("déconnexion");
               }
          }

          private void loadMessages() throws IOException {
               messages = new ArrayList<>();
               Path userDir = MAIL_ROOT.resolve(authenticatedUser);

               try (DirectoryStream<Path> stream = Files.newDirectoryStream(userDir, path -> Files.isRegularFile(path))) {
                    for (Path file : stream) {
                         messages.add(new MessageInfo(file, Files.size(file)));
                    }
               }

               messages.sort(Comparator.comparing(msg -> msg.path.getFileName().toString()));
          }

          private MessageInfo getMessage(String token) throws IOException {
               int index;
               try {
                    index = Integer.parseInt(token);
               } catch (NumberFormatException e) {
                    throw new PopException("indice de message invalide");
               }

               if (index < 1 || index > messages.size()) {
                    throw new PopException("message inexistant");
               }

               MessageInfo msg = messages.get(index - 1);
               if (msg.deleted) {
                    throw new PopException("message supprimé");
               }

               return msg;
          }

          private int messageIndex(MessageInfo messageInfo) {
               return messages.indexOf(messageInfo) + 1;
          }

          private void ok(String msg) throws IOException {
               writeLine("+OK " + msg);
               flush();
          }

          private void err(String msg) throws IOException {
               writeLine("-ERR " + msg);
               flush();
          }

          private void writeLine(String line) throws IOException {
               output.write(line);
               output.write("\r\n");
          }

          private void flush() throws IOException {
               output.flush();
          }
     }

     private static class PopException extends IOException {
          PopException(String message) {
               super(message);
          }
     }
}