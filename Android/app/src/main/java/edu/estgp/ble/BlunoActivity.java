package edu.estgp.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract  class BlunoActivity extends Activity {

    private final static String TAG = "NJOY";

    private static final int REQUEST_ENABLE_BT = 1001;
    private static final int PERMISSION_REQUEST = 2001;

    private static final String SerialPortUUID="0000dfb1-0000-1000-8000-00805f9b34fb";
    private static final String CommandUUID="0000dfb2-0000-1000-8000-00805f9b34fb";
    private static final String ModelNumberStringUUID="00002a24-0000-1000-8000-00805f9b34fb";

    private int mBaudrate = 115200;	//set the default baud rate to 115200

    private String mPassword="AT+PASSWORD=DFRobot\r\n";
    private String mBaudrateBuffer = "AT+CURRUART=" + mBaudrate + "\r\n";

    private BluetoothGattCharacteristic mSCharacteristic;
    private BluetoothGattCharacteristic mModelNumberCharacteristic;
    private BluetoothGattCharacteristic mSerialPortCharacteristic;
    private BluetoothGattCharacteristic mCommandCharacteristic;

    public static BluetoothLeService mBluetoothLeService = null;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    private BluetoothAdapter mBluetoothAdapter;
    private String mDeviceAddress;

    private BluetoothConnectionState mConnectionState = BluetoothConnectionState.NOT_INITIALIZED;

    private Handler mHandler = new Handler();

    public boolean mConnected = false;


    public abstract void onBluetoothPermissionsResult(boolean allGranted);
    public abstract void onConectionStateChange(BluetoothConnectionState theconnectionStateEnum);
    public abstract void onSerialReceived(String theString);

    public void serialSend(String theString) {
        if (mConnectionState == BluetoothConnectionState.CONNECTED) {
            mSCharacteristic.setValue(theString);
            mBluetoothLeService.writeCharacteristic(mSCharacteristic);
        }
    }

    public void serialBegin(int baud) {
        mBaudrate = baud;
        mBaudrateBuffer = "AT+CURRUART="+mBaudrate+"\r\n";
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private Runnable mConnectingOverTimeRunnable = new Runnable(){

        @Override
        public void run() {
            Log.d("NJOY", "mConnectingOverTimeRunnable");
            if (mConnectionState == BluetoothConnectionState.CONNECTING)
                mConnectionState = BluetoothConnectionState.SCAN;
            onConectionStateChange(mConnectionState);

            if (mBluetoothLeService != null) mBluetoothLeService.close();
        }};

    private Runnable mDisonnectingOverTimeRunnable = new Runnable(){

        @Override
        public void run() {
            if (mConnectionState == BluetoothConnectionState.DISCONNECTING)
                mConnectionState = BluetoothConnectionState.SCAN;

            onConectionStateChange(mConnectionState);

            if (mBluetoothLeService != null) mBluetoothLeService.close();
        }};


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_CONTACTS
                },
                PERMISSION_REQUEST
            );
        }

        // Use this check to determine whether BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE não suportado. A terminar...", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes Bluetooth adapter.
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // connect to joystick
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        serialBegin(9600);

        Log.d("NJOY", "BlunoActivity.onCreate");
    }

    @Override
    protected void onResume(){
        super.onResume();

        Log.d("NJOY", "BlunoActivity.onResume");
        // Ensures Bluetooth is enabled on the device. If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        if (mBluetoothLeService != null) {
            Log.d("NJOY", ".........connectToDevice........");
            connectToDevice();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d("NJOY", "BlunoActivity.onPause");

        unregisterReceiver(mGattUpdateReceiver);
        mConnectionState = BluetoothConnectionState.SCAN;
        onConectionStateChange(mConnectionState);

        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
            mHandler.postDelayed(mDisonnectingOverTimeRunnable, 10000);
//			mBluetoothLeService.close();
        }

        mSCharacteristic = null;
    }

    protected void onStop() {
        super.onStop();

        Log.d("NJOY", "BlunoActivity.onStop");
        if(mBluetoothLeService != null)  {
//			mBluetoothLeService.disconnect();
//            mHandler.postDelayed(mDisonnectingOverTimeRunnable, 10000);
            mHandler.removeCallbacks(mDisonnectingOverTimeRunnable);
            mBluetoothLeService.close();
        }
        mSCharacteristic = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("NJOY", "BlunoActivity.onDestroy");
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d("NJOY", "onRequestPermissionsResult: "+requestCode);
        switch (requestCode) {
            case PERMISSION_REQUEST:
                Map<String, Integer> perms = new HashMap<String, Integer>();
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);

                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    onBluetoothPermissionsResult(true);
                }
                else {
                    onBluetoothPermissionsResult(false);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Log.d("NJOY", "BroadcastReceiver.onReceive action="+action);

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                mHandler.removeCallbacks(mConnectingOverTimeRunnable);
            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mConnectionState = BluetoothConnectionState.SCAN;
                onConectionStateChange(mConnectionState);
                mHandler.removeCallbacks(mDisonnectingOverTimeRunnable);
                mBluetoothLeService.close();
            }
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                getGattServices(mBluetoothLeService.getSupportedGattServices());
            }
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (mSCharacteristic == mModelNumberCharacteristic) {
                    if (intent.getStringExtra(BluetoothLeService.EXTRA_DATA).toUpperCase().startsWith("DF BLUNO")) {
                        mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, false);

                        mSCharacteristic = mCommandCharacteristic;
                        mSCharacteristic.setValue(mPassword);
                        mBluetoothLeService.writeCharacteristic(mSCharacteristic);

                        mSCharacteristic.setValue(mBaudrateBuffer);
                        mBluetoothLeService.writeCharacteristic(mSCharacteristic);

                        mSCharacteristic = mSerialPortCharacteristic;
                        mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, true);

                        mConnectionState = BluetoothConnectionState.CONNECTED;
                        onConectionStateChange(mConnectionState);
                    }
                    else {
                        Log.d("NJOY", "XXX="+intent.getStringExtra(BluetoothLeService.EXTRA_DATA).toUpperCase());
                        mConnectionState = BluetoothConnectionState.SCAN;
                        onConectionStateChange(mConnectionState);
                    }
                }
                else if (mSCharacteristic == mSerialPortCharacteristic) {
                    onSerialReceived(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                }

                // Log.d("NJOY", "displayData "+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

//            	mPlainProtocol.mReceivedframe.append(intent.getStringExtra(BluetoothLeService.EXTRA_DATA)) ;
//            	Log.d("NJOY", ("mPlainProtocol.mReceivedframe:");
//            	Log.d("NJOY", mPlainProtocol.mReceivedframe.toString());
            }
        }
    };


    // Code to manage Service lifecycle.
    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d("NJOY", "++++++++++++ mServiceConnection.onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            else {
          //      connectToDevice();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("NJOY", "++++++++++++ mServiceConnection.onServiceDisconnected");
            mBluetoothLeService = null;
        }
    };

    private void getGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        mModelNumberCharacteristic = null;
        mSerialPortCharacteristic = null;
        mCommandCharacteristic = null;
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();

            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                uuid = gattCharacteristic.getUuid().toString();
                if(uuid.equals(ModelNumberStringUUID)){
                    mModelNumberCharacteristic=gattCharacteristic;
                }
                else if(uuid.equals(SerialPortUUID)){
                    mSerialPortCharacteristic = gattCharacteristic;
//                    updateConnectionState(R.string.comm_establish);
                }
                else if(uuid.equals(CommandUUID)){
                    mCommandCharacteristic = gattCharacteristic;
//                    updateConnectionState(R.string.comm_establish);
                }
            }
            mGattCharacteristics.add(charas);
        }

        if (mModelNumberCharacteristic==null || mSerialPortCharacteristic==null || mCommandCharacteristic==null) {
            mConnectionState = BluetoothConnectionState.SCAN;
            onConectionStateChange(mConnectionState);
        }
        else {
            mSCharacteristic = mModelNumberCharacteristic;
            mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, true);
            mBluetoothLeService.readCharacteristic(mSCharacteristic);
        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public BluetoothConnectionState getConnectionState() {
        return mConnectionState;
    }

    private void connectToDevice() {
        if (mBluetoothLeService == null || mConnectionState == BluetoothConnectionState.CONNECTED) {
            Log.d("NJOY", "connectToDevice service is NULL || CONNECTED");
            return;
        }

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("njoy", Context.MODE_PRIVATE);
        String defaultValue = "";
        mDeviceAddress = sharedPref.getString("address", defaultValue);

        //mDeviceAddress = "C4:BE:84:E3:9F:65";

        Log.d("NJOY", "---------------    connectToDevice: " + mDeviceAddress);
        if (!mDeviceAddress.equals("") && mBluetoothLeService.connect(mDeviceAddress)) {
            Log.d(TAG, "Connect request success");
            mConnectionState = BluetoothConnectionState.CONNECTING;
            onConectionStateChange(mConnectionState);
            mHandler.postDelayed(mConnectingOverTimeRunnable, 10000);
        }
        else {
            Log.d(TAG, "Connect request fail");
            mConnectionState = BluetoothConnectionState.SCAN;
            onConectionStateChange(mConnectionState);
        }
    }
}
