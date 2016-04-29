package fr.upem.matou.debug.buffer;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

import fr.upem.matou.shared.utils.ByteBuffers;

@SuppressWarnings({ "static-method", "javadoc" })
public class ByteBuffersTest {

	@Test
	public void testDeepEqualsReflexive() {
		ByteBuffer bb = ByteBuffer.allocate(10);
		bb.putInt(5).putInt(7);

		assertTrue(ByteBuffers.deepEquals(bb, bb));
	}

	@Test
	public void testDeepEqualsSymetric() {
		ByteBuffer bb1 = ByteBuffer.allocate(10);
		bb1.putInt(5).putInt(7);
		ByteBuffer bb2 = ByteBuffer.allocate(10);
		bb2.putInt(5).putInt(7);

		assertTrue(ByteBuffers.deepEquals(bb1, bb2));
		assertTrue(ByteBuffers.deepEquals(bb2, bb1));
	}

	@Test
	public void testDeepEqualsTransitive() {
		ByteBuffer bb1 = ByteBuffer.allocate(10);
		bb1.putInt(5).putInt(7);
		ByteBuffer bb2 = ByteBuffer.allocate(10);
		bb2.putInt(5).putInt(7);
		ByteBuffer bb3 = ByteBuffer.allocate(10);
		bb3.putInt(5).putInt(7);

		assertTrue(ByteBuffers.deepEquals(bb1, bb2));
		assertTrue(ByteBuffers.deepEquals(bb2, bb3));
		assertTrue(ByteBuffers.deepEquals(bb1, bb3));
	}

	@Test
	public void testDeepEqualsConsistent() {
		ByteBuffer bb1 = ByteBuffer.allocate(10);
		bb1.putInt(5).putInt(7);
		ByteBuffer bb2 = ByteBuffer.allocate(10);
		bb2.putInt(7).putInt(5);

		assertTrue(ByteBuffers.deepEquals(bb1, bb1));
		assertTrue(ByteBuffers.deepEquals(bb1, bb1));
		assertFalse(ByteBuffers.deepEquals(bb1, bb2));
		assertFalse(ByteBuffers.deepEquals(bb1, bb2));
	}

	@Test
	public void testDeepEqualsNull() {
		ByteBuffer bb = ByteBuffer.allocate(10);
		bb.putInt(5).putInt(7);
		ByteBuffer bbNull = null;

		assertFalse(ByteBuffers.deepEquals(bb, bbNull));
		assertFalse(ByteBuffers.deepEquals(bbNull, bb));
	}

	@Test
	public void testDeepNonEqualsOrder() {
		ByteBuffer bb1 = ByteBuffer.allocate(10);
		bb1.putInt(5).putInt(7);
		ByteBuffer bb2 = ByteBuffer.allocate(10);
		bb2.putInt(7).putInt(5);

		assertFalse(ByteBuffers.deepEquals(bb1, bb2));
	}

	@Test
	public void testDeepNonEqualsCapacity() {
		ByteBuffer bb1 = ByteBuffer.allocate(10);
		bb1.putInt(5).putInt(7);
		ByteBuffer bb2 = ByteBuffer.allocate(20);
		bb2.putInt(5).putInt(7);

		assertFalse(ByteBuffers.deepEquals(bb1, bb2));
	}

	@Test
	public void testDeepNonEqualsLimit() {
		ByteBuffer bb1 = ByteBuffer.allocate(20);
		bb1.putInt(5).putInt(7).putInt(10);
		ByteBuffer bb2 = ByteBuffer.allocate(20);
		bb2.putInt(5).putInt(7);

		assertFalse(ByteBuffers.deepEquals(bb1, bb2));
	}

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

		assertTrue(ByteBuffers.deepEquals(expected, src));
		assertTrue(ByteBuffers.deepEquals(expected, copy));
	}

	@Test
	public void testAppend() {
		ByteBuffer target = ByteBuffer.allocate(20);
		target.putInt(1).putInt(2).putInt(3);

		ByteBuffer source = ByteBuffer.allocate(20);
		source.putInt(4).putInt(5);

		ByteBuffers.append(target, source);

		ByteBuffer expectedTarget = ByteBuffer.allocate(20);
		expectedTarget.putInt(1).putInt(2).putInt(3).putInt(4).putInt(5);

		ByteBuffer expectedSource = ByteBuffer.allocate(20);
		expectedSource.putInt(4).putInt(5);

		System.out.println("Target : " + target + expectedTarget);
		System.out.println("Append : " + source + expectedSource);

		assertTrue(ByteBuffers.deepEquals(expectedTarget, target));
		assertTrue(ByteBuffers.deepEquals(expectedSource, source));
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

		assertTrue(ByteBuffers.deepEquals(expectedBB1, bb1));
		assertTrue(ByteBuffers.deepEquals(expectedBB2, bb2));
		assertTrue(ByteBuffers.deepEquals(expectedMerged, merged));
	}

	@Test
	public void testMerge2() {
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

		assertTrue(ByteBuffers.deepEquals(expectedBB1, bb1));
		assertTrue(ByteBuffers.deepEquals(expectedBB2, bb2));
		assertTrue(ByteBuffers.deepEquals(expectedMerged, merged));
	}

	@Test
	public void testToByteString() {
		ByteBuffer bb = ByteBuffer.allocate(20);
		bb.put((byte) 1).put((byte) 2).put((byte) 3).put((byte) 5);

		assertEquals("{1,2,3,5}", ByteBuffers.toByteString(bb));
	}

	@Test
	public void testToBinaryString() {
		ByteBuffer bb = ByteBuffer.allocate(20);
		bb.put((byte) 1).put((byte) 2).put((byte) 3).put((byte) 5);

		assertEquals("{00000001 00000010 00000011 00000101}", ByteBuffers.toBinaryString(bb));
	}

}
