package br.com.lealdn.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import android.app.IntentService;
import android.content.Intent;
import android.os.Message;
import android.util.Log;

public class ServerService extends NanoHTTPD {
	public ServerService() {
		super(8080);
	}

	@Override 
	public Response serve(IHTTPSession session) {
		final String path = session.getUri();
		for (final Responses response : Responses.values()) {
			if (path.equals(response.path)) {
				return response.handler.handle(session);
			}
		}
		
		return new Response("NoResponse");
	}
}
