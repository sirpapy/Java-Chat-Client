package fr.upem.matou.shared.utils;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * This class provides static methods on ByteBuffer objects.
 * All methods expect and return ByteBuffer in write mode.
 */
public class ByteBuffers {
	private ByteBuffers() {
	}

	/**
	 * Returns a copy of the ByteBuffer. The new buffer has same capacity as the source buffer, is in the same state and
	 * contains same bytes.
	 * The source buffer is not modified.
	 * 
	 * @param src
	 *            The buffer to copy (in write mode)
	 * @return The copied buffer (in write mode)
	 */
	public static ByteBuffer copy(ByteBuffer src) {
		requireNonNull(src);
		src.flip();
		ByteBuffer dst = ByteBuffer.allocate(src.capacity());
		dst.put(src);
		src.position(0);
		src.compact();
		return dst;
	}

	/**
	 * Appends a buffer to another buffer.
	 * The source buffer is not modified.
	 * The target buffer will be in write mode after this operation.
	 * If the target buffer does not have enough space to hold the source buffer, then neither buffers are modified
	 * false is returned.
	 * 
	 * @param target
	 *            The target buffer (in write mode)
	 * @param source
	 *            The source buffer (in write mode)
	 * @return true if the operation succeeded, false otherwise (because of insufficient space in target buffer).
	 */
	public static boolean append(ByteBuffer target, ByteBuffer source) {
		requireNonNull(target);
		requireNonNull(source);
		source.flip();
		try {
			if (source.remaining() > target.remaining()) { // insufficient space
				return false;
			}
			target.put(source);
			source.position(0);
		} finally {
			source.compact();
		}
		return true;
	}

	/**
	 * Merges two buffers in one.
	 * The two buffers are not modified.
	 * The resulting buffer will be in write mode after this operation.
	 * 
	 * @param bb1
	 *            The first buffer (in write mode)
	 * @param bb2
	 *            The second buffer (in write mode)
	 * @return The resulting buffer (in write mode)
	 */
	public static ByteBuffer merge(ByteBuffer bb1, ByteBuffer bb2) {
		requireNonNull(bb1);
		requireNonNull(bb2);
		ByteBuffer result = ByteBuffer.allocate(bb1.capacity() + bb2.capacity());
		append(result, bb1);
		append(result, bb2);
		return result;
	}

	/**
	 * Returns a string representation of bytes contained in the buffer.
	 * The buffer state is not modified.
	 * 
	 * @param bb
	 *            The buffer (in write mode)
	 * @return The string representation of bytes in this buffer.
	 */
	public static String toByteString(ByteBuffer bb) {
		requireNonNull(bb);
		bb.flip();
		ArrayList<Byte> elements = new ArrayList<>();
		while (bb.hasRemaining()) {
			elements.add(bb.get());
		}
		bb.position(0);
		bb.compact();

		String string = elements.stream().map(e -> String.valueOf(e)).collect(Collectors.joining(","));
		return "{" + string + "}";
	}

	/**
	 * Returns a string binary representation of bytes contained in the buffer.
	 * The buffer state is not modified.
	 * 
	 * @param bb
	 *            The buffer (in write mode)
	 * @return The string representation of bytes in this buffer.
	 */
	public static String toBinaryString(ByteBuffer bb) {
		requireNonNull(bb);
		bb.flip();
		ArrayList<Byte> elements = new ArrayList<>();
		while (bb.hasRemaining()) {
			elements.add(bb.get());
		}
		bb.position(0);
		bb.compact();

		String string = elements.stream()
				.map(e -> String.format("%8s", Integer.toBinaryString(e)).replace(' ', '0'))
				.collect(Collectors.joining(" "));
		return "{" + string + "}";
	}

	/**
	 * Returns a string binary representation of a byte.
	 * 
	 * @param b
	 *            The byte
	 * @return The string representation of the byte.
	 */
	public static String toBinaryString(byte b) {
		return String.format("%8s", Integer.toBinaryString(b)).replace(' ', '0');
	}

	/**
	 * Tests whether or not a ByteBuffer is deeply equals to another ByteBuffer.
	 * Two buffers are equals if and only if :
	 * - They are not null
	 * - They have the same capacity
	 * - Their remaining bytes when the buffer is flipped are equals
	 * 
	 * The two buffers are not modified after this operation.
	 * 
	 * @param bb1
	 *            The first buffer (in write mode)
	 * @param bb2
	 *            The second buffer (in write mode)
	 * @return true if the two buffers are deeply equals
	 */
	public static boolean deepEquals(ByteBuffer bb1, ByteBuffer bb2) {
		if (bb1 == null || bb2 == null) {
			return false;
		}

		if (bb1.capacity() != bb2.capacity()) {
			return false;
		}

		bb1.flip();
		bb2.flip();

		boolean equals = bb1.equals(bb2);

		bb1.position(0);
		bb1.compact();
		bb2.position(0);
		bb2.compact();

		return equals;
	}

}
