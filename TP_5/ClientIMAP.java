import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ClientIMAP {

	private static final int DEFAULT_PORT = 143;

	public static void main(String[] args) {
		if (args.length < 3 || args.length > 4) {
			System.out.println("Usage: java ClientIMAP <host> [port] <user> <password>");
			return;
		}

		String host;
		int port;
		String user;
		String password;

		if (args.length == 3) {
			host = args[0];
			port = DEFAULT_PORT;
			user = args[1];
			password = args[2];
		} else {
			host = args[0];
			try {
				port = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.println("Port invalide: " + args[1]);
				return;
			}
			user = args[2];
			password = args[3];
		}

		try {
			new ClientIMAP().run(host, port, user, password);
		} catch (IOException e) {
			System.err.println("Erreur IMAP: " + e.getMessage());
		}
	}

	private void run(String host, int port, String user, String password) throws IOException {
		try (Socket socket = new Socket(host, port);
			 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			 BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

			String greeting = input.readLine();
			if (greeting == null || !isOkUntagged(greeting)) {
				throw new IOException("salutation IMAP invalide: " + greeting);
			}

			ImapSession session = new ImapSession(input, output);

			session.simpleCommand("LOGIN " + quote(user) + " " + quote(password), "échec LOGIN");
			session.simpleCommand("SELECT INBOX", "échec SELECT INBOX");

			List<Integer> unreadIds = session.searchUnseen();

			if (unreadIds.isEmpty()) {
				System.out.println("Aucun mail non lu.");
			} else {
				for (int messageId : unreadIds) {
					String content = session.fetchMessageBody(messageId);
					System.out.println("----- MAIL " + messageId + " -----");
					System.out.println(content);
					System.out.println("-----------------------");

					session.simpleCommand("STORE " + messageId + " +FLAGS (\\\\Deleted)",
							"impossible de marquer le message " + messageId + " pour suppression");
				}

				session.simpleCommand("EXPUNGE", "échec EXPUNGE");
				System.out.println(unreadIds.size() + " mail(s) non lu(s) supprimé(s) définitivement.");
			}

			session.simpleCommand("LOGOUT", "échec LOGOUT");
		}
	}

	private static boolean isOkUntagged(String line) {
		return line.toUpperCase(Locale.ROOT).startsWith("* OK");
	}

	private static String quote(String value) {
		String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
		return '"' + escaped + '"';
	}

	private static final class ImapSession {
		private final BufferedReader input;
		private final BufferedWriter output;
		private int tagCounter = 1;

		private ImapSession(BufferedReader input, BufferedWriter output) {
			this.input = input;
			this.output = output;
		}

		void simpleCommand(String command, String errorPrefix) throws IOException {
			CommandResult result = sendCommand(command);
			if (!result.isOk()) {
				throw new IOException(errorPrefix + ": " + result.taggedLine);
			}
		}

		List<Integer> searchUnseen() throws IOException {
			CommandResult result = sendCommand("SEARCH UNSEEN");
			if (!result.isOk()) {
				throw new IOException("échec SEARCH UNSEEN: " + result.taggedLine);
			}

			for (String line : result.untaggedLines) {
				String upper = line.toUpperCase(Locale.ROOT);
				if (upper.startsWith("* SEARCH")) {
					String idsPart = line.length() > 8 ? line.substring(8).trim() : "";
					if (idsPart.isEmpty()) {
						return Collections.emptyList();
					}

					String[] tokens = idsPart.split("\\s+");
					List<Integer> ids = new ArrayList<>();
					for (String token : tokens) {
						try {
							ids.add(Integer.parseInt(token));
						} catch (NumberFormatException ignored) {
						}
					}
					return ids;
				}
			}

			return Collections.emptyList();
		}

		String fetchMessageBody(int messageId) throws IOException {
			CommandResult result = sendCommand("FETCH " + messageId + " BODY[TEXT]");
			if (!result.isOk()) {
				throw new IOException("échec FETCH pour le message " + messageId + ": " + result.taggedLine);
			}

			StringBuilder body = new StringBuilder();
			boolean collecting = false;

			for (String line : result.untaggedLines) {
				if (!collecting) {
					if (line.toUpperCase(Locale.ROOT).startsWith("* " + messageId + " FETCH")) {
						collecting = true;
					}
					continue;
				}

				if (line.trim().equals(")")) {
					break;
				}

				if (body.length() > 0) {
					body.append(System.lineSeparator());
				}
				body.append(line);
			}

			if (body.length() == 0) {
				return "(contenu vide)";
			}
			return body.toString();
		}

		private CommandResult sendCommand(String command) throws IOException {
			String tag = nextTag();
			writeLine(tag + " " + command);

			List<String> untagged = new ArrayList<>();
			String line;
			while ((line = input.readLine()) != null) {
				if (line.startsWith(tag + " ")) {
					return new CommandResult(line, untagged);
				}
				untagged.add(line);
			}

			throw new IOException("connexion fermée par le serveur");
		}

		private String nextTag() {
			return String.format(Locale.ROOT, "A%04d", tagCounter++);
		}

		private void writeLine(String line) throws IOException {
			output.write(line);
			output.write("\r\n");
			output.flush();
		}
	}

	private static final class CommandResult {
		private final String taggedLine;
		private final List<String> untaggedLines;

		private CommandResult(String taggedLine, List<String> untaggedLines) {
			this.taggedLine = taggedLine;
			this.untaggedLines = untaggedLines;
		}

		private boolean isOk() {
			return taggedLine.toUpperCase(Locale.ROOT).contains(" OK");
		}
	}
}
