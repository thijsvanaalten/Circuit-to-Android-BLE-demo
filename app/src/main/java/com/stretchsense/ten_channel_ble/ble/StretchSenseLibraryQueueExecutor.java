package com.stretchsense.ten_channel_ble.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import java.util.LinkedList;
import java.util.UUID;

/**
 * StretchSenseLibraryQueueExecutor Class
 *
 * Encapsulate a list of actions to execute. Actions are queue and execute sequentially to avoid problems
 *
 * @author StretchSense
 * @version 1.0
 * @since 07/2016
 * @see ' www.stretchsense.com
 */
public class StretchSenseLibraryQueueExecutor extends BluetoothGattCallback {
    // Constants
    private static String CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public interface ServiceAction {

        boolean execute(BluetoothGatt bluetoothGatt);
    }

    private final LinkedList<ServiceAction> mQueue = new LinkedList<>();        // list of actions to execute
    private volatile ServiceAction mCurrentAction;


    /**
     * Send a notification from a service
     *
     * @param gattService The gatt of the sensor
     * @param characteristicUuidString The UUID of the characteristic formatted in String
     * @param enable The boolean to enable or not the notification
     */
    private StretchSenseLibraryQueueExecutor.ServiceAction serviceNotifyAction(final BluetoothGattService gattService, final String characteristicUuidString, final boolean enable) {

        return new StretchSenseLibraryQueueExecutor.ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                if (characteristicUuidString != null) {
                    final UUID characteristicUuid = UUID.fromString(characteristicUuidString);
                    final BluetoothGattCharacteristic dataCharacteristic = gattService.getCharacteristic(characteristicUuid);

                    final UUID clientCharacteristicConfiguration = UUID.fromString(CHARACTERISTIC_CONFIG);
                    final BluetoothGattDescriptor config = dataCharacteristic.getDescriptor(clientCharacteristicConfiguration);

                    // enableNotification/disable locally
                    bluetoothGatt.setCharacteristicNotification(dataCharacteristic, enable);
                    // enableNotification/disable remotely
                    config.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    bluetoothGatt.writeDescriptor(config);

                    return false;
                } else {
                    return true;
                }
            }
        };
    }

    /**
     * Change the notification status of the sensor
     *
     * @param gattService The gatt of the sensor
     * @param characteristicUUID The UUID of the characteristic formatted in String
     * @param enable The boolean to enable or not the notification
     */
    void enableNotification(BluetoothGattService gattService, String characteristicUUID, boolean enable) {
        ServiceAction action = serviceNotifyAction(gattService, characteristicUUID, enable);
        mQueue.add(action);
    }

    /**
     * Execute the action on the queue
     * @param gatt The BluetoothGatt of the device
     */
    void execute(BluetoothGatt gatt) {

        if (mCurrentAction == null) {
            while (!mQueue.isEmpty()) {
                final StretchSenseLibraryQueueExecutor.ServiceAction action = mQueue.pop();
                mCurrentAction = action;
                if (!action.execute(gatt))
                    break;
                mCurrentAction = null;
            }
        }
    }

    /**
     * Defined what to do when the descriptor is read
     *
     * @param gatt The BluetoothGatt of the descriptor read
     * @param descriptor The descriptor read
     * @param status The status of the gatt
     */
    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

        super.onDescriptorRead(gatt, descriptor, status);

        mCurrentAction = null;
        execute(gatt);
    }

    /**
     * Defined what to do when the something is write on a descriptor
     *
     * @param gatt The BluetoothGatt
     * @param status The previous state of the Bluetooth
     * @param descriptor The descriptor written
     */
    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

        super.onDescriptorWrite(gatt, descriptor, status);

        mCurrentAction = null;
        execute(gatt);
    }

    /**
     * Defined what to do when the something is write on a caracteristic
     *
     * @param gatt The BluetoothGatt
     * @param status The previous state of the Bluetooth
     * @param characteristic The chareacteristic written
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        super.onCharacteristicWrite(gatt, characteristic, status);

        mCurrentAction = null;
        execute(gatt);

    }

    /**
     * Defined what to do when the status of the Bluetooth change
     *
     * @param gatt The BluetoothGatt
     * @param status The previous state of the Bluetooth
     * @param newState The new state of the Bluetooth
     */
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            mQueue.clear();
            mCurrentAction = null;
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

        super.onCharacteristicRead(gatt, characteristic, status);

        mCurrentAction = null;
        execute(gatt);

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
    }

    /**
     * Creation of the Gatt Executor listener used in the LibraryManager
     * @param listener The listener used
     */
    static StretchSenseLibraryQueueExecutor createExecutor(final StretchSenseLibraryQueueExecutorListener listener) {

        return new StretchSenseLibraryQueueExecutor() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

                super.onConnectionStateChange(gatt, status, newState);
                listener.onConnectionStateChange(gatt, status, newState);

            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                super.onServicesDiscovered(gatt, status);
                listener.onServicesDiscovered(gatt, status);

            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

                super.onCharacteristicRead(gatt, characteristic, status);
                listener.onCharacteristicRead(gatt, characteristic, status);

            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

                super.onCharacteristicChanged(gatt, characteristic);
                listener.onCharacteristicChanged(gatt, characteristic);

            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

                super.onReadRemoteRssi(gatt, rssi, status);
                listener.onReadRemoteRssi(gatt, rssi, status);

            }

        };

    }

    public interface StretchSenseLibraryQueueExecutorListener {

        /**
         * Defined what to do when the status of the Bluetooth change
         *
         * @param gatt The BluetoothGatt
         * @param status The previous state of the Bluetooth
         * @param newState The new state of the Bluetooth
         */
        void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);

        /**
         * Defined what to do when a service of the new peripheral is discovered
         *
         * @param gatt The BluetoothGatt of the service discovered
         * @param status The status of the gatt
         */
        void onServicesDiscovered(BluetoothGatt gatt, int status);

        /**
         * Defined what to do when the carateristic is read
         *
         * @param gatt The BluetoothGatt of the carateristic read
         * @param characteristic The caracteristic read
         * @param status The status of the gatt
         */
        void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

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
         * @param gatt The BluetoothGatt of the carateristic read
         * @param rssi The power of the signal rssi
         * @param status The status of the gatt
         */
        void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);

    }
}

