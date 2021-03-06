package com.niton.net.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import javax.crypto.SecretKey;

public class CryptedOutputStream extends OutputStream {
	private SecretKey key;
	private ByteArrayOutputStream bos = new ByteArrayOutputStream();
	private OutputStream os;
	private int buffer;
	private int counter = 0;

	@Override
	public void flush() throws IOException {
		byte[] cryptedData = SimpleAES.encrypt(key, bos.toByteArray());
		bos.reset();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(cryptedData);
		oos.flush();
		System.gc();
	}

	/**
	 *
	 * @param key
	 * @param stream
	 * @param buffering
	 *            0 for not
	 */
	// public CryptedOutputStream(SecretKey key, OutputStream stream,int buffering)
	// {
	// this.key = key;
	// os = stream;
	// buffer = buffering;
	// }

	@Override
	public void write(int paramInt) throws IOException {
		bos.write(paramInt);
		counter++;
		if (buffer == counter && buffer != 0) {
			counter = 0;
			flush();
		}
	}

}
