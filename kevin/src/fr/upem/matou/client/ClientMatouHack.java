package fr.upem.matou.client;

import java.io.IOException;

import fr.upem.matou.client.network.ClientCoreHack;

@SuppressWarnings("javadoc")
public class ClientMatouHack {

	private ClientMatouHack() {
	}

	private static void usage() {
		System.err.println("Usage : host port");
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 2) {
			usage();
			return;
		}

		String host = args[0];
		int port = Integer.parseInt(args[1]);

		try (ClientCoreHack client = new ClientCoreHack(host, port)) {
			client.startChat();
		}
	}
}
