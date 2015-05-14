package br.com.lealdn.server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public interface ResponseHandler {
	public Response handle(IHTTPSession session);
}
