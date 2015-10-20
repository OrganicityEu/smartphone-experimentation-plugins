package org.ambientdynamix.contextplugins.NoiseLevelPlugin;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import eu.smartsantander.androidExperimentation.jsonEntities.Reading;
import org.ambientdynamix.api.contextplugin.AutoReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class NoiseLevelPluginRuntime extends AutoReactiveContextPluginRuntime {

	private final String TAG = this.getClass().getSimpleName();
	private Context context;
	public static double REFERENCE = 0.00002;
	private MediaRecorder mRecorder = null;
	private boolean enabled = false;
	private Handler handler;
	private LinkedList<Double> queue = new LinkedList<Double>();

	private long SENSOR_POLL_INTERVAL = 1000;

	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			captureNoiseLevel();
			if (enabled) {
				handler.postDelayed(this, SENSOR_POLL_INTERVAL);
			}
		}
	};

	double captureNoiseLevel() {
		try {
			mRecorder = new MediaRecorder();
			mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			mRecorder.setOutputFile("/dev/null");
			mRecorder.prepare();
			mRecorder.start();
			double sum = 0;
			double ma = mRecorder.getMaxAmplitude();
			double value;
			for (int i = 1; i <= 10; i++) {
				Thread.sleep(100);
				sum += mRecorder.getMaxAmplitude();
				ma = sum / i;

			}
			Log.w(TAG, "NoiseLevel Max Anplitute AVG:" + ma);
			value = (ma / 51805.5336);
			double db = 20 * Math.log10(value / REFERENCE);
			Log.w(TAG, "NoiseLevel db:" + db);
			this.queue.addLast(db);
			if (this.queue.size() > 10)
				this.queue.removeFirst();
			mRecorder.stop();
			mRecorder.release();
			return db;
		} catch (Exception e) {
			Log.w("NoiseLevel Plugin Error", e.toString());
			return -1;
		}
	}

	public void broadcastNoiseLevel(UUID requestId, String contextType) {
		if (requestId != null)
			Log.w(TAG, "NoiseLevel Broadcast:" + requestId);
		else
			Log.w(TAG, "NoiseLevel Broadcast Timer!");
		List<Reading> r = new ArrayList<Reading>();
		PluginInfo info = new PluginInfo();
		if (this.queue.size() == 0) {
			double db = captureNoiseLevel();
			Log.w(TAG, "NoiseLevel Plugin:" + db);
		} else {
			for (Double dbM : this.queue) {
				JSONObject obj = new JSONObject();
				try {
					obj.put("org.ambientdynamix.contextplugins.NoiseLevel", dbM);
					r.add(new Reading(obj.toString(), PluginInfo.CONTEXT_TYPE));
				} catch (Exception e) {

				}
			}
		}
		info.setPayload(r);
		info.setState("OK");
		if (requestId != null) {
			sendContextEvent(requestId, new SecuredContextInfo(info, PrivacyRiskLevel.LOW), 60000);
			Log.w(TAG, "NoiseLevel Plugin from Request:" + info.getPayload());
		} else {
			sendBroadcastContextEvent(new SecuredContextInfo(info, PrivacyRiskLevel.LOW), 60000);
			Log.w(TAG, "NoiseLevel Plugin Broadcast:" + info.getPayload());
		}
	}

	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		this.setPowerScheme(powerScheme);
		this.context = this.getSecuredContext();
		handler = new Handler();
		Log.w(TAG, "NoiseLevel Inited!");
	}

	// handle incoming context request
	@Override
	public void handleContextRequest(UUID requestId, String contextType) {
		broadcastNoiseLevel(requestId, contextType);
	}

	@Override
	public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config) {
		handleContextRequest(requestId, contextType);
	}

	@Override
	public void start() {
		Log.d(TAG, "NoiseLevel Plugin Started!");
		enabled = true;
		runnable.run();
	}

	@Override
	public void stop() {
		Log.d(TAG, "NoiseLevel Plugin Stopped!");
		enabled = false;
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