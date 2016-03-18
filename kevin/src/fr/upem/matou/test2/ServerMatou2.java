package fr.upem.matou.test2;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Optional;

public class ServerMatou2 implements Closeable {

    private final ServerSocketChannel ssc;

    public ServerMatou2(int port) throws IOException {
	InetSocketAddress address = new InetSocketAddress(port);
	ssc = ServerSocketChannel.open();
	ssc.bind(address);
	System.out.println("SERVER : OK");
    }

    public void launch() throws IOException {
	while (!Thread.interrupted()) {
	    try (SocketChannel sc = ssc.accept()) {
		System.out.println("CONNECTION ACCEPTED : " + sc.getRemoteAddress());
		serve: while (true) {
		    Optional<NetworkProtocol2> optCode = NetworkProtocol2.receiveProtocolRequest(sc);
		    if (!optCode.isPresent()) {
			System.out.println("DECONNECTION");
			break;
		    }
		    NetworkProtocol2 code = optCode.get();
		    System.out.println("CODE = " + code);

		    switch (code) {
		    case CLIENT_PUBLIC_CONNECTION_REQUEST:
			Optional<String> optPseudo = NetworkProtocol2.receiveRequestCOREQ(sc);
			if (!optPseudo.isPresent()) {
			    System.out.println("DECONNECTION");
			    break serve;
			}
			System.out.println("OPTIONAL = " + optPseudo);
			break;
		    case CLIENT_PUBLIC_MESSAGE:
			break;
		    default:
			break;
		    }

		}
	    }
	}
    }

    @Override
    public void close() throws IOException {
	ssc.close();
    }

    private static void usage() {
	System.err.println("Usage : port");
    }

    public static void main(String[] args) throws IOException {
	if (args.length != 1) {
	    usage();
	    return;
	}
	int port = Integer.parseInt(args[0]);
	try (ServerMatou2 server = new ServerMatou2(port)) {
	    server.launch();
	}
    }

}
