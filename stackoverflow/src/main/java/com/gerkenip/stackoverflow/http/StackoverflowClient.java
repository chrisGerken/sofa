package com.gerkenip.stackoverflow.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class StackoverflowClient extends Client {

	public StackoverflowClient() {
		super();
		setProperty("Accept-Encoding","compress, gzip");
	}

	@Override
	public byte[] getResponse() {

		try {
			GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(super.getResponse()));

			ByteArrayOutputStream os = new ByteArrayOutputStream();			
			byte b[] = new byte[8000];
			int read = is.read(b);
			while (read > -1) {
				os.write(b, 0, read);
				read = is.read(b);
			}
			return os.toByteArray();
		} catch (IOException e) { }
		
		return new byte[0];

	}

	@Override
	protected long minTime() {
		return 33;
	}

	
}
