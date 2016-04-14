package fr.upem.matou.shared.network;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Optional;

/**
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

	/**
	 * Returns whether this username is valid or not.
	 * 
	 * @param username The username to test
	 * @return true if the username is valid, false otherwise.
	 */
	public static boolean checkUsernameValidity(String username) {
		if (username == null) {
			return false;
		}
		return username.chars().allMatch(NetworkCommunication::isValidUsernameCharacter);
	}

	/**
	 * Returns whether this message is valid or not.
	 * 
	 * @param username The message to test
	 * @return true if the message is valid, false otherwise.
	 */
	public static boolean checkMessageValidity(String message) {
		if (message == null) {
			return false;
		}
		return message.chars().allMatch(NetworkCommunication::isValidMessageCharacter);
	}

	/**
	 * Returns whether this encoded username is valid or not. 
	 * The buffer is not altered.
	 * 
	 * @param username The encoded username to test (in write mode)
	 * @return true if the username is valid, false otherwise.
	 */
	public static boolean checkEncodedUsernameValidity(ByteBuffer username) {
		requireNonNull(username);
		int size = username.remaining();
		return size <= USERNAME_MAX_SIZE && size > 0;
	}

	/**
	 * Returns whether this encoded message is valid or not. 
	 * The buffer is not altered.
	 * 
	 * @param username The encoded message to test (in write mode)
	 * @return true if the message is valid, false otherwise.
	 */
	public static boolean checkEncodedMessageValidity(ByteBuffer message) {
		requireNonNull(message);
		int size = message.remaining();
		return size <= MESSAGE_MAX_SIZE && size > 0;
	}

	/**
	 * Returns the charset to use to meet the protocol.
	 * 
	 * @return The protocol charset
	 */
	public static Charset getProtocolCharset() {
		return PROTOCOL_CHARSET;
	}

	/**
	 * Encodes a username.
	 * 
	 * @param username The username to encode
	 * @return An optional that contains the encoded username if valid.
	 */
	public static Optional<ByteBuffer> encodeUsername(String username) {
		requireNonNull(username);
		ByteBuffer encoded = PROTOCOL_CHARSET.encode(username);
		if (!checkEncodedUsernameValidity(encoded)) {
			return Optional.empty();
		}
		return Optional.of(encoded);
	}

	/**
	 * Encodes a message.
	 * 
	 * @param username The message to encode
	 * @return An optional that contains the encoded message if valid.
	 */
	public static Optional<ByteBuffer> encodeMessage(String message) {
		requireNonNull(message);
		ByteBuffer encoded = PROTOCOL_CHARSET.encode(message);
		if (!checkEncodedMessageValidity(encoded)) {
			return Optional.empty();
		}
		return Optional.of(encoded);
	}

	/**
	 * Returns the maximum encoded username size.
	 * 
	 * @return The maximum encoded username size.
	 */
	public static int getUsernameMaxSize() {
		return USERNAME_MAX_SIZE;
	}

	/**
	 * Returns the maximum encoded message size.
	 * 
	 * @return The maximum encoded message size.
	 */
	public static int getMessageMaxSize() {
		return MESSAGE_MAX_SIZE;
	}

	/**
	 * Returns the maximum size of a file chunk.
	 * 
	 * @return The maximum size of a file chunk.
	 */
	public static int getFileChunkSize() {
		return FILE_CHUNK_SIZE;
	}
	
	/**
	 * Closes the SocketChannel without throwing exception.
	 * 
	 * @param sc The SocketChannel to close.
	 */
	public static void silentlyClose(SocketChannel sc) {
		try {
			sc.close();
		} catch (@SuppressWarnings("unused") IOException __) {
			return;
		}
	}
}
