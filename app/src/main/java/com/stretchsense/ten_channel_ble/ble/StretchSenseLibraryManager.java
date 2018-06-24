package com.stretchsense.ten_channel_ble.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * StretchSenseLibraryManager Class
 *
 * Bluetooth manager to discover, connect and communicate with a StretchSense sensor
 *
 * @author StretchSense
 * @version 1.0
 * @since 07/2016
 * @see ' www.stretchsense.com
 */


public class StretchSenseLibraryManager implements StretchSenseLibraryQueueExecutor.StretchSenseLibraryQueueExecutorListener {

    ////////////////
    // PARAMETERS
    ////////////////

    // Array list of the StretchSense Sensors available and connected
    /**
     * List of all the StretchSense sensor connected
     */
    public ArrayList<StretchSensePeripheralConnected> listStretchSensePeripheralsConnected = new ArrayList<>(); // Peripheral connected
    /**
     * List of all the StretchSense sensor once connected
     */
    public ArrayList<StretchSensePeripheralConnected> listStretchSensePeripheralsOnceConnected = new ArrayList<>(); // Peripheral connected
    /**
     * List of all the StretchSense sensor available
     */
    public ArrayList<StretchSensePeripheralAvailable> listStretchSensePeripheralsAvailable = new ArrayList<>(); // Peripheral connected

    // Enumeration Status of the BLE
    private static final int STATUS_BLE_ENABLED = 0;
    private static final int STATUS_BLUETOOTH_NOT_AVAILABLE = 1;
    private static final int STATUS_BLE_NOT_AVAILABLE = 2;
    private static final int STATUS_BLUETOOTH_DISABLED = 3;

    // Enumerations State of the peripheral
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    // UUID of the StretchSense Sensor
    private static String serviceStretchSenseUUID_10channel = "00001701-7374-7265-7563-6873656e7365";
    private static String dataStretchSenseUUID_10channel = "00001702-7374-7265-7563-6873656e7365";
    private static String samplingTimeStretchSenseUUID_10channel = "00001705-7374-7265-7563-6873656e7365";
    private static String filteringStretchSenseUUID_10channel = "00001706-7374-7265-7563-6873656e7365";

   /* private static String serviceStretchSenseUUID_1channel = "00001701-7374-7265-7563-6873656e7365";
    private static String dataStretchSenseUUID_1channel = "00001702-7374-7265-7563-6873656e7365";
    private static String samplingTimeStretchSenseUUID_1channel = "00001705-7374-7265-7563-6873656e7365";
    private static String filteringStretchSenseUUID_1channel = "00001706-7374-7265-7563-6873656e7365";
    */

    // Filter UUID
    private List<UUID> mServicesToDiscover;
    private UUID uuidServiceStretchSense_10channel = UUID.fromString(serviceStretchSenseUUID_10channel);
    //private UUID uuidServiceStretchSense_1channel = UUID.fromString(serviceStretchSenseUUID_1channel);

    private UUID[] uuidServicesFilter = new UUID[2];

    private boolean isNotificationsEnabled = false;

