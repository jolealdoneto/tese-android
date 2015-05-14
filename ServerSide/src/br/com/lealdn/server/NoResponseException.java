package br.com.lealdn.server;

public class NoResponseException extends Exception {
	final String path;
	
	public String getPath() {
		return this.path;
	}
	
	public NoResponseException(final String path) {
		super();
		this.path = path;
	}
	
}
