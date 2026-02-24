import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Scypting {
	private static final String SMTP_HOST = "stadium.univ-lyon1.fr";
	private static final int SMTP_PORT = 25;
	private static final String FROM_ADDRESS = "utilisateur@etu.univ-lyon1.fr";

	private static class Recipient {
		private final String name;
		private final String email;

		private Recipient(String name, String email) {
			this.name = name;
			this.email = email;
		}
	}

	public static void main(String[] args) throws IOException {
		List<Recipient> recipients = readRecipients();

		for (Recipient recipient : recipients) {
			String body = "Bonjour " + recipient.name + " !";
			sendMail(recipient.email, body);
		}
	}

	private static List<Recipient> readRecipients() throws IOException {
		List<Recipient> recipients = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String line;

		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty()) {
				continue;
			}

			Recipient recipient = parseRecipient(line);
			if (recipient != null) {
				recipients.add(recipient);
			}
		}

		return recipients;
	}

	private static Recipient parseRecipient(String line) {
		String[] parts = line.split("[,;\\s]+", 2);
		if (parts.length < 2) {
			return null;
		}

		String name = parts[0].trim();
		String email = parts[1].trim();

		if (name.isEmpty() || email.isEmpty()) {
			return null;
		}

		return new Recipient(name, email);
	}

	private static void sendMail(String email, String body) throws IOException {
		try (Socket socket = new Socket(SMTP_HOST, SMTP_PORT);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

			expectCode(readLine(reader), 220, "Server greeting");

			String heloHost = getLocalHostName();
			sendCommand(writer, "HELO " + heloHost);
			expectCode(readLine(reader), 250, "HELO");

			sendCommand(writer, "MAIL FROM:<" + FROM_ADDRESS + ">");
			expectCode(readLine(reader), 250, "MAIL FROM");

			sendCommand(writer, "RCPT TO:<" + email + ">");
			expectCode(readLine(reader), 250, "RCPT TO");

			sendCommand(writer, "DATA");
			expectCode(readLine(reader), 354, "DATA");

			writeData(writer,
					"From: " + FROM_ADDRESS,
					"To: " + email,
					"Subject: Bonjour",
					"",
					body,
					".");
			expectCode(readLine(reader), 250, "Message body");

			sendCommand(writer, "QUIT");
			readLine(reader);
		}
	}

	private static void sendCommand(BufferedWriter writer, String command) throws IOException {
		writer.write(command);
		writer.write("\r\n");
		writer.flush();
	}

	private static void writeData(BufferedWriter writer, String... lines) throws IOException {
		for (String line : lines) {
			writer.write(line);
			writer.write("\r\n");
		}
		writer.flush();
	}

	private static String readLine(BufferedReader reader) throws IOException {
		String line = reader.readLine();
		if (line == null) {
			throw new IOException("Unexpected end of SMTP stream");
		}
		return line;
	}

	private static void expectCode(String response, int expected, String step) throws IOException {
		if (!response.startsWith(String.valueOf(expected))) {
			throw new IOException("SMTP error at " + step + ": " + response);
		}
	}

	private static String getLocalHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (IOException ex) {
			return "localhost";
		}
	}
}
