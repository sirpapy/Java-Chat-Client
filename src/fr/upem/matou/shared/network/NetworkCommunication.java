package fr.upem.matou.shared.network;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;

/**
 * This class gathers the common factors between Client and Server communication. All these methods should be used in
 * order to meet the protocol requirements.
 */
public class NetworkCommunication {
	private static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");

	// Maximum sizes in bytes :
	static final int USERNAME_MAX_SIZE = 32;
	static final int MESSAGE_MAX_SIZE = 512;
	static final int FILE_CHUNK_SIZE = 4096;
	static final int FILENAME_MAX_SIZE = 64;

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
	 * @param username
	 *            The username to test.
	 * @return true if the username is valid or false otherwise.
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
	 * @param message
	 *            The message to test.
	 * @return true if the message is valid or false otherwise.
	 */
	public static boolean checkMessageValidity(String message) {
		if (message == null) {
			return false;
		}
		return message.chars().allMatch(NetworkCommunication::isValidMessageCharacter);
	}

	/*
	 * Returns whether this encoded username is valid or not. The buffer is not decoded or altered.
	 */
	private static boolean checkEncodedUsernameValidity(ByteBuffer username) {
		int size = username.remaining();
		return size <= USERNAME_MAX_SIZE && size > 0;
	}

	/*
	 * Returns whether this encoded message is valid or not. The buffer is not decoded or altered.
	 */
	private static boolean checkEncodedMessageValidity(ByteBuffer message) {
		int size = message.remaining();
		return size <= MESSAGE_MAX_SIZE && size > 0;
	}

	/*
	 * Returns whether this encoded path is valid or not. The buffer is not decoded or altered.
	 */
	private static boolean checkEncodedPathValidity(ByteBuffer path) {
		int size = path.remaining();
		return size <= FILENAME_MAX_SIZE && size > 0;
	}

	/**
	 * Returns the charset to use in order to meet the protocol.
	 * 
	 * @return The protocol charset.
	 */
	public static Charset getProtocolCharset() {
		return PROTOCOL_CHARSET;
	}

	/**
	 * Encodes a username. This method checks the username validity.
	 * 
	 * @param username
	 *            The username to encode
	 * @return An optional that contains the encoded username if valid or an empty optional otherwise.
	 */
	public static Optional<ByteBuffer> encodeUsername(String username) {
		requireNonNull(username);
		if (!checkUsernameValidity(username)) {
			return Optional.empty();
		}
		ByteBuffer encoded = PROTOCOL_CHARSET.encode(username);
		if (!checkEncodedUsernameValidity(encoded)) {
			return Optional.empty();
		}
		return Optional.of(encoded);
	}

	/**
	 * Encodes a message. This method checks the message validity.
	 * 
	 * @param message
	 *            The message to encode.
	 * @return An optional that contains the encoded message if valid or an empty optional otherwise.
	 */
	public static Optional<ByteBuffer> encodeMessage(String message) {
		requireNonNull(message);
		if (!checkMessageValidity(message)) {
			return Optional.empty();
		}
		ByteBuffer encoded = PROTOCOL_CHARSET.encode(message);
		if (!checkEncodedMessageValidity(encoded)) {
			return Optional.empty();
		}
		return Optional.of(encoded);
	}

	/**
	 * Encodes a path. This method checks the path validity.
	 * 
	 * @param path
	 *            The path to encode.
	 * @return An optional that contains the encoded path if valid or an empty optional otherwise.
	 */
	public static Optional<ByteBuffer> encodePath(Path path) {
		requireNonNull(path);
		ByteBuffer encoded = PROTOCOL_CHARSET.encode(path.toString());
		if (!checkEncodedPathValidity(encoded)) {
			return Optional.empty();
		}
		return Optional.of(encoded);
	}

	/**
	 * Returns the maximum size of an encoded username.
	 * 
	 * @return The maximum size of an encoded username.
	 */
	public static int getUsernameMaxSize() {
		return USERNAME_MAX_SIZE;
	}

	/**
	 * Returns the maximum size of an encoded message.
	 * 
	 * @return The maximum size of an encoded message.
	 */
	public static int getMessageMaxSize() {
		return MESSAGE_MAX_SIZE;
	}

	/**
	 * Returns the maximum size of an encoded filename size.
	 * 
	 * @return The maximum size of an encoded filename size.
	 */
	public static int getFilenameMaxSize() {
		return FILENAME_MAX_SIZE;
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
	 * Closes the SocketChannel without throwing exceptions.
	 * 
	 * @param sc
	 *            The SocketChannel to close.
	 */
	public static void silentlyClose(SocketChannel sc) {
		requireNonNull(sc);
		try {
			sc.close();
		} catch (@SuppressWarnings("unused") IOException __) {
			// Ignored
		}
	}
}
