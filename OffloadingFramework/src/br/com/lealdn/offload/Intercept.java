package br.com.lealdn.offload;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Intercept {
    private static String testing = "NOT MODIFIED";
    final static Kryo kryo = new Kryo();
    
    public static void setTesting(final String testing) {
        Intercept.testing = testing;
    }
    public static int intercept(final String methodName) {
        System.out.println("Intercepted: " + methodName);
        return 0;
    }
    
    public static String m() {
        return testing;
    }
    
    public static boolean shouldOffload(final String methodSignature, final Map<Object, Object> args) {
    	return true;
    }
    
    public static Object sendAndSerialize(final String methodSignature, final Map<Object, Object> args) {
        Log.d("OFFLOADING", "Intecept");
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            final Output output = new Output(baos, 1024);
            kryo.writeObject(output, methodSignature);
            kryo.writeObject(output, args);
            output.close();
            
            Log.d("OFFLOADING", args.toString());
            Log.d("OFFLOADING", "Intercept DONE.");
            
            return Intercept.sendFile(baos);
        } catch(Exception ex) {
            Log.e("OFFLOADING", ex.getMessage());
            return null;
        }
    }
    
    private static String readErrorMessageFromResponse(final HttpResponse response) {
    	final byte[] bresp = readResponse(response);
    	return new String(bresp);
    }
    
    private static Object readKryoObjectFromResponse(final HttpResponse response) {
    	final byte[] bresp = readResponse(response);
    	final Input input = new Input(bresp);
    	try {
    		final Map<Object, Object> result = kryo.readObject(input, HashMap.class);
            return result.get("r");
    	} catch(Exception ex) {
    		return null;
    	} finally {
    		input.close();
    	}
    }
    
    private static byte[] readResponse(final HttpResponse response) {
    	Log.d("OFFLOADING", Arrays.toString(response.getAllHeaders()));
    	final int length = Integer.valueOf(response.getFirstHeader("Content-Length").getValue());
		Log.d("OFFLOADING", "Lneght:" + length);
		final byte[] buffer = new byte[(int)length];
		DataInputStream dataIs;
		try {
			dataIs = new DataInputStream(response.getEntity().getContent());
			dataIs.readFully(buffer);
		} catch (Exception e1) {
			Log.d("OFFLOADING", e1.getMessage());
			e1.printStackTrace();
			return null;
		}
		
		return buffer;
    }

    @SuppressWarnings("deprecation")
    private static Object sendFile(final ByteArrayOutputStream baos) {
    	String url = "http://192.168.1.101:8080/output.bin";
    	try {
    		final HttpClient httpclient = new DefaultHttpClient();
    		final HttpPost httppost = new HttpPost(url);
    		final ByteArrayEntity fileEntity = new ByteArrayEntity(baos.toByteArray());

    		httppost.setEntity(fileEntity);
    		final HttpResponse response = httpclient.execute(httppost);
    		Log.d("OFFLOADING", "status: " + response.getStatusLine().getStatusCode());

    		switch(response.getStatusLine().getStatusCode()) {
    		case 200:
    			return readKryoObjectFromResponse(response);
    		case 204:
    			Log.d("OFFLOADING", "OKAY");
    			return null;
    		case 500:
    			Log.e("OFFLOADING", readErrorMessageFromResponse(response));
    			return null;
    		default:
    			return null;
    		}
    	} catch(Exception e) {
    		Log.e("OFFLOADING", e.getMessage());
    		return null;
    	}
    }
}