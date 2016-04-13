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
		public boolean execute(ClientSession session) throws IOException {
			return session.sendMessage(message);
		}
	}

	public static class ClientEventClosePrivate implements ClientEvent {
		private final String username;

		public ClientEventClosePrivate(String username) {
			this.username = username;
		}

		@Override
		public boolean execute(ClientSession session) throws IOException {
			return session.closePrivateConnection(username);
		}

	}

	public static class ClientEventOpenPrivate implements ClientEvent {
		private final String username;

		public ClientEventOpenPrivate(String username) {
			this.username = username;
		}

		@Override
		public boolean execute(ClientSession session) throws IOException {
			return session.openPrivateConnection(username);
		}

	}

	public static class ClientEventAcceptPrivate implements ClientEvent {
		private final String username;

		public ClientEventAcceptPrivate(String username) {
			this.username = username;
		}

		@Override
		public boolean execute(ClientSession session) throws IOException {
			return session.acceptPrivateConnection(username);
		}
	}

	public static class ClientEventSendPrivateFile implements ClientEvent {
		private final String username;
		private final Path path;

		public ClientEventSendPrivateFile(String username, Path path) {
			this.username = username;
			this.path = path;
		}

		@Override
		public boolean execute(ClientSession session) throws IOException {
			return session.sendPrivateFile(username,path);
		}

	}

	public static class ClientEventSendPrivateMessage implements ClientEvent {
		private final String username;
		private final String message;

		public ClientEventSendPrivateMessage(String username, String message) {
			this.username = username;
			this.message = message;
		}

		@Override
		public boolean execute(ClientSession session) throws IOException {
			return session.sendPrivateMessage(username,message);
		}

	}

	public boolean execute(ClientSession session) throws IOException;

}
