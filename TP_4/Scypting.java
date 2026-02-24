import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Scypting {
	private static final String DOMAIN = "etu.univ-lyon1.fr";

	public static void main(String[] args) {
		if (args.length < 4) {
			System.out.println("Usage:");
			System.out.println("java Scypting <smtpHost> <smtpPort> <fromEmail> <nom1> [nom2] [nom3] ...");
			System.out.println("Exemple:");
			System.out.println("java Scypting smtp.univ-lyon1.fr 25 monlogin@etu.univ-lyon1.fr alice bob");
			return;
		}

		String smtpHost = args[0];
		int smtpPort = Integer.parseInt(args[1]);
		String fromEmail = args[2];

		try (Socket socket = new Socket(smtpHost, smtpPort);
			 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

			readResponse(in, 220);
			sendCommand(out, in, "HELO localhost", 250);

			for (int i = 3; i < args.length; i++) {
				String nom = args[i].trim().toLowerCase();
				if (nom.isEmpty()) {
					continue;
				}

				String toEmail = nom + "@" + DOMAIN;
				sendOneMail(out, in, fromEmail, toEmail, nom);
				System.out.println("Mail envoyé à " + toEmail);
			}

			sendCommand(out, in, "QUIT", 221);
			System.out.println("Terminé.");
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	private static void sendOneMail(BufferedWriter out, BufferedReader in, String fromEmail, String toEmail, String nom) throws IOException {
		sendCommand(out, in, "MAIL FROM:<" + fromEmail + ">", 250);
		sendCommand(out, in, "RCPT TO:<" + toEmail + ">", 250);
		sendCommand(out, in, "DATA", 354);

		out.write("From: " + fromEmail + "\r\n");
		out.write("To: " + toEmail + "\r\n");
		out.write("Subject: Bonjour\r\n");
		out.write("\r\n");
		out.write("Bonjour " + nom + " !\r\n");
		out.write(".\r\n");
		out.flush();

		readResponse(in, 250);
	}

	private static void sendCommand(BufferedWriter out, BufferedReader in, String command, int expectedCode) throws IOException {
		out.write(command + "\r\n");
		out.flush();
		readResponse(in, expectedCode);
	}

	private static void readResponse(BufferedReader in, int expectedCode) throws IOException {
		String line;
		String lastLine = null;

		do {
			line = in.readLine();
			if (line == null) {
				throw new IOException("Connexion fermée par le serveur SMTP");
			}
			lastLine = line;
			System.out.println("SMTP < " + line);
		} while (line.length() >= 4 && line.charAt(3) == '-');

		if (lastLine == null || lastLine.length() < 3) {
			throw new IOException("Réponse SMTP invalide");
		}

		int code = Integer.parseInt(lastLine.substring(0, 3));
		if (code != expectedCode) {
			throw new IOException("Code SMTP inattendu. Attendu " + expectedCode + ", reçu " + code + ". Détail: " + lastLine);
		}
	}
}
