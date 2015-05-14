package br.com.lealdn.offload;

import java.io.IOException;
import java.io.OutputStream;

public class UploadStream extends OutputStream {
	private long transferred = 0;
	private long lastTime = 0;
	private OutputStream out;

	public UploadStream(OutputStream out) {
		this();
		this.out = out;
	}
	
	public UploadStream() {
		this.transferred = 0;
	}
	
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}

	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
		this.transferred += len;
		//System.out.println(this.transferred/1024+" KB");
		this.lastTime = System.currentTimeMillis();
	}

	public void write(int b) throws IOException	{
		out.write(b);
		this.transferred++;
		this.lastTime = System.currentTimeMillis();
	}
	
	public long getLastTime() {
		return this.lastTime;
	}
}
