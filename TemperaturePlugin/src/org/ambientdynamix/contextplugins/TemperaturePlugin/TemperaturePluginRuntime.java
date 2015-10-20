package org.ambientdynamix.contextplugins.TemperaturePlugin;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import eu.smartsantander.androidExperimentation.jsonEntities.Reading;
import org.ambientdynamix.api.contextplugin.AutoReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;
import org.ambientdynamix.api.contextplugin.security.SecuredSensorManager;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TemperaturePluginRuntime extends AutoReactiveContextPluginRuntime {

	private final String TAG = this.getClass().getSimpleName();
	private Context context;
	private SensorEventListener listener;
	private SecuredSensorManager mSensorManager;
	private Sensor mSensor;
	private float temperature;

	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		this.setPowerScheme(powerScheme);
		this.context = this.getPluginFacade().getSecuredContext(getSessionId());
		try {
			listener = new SensorEventListener() {

				@Override
				public void onSensorChanged(SensorEvent event) {
					// TODO Auto-generated method stub
					temperature = event.values[0];

				}

				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy) {
					// TODO Auto-generated method stub

				}
			};
			mSensorManager = (SecuredSensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
			mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		} catch (Exception e) {
			e.printStackTrace();

		}
		Log.w(TAG, "Temperature Inited!");
	}

	// handle incoming context request
	@Override
	public void handleContextRequest(UUID requestId, String contextType) {
		if (requestId == null) {
			return;
		}
		Log.w(TAG, "Temperature Broadcast Timer!");
		List<Reading> r = new ArrayList<Reading>();
		PluginInfo info = new PluginInfo();
		try {
			JSONObject obj = new JSONObject();
			obj.put("org.ambientdynamix.contextplugins.AmbientTemperature", temperature);
			r.add(new Reading(obj.toString(), PluginInfo.CONTEXT_TYPE));
			info.setPayload(r);
			info.setState("OK");
			if (requestId != null) {
				sendContextEvent(requestId, new SecuredContextInfo(info, PrivacyRiskLevel.LOW), 60000);
				Log.w(TAG, "Temperature Plugin from Request:" + info.getPayload());
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	@Override
	public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config) {
		handleContextRequest(requestId, contextType);
	}

	@Override
	public void start() {
		Log.d(TAG, "Temperature Plugin Started!");
		if (mSensorManager != null) {
			mSensorManager.registerSensorListener(listener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	@Override
	public void stop() {
		Log.d(TAG, "Temperature Plugin Stopped!");
		if (mSensorManager != null) {
			mSensorManager.unregisterListener(listener);
		}

	}

	@Override
	public void destroy() {
		this.stop();
		Log.d(TAG, "NoiseLevel Plugin Destroyed!");
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