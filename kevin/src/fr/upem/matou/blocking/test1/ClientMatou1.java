package fr.upem.matou.blocking.test1;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ClientMatou1 implements Closeable {

    private final SocketChannel sc;

    public ClientMatou1(String host, int port) throws IOException {
	InetSocketAddress address = new InetSocketAddress(host, port);
	sc = SocketChannel.open(address);
	System.out.println("CONNECTED TO : " + sc.getRemoteAddress());
    }

    public void startChat() throws IOException {
	try (Scanner scanner = new Scanner(System.in)) {
	    while (true) {
		System.out.print("Pseudo : ");
		String pseudo = scanner.nextLine();
		NetworkProtocol1.sendMessage(sc, NetworkProtocol1.CLIENT_PUBLIC_CONNECTION_REQUEST, pseudo);
	    }
	}
    }

    @Override
    public void close() throws IOException {
	sc.close();
    }

    private static void usage() {
	System.err.println("Usage : host port");
    }

    public static void main(String[] args) throws IOException {
	if (args.length != 2) {
	    usage();
	    return;
	}

	String host = args[0];
	int port = Integer.parseInt(args[1]);

	try (ClientMatou1 client = new ClientMatou1(host, port)) {
	    client.startChat();
	}
    }

}
