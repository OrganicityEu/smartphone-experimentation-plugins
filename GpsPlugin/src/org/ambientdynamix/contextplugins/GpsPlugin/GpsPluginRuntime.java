package org.ambientdynamix.contextplugins.GpsPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.ambientdynamix.api.contextplugin.AutoReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;
import org.json.JSONObject;

import eu.smartsantander.androidExperimentation.jsonEntities.Reading;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class GpsPluginRuntime extends AutoReactiveContextPluginRuntime {

	private final String TAG = this.getClass().getSimpleName();
	private Context context;
	private String location = "unknown";
	private JSONObject locationJson = new JSONObject();
	private String status;
	private LocationManager locationManager;

	public void broadcastGPS(UUID requestId) {
		if (requestId != null)
			Log.w(TAG, "GPS Broadcast:" + requestId);
		else
			Log.w(TAG, "GPS Broadcast Timer!");
		Location gps;
		try {
			locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
			gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (gps == null)
				gps = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (gps != null) {
				this.location = gps.getLatitude() + "," + gps.getLongitude();
				this.locationJson = new JSONObject();
				this.locationJson.put("org.ambientdynamix.contextplugins.Latitude", gps.getLatitude());
				this.locationJson.put("org.ambientdynamix.contextplugins.Longitude", gps.getLongitude());
				this.status = "valid";
			} else {
				this.locationJson = null;
				this.location = "";
				this.status = "invalid";
			}
		} catch (Exception e) {
			Log.w("GPS Plugin Error", e.toString());
			this.location = "";
			this.status = "invalid";
		}

		if (locationJson != null) {
			Log.w(TAG, "GPS Plugin:" + this.location);
			PluginInfo info = new PluginInfo();
			info.setState(this.status);
			List<Reading> r = new ArrayList<Reading>();
			r.add(new Reading(Reading.Datatype.String, this.locationJson.toString(), PluginInfo.CONTEXT_TYPE));
			info.setPayload(r);
			Log.w(TAG, "GPS Plugin:" + info.getPayload());
			if (requestId != null) {
				sendContextEvent(requestId, new SecuredContextInfo(info, PrivacyRiskLevel.LOW), 60000);
				Log.w(TAG, "GPS Plugin from Request:" + info.getPayload());
			} else {
				sendBroadcastContextEvent(new SecuredContextInfo(info, PrivacyRiskLevel.LOW), 60000);
				Log.w(TAG, "GPS Plugin Broadcast:" + info.getPayload());
			}
		}
	}

	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		this.setPowerScheme(powerScheme);
		this.context = this.getSecuredContext();
		location = "";
		Log.w(TAG, "GPS Inited!");
	}

	// handle incoming context request
	@Override
	public void handleContextRequest(UUID requestId, String contextType) {
		broadcastGPS(requestId);
	}

	@Override
	public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config) {
		handleContextRequest(requestId, contextType);
	}

	@Override
	public void start() {
		Log.d(TAG, "GPS Plugin Started!");
	}

	@Override
	public void stop() {
		Log.d(TAG, "GPS Plugin Stopped!");
	}

	@Override
	public void destroy() {
		this.stop();
		Log.d(TAG, "GPS Plugin Destroyed!");
	}

	@Override
	public void updateSettings(ContextPluginSettings settings) {
	}

	@Override
	public void setPowerScheme(PowerScheme scheme) {
	}

	@Override
	public void doManualContextScan() {
	}

}