package br.com.lealdn.offload;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class RTTService extends Service {
	// constant
    public static final long NOTIFY_INTERVAL = 10 * 1000; // 10 seconds
    public static final String RTT_BAND_TX = "rrt-tx";
    public static final String RTT_BAND_RX = "rrt-rx";
 
    SharedPreferences preferences;
    // run on another Thread to avoid crash
    private Handler handler = new Handler();
    // timer handling
    private Timer timer = null;
    final IBinder binder = new LocalBind();
    PackageManager pm;
    
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	@Override
	public void onCreate() {	
	 	this.preferences = this.getSharedPreferences("br.com.lealdn.offload.STORAGE", Context.MODE_PRIVATE);
	 	this.pm = this.getPackageManager();
	    this.startTimer();
    }
	
	public void stopTimer() {
		if (timer != null) {
			timer.cancel();
		}
	}
	
	public void startTimer() {
		if (timer != null) {
			timer.cancel();
		}
		
		timer = new Timer();
        timer.scheduleAtFixedRate(new PerformRTTCheck(), 0, NOTIFY_INTERVAL);
	}
	
	private void performPing() {
		int uid;
		try {
			uid = pm.getApplicationInfo("br.com.lealdn.client", 0).uid;
    		final Long rxBytes = TrafficStats.getUidRxBytes(uid);
    		final Long txBytes = TrafficStats.getUidTxBytes(uid);
    		final Long rtt = ConnectionUtils.pingServer();
    		if (rtt != null) {
        		final Long totalRx = TrafficStats.getUidRxBytes(uid) - rxBytes;
        		final Long totalTx = TrafficStats.getUidTxBytes(uid) - txBytes;
    			Editor editor = this.preferences.edit();
    			editor.putLong("rtt", rtt);
    			editor.commit();

    			OffloadingManager.getBandwidthManager().setBandwidth(RTT_BAND_TX, totalTx / (float)rtt);
    			OffloadingManager.getBandwidthManager().setBandwidth(RTT_BAND_RX, totalRx / (float)rtt);
    		}
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private long getRTT() {
		return this.preferences.getLong("rtt", -1);
	}
	
	public class LocalBind extends Binder {
		RTTService getService() {
			return RTTService.this;
		}
	}

	class PerformRTTCheck extends TimerTask {
		@Override
		public void run() {
			RTTService.this.performPing();
			final String timer = "Timer: " + getRTT();
			// run on another thread
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // display toast
                    Toast.makeText(getApplicationContext(), timer,
                            Toast.LENGTH_SHORT).show();
                }
            });
		}
	}
}
