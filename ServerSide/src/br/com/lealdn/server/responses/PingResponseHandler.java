package br.com.lealdn.server.responses;

import java.util.Date;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import br.com.lealdn.server.ResponseHandler;
import br.com.lealdn.server.ServerActivity;

public class PingResponseHandler implements ResponseHandler {
	@Override
	public Response handle(IHTTPSession session) {
		ServerActivity.debug("PING REQUEST: " + new Date());
		return new Response(Response.Status.OK, "text/plain", "ACK");
	}

}
