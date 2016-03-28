package fr.upem.matou.tcp;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

@SuppressWarnings("static-method")
public class ByteBuffersTest {

	@Test
	public void testCopy() {
		ByteBuffer src = ByteBuffer.allocate(10);
		src.putInt(5).putInt(7);

		ByteBuffer copy = ByteBuffers.copy(src);

		ByteBuffer expected = ByteBuffer.allocate(10);
		expected.putInt(5).putInt(7);

		System.out.println("src = " + src);
		System.out.println("copy = " + copy);
		System.out.println("expected = " + expected);

		assertEquals(expected, src);
		assertEquals(expected, copy);
	}

	@Test
	public void testAppend() {
		ByteBuffer target = ByteBuffer.allocate(20);
		target.putInt(1).putInt(2).putInt(3);

		ByteBuffer toAppend = ByteBuffer.allocate(20);
		toAppend.putInt(4).putInt(5);

		ByteBuffers.append(target, toAppend);

		ByteBuffer expectedTarget = ByteBuffer.allocate(20);
		expectedTarget.putInt(1).putInt(2).putInt(3).putInt(4).putInt(5);

		ByteBuffer expectedAppend = ByteBuffer.allocate(20);
		expectedAppend.putInt(4).putInt(5);

		System.out.println("Target : " + target + expectedTarget);
		System.out.println("Append : " + toAppend + expectedAppend);
		
		assertEquals(expectedTarget, target);
		assertEquals(expectedAppend, toAppend);
	}

	@Test
	public void testMerge() {
		ByteBuffer bb1 = ByteBuffer.allocate(10);
		ByteBuffer bb2 = ByteBuffer.allocate(10);

		bb1.putInt(10).putInt(100);
		bb2.putInt(20).putInt(200);

		ByteBuffer merged = ByteBuffers.merge(bb1, bb2);

		ByteBuffer expectedBB1 = ByteBuffer.allocate(10);
		ByteBuffer expectedBB2 = ByteBuffer.allocate(10);
		ByteBuffer expectedMerged = ByteBuffer.allocate(20);

		expectedBB1.putInt(10).putInt(100);
		expectedBB2.putInt(20).putInt(200);
		expectedMerged.putInt(10).putInt(100).putInt(20).putInt(200);

		System.out.println("BB1 : " + bb1 + expectedBB1);
		System.out.println("BB2 : " + bb2 + expectedBB2);
		System.out.println("Merged : " + merged + expectedMerged);

		assertEquals(expectedBB1, bb1);
		assertEquals(expectedBB2, bb2);
		assertEquals(expectedMerged, merged);
	}

}
