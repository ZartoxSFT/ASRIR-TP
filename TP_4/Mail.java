import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Mail {

	private static final Contact[] CONTACTS = {
			new Contact("Cedric", "cedric.momprive@etu.univ-lyon1.fr"),
			new Contact("Amin", "amin.messaoudi@etu.univ-lyon1.fr"),
			new Contact("Amin2", "darkfireyo@gmail.com")
	};

	private static final String SENDER_EMAIL = "test@localhost";
	private static final String SUBJECT = "CUSTOM EMAIL TEST";

	public static void main(String[] args) {
		try (Socket socket = new Socket("localhost", 25);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

			readResponse(reader, 220);
			sendCommand(writer, reader, "HELO localhost", 250);

			for (Contact contact : CONTACTS) {
				try {
					sendCommand(writer, reader, "MAIL FROM:<" + SENDER_EMAIL + ">", 250);
					sendCommand(writer, reader, "RCPT TO:<" + contact.email + ">", 250, 251);
					sendCommand(writer, reader, "DATA", 354);

					String body = "Bonjour " + contact.name + "!\n\n"
							+ "Ceci est un test d'email envoyé via un script Java.";

					writeDataLine(writer, "Subject: " + SUBJECT);
					writeDataLine(writer, "From: " + SENDER_EMAIL);
					writeDataLine(writer, "To: " + contact.email);
					writeDataLine(writer, "Content-Type: text/plain; charset=UTF-8");
					writeDataLine(writer, "");
					for (String line : body.split("\\n", -1)) {
						if (line.startsWith(".")) {
							line = "." + line;
						}
						writeDataLine(writer, line);
					}
					writeDataLine(writer, ".");
					writer.flush();
					readResponse(reader, 250);

					System.out.println("Mail envoyé à " + contact.name + " (" + contact.email + ")");
				} catch (Exception e) {
					System.out.println("Erreur d'envoi à " + contact.name + " (" + contact.email + "): " + e.getMessage());
					try {
						sendCommand(writer, reader, "RSET", 250);
					} catch (Exception ignored) {
					}
				}
			}

			sendCommand(writer, reader, "QUIT", 221);

		} catch (Exception e) {
			System.err.println("Erreur de connexion SMTP: " + e.getMessage());
		}
	}

	private static void sendCommand(BufferedWriter writer, BufferedReader reader, String command, int... expectedCodes)
			throws IOException {
		writer.write(command + "\r\n");
		writer.flush();
		readResponse(reader, expectedCodes);
	}

	private static void writeDataLine(BufferedWriter writer, String line) throws IOException {
		writer.write(line + "\r\n");
	}

	private static void readResponse(BufferedReader reader, int... expectedCodes) throws IOException {
		String line;
		String lastLine = null;

		do {
			line = reader.readLine();
			if (line == null) {
				throw new IOException("Connexion SMTP fermée par le serveur.");
			}
			lastLine = line;
		} while (line.length() >= 4 && line.charAt(3) == '-');

		int code;
		try {
			code = Integer.parseInt(lastLine.substring(0, 3));
		} catch (Exception e) {
			throw new IOException("Réponse SMTP invalide: " + lastLine);
		}

		for (int expected : expectedCodes) {
			if (code == expected) {
				return;
			}
		}

		throw new IOException("Réponse SMTP inattendue: " + lastLine);
	}

	private static class Contact {
		final String name;
		final String email;

		Contact(String name, String email) {
			this.name = name;
			this.email = email;
		}
	}
}