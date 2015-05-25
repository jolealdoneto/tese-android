package br.com.lealdn.offload;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class ConnectionUtils {
	public final static String ip = "192.168.2.100";

	public static Long pingServer() {
		String url = "http://"+ ip +":8080/ping";
    	try {
    		final HttpClient httpclient = new DefaultHttpClient();
    		final HttpGet httpGet = new HttpGet(url);
    		
    		final long startTime = System.currentTimeMillis();
    		final HttpResponse response = httpclient.execute(httpGet);
    		final long endTime = System.currentTimeMillis();
    		Log.d("OFFLOADING", "Pinging server..");

    		switch(response.getStatusLine().getStatusCode()) {
    		case 200:
    			return endTime - startTime;
    		default:
    			return null;
    		}
    	} catch(Exception e) {
    		Log.e("OFFLOADING", e.getMessage());
    		return null;
    	}
	}
}
