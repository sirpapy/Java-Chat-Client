package fr.upem.matou.shared.network;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Optional;

import fr.upem.matou.shared.logger.Logger;

/*
 * This class gathers the common factors between ClientCommunication and ServerCommunication.
 */
public class NetworkCommunication {
	private static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");

	static final int LENGTH_SIZE = Integer.BYTES;
	static final int USERNAME_MAX_SIZE = 32;
	static final int MESSAGE_MAX_SIZE = 512;
	static final int FILE_CHUNK_SIZE = 4096;

	private NetworkCommunication() {
	}

	private static boolean isValidUsernameCharacter(int codePoint) {
		return Character.isLetterOrDigit(codePoint);
	}

	private static boolean isValidMessageCharacter(int codePoint) {
		return !Character.isISOControl(codePoint);
	}

	public static boolean checkUsernameValidity(String username) {
		if (username == null) {
			return false;
		}
		return username.chars().allMatch(NetworkCommunication::isValidUsernameCharacter);
	}

	public static boolean checkMessageValidity(String message) {
		if (message == null) {
			return false;
		}
		return message.chars().allMatch(NetworkCommunication::isValidMessageCharacter);
	}

	public static boolean checkEncodedUsernameValidity(ByteBuffer username) {
		requireNonNull(username);
		int size = username.remaining();
		return size <= USERNAME_MAX_SIZE && size > 0;
	}

	public static boolean checkEncodedMessageValidity(ByteBuffer message) {
		requireNonNull(message);
		int size = message.remaining();
		return size <= MESSAGE_MAX_SIZE && size > 0;
	}

	public static Charset getProtocolCharset() {
		return PROTOCOL_CHARSET;
	}

	public static Optional<ByteBuffer> encodeUsername(String username) {
		requireNonNull(username);
		ByteBuffer encoded = PROTOCOL_CHARSET.encode(username);
		Logger.debug("ENCODED USERNAME SIZE : " + encoded.remaining());
		if (!checkEncodedUsernameValidity(encoded)) {
			return Optional.empty();
		}
		return Optional.of(encoded);
	}

	public static Optional<ByteBuffer> encodeMessage(String message) {
		requireNonNull(message);
		ByteBuffer encoded = PROTOCOL_CHARSET.encode(message);
		Logger.debug("ENCODED MESSAGE SIZE : " + encoded.remaining());
		if (!checkEncodedMessageValidity(encoded)) {
			return Optional.empty();
		}
		return Optional.of(encoded);
	}

	public static int getUsernameMaxSize() {
		return USERNAME_MAX_SIZE;
	}

	public static int getMessageMaxSize() {
		return MESSAGE_MAX_SIZE;
	}

	public static int getFileChunkSize() {
		return FILE_CHUNK_SIZE;
	}
	
	public static void silentlyClose(SocketChannel sc) {
		try {
			sc.close();
		} catch (@SuppressWarnings("unused") IOException __) {
			return;
		}
	}
}
