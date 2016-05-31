package org.ambientdynamix.contextplugins.BleReaderPlugin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import eu.smartsantander.androidExperimentation.jsonEntities.Reading;

import org.ambientdynamix.api.contextplugin.AutoReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;
import org.ambientdynamix.api.contextplugin.security.SecuredSensorManager;
import org.ambientdynamix.contextplugins.BleReaderPlugin.PluginInfo;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BleReaderPluginRuntime extends AutoReactiveContextPluginRuntime {

	private final String TAG = this.getClass().getSimpleName();
	private static final long SCAN_PERIOD = 100;
	private static final long DATA_SCAN_PERIOD = 20000;
	private Context context;
	public static double REFERENCE = 0.00002;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String devicename = "Blend";
	private Handler handler = new Handler();
	private Map<String, String> values = new HashMap<>();
	private List<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();
	private String DEVICE_NAME = "name";
	private String DEVICE_ADDRESS = "address";
	private int numDevicesReceive;
	private BluetoothGatt mBluetoothGatt;
	public final static UUID UUID_BLE_SHIELD_TX = UUID
			.fromString(RBLGattAttributes.BLE_SHIELD_TX);
	public final static UUID UUID_BLE_SHIELD_RX = UUID
			.fromString(RBLGattAttributes.BLE_SHIELD_RX);
	public final static UUID UUID_BLE_SHIELD_SERVICE = UUID
			.fromString(RBLGattAttributes.BLE_SHIELD_SERVICE);
	private boolean enabled = false;
	private BluetoothGattService gattService = null;
	byte[] tx = new byte[] { 0x00, 'V', 'n' };

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				byte[] scanRecord) {
			if (device.getName() != null
					&& device.getName().contains(devicename)
					&& !mDevices.contains(device)) {
				Log.i(TAG, "Found " + devicename + ":" + device.toString());
				mDevices.add(device);
			}
		}
	};

	private Runnable runnableCode = new Runnable() {
		@Override
		public void run() {
			if (enabled) {
				//
				if (mBluetoothGatt != null) {
					mDevices.clear();
					mBluetoothGatt.disconnect();
					mBluetoothGatt.close();
					mBluetoothGatt = null;
				}
				findDevice();

				Log.d(TAG, "Periodic conection");
			}
			// Repeat this runnable code again every DATA_SCAN_PERIOD seconds
			handler.postDelayed(runnableCode, DATA_SCAN_PERIOD);
		}
	};

	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings)
			throws Exception {
		this.setPowerScheme(powerScheme);
		this.context = this.getPluginFacade().getSecuredContext(getSessionId());
		Log.i(TAG, "BleReader init");
		boolean result = initialize();
		// handler.post(runnableCode);
	}

	private boolean initialize() {
		try {
			mBluetoothManager = (BluetoothManager) this.context
					.getSystemService(Context.BLUETOOTH_SERVICE);
			mBluetoothAdapter = mBluetoothManager.getAdapter();
			if (mBluetoothAdapter == null) {
				Log.e(TAG, "Ble not supported");
				return false;
			}
			if (!mBluetoothAdapter.isEnabled()) {
				Log.e(TAG, "You need to enable Bluetooth");
				return false;
			}
			Log.d(TAG, "BleReader Initialized");
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
		}
		return true;
	}

	// handle incoming context request
	@Override
	public void handleContextRequest(UUID requestId, String contextType) {
		if (requestId == null) {
			return;
		}
		final List<Reading> r = new ArrayList<Reading>();
		final PluginInfo info = new PluginInfo();

		final JSONObject obj = new JSONObject();
		if (!values.isEmpty()) {
			for (final String key : values.keySet()) {
				final String val = values.get(key);
				try {
					obj.put("org.ambientdynamix.contextplugins."
							+ key.trim().toLowerCase(), val);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Log.d(TAG, obj.toString());
			r.add(new Reading(obj.toString(), PluginInfo.CONTEXT_TYPE));
			info.setPayload(r);
			info.setState("OK");
			if (requestId != null) {
				sendContextEvent(requestId, new SecuredContextInfo(info,
						PrivacyRiskLevel.LOW), 60000);
				Log.w(TAG, "from Request:" + info.getPayload());
			}
			values.clear();
		}
	}

	@Override
	public void handleConfiguredContextRequest(UUID requestId,
			String contextType, Bundle config) {
		handleContextRequest(requestId, contextType);
	}

	@Override
	public void start() {
		Log.d(TAG, "BleReader sensor Started!");
		if (!enabled) {
			handler.post(runnableCode);
		}
		enabled = true;
	}

	@Override
	public void stop() {
		Log.d(TAG, "BleReader sensor Stopped!");
		enabled = false;
		handler.removeCallbacks(runnableCode);
	}

	@Override
	public void destroy() {
		this.stop();
		Log.d(TAG, "BleReader sensor Destroyed!");
		enabled = false;
		handler.removeCallbacks(runnableCode);
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

	private boolean connectWithDevice(final BluetoothDevice inputDevice) {
		boolean result = initialize();
		final BluetoothDevice device = mBluetoothAdapter
				.getRemoteDevice(inputDevice.getAddress());
		if (device == null) {
			Log.w(TAG, "Device not found. Unable to connect.");
			return false;
		}
		Log.i(TAG, "Remote Device : " + inputDevice);
		// We want to directly connect to the device, so we are setting the
		// autoConnect parameter to false.
		mBluetoothGatt = device.connectGatt(this.context, false, mGattCallback);
		Log.d(TAG, "Trying to create a new connection.");
		return true;
	}

	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
		public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
		public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
		public final static String ACTION_GATT_RSSI = "ACTION_GATT_RSSI";
		public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
		public final static String EXTRA_DATA = "EXTRA_DATA";

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {

			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.d(TAG, "Connected to GATT server.");
				// Attempts to discover services after successful connection.
				Log.d(TAG, "Attempting to start service discovery:"
						+ mBluetoothGatt.discoverServices());
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.d(TAG, "Disconnected from GATT server.");
				mBluetoothGatt.disconnect();
				mBluetoothGatt.close();
				mBluetoothGatt = null;

				numDevicesReceive++;
				Log.d(TAG, "Number of devices received until now:"
						+ numDevicesReceive);
				if (numDevicesReceive < mDevices.size()) {
					// connect with the next device until all devices are read
					Log.d(TAG, String.valueOf(numDevicesReceive) + " device:"
							+ mDevices.get(numDevicesReceive));
					connectWithDevice(mDevices.get(numDevicesReceive));
				} else if (numDevicesReceive == (mDevices.size())) {
					// all devices are read
					numDevicesReceive = 0;
					Log.i(TAG, "Read sensors from all BLE devices.");
				}

			}
		}

		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
		};

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.d(TAG, "onServicesDiscovered");
				gattService = mBluetoothGatt
						.getService(UUID_BLE_SHIELD_SERVICE);
				if (gattService == null) {
					return;
				}

				setCharacteristicNotification(
						gattService.getCharacteristic(UUID_BLE_SHIELD_RX), true);
				readCharacteristic(gattService
						.getCharacteristic(UUID_BLE_SHIELD_RX));

			} else {
				Log.w(TAG, "onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.d(TAG, "onCharacteristicRead");
				// broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			Log.d(TAG, "onCharacteristicChanged");
			final byte[] byteArray = characteristic.getValue();
			String data = new String(byteArray);

			if (data.charAt(0) == 'V') {
				// The device sent one variable
				final int varNameLenght = byteArray[1];
				final String varName = data.substring(2, varNameLenght + 2);
				byte floatdata[] = new byte[4];
				floatdata[0] = byteArray[varNameLenght + 2];
				floatdata[1] = byteArray[varNameLenght + 3];
				floatdata[2] = byteArray[varNameLenght + 4];
				floatdata[3] = byteArray[varNameLenght + 5];
				ByteBuffer bufferfloat = ByteBuffer.wrap(floatdata);
				float value = bufferfloat.getFloat(); // This helps to

				Log.i(TAG, "Received BLE data {" + varName + ":" + value + "}");

				values.put(varName, String.valueOf(value));

				// Request a new sensor value
				requestNewVariable();

			} else if (data.charAt(0) == 'G') {
				// Request a new sensor value
				requestNewVariable();
			}
		}

		void requestNewVariable() {
			try {
				final BluetoothGattCharacteristic bleTX = gattService
						.getCharacteristic(UUID_BLE_SHIELD_TX);
				bleTX.setValue(tx);
				mBluetoothGatt.writeCharacteristic(bleTX);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, e.getMessage());
			}
		}
	};

	private void findDevice() {
		Timer mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				mBluetoothAdapter.startLeScan(mLeScanCallback);
				try {
					Thread.sleep(SCAN_PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				Log.d(TAG,
						"Number of devices:" + String.valueOf(mDevices.size()));
				numDevicesReceive = 0;
				if (!mDevices.isEmpty()) {
					// Make a connection with the first device on the list
					connectWithDevice(mDevices.get(0));
				}
			}
		}, SCAN_PERIOD);
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		mBluetoothGatt.readCharacteristic(characteristic);
	}

	/**
	 * Enables or disables notification on a give characteristic.
	 * 
	 * @param characteristic
	 *            Characteristic to act on.
	 * @param enabled
	 *            If true, enable notification. False otherwise.
	 */
	public void setCharacteristicNotification(
			BluetoothGattCharacteristic characteristic, boolean enabled) {

		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		if (UUID_BLE_SHIELD_RX.equals(characteristic.getUuid())) {
			BluetoothGattDescriptor descriptor = characteristic
					.getDescriptor(UUID
							.fromString(RBLGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
			descriptor
					.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		}
	}
}