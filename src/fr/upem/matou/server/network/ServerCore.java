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

import fr.upem.matou.buffer.ByteBuffers;
import fr.upem.matou.logger.Logger;

/*
 * This class is the core of the server.
 */
@SuppressWarnings("resource")
public class ServerCore implements Closeable {

	private static final boolean DELAY_ENABLED = false;
	private static final long SERVER_DELAY = 100; // TEMP in millis

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
		ServerSession session = new ServerSession(db, acceptedChannel);
		registeredKey.attach(session);
	}

	private void doRead(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ServerSession session = (ServerSession) key.attachment();
		ByteBuffer bb = session.getReadBuffer();

		if (channel.read(bb) == -1) {
			session.disconnectClient();
			return;
		}

		session.updateStateRead();
		db.updateStateReadAll();

		if (!key.isValid()) {
			Logger.debug("Key not valid anymore");
			return;
		}

		int ops = session.computeInterestOps();
		if (ops == 0) {
			throw new AssertionError("Key is inactive after read");
		}
		key.interestOps(ops);

	}

	private void doWrite(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ServerSession session = (ServerSession) key.attachment();
		ByteBuffer bb = session.getWriteBuffer();

		bb.flip();
		Logger.debug("WRITING BUFFER : " + bb);
		if (DELAY_ENABLED) {
			for (int i = 1; bb.hasRemaining(); i++) { // TEMP
				ByteBuffer writter = ByteBuffer.allocate(1);
				byte oneByte = bb.get();
				writter.put(oneByte);
				writter.flip();
				int written = channel.write(writter);
				if (written == 0) {
					Logger.debug("/!\\ WRITING NOT FINISHED /!\\");
					break;
				}
				Logger.debug("Byte #" + i + " sent : " + ByteBuffers.toBinaryString(oneByte)
						+ " (~" + ((i - 1) * SERVER_DELAY) + "ms)");
				try {
					Thread.sleep(SERVER_DELAY);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} else {
			channel.write(bb);
		}
		bb.compact();

		session.updateStateWrite();
		db.updateStateWriteAll();

		if (!key.isValid()) {
			Logger.debug("Key not valid anymore");
			return;
		}

		int ops = session.computeInterestOps();
		if (ops == 0) {
			throw new AssertionError("Key is inactive after write");
		}
		key.interestOps(ops);
	}

	@Override
	public void close() throws IOException {
		ssc.close();
	}

}
