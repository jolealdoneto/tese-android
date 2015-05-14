package br.com.lealdn.server;

import br.com.lealdn.server.responses.ExecuteResponseHandler;
import br.com.lealdn.server.responses.PingResponseHandler;

public enum Responses {
	EXECUTE("/execute", new ExecuteResponseHandler()),
	PING("/ping", new PingResponseHandler());
	
	final String path;
	final ResponseHandler handler;
	
	Responses(String path, ResponseHandler handler) {
		this.path = path;
		this.handler = handler;
	}
}
