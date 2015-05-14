package br.com.lealdn.offload;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ReadOnlyBufferException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.util.Log;
import br.com.lealdn.offload.utils.Utils;

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
		try {
			final Class<?> clazz = Utils.getClassFromSignature(methodSignature);
            final Method method = Utils.getMethodFromSignature(clazz, methodSignature);
            final Object[] argArray = getArgsAsArray(args);
            final int remoteSize = OffloadingManager.getExecutionManager().getMethodRuntimeCount(method, false);
            final int localSize = OffloadingManager.getExecutionManager().getMethodRuntimeCount(method, true);
            if (remoteSize <= 5 || localSize <= 5) {
            	final boolean response = remoteSize < localSize;
            	OffloadingManager.getLogManager().addToLog(methodSignature, response);
            	return response;
            }
            final boolean canInterpolateLocal = OffloadingManager.getExecutionManager().canInterpolateAssessment(method, argArray, true);
            if (!canInterpolateLocal) {
            	OffloadingManager.getLogManager().addToLog(methodSignature, false);
            	return false;
            }
            final boolean canInterpolateRemote = OffloadingManager.getExecutionManager().canInterpolateAssessment(method, argArray, false);
            if (!canInterpolateRemote) {
            	OffloadingManager.getLogManager().addToLog(methodSignature, true);
            	return true;
            }

            final Double interpolateLocal = OffloadingManager.getExecutionManager().interpolateAssessment(method, argArray, true);
            final Double interpolateRemote = OffloadingManager.getExecutionManager().interpolateAssessment(method, argArray, false);
            final long sizeOfSerializedObject = serialize(methodSignature, args).toByteArray().length;
            
            final Double remoteTotalDuration = interpolateRemote + sizeOfSerializedObject / OffloadingManager.getBandwidthManager().getRTTTxBandwidth();

            final boolean response = remoteTotalDuration < interpolateLocal;
            OffloadingManager.getLogManager().addToLog(methodSignature, response);
            return response;
		} catch (Exception e) {
    		Log.d("OFFLOADING", "Error on shouldOffload. " + e.getMessage());
    		return false;
		}
    }
    
    public static void updateMethodRuntime(final String methodSignature, final long startTime, final Map<Object, Object> args) {
    	try {
    		final long time = System.currentTimeMillis() - startTime;
	    	final Class<?> clazz = Utils.getClassFromSignature(methodSignature);
	    	final Method method = Utils.getMethodFromSignature(clazz, methodSignature);
	    	
	    	OffloadingManager.getExecutionManager().updateMethodRuntimeAssessment(method, true, time, getArgsAsArray(args));
    	} catch(Exception ex) {
    		Log.d("OFFLOADING", "Error on updatingMethodRuntime. " + ex.getMessage());
    	}
    }
    
    private static Object[] getArgsAsArray(final Map<Object, Object> args) {
    	final Object[] params = new Object[args.keySet().size()];
    	int counter = 0;
    	for (final Map.Entry<Object, Object> entry : args.entrySet()) {
    		if (((String)entry.getKey()).startsWith("@arg")) {
    			final int index = Integer.valueOf(((String)entry.getKey()).substring(4));
    			params[index] = entry.getValue();
    			counter++;
    		}
    	}
    	final Object[] copy = new Object[counter];
    	for (int i = 0; i < copy.length; i++) {
    		copy[i] = params[i];
    	}
    	return copy;
    }
    
    private static ByteArrayOutputStream serialize(final String methodSignature, final Map<Object, Object> args) {
       final ByteArrayOutputStream baos = new ByteArrayOutputStream();
       
       final Output output = new Output(baos, 1024);
       kryo.writeObject(output, methodSignature);
       kryo.writeObject(output, args);
       output.close();
       
       return baos;
    }
    
    public static Object sendAndSerialize(final String methodSignature, final Map<Object, Object> args) {
        Log.d("OFFLOADING", "Intecept");
        try {
            final ByteArrayOutputStream baos = serialize(methodSignature, args);
            
            Log.d("OFFLOADING", args.toString());
            Log.d("OFFLOADING", "Intercept DONE.");
            
            final Class<?> clazz = Utils.getClassFromSignature(methodSignature);
            final Method method = Utils.getMethodFromSignature(clazz, methodSignature);
            final Object[] params = getArgsAsArray(args);
            
            return Intercept.sendFile(method, params, baos);
        } catch(Exception ex) {
            Log.e("OFFLOADING", ex.getMessage());
            return null;
        }
    }
    
    private static String readErrorMessageFromResponse(final HttpResponse response) {
    	final byte[] bresp = readResponse(response);
    	return new String(bresp);
    }
    
    private static Object readKryoObjectFromResponse(final HttpResponse response, final long uploadEllapsedTime, final long startTime, final Method method, final Object[] args) {
    	final long totalRequestTime = System.currentTimeMillis();
    	final byte[] bresp = readResponse(response);
    	final Input input = new Input(bresp);
    	try {
    		final Map<Object, Object> result = kryo.readObject(input, HashMap.class);
    		final long executionTimeInServer = (Long)result.get("t");
    		final double downloadBandwidth = Utils.calculateBandwidth(startTime + uploadEllapsedTime + executionTimeInServer, totalRequestTime, (long)bresp.length);
            OffloadingManager.getBandwidthManager().setDownloadBandwidth(downloadBandwidth);
            OffloadingManager.getExecutionManager().updateMethodRuntimeAssessment(method, false, executionTimeInServer, args);
    		return result.get("r");
    	} catch(Exception ex) {
    		ex.printStackTrace();
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
    private static Object sendFile(final Method method, final Object[] args, final ByteArrayOutputStream baos) {
    	String url = "http://"+ ConnectionUtils.ip +":8080/execute";
    	try {
    		final HttpClient httpclient = new DefaultHttpClient();
    		final HttpPost httppost = new HttpPost(url);
    		final UploadStream uploadStream = new UploadStream();
    		final ByteArrayEntity fileEntity = new ByteArrayEntity(baos.toByteArray()) {
    			@Override
    			public void writeTo(final OutputStream outstream) throws IOException {
    				uploadStream.setOutputStream(outstream);
                    super.writeTo(uploadStream);                                 
                }
    		};
    		
    		httppost.setEntity(fileEntity);
    		final long uploadStartTime = System.currentTimeMillis();
    		final HttpResponse response = httpclient.execute(httppost);
    		
    		final long uploadEndTime = uploadStream.getLastTime();
    		final double bandwidth = Utils.calculateBandwidth(uploadStartTime, uploadEndTime, fileEntity.getContentLength());
    		OffloadingManager.getBandwidthManager().setUploadBandwidth(bandwidth);
    		Log.d("OFFLOADING", "status: " + response.getStatusLine().getStatusCode());
    		
    		switch(response.getStatusLine().getStatusCode()) {
    		case 200:
    			return readKryoObjectFromResponse(response, uploadEndTime - uploadStartTime, uploadStartTime, method, args);
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