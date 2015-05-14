package br.com.lealdn.offload;

import br.com.lealdn.offload.RTTService.LocalBind;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;

public class OffloadingManager {
	private static OffloadingManager self;
	private final Activity mainActivity;
	private RTTService rttService;
	private SharedPreferences preferences;
	private BandwidthManager bandwidthManager;
	private ExecutionManager executionManager;
	private LogManager logManager;
	
	private OffloadingManager(final Activity mainActivity) {
		this.mainActivity = mainActivity;
	}
	
	public static void initialize(final Activity mainActivity) {
		if (self == null) {
			self = new OffloadingManager(mainActivity);
			self.preferences = self.mainActivity.getSharedPreferences("br.com.lealdn.offload.STORAGE", Context.MODE_PRIVATE);
		    self.bandwidthManager = new BandwidthManager(self.preferences);
		    self.executionManager = new ExecutionManager(self.preferences);
		    self.logManager = new LogManager();
			startService();
		}
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
