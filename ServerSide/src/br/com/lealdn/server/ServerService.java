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
		final int length = Integer.valueOf(session.getHeaders().get("content-length"));
		
		final byte[] buffer = new byte[(int)length];
		final DataInputStream dataIs = new DataInputStream(session.getInputStream());
		try {
			dataIs.readFully(buffer);
		} catch (IOException e) {
			ServerActivity.debug(e.getMessage());
		}
		
		try {
			final Object result = ExecuteMethod.executeMethod(buffer);
            if (result != null) {
    			final ByteArrayOutputStream serialized = ExecuteMethod.serializeResult(result);
    			
    			ServerActivity.debug("Ok. Returning");
            	return new Response(Response.Status.OK, "application/octet-stream", new ByteArrayInputStream(serialized.toByteArray()));
            }
            ServerActivity.debug("Ok. VOID.");
            return new Response(Response.Status.NO_CONTENT, "application/octet-stream", new ByteArrayInputStream(new byte[]{}));
		} catch (ClassNotFoundException | NoSuchFieldException
				| SecurityException | IllegalArgumentException
				| IllegalAccessException | NoSuchMethodException
				| InvocationTargetException e) {
			
			ServerActivity.error("Error: " + e.getMessage());
			
			return new Response(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
		}
	}
	private String toString(Map<String, ? extends Object> map) {
		if (map.size() == 0) {
			return "";
		}
		return unsortedList(map);
	}

	private String unsortedList(Map<String, ? extends Object> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("<ul>");
		for (Map.Entry entry : map.entrySet()) {
			listItem(sb, entry);
		}
		sb.append("</ul>");
		return sb.toString();
	}

	private void listItem(StringBuilder sb, Map.Entry entry) {
		sb.append("<li><code><b>").append(entry.getKey()).
		append("</b> = ").append(entry.getValue()).append("</code></li>");
	}
}
