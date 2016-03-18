package fr.upem.matou.test1;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Optional;

public class ServerMatou1 implements Closeable {

    private final ServerSocketChannel ssc;

    public ServerMatou1(int port) throws IOException {
	InetSocketAddress address = new InetSocketAddress(port);
	ssc = ServerSocketChannel.open();
	ssc.bind(address);
	System.out.println("SERVER : OK");
    }

    public void launch() throws IOException {
	while (!Thread.interrupted()) {
	    try (SocketChannel sc = ssc.accept()) {
		System.out.println("CONNECTION ACCEPTED : " + sc.getRemoteAddress());
		while (true) {
		    Optional<Object> opt = NetworkProtocol1.receiveMessage(sc);
		    if(!opt.isPresent()) {
			System.out.println("DECONNECTION");
			break;
		    }
		    System.out.println("OPTIONAL = " + opt);
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
	try (ServerMatou1 server = new ServerMatou1(port)) {
	    server.launch();
	}
    }

}
