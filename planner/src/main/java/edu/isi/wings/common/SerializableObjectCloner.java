/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.isi.wings.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * ByteArrayOutputStream implementation that doesn't synchronize methods and
 * doesn't copy the data on toByteArray().
 */
class FastByteArrayOutputStream extends OutputStream {
	/**
	 * Buffer and size
	 */
	protected byte[] buf = null;
	protected int size = 0;

	/**
	 * Constructs a stream with buffer capacity size 5K
	 */
	public FastByteArrayOutputStream() {
		this(5 * 1024);
	}

	/**
	 * Constructs a stream with the given initial size
	 */
	public FastByteArrayOutputStream(int initSize) {
		this.size = 0;
		this.buf = new byte[initSize];
	}

	/**
	 * Ensures that we have a large enough buffer for the given size.
	 */
	private void verifyBufferSize(int sz) {
		if (sz > buf.length) {
			byte[] old = buf;
			buf = new byte[Math.max(sz, 2 * buf.length)];
			System.arraycopy(old, 0, buf, 0, old.length);
			old = null;
		}
	}

	public int getSize() {
		return size;
	}

	/**
	 * Returns the byte array containing the written data. Note that this array
	 * will almost always be larger than the amount of data actually written.
	 */
	public byte[] getByteArray() {
		return buf;
	}

	public final void write(byte b[]) {
		verifyBufferSize(size + b.length);
		System.arraycopy(b, 0, buf, size, b.length);
		size += b.length;
	}

	public final void write(byte b[], int off, int len) {
		verifyBufferSize(size + len);
		System.arraycopy(b, off, buf, size, len);
		size += len;
	}

	public final void write(int b) {
		verifyBufferSize(size + 1);
		buf[size++] = (byte) b;
	}

	public void reset() {
		size = 0;
	}

	/**
	 * Returns a ByteArrayInputStream for reading back the written data
	 */
	public InputStream getInputStream() {
		return new FastByteArrayInputStream(buf, size);
	}

}

/**
 * ByteArrayInputStream implementation that does not synchronize methods.
 */
class FastByteArrayInputStream extends InputStream {
	/**
	 * Our byte buffer
	 */
	protected byte[] buf = null;

	/**
	 * Number of bytes that we can read from the buffer
	 */
	protected int count = 0;

	/**
	 * Number of bytes that have been read from the buffer
	 */
	protected int pos = 0;

	public FastByteArrayInputStream(byte[] buf, int count) {
		this.buf = buf;
		this.count = count;
	}

	public final int available() {
		return count - pos;
	}

	public final int read() {
		return (pos < count) ? (buf[pos++] & 0xff) : -1;
	}

	public final int read(byte[] b, int off, int len) {
		if (pos >= count)
			return -1;

		if ((pos + len) > count)
			len = (count - pos);

		System.arraycopy(buf, pos, b, off, len);
		pos += len;
		return len;
	}

	public final long skip(long n) {
		if ((pos + n) > count)
			n = count - pos;
		if (n < 0)
			return 0;
		pos += n;
		return n;
	}

}

/**
 * Deep-copy of Serializable objects
 */
public class SerializableObjectCloner {

	public static Object clone(Object original) {
		Object clone = null;
		try {
			// Increased buffer size to speed up writing
			FastByteArrayOutputStream fbos = new FastByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(fbos);
			out.writeObject(original);
			out.flush();
			out.close();

			ObjectInputStream in = new ObjectInputStream(fbos.getInputStream());
			clone = in.readObject();

			in.close();
			fbos.close();

			return clone;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}

		return null;
	}
}
