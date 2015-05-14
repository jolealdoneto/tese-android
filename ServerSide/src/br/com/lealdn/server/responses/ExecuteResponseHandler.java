package br.com.lealdn.server.responses;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import br.com.lealdn.server.ExecuteMethod;
import br.com.lealdn.server.ResponseHandler;
import br.com.lealdn.server.ServerActivity;

public class ExecuteResponseHandler implements ResponseHandler {

	@Override
	public Response handle(IHTTPSession session) {
		ServerActivity.debug("Incoming request");
		final int length = Integer.valueOf(session.getHeaders().get("content-length"));
		final byte[] buffer = new byte[(int)length];
		final DataInputStream dataIs = new DataInputStream(session.getInputStream());
		try {
			dataIs.readFully(buffer);
		} catch (IOException e) {
			ServerActivity.debug(e.getMessage());
		}
		
		final long start = System.currentTimeMillis();

		try {
			final Object result = ExecuteMethod.executeMethod(buffer);
			if (result != null) {
				final ByteArrayOutputStream serialized = ExecuteMethod.serializeResult(result, start);

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
