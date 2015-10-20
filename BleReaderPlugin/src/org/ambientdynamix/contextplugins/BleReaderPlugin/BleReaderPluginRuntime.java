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

import java.util.ArrayList;
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
	private static final long SCAN_PERIOD = 3000;
	private static final long DATA_SCAN_PERIOD = 30000;
	private Context context;
	public static double REFERENCE = 0.00002;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String devicename = "Blend";
	private Handler handler = new Handler();
	private Map<String, String> values = new HashMap<>();
	private List<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();
	private List<Map<String, String>> listItems = new ArrayList<Map<String, String>>();
	private Map<String, String> map = new HashMap<String, String>();
	private String DEVICE_NAME = "name";
	private String DEVICE_ADDRESS = "address";
	private int numDevicesReceive;
	private BluetoothGatt mBluetoothGatt;
	private Map<UUID, BluetoothGattCharacteristic> mapa = new HashMap<UUID, BluetoothGattCharacteristic>();
	public final static UUID UUID_BLE_SHIELD_TX = UUID.fromString(RBLGattAttributes.BLE_SHIELD_TX);
	public final static UUID UUID_BLE_SHIELD_RX = UUID.fromString(RBLGattAttributes.BLE_SHIELD_RX);
	public final static UUID UUID_BLE_SHIELD_SERVICE = UUID.fromString(RBLGattAttributes.BLE_SHIELD_SERVICE);
	private boolean enabled = false;
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
			if (device.getName() != null && device.getName().contains(devicename) && !mDevices.contains(device)) {
				Log.i(TAG, "device:" + device.toString());
				mDevices.add(device);
			}
		}
	};

	private Runnable runnableCode = new Runnable() {
		@Override
		public void run() {
			if (enabled) {
				mDevices.clear();
				listItems.clear();
				mapa.clear();
				//
				if (mBluetoothGatt != null) {
					mBluetoothGatt.disconnect();
					mBluetoothGatt.close();
					mBluetoothGatt = null;
				}

				mBluetoothAdapter.startLeScan(mLeScanCallback);
				try {
					Thread.sleep(SCAN_PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				FindDevice();

				Log.i(TAG, "Periodic conection");
			}
			// Repeat this runnable code again every 30 seconds
			handler.postDelayed(runnableCode, DATA_SCAN_PERIOD);
		}

	};

	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		this.setPowerScheme(powerScheme);
		this.context = this.getPluginFacade().getSecuredContext(getSessionId());
		Log.i(TAG, "BleReader Initializing!!!!");
		boolean result = initialize();
		Log.i(TAG, "BleReader init:" + result);

		handler.post(runnableCode);

	}

	private boolean initialize() {
		try {
			mBluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
			mBluetoothAdapter = mBluetoothManager.getAdapter();
			if (mBluetoothAdapter == null) {
				Log.i(TAG, "Ble not supported");
				return false;
			}
			if (!mBluetoothAdapter.isEnabled()) {
				Log.i(TAG, "You need to enable Bluetooth");
				return false;
			}
			Log.i(TAG, "BleReader Initialized!!!!");
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
		Log.w(TAG, "Broadcast Timer!");
		List<Reading> r = new ArrayList<Reading>();
		PluginInfo info = new PluginInfo();

		JSONObject obj = new JSONObject();
		if (!values.isEmpty()) {
			for (final String key : values.keySet()) {
				final String val = values.get(key);
				try {
					Log.i(TAG, key + ":" + val);
					obj.put("org.ambientdynamix.contextplugins." + key.trim().toLowerCase(), val);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Log.i(TAG, obj.toString());
			r.add(new Reading(obj.toString(), PluginInfo.CONTEXT_TYPE));
			info.setPayload(r);
			info.setState("OK");
			if (requestId != null) {
				sendContextEvent(requestId, new SecuredContextInfo(info, PrivacyRiskLevel.LOW), 60000);
				Log.w(TAG, "from Request:" + info.getPayload());
			}
			values.clear();
		}
	}

	@Override
	public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config) {
		handleContextRequest(requestId, contextType);
	}

	@Override
	public void start() {
		Log.d(TAG, "BleReader Started!");
		enabled = true;

	}

	@Override
	public void stop() {
		Log.d(TAG, "BleReader Plugin Stopped!");
		enabled = false;
	}

	@Override
	public void destroy() {
		this.stop();
		Log.d(TAG, "BleReader Plugin Destroyed!");
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

	private boolean ConnectWithDevice(BluetoothDevice inputDevice) {
		boolean result = initialize();
		Log.i(TAG, "Reinit:" + result);
		Log.i(TAG, "Calling get remote device : " + inputDevice.getAddress());
		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(inputDevice.getAddress());
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
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.i(TAG, "Connected to GATT server.");
				// Attempts to discover services after successful connection.
				Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.i(TAG, "Disconnected from GATT server.");
				mBluetoothGatt.close();
				mBluetoothGatt = null;
				mapa.clear();

				numDevicesReceive++;
				Log.i(TAG, "Number of devices received until now:" + numDevicesReceive);
				if (numDevicesReceive < mDevices.size()) {// connect with a
															// other
															// device until has
															// been
															// receive the
															// variables
															// from all of them

					Log.i(TAG, String.valueOf(numDevicesReceive) + " device:" + mDevices.get(numDevicesReceive));
					ConnectWithDevice(mDevices.get(numDevicesReceive));
				} else if (numDevicesReceive == (mDevices.size())) {
					// all the device have send their variables
					numDevicesReceive = 0;
					mDevices.clear();
					Log.i(TAG, "Has been receive variables from every accesable device");
				}

			}
		}

		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				// broadcastUpdate(ACTION_GATT_RSSI, rssi);
				Log.i(TAG, "onReadRemoteRssi:" + rssi);

			} else {
				Log.w(TAG, "onReadRemoteRssi received: " + status);
			}
		};

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.i(TAG, "onServicesDiscovered.");
				BluetoothGattService gattService = mBluetoothGatt.getService(UUID_BLE_SHIELD_SERVICE);

				if (gattService == null)
					return;
				BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(UUID_BLE_SHIELD_TX);
				mapa.put(characteristic.getUuid(), characteristic);

				BluetoothGattCharacteristic characteristicRx = gattService.getCharacteristic(UUID_BLE_SHIELD_RX);
				setCharacteristicNotification(characteristicRx, true);
				readCharacteristic(characteristicRx);

			} else {
				Log.w(TAG, "onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.i(TAG, "onCharacteristicRead.");
				// broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			Log.i(TAG, "onCharacteristicChanged.");
			final byte[] byteArray = characteristic.getValue();
			String data = new String(byteArray);

			if (data.charAt(0) == 'V') {
				int VarNameLenght = byteArray[1];
				// Log.i(TAG, "Name longitud"+String.valueOf(VarNameLenght));

				String VarName = data.substring(1, VarNameLenght + 2);

				Log.i(TAG, "Name of receive variable:" + VarName);

				int Value = byteArray[VarNameLenght + 3] << 8 | 0x00 << 24 | byteArray[VarNameLenght + 2] & 0xff;

				Log.i(TAG, "Value of receive variable" + String.valueOf(Value));
				// Send an ask for a new Variable
				BluetoothGattCharacteristic characteristic1 = mapa.get(UUID_BLE_SHIELD_TX);

				values.put(VarName, String.valueOf(Value));

				byte b = 0x00;

				byte[] tx = new byte[3];
				tx[0] = b;
				tx[1] = 'V';
				tx[2] = 'n';
				Log.i(TAG, "Send command:" + tx[1]);
				characteristic1.setValue(tx);
				mBluetoothGatt.writeCharacteristic(characteristic1);

			} else if (data.charAt(0) == 'G') {
				BluetoothGattCharacteristic characteristic1 = mapa.get(UUID_BLE_SHIELD_TX);

				byte b = 0x00;

				byte[] tx = new byte[3];
				tx[0] = b;
				tx[1] = 'V';
				tx[2] = 'n';
				try {
					Log.i(TAG, "Sending command:" + tx[1]);
					characteristic1.setValue(tx);
					mBluetoothGatt.writeCharacteristic(characteristic1);
					Log.i(TAG, "Sent command:" + tx[1]);
				} catch (Exception e) {
					e.printStackTrace();
					Log.i(TAG, e.getMessage());
				}

			} else if (data.charAt(0) == 'F') {// The device dont have more
												// variable to send

				Log.i(TAG, "Ble device has finish of send variables:");

				Log.i(TAG, "Disconnecting..");
				mBluetoothGatt.disconnect();

			}

		}
	};

	private void FindDevice() {

		for (BluetoothDevice device : mDevices) {
			map = new HashMap<String, String>();
			map.put(DEVICE_NAME, device.getName());
			map.put(DEVICE_ADDRESS, device.getAddress());
			listItems.add(map);
		}
		HashMap<String, String> hashMap;
		Log.i(TAG, "Number of devices:" + String.valueOf(mDevices.size()));

		if (mDevices.size() > 0) {
			// Make a connection with the first device on the list
			numDevicesReceive = 0;
			hashMap = (HashMap<String, String>) listItems.get(0);
			Log.i(TAG, "First device:" + mDevices.get(0));
			ConnectWithDevice(mDevices.get(0));
		}
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
	public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {

		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		if (UUID_BLE_SHIELD_RX.equals(characteristic.getUuid())) {
			BluetoothGattDescriptor descriptor = characteristic
					.getDescriptor(UUID.fromString(RBLGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		}
	}
}