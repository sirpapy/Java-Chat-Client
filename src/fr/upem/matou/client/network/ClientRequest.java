package fr.upem.matou.client.network;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.logger.Logger.NetworkLogType;
import fr.upem.matou.shared.network.NetworkProtocol;

interface ClientRequest {
	static final String COMMAND_TOKEN = "/";

	static enum ClientCommandType {
		OPEN_PV("open"), SEND_PM("pv"), SEND_PF("file"), CLOSE_PV("close");

		private final String commandName;

		private ClientCommandType(String commandName) {
			this.commandName = commandName;
		}

		String getCommandName() {
			return commandName;
		}

		ClientRequest parseCommand(String[] tokens) {
			switch (this) {
			case CLOSE_PV: {
				String pseudo = tokens[1];
				return new ClientRequestClosePV(pseudo);
			}
			case OPEN_PV: {
				String pseudo = tokens[1];
				int port1 = Integer.parseInt(tokens[2]);
				int port2 = Integer.parseInt(tokens[3]);
				return new ClientRequestOpenPV(pseudo, port1, port2);
			}
			case SEND_PF: {
				String pseudo = tokens[1];
				Path path = Paths.get(tokens[2]);
				return new ClientRequestSendPF(pseudo, path);
			}
			case SEND_PM: {
				String pseudo = tokens[1];
				String message = Arrays.stream(tokens).skip(2).collect(Collectors.joining(" "));
				return new ClientRequestSendPM(pseudo, message);
			}
			default:
				throw new UnsupportedOperationException("Invalid command : " + this);
			}
		}
	}

	static class ClientRequestMSG implements ClientRequest {
		private final String message;

		ClientRequestMSG(String message) {
			this.message = message;
		}

		@Override
		public boolean sendRequest(SocketChannel sc) throws IOException {
			Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.MSG);
			Logger.network(NetworkLogType.WRITE, "MESSAGE : " + message);
			return ClientCommunication.sendRequestMSG(sc, message);
		}
	}
	
	static class ClientRequestClosePV implements ClientRequest {
		private final String pseudo;
		
		public ClientRequestClosePV(String pseudo) {
			this.pseudo = pseudo;
		}

		@Override
		public boolean sendRequest(SocketChannel sc) throws IOException {
			// TODO Auto-generated method stub
			return false;
		}

	}

	
	static class ClientRequestOpenPV implements ClientRequest {
		private final String pseudo;
		private final int portMessages;
		private final int portFiles;
		
		public ClientRequestOpenPV(String pseudo, int portMessages, int portFiles) {
			this.pseudo = pseudo;
			this.portMessages = portMessages;
			this.portFiles = portFiles;
		}

		@Override
		public boolean sendRequest(SocketChannel sc) throws IOException {
			// TODO Auto-generated method stub
			return false;
		}

	}
	
	static class ClientRequestSendPF implements ClientRequest {
		private final String pseudo;
		private final Path path;
		
		public ClientRequestSendPF(String pseudo, Path path) {
			this.pseudo = pseudo;
			this.path = path;
		}

		@Override
		public boolean sendRequest(SocketChannel sc) throws IOException {
			// TODO Auto-generated method stub
			return false;
		}

	}
	
	static class ClientRequestSendPM implements ClientRequest {
		private final String pseudo;
		private final String message;

		public ClientRequestSendPM(String pseudo, String message) {
			this.pseudo = pseudo;
			this.message = message;
		}

		@Override
		public boolean sendRequest(SocketChannel sc) throws IOException {
			// TODO Auto-generated method stub
			return false;
		}

	}
	
	static boolean isCommandMessage(String message) {
		return message.startsWith(COMMAND_TOKEN);
	}

	static ClientRequest parseLine(String message) {
		if (!isCommandMessage(message)) {
			return new ClientRequestMSG(message);
		}
		String[] tokens = message.split(" ");

		String command = tokens[0].replace(COMMAND_TOKEN, "");

		for (ClientCommandType type : ClientCommandType.values()) {
			String commandName = type.getCommandName();
			if (command.equals(commandName)) {
				return type.parseCommand(tokens);
			}
		}

		throw new IllegalArgumentException("Command " + command + " does not exist");
	}

	boolean sendRequest(SocketChannel sc) throws IOException;

}
