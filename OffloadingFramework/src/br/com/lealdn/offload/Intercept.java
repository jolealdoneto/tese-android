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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.net.TrafficStats;
import android.util.Log;
import br.com.lealdn.offload.utils.Utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Intercept {
    private static String testing = "NOT MODIFIED";
    final static Kryo kryo = new Kryo();
    private final static double ALPHA = 0.35;
    
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
			// Should never offload if we have no internet
			if (OffloadingManager.getNetworkState() == OffloadingManager.NONE) {
				return false;
			}
			
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

            final Double[] interpolateLocal = OffloadingManager.getExecutionManager().interpolateAssessment(method, argArray, true);
            final Double[] interpolateRemote = OffloadingManager.getExecutionManager().interpolateAssessment(method, argArray, false);
            final long sizeOfSerializedObject = serialize(methodSignature, args).toByteArray().length;
            final Double bandwidth = getBandwidth();

            final double totalTimeInUploadingArgs = sizeOfSerializedObject / bandwidth;
            
            final Double timeLocal = interpolateLocal[0];
            final Double timeRemote = interpolateRemote[0] + totalTimeInUploadingArgs;
            final Double energyLocal = interpolateLocal[0] + interpolateLocal[1] / bandwidth;
            final Double energyRemote = totalTimeInUploadingArgs;

            final Double utilityLocal = ALPHA*timeLocal + (1-ALPHA)*energyLocal;
            final Double utilityRemote = ALPHA*timeRemote + (1-ALPHA)*energyRemote;

            final boolean response = utilityRemote < utilityLocal;
            OffloadingManager.getLogManager().addToLog(methodSignature, response);
            return response;
		} catch (Exception e) {
    		Log.d("OFFLOADING", "Error on shouldOffload. " + e.getMessage());
    		return false;
		}
    }
    
    private static Double getBandwidth() {
    	Double bandwidth = OffloadingManager.getBandwidthManager().getBandwidth();
    	if (bandwidth == null) {
    		return OffloadingManager.getBandwidthManager().getUploadBandwidth();
    	}
    	return bandwidth;
    }
    
    public static void updateMethodRuntime(final String methodSignature, final long startTime, final Map<Object, Object> args, final long[] rxTxBytes) {
    	try {
    		final long time = System.currentTimeMillis() - startTime;
    		final long[] currentRxTxBytes = getRxTxCount();
	    	final Class<?> clazz = Utils.getClassFromSignature(methodSignature);
	    	final Method method = Utils.getMethodFromSignature(clazz, methodSignature);
	    	
	    	OffloadingManager.getExecutionManager().updateMethodRuntimeAssessment(method, true, time, getArgsAsArray(args), (currentRxTxBytes[0] - rxTxBytes[0]) + (currentRxTxBytes[1] - rxTxBytes[1]));
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
    
    public static Object sendAndSerialize(final String methodSignature, final Map<Object, Object> args) throws Throwable {
        Log.d("OFFLOADING", "Intecept");
        try {
            final ByteArrayOutputStream baos = serialize(methodSignature, args);
            
            Log.d("OFFLOADING", args.toString());
            Log.d("OFFLOADING", "Intercept DONE.");
            
            final Class<?> clazz = Utils.getClassFromSignature(methodSignature);
            final Method method = Utils.getMethodFromSignature(clazz, methodSignature);
            final Object[] params = getArgsAsArray(args);
            
            Future<Object> result = OffloadingManager.getExecutor().submit(new Callable<Object>() {
				@Override
				public Object call() throws ClientProtocolException, IOException {
					return Intercept.sendFile(method, params, baos);
				}
			});
            
            return result.get();
        } catch(ClassNotFoundException ex) {
            Log.e("OFFLOADING", ex.getMessage());
            return null;
        } catch(NoSuchMethodException ex) {
            Log.e("OFFLOADING", ex.getMessage());
            return null;
        } catch(ExecutionException ex) {
        	throw ex.getCause();
        } catch(InterruptedException ex) {
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
            OffloadingManager.getBandwidthManager().setLocationBasedBandwidth(downloadBandwidth);
            OffloadingManager.getExecutionManager().updateMethodRuntimeAssessment(method, false, executionTimeInServer, args, bresp.length);
    		return result.get("r");
    	} catch(Exception ex) {
    		ex.printStackTrace();
    		return null;
    	} finally {
    		input.close();
    	}
    }
    
    public static long[] getRxTxCount() {
    	final int uid = OffloadingManager.getUid();
    	final Long rxBytes = TrafficStats.getUidRxBytes(uid);
    	final Long txBytes = TrafficStats.getUidTxBytes(uid);
    	
    	return new long[] { rxBytes, txBytes };
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
    private static Object sendFile(final Method method, final Object[] args, final ByteArrayOutputStream baos) throws ClientProtocolException, IOException {
    	String url = "http://"+ ConnectionUtils.ip +":8080/execute";
    	HttpParams params = new BasicHttpParams();
    	HttpConnectionParams.setConnectionTimeout(params, 5000);
    	HttpConnectionParams.setSoTimeout(params, 5000);

    	final HttpClient httpclient = new DefaultHttpClient(params);
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
    	OffloadingManager.getBandwidthManager().setLocationBasedBandwidth(bandwidth);
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
    }
}