import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class POP {

	private static final String POP_SERVER = "localhost";
	private static final int POP_PORT = 25;
	private static final String USERNAME = "test@localhost";
	private static final String PASSWORD = "test";

	public static void main(String[] args) {
		try (Socket socket = new Socket(POP_SERVER, POP_PORT);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

			// Lecture du message de bienvenue
			readResponse(reader);
			System.out.println("Connecté au serveur POP3");

			// Authentification
			sendCommand(writer, reader, "USER " + USERNAME);
			System.out.println("Utilisateur envoyé");

			sendCommand(writer, reader, "PASS " + PASSWORD);
			System.out.println("Authentification réussie\n");

			// Obtenir le nombre de messages
			String statResponse = sendCommand(writer, reader, "STAT");
			System.out.println("Statut: " + statResponse);

			// Parser le nombre de messages
			String[] statParts = statResponse.split(" ");
			int messageCount = 0;
			if (statParts.length >= 2) {
				try {
					messageCount = Integer.parseInt(statParts[1]);
				} catch (NumberFormatException e) {
					System.err.println("Erreur lors du parsing du nombre de messages");
				}
			}

			System.out.println("Nombre de messages: " + messageCount + "\n");

			// Récupérer et afficher chaque message
			for (int i = 1; i <= messageCount; i++) {
				System.out.println("========== MESSAGE " + i + " ==========");
				try {
					// Récupérer le message
					sendCommand(writer, reader, "RETR " + i);
					String message = readMultilineResponse(reader);
					System.out.println(message);
					System.out.println();

					// Supprimer le message
					sendCommand(writer, reader, "DELE " + i);
					System.out.println("Message " + i + " marqué pour suppression");
					System.out.println("=====================================\n");

				} catch (IOException e) {
					System.err.println("Erreur lors du traitement du message " + i + ": " + e.getMessage());
				}
			}

			// Déconnexion (les messages sont supprimés à la déconnexion)
			sendCommand(writer, reader, "QUIT");
			System.out.println("Déconnexion du serveur POP3");
			System.out.println("Les messages ont été supprimés du serveur");

		} catch (Exception e) {
			System.err.println("Erreur de connexion POP3: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static String sendCommand(BufferedWriter writer, BufferedReader reader, String command)
		throws IOException {
		writer.write(command + "\r\n");
		writer.flush();
		return readResponse(reader);
	}

	private static String readResponse(BufferedReader reader) throws IOException {
		String line = reader.readLine();
		if (line == null) {
			throw new IOException("Connexion POP3 fermée par le serveur.");
		}

		if (!line.startsWith("+OK")) {
			throw new IOException("Erreur POP3: " + line);
		}

		return line;
	}

	private static String readMultilineResponse(BufferedReader reader) throws IOException {
		StringBuilder content = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null) {
			// Le serveur POP3 termine une réponse multiligne avec un point seul sur une ligne
			if (line.equals(".")) {
				break;
			}

			// Si la ligne commence par deux points, enlever le premier (byte-stuffing)
			if (line.startsWith("..")) {
				line = line.substring(1);
			}

			content.append(line).append("\n");
		}

		return content.toString();
	}
}
