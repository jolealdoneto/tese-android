package br.com.lealdn.offload;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class BandwidthManager {
	private SharedPreferences preferences;
	private float updateConstant = 0.2F;
	private final String upBand = "upBand";
	private final String downBand = "downBand";

	public BandwidthManager(final SharedPreferences preferences) {
		this.preferences = preferences;
	}

	public void setUploadBandwidth(final double bandwidth) {
		setBandwidth(upBand, bandwidth);
	}

	public void setDownloadBandwidth(final double bandwidth) {
		setBandwidth(downBand, bandwidth);
	}
	
	public float getRTTRxBandwidth() {
		return this.preferences.getFloat(RTTService.RTT_BAND_RX, 0);
	}

	public float getRTTTxBandwidth() {
		return this.preferences.getFloat(RTTService.RTT_BAND_TX, 0);
	}
	
	public void setBandwidth(final String key, final double value) {
		final float oldBandwidth = this.preferences.getFloat(key, (float)value);
		Editor editor = this.preferences.edit();
		editor.putFloat(key, oldBandwidth * updateConstant + (float)value * (1-updateConstant));
		editor.commit();
	}

	public double getUploadBandwidth() {
		return (double)this.preferences.getFloat(upBand, 0);
	}
	public double getDownloadBandwidth() {
		return (double)this.preferences.getFloat(downBand, 0);
	}
}
