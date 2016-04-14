package fr.upem.matou.client.network;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;

import fr.upem.matou.shared.network.Username;

public interface ClientEvent {
	static final String COMMAND_TOKEN = "/";

	public static enum ClientEventType {
		SEND_MESSAGE, OPEN_PRIVATE, ACCEPT_PRIVATE, SEND_PRIVATE_MESSAGE, SEND_PRIVATE_FILE, CLOSE_PRIVATE;
	}

	public static class ClientEventSendMessage implements ClientEvent {
		private final String message;

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

	public static class ClientEventClosePrivate implements ClientEvent {
		private final Username username;

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

	public static class ClientEventOpenPrivate implements ClientEvent {
		private final Username username;

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

	public static class ClientEventAcceptPrivate implements ClientEvent {
		private final Username username;

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

	public static class ClientEventSendPrivateFile implements ClientEvent {
		private final Username username;
		private final Path path;

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

	public static class ClientEventSendPrivateMessage implements ClientEvent {
		private final Username username;
		private final String message;

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

	public boolean execute(ClientSession session) throws IOException;

}
