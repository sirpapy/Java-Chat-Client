package fr.upem.matou.server.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.utils.ByteBuffers;

/*
 * This class is the core of the server.
 */
@SuppressWarnings("resource")
public class ServerCore implements Closeable {

	private static final boolean DELAY_ENABLED = false;

	private final ServerSocketChannel ssc;
	private final Selector selector;
	private final ServerDataBase db;

	public ServerCore(int port) throws IOException {
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
				SocketChannel sc = (SocketChannel) key.channel();
				Logger.warning(sc.getRemoteAddress() + " | " + e.toString());
				ServerSession session = (ServerSession) key.attachment();
				session.disconnectClient();
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
		ServerSession session = db.newServerSession(acceptedChannel, registeredKey);
		registeredKey.attach(session);
	}

	private static void doRead(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ServerSession session = (ServerSession) key.attachment();
		ByteBuffer bb = session.getReadBuffer();

		if (channel.read(bb) == -1) {
			session.disconnectClient();
			return;
		}

		session.updateStateRead();

		session.updateKey();
	}

	private static void doWrite(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ServerSession session = (ServerSession) key.attachment();
		ByteBuffer bb = session.getWriteBuffer();

		bb.flip();
		try {
			Logger.debug("WRITING BUFFER : " + bb);
			channel.write(bb);
		} finally {
			bb.compact();
		}
		
		session.updateKey();
	}

	@Override
	public void close() throws IOException {
		ssc.close();
	}

}
