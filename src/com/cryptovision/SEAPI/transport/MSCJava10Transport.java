/*
 * Copyright (c) 2019
 * cv cryptovision GmbH
 * Munscheidstr. 14
 * 45886 Gelsenkirchen
 * Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cryptovision.SEAPI.transport;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import com.sun.nio.file.ExtendedOpenOption;

/** exclude from build or remove this file for Java versions before 10 */
public class MSCJava10Transport extends MSCTransport {

	private final int ALIGNMENT_SIZE;

	public MSCJava10Transport(Properties props) throws IOException {
		super(props);

		String alignment = props.getProperty("alignment");
		ALIGNMENT_SIZE = alignment == null ? 512 : Integer.parseInt(alignment);

		setSuspendMode(false);
	}

	@Override
	public void close() throws IOException {
		setSuspendMode(true);

		super.close();
	}

	private void setSuspendMode(boolean suspending) throws IOException {
		ByteBuffer bb = bb(32+4+2);
		bb.put(FILE_HEADER);
		bb.putShort((short) 2);
		bb.putShort((short) (suspending ? 0x5345 : 0x5344));
		bb.putShort((short) 0);
		((Buffer)bb).flip();

		write(bb);

		bb.clear();
		try {
			receive(bb, 0, 100);
		} catch(IOException expectedDevSample) {
		} catch(BufferUnderflowException expectedEngSample) {}
	}

	protected int aligned(int size) {
		return (size + (ALIGNMENT_SIZE-1)) / ALIGNMENT_SIZE * ALIGNMENT_SIZE;
	}

	@Override
	protected void write(ByteBuffer bb) throws IOException {
//		long start = System.currentTimeMillis();
		FileChannel fc = FileChannel.open(io.toPath(), StandardOpenOption.WRITE, ExtendedOpenOption.DIRECT);
		((Buffer)bb).limit(aligned(((Buffer)bb).limit()));
		try {
			fc.write(bb);
		} catch (IOException e) {
			fc.close();
			if(e.getMessage() == null || !e.getMessage().contains("is not a multiple of the block size"))
				throw e;
			int from = e.getMessage().lastIndexOf('(')+1;
			int to = e.getMessage().lastIndexOf(')');
			String sub = e.getMessage().substring(from, to);
			System.err.println("try \"alignment="+sub+"\" in config.txt");
			throw e;
		}
		fc.close();
//		if(System.currentTimeMillis() - start > 1000)
//			System.err.println("write");
	}

	protected short receive(ByteBuffer bb, int count, int timeout) throws IOException {
		long end = System.currentTimeMillis() + timeout;

		do {
			((Buffer)bb).clear();
			((Buffer)bb).limit(ALIGNMENT_SIZE);

			try { Thread.sleep(IO_DELAY); } catch (InterruptedException e) { }
//			long start = System.currentTimeMillis();
			FileChannel fc = FileChannel.open(io.toPath(), StandardOpenOption.READ, ExtendedOpenOption.DIRECT);
//			if(System.currentTimeMillis() - start > 1000)
//				System.err.println("open");
			try {
				short result = -1;
				do {
					while(((Buffer)bb).hasRemaining()) {
//						start = System.currentTimeMillis();
						int c = fc.read(bb);
//						if(System.currentTimeMillis() - start > 5000)
//							System.err.println("read");
						if(c == -1)
							throw new IOException("read failed");
						if(result == -1 && ((Buffer)bb).limit() == ALIGNMENT_SIZE && ((Buffer)bb).position() > 0x22) {
							result = bb.getShort(0x20);
							if(result <= 0) {
								try { Thread.sleep(IO_DELAY > 10 ? IO_DELAY : 10); } catch (InterruptedException e) { }
//								System.out.print(".");
								break;
							} else if(result < 2)
								throw new IOException("MSC transport length = "+result);
							else
								((Buffer)bb).limit(aligned(0x22 + result));
						}
					}

//					if(!bb.hasRemaining()) {
//						// logging only
//						int pos = ((Buffer)bb).position();
//						bb.position(32);
//						byte[] log = new byte[128];
//						bb.get(log);
//						bb.position(pos);
//						System.out.println("MSC < "+new String(Hex.encode(log)));
//						// logging only /
//						break;
//					}
				} while(((Buffer)bb).hasRemaining() && System.currentTimeMillis() < end);
			} finally {
//				start = System.currentTimeMillis();
				fc.close();
//				if(System.currentTimeMillis() - start > 1000)
//					System.err.println("close");
			}
		} while(System.currentTimeMillis() < end && (((Buffer)bb).position() < 0x22 || bb.getShort(0x20) == (short) 0xFFFF));

		if(((Buffer)bb).position() < 0x22 || bb.getShort(0x20) == (short) 0xFFFF || bb.getShort(0x20) == (short) 0xFF45 || ((Buffer)bb).hasRemaining())
			throw new IOException("timeout");

		((Buffer)bb).flip();
		((Buffer)bb).position(0x22);
		((Buffer)bb).limit(0x22 + bb.getShort(0x20));
		short len = bb.getShort();
		return len;
	}

	@Override
	protected ByteBuffer bb(int size) {
		return ByteBuffer.allocateDirect(aligned(size + ALIGNMENT_SIZE)).alignedSlice(ALIGNMENT_SIZE);
	}

	@Override
	protected byte[] slice(ByteBuffer bb, int start, int len) {
		int pos = ((Buffer)bb).position();
		bb.position(start);
		byte[] data = new byte[len > bb.remaining() ? bb.remaining() : len];
		bb.get(data);
		bb.position(pos);
		return data;
	}
}