    // Scanner Callbacks
    private LeScansPoster mLeScansPoster;
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    //noinspection SynchronizeOnNonFinalField
                    synchronized (mLeScansPoster) {
                        if (mServicesToDiscover == null || !Collections.disjoint(parseUuids(scanRecord), mServicesToDiscover)) {       // only process the devices with uuids in mServicesToDiscover
                            mLeScansPoster.set(device, rssi, scanRecord);
                            mMainThreadHandler.post(mLeScansPoster);
                        }
                    }
                }
            };

    // Listener
    private StretchSenseLibraryManagerListener mBleListener;

    // Context
    private Context mContext;

    // Boolean
    /**
     * Boolean to know if the bluetooth is scanning for sensors
     */
    private boolean mIsScanning = false;
    private int numberOfTheSensor;

    private final StretchSenseLibraryQueueExecutor mExecutor = StretchSenseLibraryQueueExecutor.createExecutor(this);
    private BluetoothAdapter mAdapter;
    private BluetoothGatt mGatt;

    // Handler
    private Handler mHandler;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    // User Parameter
    /**
     * Boolean to connect automatically when a sensor is detected or wait for the connect function
     */
    public boolean AUTOCONNECT;
    /**
     * Time period to scan for a sensor
     */
    public long SCAN_PERIOD; // 20 * 1000ms
    /**
     * Initial value of sampling time between two sample ((value + 1)*40ms)
     */
    public int VALUE_SAMPLING_TIME; // (value + 1)*40ms
    /**
     * Type of average used (NO_AVERAGE, AVERAGE_IIR or AVERAGE_FIR)
     */
    public int TYPE_OF_AVERAGE;

    private long currentTime = System.currentTimeMillis();

    ////////////////
    // CONSTRUCTOR
    ////////////////

    public StretchSenseLibraryManager(Context context) {
        // Init Adapter
        mContext = context.getApplicationContext();
        if (mAdapter == null) {
            mAdapter = getBluetoothAdapter(mContext);
        }

        if (mAdapter == null || !mAdapter.isEnabled()) {
            Log.e("Manager()", "Unable to obtain a BluetoothAdapter.");
        }
        // Constants
        AUTOCONNECT = Constants.AUTOCONNECT;
        SCAN_PERIOD = Constants.SCAN_PERIOD;
        TYPE_OF_AVERAGE = Constants.TYPE_OF_AVERAGE;
        VALUE_SAMPLING_TIME = Constants.VALUE_SAMPLING_TIME;

        // For scanner
        mLeScansPoster = new LeScansPoster(null);
        uuidServicesFilter[0] = uuidServiceStretchSense_10channel;
       // uuidServicesFilter[1] = uuidServiceStretchSense_1channel;

        mServicesToDiscover = Arrays.asList(uuidServicesFilter);
        mHandler = new Handler();
        //
    }

    ////////////////
    // FUNCTIONS
    ////////////////

    /**
     * Method that initialize the listener and the scanner
     *
     * @param listener The listener is a BleManagerListener, usualy use 'this' as parameter
     */
    public void initialiseTheManager(StretchSenseLibraryManagerListener listener){
        setBleListener(listener);
        InitialiseScanner();
    }

    // region Scan functions

    /**
     * Method that initialize the scanner before scanning
     *
     */
    private void InitialiseScanner() {
        if (mBleListener != null) {
            // Stop current scanning (if needed)
            stopScanning();

            if (getStatusOfTheBle(mContext) != STATUS_BLE_ENABLED) {
                Log.e("Manager(203)", "BluetoothAdapter not initialized or unspecified address.");
            } else {
     //           Log.e("Manager(205)", "Peripheral Discovered");
                // Initiate a scanner waiting for device called "StretchSense"
                BluetoothAdapter.LeScanCallback callbackStretchsense = (new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                        // If a device called "StretchSense" is discovered, ca
                        onPeripheralDiscovered("StretchSense", device, rssi, scanRecord);
                    }
                });
                //
                mLeScansPoster = new LeScansPoster(callbackStretchsense);

                startScanningForAPeriod();
            }
        }
    }

    /**
     * Method that scan for StretchSense sensors during a period in secondes fixed in the constants
     *
     */
    public void startScanningForAPeriod() {

        if (SCAN_PERIOD > 0) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mIsScanning) {
                        Log.d("Manager()", "Scan timer expired. Restart scan");
                        stopScanning();
                    }
                }
            }, SCAN_PERIOD);
        }

        mIsScanning = true;
        //noinspection deprecation
        mAdapter.startLeScan(mLeScanCallback);

    }

    /**
     * Method to know if the device is scanning
     *
     */
    public boolean isScanning(){

        return mIsScanning;

    }


    /**
     * Method that stop the scanning for StretchSense sensors
     *
     */
    public void stopScanning() {

        if (mIsScanning) {
            mHandler.removeCallbacksAndMessages(null);      // cancel pending calls to stop
            mIsScanning = false;
            //noinspection deprecation
            mAdapter.stopLeScan(mLeScanCallback);

        }
    }

    public boolean isTheBleEnable(Context context){

        if (getStatusOfTheBle(context) == STATUS_BLE_ENABLED) {
            return true;
        }
        else{
            return false;
        }
    }


    /**
     * Private inner class for running the scan
     *
     */
    private static class LeScansPoster implements Runnable {

        private final BluetoothAdapter.LeScanCallback leScanCallback;

        private BluetoothDevice device;
        private int rssi;
        private byte[] scanRecord;

        private LeScansPoster(BluetoothAdapter.LeScanCallback leScanCallback) {
            this.leScanCallback = leScanCallback;
        }

        public void set(BluetoothDevice device, int rssi, byte[] scanRecord) {
            this.device = device;
            this.rssi = rssi;
            this.scanRecord = scanRecord;
        }

        @Override
        public void run() {
            leScanCallback.onLeScan(device, rssi, scanRecord);
        }

    }

    //endregions

    // region Connect functions

    /**
     * Connect the manager with a sensor using his address
     *
     * @param address The address of the sensor
     */
    public void connectWithAddress(String address) {

        BluetoothDevice myDevice;

        myDevice = mAdapter.getRemoteDevice(address);

        if (mAdapter == null || address == null || myDevice == null) {
            Log.e("Manager(329)", "connect: BluetoothAdapter not initialized or unspecified address.");
            return;
        }

        Log.e("Manager(333)", "connect Gatt");
        mGatt = myDevice.connectGatt(mContext, false, mExecutor);

        if (mBleListener != null) {
            mBleListener.onConnecting();
        }


    }

    public void disconnectWithAddress(String address) {

        for (StretchSensePeripheralConnected myPeripheral: listStretchSensePeripheralsOnceConnected) {


        }

            for (StretchSensePeripheralConnected myPeripheral: listStretchSensePeripheralsConnected) {
                if (myPeripheral.gatt.getDevice().getAddress().toString() == address) {

                    myPeripheral.gatt.disconnect();
                }
            }
    }



    public void disconnectAll (){

        for (StretchSensePeripheralConnected myPeripheral: listStretchSensePeripheralsConnected) {

                mBleListener.onDisconnected(myPeripheral.gatt.getDevice().getAddress().toString());
                myPeripheral.gatt.disconnect();

        }



        //Will have to create something to disconnect all main views
    }


    /**
     * Connect the manager with a sensor using his address
     *
     * @param device The BluetoothDevice of the sensor
     */
    public void connectWithDevice(BluetoothDevice device) {

        connectWithAddress(device.getAddress());
    }
    // endregion

    // region Service & Characteristic functions

    /**
     * Get the service from the Gatt of the device discovered
     *
     */
    private List<BluetoothGattService> getSupportedGattServices() {

        if (mGatt != null) {
            return mGatt.getServices();
        } else {
            return null;
        }
    }

    /**
     * Get the characteristics from a service
     *
     * @param service The service of the sensor discovered
     * @param characteristicUUIDString The UUID formatted in Stringyou are looking for
     */
    private int getCharacteristicProperties(BluetoothGattService service, String characteristicUUIDString) {

        final UUID characteristicUuid = UUID.fromString(characteristicUUIDString);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        int properties = 0;
        if (characteristic != null) {
            properties = characteristic.getProperties();
        }

        return properties;
    }

    /**
     * Look if the characteristic is notifiable
     *
     * @param service The service of the sensor discovered
     * @param characteristicUUIDString The UUID formatted in Stringyou are looking for
     * @return The boolean if the characteristic is notifiable
     */
    private boolean isCharacteristicNotifiable(BluetoothGattService service, String characteristicUUIDString) {

        final int properties = getCharacteristicProperties(service, characteristicUUIDString);
        return (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    /**
     * Change the notification status of the sensor
     *
     * @param service The service of the sensor discovered
     * @param uuid The UUID of the characteristic formatted in String
     * @param enabled The boolean to enable or not the notification
     */
    private void enableNotification(BluetoothGattService service, String uuid, boolean enabled, BluetoothGatt gatt) {

        if (service != null) {

            if (mAdapter == null || gatt == null) {
                Log.e("Manager(419)", "BluetoothAdapter not initialized");
                return;
            }
            Log.e("Manager(422)", "enable Notification");

                mExecutor.enableNotification(service, uuid, enabled);
                mExecutor.execute(gatt);

                isNotificationsEnabled = true;

        }
    }

    /**
     * Change the value of the sampling time on all the sensor connected
     *
     * @param valueSlider The sampling time value you want to send to the sensor
     */
    public void writeCharacteristicSamplingTime(int valueSlider){

        // TODO Update for 1 channel FEK circuit

        for (StretchSensePeripheralConnected myPeripheral: listStretchSensePeripheralsConnected) {
            BluetoothGatt myGatt = myPeripheral.gatt;

            BluetoothGattService Service = myGatt.getService(UUID.fromString(serviceStretchSenseUUID_10channel));
            BluetoothGattCharacteristic charac = Service.getCharacteristic(UUID.fromString(samplingTimeStretchSenseUUID_10channel));

            byte[] value = new byte[1];
            value[0] = (byte) (valueSlider & 0xFF);
            charac.setValue(value);
            myGatt.writeCharacteristic(charac);
        }
    }

    /**
     * Change the value of the shutdown on all the sensor connected
     *
     * @param filterValue The shutdown value you want to send to the sensor
     */
    public void writeCharacteristicFilter(int filterValue){

        // TODO Update for 1 channel FEK circuit

        Log.e("Debug","Write filter char"+ filterValue);

        for (StretchSensePeripheralConnected myPeripheral: listStretchSensePeripheralsConnected) {

            Log.e("Debug","connected 1");
            BluetoothGatt myGatt = myPeripheral.gatt;
            BluetoothGattService Service = myGatt.getService(UUID.fromString(serviceStretchSenseUUID_10channel));

            Log.e("Debug","connected 2");
            BluetoothGattCharacteristic charac = Service.getCharacteristic(UUID.fromString(filteringStretchSenseUUID_10channel));


            Log.e("Debug","connected 3");
            byte[] value = new byte[1];
            value[0] = (byte) (filterValue & 0xFF);
            charac.setValue(value);
            myGatt.writeCharacteristic(charac);
            Log.e("Debug","connected 4");
        }
    }



    // region BleExecutorListener Override functions

    /**
     * Defined what to do when the status of the Bluetooth change
     *
     * @param gatt The BluetoothGatt
     * @param status The previous state of the Bluetooth
     * @param newState The new state of the Bluetooth
     */
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

        StretchSensePeripheralConnected peripheralChanging = getDeviceFromGatt(gatt);

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.e("Manager(494)", "State connected");

            peripheralChanging.state = STATE_CONNECTED;


            // Attempts to discover services after successful connection.
            gatt.discoverServices();

            if (mBleListener != null) {
                mBleListener.onConnected(gatt.getDevice().getAddress().toString());
            }
            else{
                Log.e("Manager(502)", "listener==null!!!");

            }
            //stopScanning();


        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.e("Manager(511)", "State disconnected"+gatt.getDevice().getName().toString());
            peripheralChanging.state = STATE_DISCONNECTED;
            String addressOfTheDeviceChangingStatus = peripheralChanging.id;


            Log.e("Debug","line0");

            int indexToRemove = -1;
            for (StretchSensePeripheralConnected myPeripheralConnected: listStretchSensePeripheralsConnected) {

                if (myPeripheralConnected.id.equals(addressOfTheDeviceChangingStatus)){
                    indexToRemove = listStretchSensePeripheralsConnected.indexOf(myPeripheralConnected);
                }
            }
            if (indexToRemove != -1) {
                listStretchSensePeripheralsConnected.remove(indexToRemove);
            }

            Log.e("Debug","line1");

            indexToRemove = -1;
            for (StretchSensePeripheralAvailable myPeripheralAvailable: listStretchSensePeripheralsAvailable) {

                if (myPeripheralAvailable.id.equals(addressOfTheDeviceChangingStatus)){
                    indexToRemove = listStretchSensePeripheralsAvailable.indexOf(myPeripheralAvailable);
                }
            }

            Log.e("Debug","onDisconnected");

            String close = gatt.getDevice().getAddress().toString();

            Log.e("Debug","close gatt");
            gatt.close();
            Log.e("Debug","closed gatt");

            if (mBleListener != null) {
                mBleListener.onDisconnected(close);
            }


            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                stopScanning();
                peripheralChanging.state = STATE_CONNECTING;
                if (mBleListener != null) {
                    mBleListener.onConnecting();
                }
            }


    }

    /**
     * Defined what to do when a peripheral is discovered
     *
     * @param deviceNameToScanFor The filter of name to scan for
     * @param device The device found
     * @param rssi The power of the device found (in dB)
     * @param scanRecord The bytes[] of the device
     */
    public void onPeripheralDiscovered(String deviceNameToScanFor, final BluetoothDevice device, final int rssi, byte[] scanRecord) {

        boolean inTheListAvailable = isTheAddressOnTheListAvailable(device.getAddress());
        boolean inTheListOnceConnected = isTheAddressOnTheListOnceConnected(device.getAddress());
        if (!inTheListAvailable) {
            if (inTheListOnceConnected){
                for (StretchSensePeripheralConnected myPeripheral : listStretchSensePeripheralsOnceConnected) {
                    if (Objects.equals(myPeripheral.id, device.getAddress())) {

                        StretchSensePeripheralAvailable newDeviceAvailable = new StretchSensePeripheralAvailable(device, device.getAddress(), myPeripheral.uniqueNumber);
                        listStretchSensePeripheralsAvailable.add(newDeviceAvailable);
                        if (AUTOCONNECT) {
                           
                            connectWithDevice(device);
                        }
                    }
                }
            }
            else{
                StretchSensePeripheralAvailable newDeviceAvailable = new StretchSensePeripheralAvailable(device , device.getAddress());
                listStretchSensePeripheralsAvailable.add(newDeviceAvailable);
                if (AUTOCONNECT) {
                    connectWithDevice(device);
                }
            }
        }

        // Call listener
        if (mBleListener != null)
            mBleListener.onPeripheralDiscovered(deviceNameToScanFor, device, rssi, scanRecord);
}


    public int getConnectedCount(){

        return listStretchSensePeripheralsConnected.size();

    }

    /**
     * Defined what to do when a service of the new peripheral is discovered
     *
     * @param gatt The BluetoothGatt of the service discovered
     * @param status The status of the gatt
     */
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        // Add the device in the connected list
        String addressOfTheDevice = (gatt.getDevice()).getAddress();
        if(!isTheAddressOnTheListConnected(addressOfTheDevice)){
            Log.e("Debug","!isTheAddressOnTheListConnected(addressOfTheDevice)");
            boolean isThePeripheralNew = true;
            for (StretchSensePeripheralConnected myPeripheralConnected: listStretchSensePeripheralsOnceConnected) {
                if (myPeripheralConnected.id.equals(addressOfTheDevice)) {
                    isThePeripheralNew = false;

                    Log.e("Debug","isThePeripheralNew = false");
                    listStretchSensePeripheralsConnected.add(myPeripheralConnected);

                    // Set the device available to STATE_CONNECTED
                    for (StretchSensePeripheralAvailable myPeripheralAvailable: listStretchSensePeripheralsAvailable) {
                        if (myPeripheralAvailable.device.getAddress().equals(addressOfTheDevice)){
                            myPeripheralAvailable.state = STATE_CONNECTED;
                            myPeripheralAvailable.uniqueNumber = myPeripheralConnected.uniqueNumber;
                        }
                    }
                }
            }
            if (isThePeripheralNew) {
                Log.e("Debug","isThePeripheralNew = isNew");

                numberOfTheSensor++;
                StretchSensePeripheralConnected newDevice = new StretchSensePeripheralConnected(addressOfTheDevice, STATE_CONNECTED, numberOfTheSensor, gatt);
                listStretchSensePeripheralsConnected.add(newDevice);
                //listStretchSensePeripheralsOnceConnected.add(newDevice);
                // Set the device available to STATE_CONNECTED
                for (StretchSensePeripheralAvailable myPeripheral: listStretchSensePeripheralsAvailable) {
                    if (myPeripheral.device.getAddress().equals(addressOfTheDevice)){
                        myPeripheral.state = STATE_CONNECTED;
                        myPeripheral.uniqueNumber = numberOfTheSensor;
                    }
                }
            }
        }


        // Services
        List<BluetoothGattService> services = getSupportedGattServices();

        assert services != null;
        for (BluetoothGattService service : services) {
            String serviceUuid = service.getUuid().toString();

            // Check if it's a StretchSense Sensor
            if (Objects.equals(serviceUuid, serviceStretchSenseUUID_10channel)){//||Objects.equals(serviceUuid, serviceStretchSenseUUID_1channel)) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    if (characteristic.getUuid().toString().equals(dataStretchSenseUUID_10channel)) {
                        Log.e("Set Notification",service.toString());
                        enableNotification(service, dataStretchSenseUUID_10channel, true, gatt);
                    }
                    /*else if (characteristic.getUuid().toString().equals(dataStretchSenseUUID_1channel)) {
                        Log.e("Set Notification",service.toString());
                        enableNotification(service, dataStretchSenseUUID_1channel, true);
                    }*/
                    if (mBleListener != null) {
                        mBleListener.onCharacteristicDiscovered(gatt, characteristic);
                    }
                }
            }
        }

    }

    /**
     * Defined what to do when the carateristic is read
     *
     * @param gatt The BluetoothGatt of the carateristic read
     * @param characteristic The caracteristic read
     * @param status The status of the gatt
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

    }

    /**
     * Defined what to do when the carateristic has changed
     *
     * @param gatt The BluetoothGatt of the carateristic read
     * @param characteristic The caracteristic read
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        byte[] valueByte = characteristic.getValue();
        float [] capacitance = new float[10];

        for (int count=0;count<10;count++){

            String valueHex = bytesToHex(valueByte);
            Long valueLong = (Long.parseLong(valueHex.substring(count*4,(count+1)*4), 16));
            capacitance[count] = (valueLong+0.0f)/10;
        }

        BluetoothDevice device = gatt.getDevice();
        String address = device.getAddress();
        for (StretchSensePeripheralConnected myPeripheral: listStretchSensePeripheralsConnected) {
            if (Objects.equals(myPeripheral.id, address)){
                // Capacitance of the sensor
                myPeripheral.setValue(capacitance);

            }
        }

        if (mBleListener != null&&System.currentTimeMillis()-currentTime>20){
            currentTime = System.currentTimeMillis();
            mBleListener.onCharacteristicChanged(gatt, characteristic);
        }

    }

    /**
     * Defined what to do when the rssi is read
     *
     * @param gatt The BluetoothGatt of the carateristic read
     * @param rssi The power of the signal rssi
     * @param status The status of the gatt
     */
    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

        if (mBleListener != null) {
            mBleListener.onReadRemoteRssi(rssi);
        }

    }
    // endregion

    // region Utils conversion functions

    /**
     * Return the status of the bluetooth
     *
     * @param context The Context of the manager
     */
    public int getStatusOfTheBle(Context context) {
        //Log.i("BleUtils()", "getBleStatus()");

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return STATUS_BLE_NOT_AVAILABLE;
        }

        final BluetoothAdapter adapter = getBluetoothAdapter(context);
        // Checks if Bluetooth is supported on the device.
        if (adapter == null) {
            return STATUS_BLUETOOTH_NOT_AVAILABLE;
        }

        if (!adapter.isEnabled()) {
            return STATUS_BLUETOOTH_DISABLED;
        }

        return STATUS_BLE_ENABLED;
    }

    /**
     * Return the BluetoothAdapter
     *
     * @param context The Context of the manager
     */
    private static BluetoothAdapter getBluetoothAdapter(Context context) {

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return null;
        } else {
            return bluetoothManager.getAdapter();
        }
    }

    /**
     * Return the last sensor connected
     *
     * @param gatt The Gatt of the manager
     */
    private StretchSensePeripheralConnected getDeviceFromGatt(BluetoothGatt gatt) {

        String addressOfTheGatt = (gatt.getDevice()).getAddress();
        StretchSensePeripheralConnected deviceFromGatt;

        for (StretchSensePeripheralConnected myPeripheral : listStretchSensePeripheralsConnected) {
            if (Objects.equals(addressOfTheGatt, myPeripheral.id)) {
                deviceFromGatt = myPeripheral;
                return deviceFromGatt;
            }
        }

        return new StretchSensePeripheralConnected(addressOfTheGatt, STATE_DISCONNECTED, 0, gatt);

    }

    /**
     * Look if the address is in the list of the connected device
     *
     * @param address The String address
     */
    public boolean isTheAddressOnTheListConnected(String address) {
        for (StretchSensePeripheralConnected myPeripheral : listStretchSensePeripheralsConnected) {
            if (Objects.equals(myPeripheral.id, address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Look if the address is in the list of the device ever connected
     *
     * @param address The String address
     */
    private boolean isTheAddressOnTheListOnceConnected(String address) {
        for (StretchSensePeripheralConnected myPeripheral : listStretchSensePeripheralsOnceConnected) {
            if (Objects.equals(myPeripheral.id, address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Look if the address is in the list of the device available
     *
     * @param address The String address
     */
    private boolean isTheAddressOnTheListAvailable(String address) {
        for (StretchSensePeripheralAvailable myPeripheral : listStretchSensePeripheralsAvailable) {
            if (Objects.equals(myPeripheral.device.getAddress(), address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convert the bytes to Hex
     *
     * @param bytes The String address
     */
    private static String bytesToHex(byte[] bytes) {

        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        if (bytes != null) {
            char[] hexChars = new char[bytes.length * 2];
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            return new String(hexChars);
        }
        else return null;

    }

    /**
     * Parse the different UUIDs from the byte
     *
     * @param advertisedData The byte[] you want to parse
     * @return The list of uuid parse
     */
    private List<UUID> parseUuids(byte[] advertisedData) {

        List<UUID> uuids = new ArrayList<>();

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;

                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;

                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }

        return uuids;

    }

    //endregion

    // region BleManagerListener

    /**
     * Set the listener
     *
     * @param listener The BleManagerListener
     */
    private void setBleListener(StretchSenseLibraryManagerListener listener) {

        mBleListener = listener;

    }

    /**
     * Defined what to do for every step of connection and communication with the device
     *
     */
    public interface StretchSenseLibraryManagerListener {

        /**
         * Defined what to do when a peripheral is discovered
         *
         * @param deviceNameToScanFor The filter of name to scan for
         * @param device The device found
         * @param rssi The power of the device found (in dB)
         * @param scanRecord The bytes[] of the device
         */
        void onPeripheralDiscovered(String deviceNameToScanFor, final BluetoothDevice device, final int rssi, byte[] scanRecord);

        /**
         * Defined what to do when a peripheral is connected
         *
         */
        void onConnected(String address);

        /**
         * Defined what to do when a peripheral is connecting
         *
         */
        void onConnecting();

        /**
         * Defined what to do when a peripheral is disconnected
         *
         */
        void onDisconnected(String address);

        /**
         * Defined what to do when a service of the new peripheral is discovered
         *
         * @param gatt The BluetoothGatt of the service discovered
         * @param status The status of the gatt
         */
        void onServicesDiscovered(BluetoothGatt gatt, int status);

        /**
         * Defined what to do when a characterisctic of the new peripheral is discovered
         *
         * @param gatt The BluetoothGatt of the service discovered
         * @param characteristic The characteristic discovered
         */
        void onCharacteristicDiscovered(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

        /**
         * Defined what to do when the characterisctic is available
         *
         * @param characteristic The characteristic available
         */
        void onDataAvailable(BluetoothGattCharacteristic characteristic);

        /**
         * Defined what to do when the carateristic has changed
         *
         * @param gatt The BluetoothGatt of the carateristic read
         * @param characteristic The caracteristic read
         */
        void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

        /**
         * Defined what to do when the rssi is read
         *
         * @param rssi The power of the signal rssi
         */
        void onReadRemoteRssi(int rssi);
    }

    //endregion

}

