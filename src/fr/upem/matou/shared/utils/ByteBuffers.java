package fr.upem.matou.shared.utils;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;

/**
 * This class provides static methods on ByteBuffer objects. All methods expect and return ByteBuffer in write mode.
 */
public class ByteBuffers {

	private ByteBuffers() {
	}

	/**
	 * Appends a source buffer to target buffer. The source buffer is not modified. The target buffer will be in write
	 * mode after this operation. If the target buffer does not have enough space to hold the source buffer, then
	 * neither buffers are modified and false is returned.
	 * 
	 * @param target
	 *            The target buffer (in write mode).
	 * @param source
	 *            The source buffer (in write mode).
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

}
