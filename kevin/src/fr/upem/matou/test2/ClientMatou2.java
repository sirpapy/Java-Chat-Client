package fr.upem.matou.test2;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ClientMatou2 implements Closeable {

    private final SocketChannel sc;

    public ClientMatou2(String host, int port) throws IOException {
	InetSocketAddress address = new InetSocketAddress(host, port);
	sc = SocketChannel.open(address);
	System.out.println("CONNECTED TO : " + sc.getRemoteAddress());
    }

    public void startChat() throws IOException {
	try (Scanner scanner = new Scanner(System.in)) {
	    while (true) {
		System.out.print("Pseudo : ");
		String pseudo = scanner.nextLine();
		NetworkProtocol2.sendRequestCOREQ(sc, pseudo);
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

	try (ClientMatou2 client = new ClientMatou2(host, port)) {
	    client.startChat();
	}
    }

}
