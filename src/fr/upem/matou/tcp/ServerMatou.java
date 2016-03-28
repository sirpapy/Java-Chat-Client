package fr.upem.matou.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import fr.upem.matou.logger.Logger;

/*
 * This class is the core of the server.
 */
@SuppressWarnings("resource")
public class ServerMatou implements Closeable {

	private final ServerSocketChannel ssc;
	private final Selector selector;
	private final ServerDataBase db;

	public ServerMatou(int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(port);
		ssc = ServerSocketChannel.open();
		ssc.bind(address);
		ssc.configureBlocking(false);
		selector = Selector.open();
		ssc.register(selector, SelectionKey.OP_ACCEPT);
		db = new ServerDataBase(selector.keys());
	}

	public void launch() throws IOException {
		Set<SelectionKey> selectedKeys = selector.selectedKeys();

		while (!Thread.interrupted()) {

			SelectorDebug.printKeys(selector);
			selector.select();

			SelectorDebug.printSelectedKeys(selectedKeys);
			processSelectedKeys(selectedKeys);

			selectedKeys.clear();

			Logger.selectInfo("");
		}
	}

	private void processSelectedKeys(Set<SelectionKey> selectedKeys) throws IOException {
		for (SelectionKey key : selectedKeys) {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
			try {
				if (key.isValid() && key.isWritable()) {
					doWrite(key);
				}
				if (key.isValid() && key.isReadable()) {
					doRead(key);
				}
			} catch (IOException e) {
				Logger.exception(e);
				ServerSession session = (ServerSession) key.attachment();
				session.silentlyClose();
			}
		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		ServerSocketChannel channel = (ServerSocketChannel) key.channel();
		SocketChannel acceptedChannel = channel.accept();
		if (acceptedChannel == null) {
			return;
		}
		acceptedChannel.configureBlocking(false);
		SelectionKey registeredKey = acceptedChannel.register(selector, SelectionKey.OP_READ);
		ServerSession session = new ServerSession(db,acceptedChannel);
		registeredKey.attach(session);
	}

	private void doRead(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ServerSession session = (ServerSession) key.attachment();
		ByteBuffer bb = session.getReadBuffer();

		if (channel.read(bb) == -1) {
			session.silentlyClose();
			return;
		}

		session.updateStateRead();
		db.updateStateReadAll();

		boolean active = session.updateInterestOps(key);
		if (!active) {
			Logger.debug("INACTIVE AFTER READ");
			session.silentlyClose();
		}
	}

	@SuppressWarnings("static-method") // TEMP
	private void doWrite(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ServerSession session = (ServerSession) key.attachment();
		ByteBuffer bb = session.getWriteBuffer();

		bb.flip();
		channel.write(bb);
		bb.compact();

		session.updateStateWrite();

		boolean active = session.updateInterestOps(key);
		if (!active) {
			Logger.debug("INACTIVE AFTER WRITE");
			session.silentlyClose();
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
		try (ServerMatou server = new ServerMatou(port)) {
			server.launch();
		}
	}

}
