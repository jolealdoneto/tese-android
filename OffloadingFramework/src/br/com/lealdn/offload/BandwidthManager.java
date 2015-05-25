package br.com.lealdn.offload;

import java.util.HashSet;
import java.util.Set;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;

import com.google.gson.Gson;

public class BandwidthManager {
	private SharedPreferences preferences;
	private float updateConstant = 0.2F;
	private final String upBand = "rtt-upBand";
	private final String downBand = "rtt-downBand";
	private Gson gson = new Gson();
	private Set<LocationBasedBandwidth> locationBasedBandwidthList;

	public BandwidthManager(final SharedPreferences preferences) {
		this.preferences = preferences;
		this.locationBasedBandwidthList = new HashSet<LocationBasedBandwidth>();
	}

	public void setUploadBandwidth(final double bandwidth) {
		setBandwidth(upBand, bandwidth);
	}

	public void setDownloadBandwidth(final double bandwidth) {
		setBandwidth(downBand, bandwidth);
	}
	
	public void setBandwidth(final String key, final double value) {
		final float oldBandwidth = this.preferences.getFloat(key, (float)value);
		Editor editor = this.preferences.edit();
		editor.putFloat(key, oldBandwidth * updateConstant + (float)value * (1-updateConstant));
		editor.commit();
	}
	
	public void setLocationBasedBandwidth(final double bandwidth) {
		final Location lastKnowLocation = OffloadingManager.getLastKnownLocation();
		if (lastKnowLocation != null) {
			final int networkState = OffloadingManager.getNetworkState();
			final LocationBasedBandwidth lbb = new LocationBasedBandwidth(bandwidth, networkState, lastKnowLocation.getLatitude(), lastKnowLocation.getLongitude());

			this.locationBasedBandwidthList.remove(lbb);
			this.locationBasedBandwidthList.add(lbb);
		}
		else {
			setUploadBandwidth(bandwidth);
		}
	}
	
	public Double getBandwidth() {
		final Location lastKnowLocation = OffloadingManager.getLastKnownLocation();
		final int networkState = OffloadingManager.getNetworkState();
		final Object[] closestLocation = getClosestLocationBasedBandwidth(lastKnowLocation, networkState);
		if (closestLocation[0] != null) {
			final int distance = (Integer)closestLocation[1];
			final LocationBasedBandwidth loc = (LocationBasedBandwidth)closestLocation[0];
			if (networkState == OffloadingManager.WIFI && distance < 500 || networkState == OffloadingManager.MOBILE && distance < 1500) {
				return loc.bandwidth; 
			}
		}
		return null;
	}
	
	private Object[] getClosestLocationBasedBandwidth(Location location, int networkState) {
		LocationBasedBandwidth lowestLoc = null;
		int lowestDist = Integer.MAX_VALUE;

		for (LocationBasedBandwidth loc : this.locationBasedBandwidthList) {
			if (loc.connectionType == networkState) {
				final int dist = calculateLatLonDistance(location.getLatitude(), loc.lat, location.getLongitude(), loc.lon);
				if (dist < lowestDist) {
					lowestDist = dist;
					lowestLoc = loc;
				}
			}
		}
		
		return new Object[] { lowestLoc, lowestDist };
	}

	private final static double AVERAGE_RADIUS_OF_EARTH = 6371;
	private int calculateLatLonDistance(double userLat, double userLng,
			double venueLat, double venueLng) {

		double latDistance = Math.toRadians(userLat - venueLat);
		double lngDistance = Math.toRadians(userLng - venueLng);

		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
				+ Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(venueLat))
				* Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return (int) (Math.round(AVERAGE_RADIUS_OF_EARTH * c));
	}
	
	public double getUploadBandwidth() {
		return (double)this.preferences.getFloat(upBand, 0);
	}
	public double getDownloadBandwidth() {
		return (double)this.preferences.getFloat(downBand, 0);
	}
	
	public static class LocationBasedBandwidth {
		public final double bandwidth;
		public final int connectionType;
		public final double lat;
		public final double lon;
		
		public LocationBasedBandwidth(double bandwidth, int connectionType, double lat, double lon) {
			this.bandwidth = bandwidth;
			this.connectionType = connectionType;
			this.lat = lat;
			this.lon = lon;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + connectionType;
			long temp;
			temp = Double.doubleToLongBits(lat);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(lon);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LocationBasedBandwidth other = (LocationBasedBandwidth) obj;
			if (connectionType != other.connectionType)
				return false;
			if (Double.doubleToLongBits(lat) != Double
					.doubleToLongBits(other.lat))
				return false;
			if (Double.doubleToLongBits(lon) != Double
					.doubleToLongBits(other.lon))
				return false;
			return true;
		}
	}
}
