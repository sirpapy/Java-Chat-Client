package fr.upem.matou.client.network;

import java.io.IOException;
import java.nio.file.Path;

public interface ClientEvent {
	static final String COMMAND_TOKEN = "/";

	public static enum ClientEventType {
		SEND_MESSAGE, OPEN_PRIVATE, ACCEPT_PRIVATE, SEND_PRIVATE_MESSAGE, SEND_PRIVATE_FILE, CLOSE_PRIVATE;
	}

	public static class ClientEventSendMessage implements ClientEvent {
		private final String message;

		public ClientEventSendMessage(String message) {
			this.message = message;
		}

		@Override
		public boolean execute(ClientDataBase db) throws IOException {
			return db.sendMessage(message);
		}
	}

	public static class ClientEventClosePrivate implements ClientEvent {
		private final String pseudo;

		public ClientEventClosePrivate(String pseudo) {
			this.pseudo = pseudo;
		}

		@Override
		public boolean execute(ClientDataBase db) throws IOException {
			// TODO
			throw new UnsupportedOperationException("Unimplemented");
		}

	}

	public static class ClientEventOpenPrivate implements ClientEvent {
		private final String pseudo;

		public ClientEventOpenPrivate(String pseudo) {
			this.pseudo = pseudo;
		}

		@Override
		public boolean execute(ClientDataBase db) throws IOException {
			return db.openPrivateConnection(pseudo);
		}

	}

	public static class ClientEventAcceptPrivate implements ClientEvent {
		private final String pseudo;

		public ClientEventAcceptPrivate(String pseudo) {
			this.pseudo = pseudo;
		}

		@Override
		public boolean execute(ClientDataBase db) throws IOException {
			// TODO
			throw new UnsupportedOperationException("Unimplemented");
		}
	}

	public static class ClientEventSendPrivateFile implements ClientEvent {
		private final String pseudo;
		private final Path path;

		public ClientEventSendPrivateFile(String pseudo, Path path) {
			this.pseudo = pseudo;
			this.path = path;
		}

		@Override
		public boolean execute(ClientDataBase db) throws IOException {
			// TODO
			throw new UnsupportedOperationException("Unimplemented");
		}

	}

	public static class ClientEventSendPrivateMessage implements ClientEvent {
		private final String pseudo;
		private final String message;

		public ClientEventSendPrivateMessage(String pseudo, String message) {
			this.pseudo = pseudo;
			this.message = message;
		}

		@Override
		public boolean execute(ClientDataBase db) throws IOException {
			// TODO
			throw new UnsupportedOperationException("Unimplemented");
		}

	}

	public boolean execute(ClientDataBase db) throws IOException;

}
