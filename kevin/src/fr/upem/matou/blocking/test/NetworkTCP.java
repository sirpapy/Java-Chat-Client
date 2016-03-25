package fr.upem.matou.blocking.test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Optional;

public class NetworkTCP {
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private NetworkTCP() {
    }

    public static boolean netRead(SocketChannel sc, ByteBuffer bb) throws IOException {
	while (bb.hasRemaining()) {
	    if (sc.read(bb) == -1) {
		return false;
	    }
	}
	return true;
    }

    public static int netWrite(SocketChannel sc, ByteBuffer bb) throws IOException {
	bb.flip();
	int written = sc.write(bb);
	return written;
    }

    public static Optional<Integer> readInt(SocketChannel sc) throws IOException {
	ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
	if (!netRead(sc, bb)) {
	    return Optional.empty();
	}
	bb.flip();
	int value = bb.getInt();
	return Optional.of(value);
    }

    public static Optional<String> readStringUTF8(SocketChannel sc, int size) throws IOException {
	ByteBuffer bb = ByteBuffer.allocate(size);
	if (!netRead(sc, bb)) {
	    return Optional.empty();
	}
	bb.flip();
	String str = UTF8_CHARSET.decode(bb).toString();
	return Optional.of(str);
    }

    public static int writeInt(SocketChannel sc, int value) throws IOException {
	ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
	bb.putInt(value);
	return netWrite(sc, bb);
    }

    public static int writeStringUTF8(SocketChannel sc, String str) throws IOException {
	ByteBuffer bb = UTF8_CHARSET.encode(str);
	int size = bb.remaining();
	bb.compact();
	return writeInt(sc, size) + netWrite(sc, bb);
    }
}
