package org.ambientdynamix.contextplugins.WifiScanPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.ambientdynamix.api.contextplugin.AutoReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;
import org.json.JSONObject;

import com.google.gson.Gson;

import eu.smartsantander.androidExperimentation.jsonEntities.Reading;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class WifiScanPluginRuntime extends AutoReactiveContextPluginRuntime {
	// Static logging TAG
	private final String TAG = this.getClass().getSimpleName();
	// Our secure context
	private Context context;

	WifiManager mainWifi;
	List<ScanResult> wifiList;
	private String scanJson = "-1";
	private boolean running = false;
	private Handler handler;
	private JSONObject wifisJson = new JSONObject();

	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			broadcastWifiScan(null);
			if (running) {
				handler.postDelayed(this, 20000);
			}
		}
	};

	public void broadcastWifiScan(UUID requestId) {
		if (requestId != null) {
			Log.w(TAG, "WifiScan Broadcast:" + requestId);
		} else {
			Log.w(TAG, "WifiScan Broadcast Timer!");
		}

		wifiList = new ArrayList<ScanResult>();
		mainWifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		mainWifi.startScan();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		wifiList = mainWifi.getScanResults();

		Gson gson = new Gson();
		scanJson = gson.toJson(wifiList);

		Log.i("scan wifi scan plugin", this.scanJson);
		PluginInfo info = new PluginInfo();
		info.setState("valid");
		try {
			List<Reading> readings = new ArrayList<Reading>();
			wifisJson.put("org.ambientdynamix.contextplugins.WifiList", scanJson);
			readings.add(new Reading(Reading.Datatype.String, wifisJson.toString(), PluginInfo.CONTEXT_TYPE));
			info.setPayload(readings);
			Log.w(TAG, "WifiScan Plugin:" + info.getPayload());
			if (requestId != null) {
				sendContextEvent(requestId, new SecuredContextInfo(info, PrivacyRiskLevel.LOW), 60000);
				Log.w(TAG, "WifiScan Plugin from Request:" + info.getPayload());
			} else {
				sendBroadcastContextEvent(new SecuredContextInfo(info, PrivacyRiskLevel.LOW), 60000);
				Log.w(TAG, "WifiScan Plugin Broadcast:" + info.getPayload());
			}
			wifiList.clear();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		this.setPowerScheme(powerScheme);
		this.context = this.getSecuredContext();
		Log.w(TAG, "WifiScan Inited!");
	}

	// handle incoming context request
	@Override
	public void handleContextRequest(UUID requestId, String contextType) {
		broadcastWifiScan(requestId);
	}

	@Override
	public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config) {
		handleContextRequest(requestId, contextType);
	}

	@Override
	public void start() {
		Log.d(TAG, "WifiScan Plugin Started!");
		running = true;
		handler.postDelayed(runnable, 20000);
	}

	@Override
	public void stop() {
		Log.d(TAG, "WifiScan Plugin Stoped!");
		running = false;
	}

	@Override
	public void destroy() {
		this.stop();
		Log.d(TAG, "WifiScan Plugin Destroyed!");
	}

	@Override
	public void updateSettings(ContextPluginSettings settings) {
		// Not supported
	}

	@Override
	public void setPowerScheme(PowerScheme scheme) {
		// Not supported
	}

	@Override
	public void doManualContextScan() {
		// Not supported
	}

}