package com.stretchsense.ten_channel_ble.ble;

import android.bluetooth.BluetoothDevice;

/**
 * StretchSensePeripheralAvailable Class
 *
 * Define a sensor available
 *
 * @author StretchSense
 * @version 1.0
 * @since 07/2016
 * @see ' www.stretchsense.com
 */
public class StretchSensePeripheralAvailable {
    /**
     * The device available
     */
    public BluetoothDevice device;
    /**
     * The power of the signal
     */
    public int rssi;
    /**
     * The state of the device
     */
    public int state;
    public int uniqueNumber;
    public String id;

    // Enumeration of the states available for the sensor
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public StretchSensePeripheralAvailable(BluetoothDevice device, String id) {
        this.device = device;
        this.id = id;
        this.state = STATE_DISCONNECTED;
        this.uniqueNumber = 0;

    }

    public StretchSensePeripheralAvailable(BluetoothDevice device, String id, int uniqueNumber) {
        this.uniqueNumber = uniqueNumber;
        this.id = id;
        this.device = device;
        this.state = STATE_DISCONNECTED;

    }
}
