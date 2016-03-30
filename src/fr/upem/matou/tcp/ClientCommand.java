package fr.upem.matou.tcp;

import java.nio.ByteBuffer;

class ClientCommand {
	private static final String COMMAND_TOKEN = "/";

	static enum ClientCommandType {
		OPEN_PV("open"), SEND_PM("pv"), SEND_PF("file"), CLOSE_PV("close");

		private final String commandName;

		private ClientCommandType(String commandName) {
			this.commandName = commandName;
		}

		String getCommandName() {
			return commandName;
		}

	}

	private final ClientCommandType commandType;
	private final String argument;

	private ClientCommand(ClientCommandType commandType, String argument) {
		this.commandType = commandType;
		this.argument = argument;
	}

	static boolean isCommandMessage(String message) {
		return message.startsWith(COMMAND_TOKEN);
	}

	static ClientCommand parseCommand(String message) {
		String[] tokens = message.split(" ");

		if (tokens.length != 2) {
			throw new IllegalArgumentException("Command must have 1 and only 1 argument");
		}

		String command = tokens[0];
		String arg = tokens[1];

		for (ClientCommandType type : ClientCommandType.values()) {
			String commandName = type.getCommandName();
			if (command.equals(commandName)) {
				return new ClientCommand(type, arg);
			}
		}

		throw new IllegalArgumentException("Command " + command + " does not exist");
	}

	ByteBuffer encodeToRequest() {
		// TODO
		
		ByteBuffer request = null; 
		
		switch (commandType) {
		case CLOSE_PV:
//			ClientCommunication.encodeRequestPVDISCO();
			break;
		case OPEN_PV:
//			ClientCommunication.encodeRequestPVCOREQ();
			break;
		case SEND_PF:
//			ClientCommunication.encodeRequestPVFILE();
			break;
		case SEND_PM:
//			ClientCommunication.encodeRequestPVMG();
			break;
		default:
			throw new UnsupportedOperationException("This command cannot be encoded");
		}
		
		return request;
	}

}
