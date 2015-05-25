package br.com.lealdn.offload;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.lealdn.offload.RTTService.LocalBind;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.util.Log;

public class OffloadingManager {
	private static OffloadingManager self;
	private final Activity mainActivity;
	private RTTService rttService;
	private SharedPreferences preferences;
	private BandwidthManager bandwidthManager;
	private ExecutionManager executionManager;
	private LogManager logManager;
	private ActivityManager activityManager;
	private PackageManager pm;
	private int uid;
	public final static String APPLICATION_NAME = "br.com.lealdn.client";
	private volatile boolean shouldRunRTT = true;
	private ExecutorService executor;
	private LocationManager locationManager;
	private ConnectivityManager connectivityManager;

	public final static int WIFI = 0;
	public final static int MOBILE = 1;
	public final static int NONE = 2;

	private OffloadingManager(final Activity mainActivity) {
		this.mainActivity = mainActivity;
	}
	
	public static boolean shouldRunRTT() {
		return self.shouldRunRTT;
	}
	
	public synchronized static void setShouldRunRTT(boolean shouldRunRTT) {
		self.shouldRunRTT = shouldRunRTT;
	}
	
	public static void initialize(final Activity mainActivity) {
		if (self == null) {
			self = new OffloadingManager(mainActivity);
			self.preferences = self.mainActivity.getSharedPreferences("br.com.lealdn.offload.STORAGE", Context.MODE_PRIVATE);
		    self.bandwidthManager = new BandwidthManager(self.preferences);
		    self.executionManager = new ExecutionManager(self.preferences);
		    self.logManager = new LogManager();
		    self.activityManager = (ActivityManager)self.mainActivity.getSystemService(Context.ACTIVITY_SERVICE);
		    self.pm = self.mainActivity.getPackageManager();
		    self.executor = Executors.newFixedThreadPool(10);
		    self.locationManager = (LocationManager)self.mainActivity.getSystemService(Context.LOCATION_SERVICE);
		    self.connectivityManager = (ConnectivityManager)self.mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

		    try {
		    	self.uid = self.pm.getApplicationInfo(APPLICATION_NAME, 0).uid;
		    } catch (NameNotFoundException e) {
		    	Log.d("OFFLOADING", "Error on initialize UID. " + e.getMessage());
		    }
			startService();
		}
	}
	
	public static ExecutorService getExecutor() {
		return self.executor;
	}
	
	public static int getUid() {
		return self.uid;
	}
	
	public static void onResume() {
		if (self != null && self.rttService != null) {
			self.rttService.startTimer();
		}
	}
	
	public static void onPause() {
		if (self != null && self.rttService != null) {
			self.rttService.stopTimer();
		}
	}
	
	public static void onStop() {
		if (self != null && self.rttService != null) {
			self.rttService.stopTimer();
		}
	}
	
	public static void startService() {
		if (self.rttService == null) {
			final Intent intent = new Intent(self.mainActivity, RTTService.class);
			self.mainActivity.startService(intent);
	        self.mainActivity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
		}
	}
	
	public static Location getLastKnownLocation() {
		return self.locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	}
	
	public static int getNetworkState() {
		final android.net.NetworkInfo wifi = self.connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		final android.net.NetworkInfo mobile = self.connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		if (wifi.isAvailable()) {
			return WIFI;
		} else if (mobile.isAvailable()) {
			return MOBILE;
		} else {
			return NONE;
		}
	}
	
	public static LogManager getLogManager() {
		return self.logManager;
	}
	public static BandwidthManager getBandwidthManager() {
		return self.bandwidthManager;
	}
	public static ExecutionManager getExecutionManager() {
		return self.executionManager;
	}
	
	 /** Callbacks for service binding, passed to bindService() */
    private static ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // cast the IBinder and get MyService instance
            LocalBind binder = (LocalBind) service;
            self.rttService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };
}
