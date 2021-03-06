package fr.upem.matou.client.network;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;

import fr.upem.matou.shared.network.Username;

/**
 * This class provides events requested by a client user through the user interface on a client chat session.
 */
public interface ClientEvent {

	/**
	 * Sending a public connection request.
	 */
	public static class ClientEventConnection implements ClientEvent {
		private final Username username;

		/**
		 * Public connection event.
		 * 
		 * @param username
		 *            The requested username.
		 */
		public ClientEventConnection(Username username) {
			requireNonNull(username);
			this.username = username;
		}

		@Override
		public boolean execute(ClientSession session) throws IOException {
			requireNonNull(session);
			return session.sendUsernameRequest(username);
		}

	}

	/**
	 * Sending a public message.
	 */
	public static class ClientEventSendMessage implements ClientEvent {
		private final String message;

		/**
		 * Public message event.
		 * 
		 * @param message
		 *            The message.
		 */
		public ClientEventSendMessage(String message) {
			requireNonNull(message);
			this.message = message;
		}

		@Override
		public boolean execute(ClientSession session) throws IOException {
			requireNonNull(session);
			return session.sendMessage(message);
		}

	}

	/**
	 * Sending a private connection request.
	 */
	public static class ClientEventOpenPrivate implements ClientEvent {
		private final Username username;

		/**
		 * Private connection request event.
		 * 
		 * @param username
		 *            The target username.
		 */
		public ClientEventOpenPrivate(String username) {
			requireNonNull(username);
			this.username = new Username(username);
		}

		@Override
		public boolean execute(ClientSession session) throws IOException {
			requireNonNull(session);
			return session.openPrivateConnection(username);
		}

	}

	/**
	 * Sending a private connection acceptation.
	 */
	public static class ClientEventAcceptPrivate implements ClientEvent {
		private final Username username;

		/**
		 * Private connection request event.
		 * 
		 * @param username
		 *            The target username.
		 */
		public ClientEventAcceptPrivate(String username) {
			requireNonNull(username);
			this.username = new Username(username);
		}

		@Override
		public boolean execute(ClientSession session) throws IOException {
			requireNonNull(session);
			return session.acceptPrivateConnection(username);
		}

	}

	/**
	 * Sending a private message.
	 */
	public static class ClientEventSendPrivateMessage implements ClientEvent {
		private final Username username;
		private final String message;

		/**
		 * Private message event.
		 * 
		 * @param username
		 *            The target username.
		 * @param message
		 *            The message to send.
		 */
		public ClientEventSendPrivateMessage(String username, String message) {
			requireNonNull(username);
			requireNonNull(message);
			this.username = new Username(username);
			this.message = message;
		}

		@Override
		public boolean execute(ClientSession session) {
			requireNonNull(session);
			return session.sendPrivateMessage(username, message);
		}

	}

	/**
	 * Sending a private file.
	 */
	public static class ClientEventSendPrivateFile implements ClientEvent {
		private final Username username;
		private final Path path;

		/**
		 * Private file event.
		 * 
		 * @param username
		 *            The target username.
		 * @param path
		 *            The path of the file to send.
		 */
		public ClientEventSendPrivateFile(String username, Path path) {
			requireNonNull(username);
			requireNonNull(path);
			this.username = new Username(username);
			this.path = path;
		}

		@Override
		public boolean execute(ClientSession session) {
			requireNonNull(session);
			return session.sendPrivateFile(username, path);
		}

	}

	/**
	 * Closing a private connection.
	 */
	public static class ClientEventClosePrivate implements ClientEvent {
		private final Username username;

		/**
		 * Private connection close event.
		 * 
		 * @param username
		 *            The target username.
		 */
		public ClientEventClosePrivate(String username) {
			requireNonNull(username);
			this.username = new Username(username);
		}

		@Override
		public boolean execute(ClientSession session) throws IOException {
			requireNonNull(session);
			return session.closePrivateConnection(username);
		}

	}

	/**
	 * Executes this event on a session.
	 * 
	 * @param session
	 *            The current client session.
	 * @return true if the event succeeded, false otherwise.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public boolean execute(ClientSession session) throws IOException;

}
