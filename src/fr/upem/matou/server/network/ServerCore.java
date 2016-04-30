package fr.upem.matou.server.network;

import static fr.upem.matou.shared.logger.Logger.formatNetworkData;
import static fr.upem.matou.shared.logger.Logger.formatNetworkRequest;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.Set;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.logger.Logger.NetworkLogType;
import fr.upem.matou.shared.network.NetworkCommunication;

/**
 * This class is the core of the chat server.
 */
@SuppressWarnings("resource")
public class ServerCore implements Closeable {

	private final ServerSocketChannel ssc;
	private final Selector selector;
	private final ServerDataBase db;

	/**
	 * Constructs a new server core.
	 * 
	 * @param port
	 *            The port to listen.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public ServerCore(int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(port);
		ssc = ServerSocketChannel.open();
		ssc.bind(address);
		ssc.configureBlocking(false);
		selector = Selector.open();
		ssc.register(selector, SelectionKey.OP_ACCEPT);
		db = new ServerDataBase(selector.keys());
	}

	/**
	 * Starts a server chat.
	 * 
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public void launch() throws IOException {
		Set<SelectionKey> selectedKeys = selector.selectedKeys();

		while (!Thread.interrupted()) {

			ServerLogger.logSelector(selector);
			selector.select();

			ServerLogger.logSelectedKeys(selectedKeys);
			processSelectedKeys(selectedKeys);

			selectedKeys.clear();
			
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
				Logger.warning(formatNetworkData(sc, e.toString()));
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
		Optional<ServerSession> optional = db.newServerSession(acceptedChannel, registeredKey);
		if (!optional.isPresent()) {
			Logger.warning(formatNetworkData(acceptedChannel, "Server is full"));
			NetworkCommunication.silentlyClose(acceptedChannel);
			return;
		}
		ServerSession session = optional.get();
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
			Logger.info(formatNetworkRequest(channel, NetworkLogType.WRITE, "BUFFER = " + bb));
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
