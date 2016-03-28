package fr.upem.matou.tcp;

import java.nio.ByteBuffer;

class ByteBuffers {
	private ByteBuffers() {
	}

	static ByteBuffer copy(ByteBuffer src) {
		src.flip();
		ByteBuffer dst = ByteBuffer.allocate(src.capacity());
		dst.put(src);
		src.position(0);
		src.compact();
		return dst;
	}

	static void append(ByteBuffer target, ByteBuffer toAppend) {
		toAppend.flip();
		target.put(toAppend);
		toAppend.position(0);
		toAppend.compact();
	}

	static ByteBuffer merge(ByteBuffer bb1, ByteBuffer bb2) {
		ByteBuffer result = ByteBuffer.allocate(bb1.capacity() + bb2.capacity());
		append(result, bb1);
		append(result, bb2);
		return result;
	}
}
