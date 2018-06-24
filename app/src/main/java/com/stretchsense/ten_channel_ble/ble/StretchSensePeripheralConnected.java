package com.stretchsense.ten_channel_ble.ble;

import android.bluetooth.BluetoothGatt;

import java.util.ArrayList;

/**
 * StretchSensePeripheralConnected Class
 *
 * Defined a StretchSense sensor available
 *
 * @author StretchSense
 * @version 1.0
 * @since 07/2016
 * @see ' www.stretchsense.com
 */
public class StretchSensePeripheralConnected {

    ////////////////
    // PARAMETERS
    ////////////////

    public BluetoothGatt gatt;

    /**
     * The id of the device
     */
    public String id;
    /**
     * The actual value of the device
     */
    public float[] value;
    /**
     * The state of the device
     */
    public int state = 0;
    /**
     * The unique number of the device (defined by order of appearance)
     */
    public int uniqueNumber = 0;
    /**
     * The list of previous values raw of the device
     */
    public ArrayList<Double> listPreviousRawValues = new ArrayList<>();
    /**
     * The list of previous value averaged of the device
     */
    public ArrayList<Double> listPreviousAveragedValues = new ArrayList<>();

    /**
     * The number of sample saved in the lists
     */
    public int NUMBER_OF_SAMPLE = 10;

    // Enumerate
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    ////////////////
    // CONSTRUCTORS
    ////////////////

    public StretchSensePeripheralConnected(){
        id = "";
        value = new float[10] ;
        this.state = STATE_DISCONNECTED;
    }

    public StretchSensePeripheralConnected(String id, int state, int uniqueNumber, BluetoothGatt gatt) {
        this.id = id;
        this.value = new float[10];
        this.state = state;
        this.uniqueNumber = uniqueNumber;
        this.gatt = gatt;

        for (int i = 0; i < NUMBER_OF_SAMPLE ; i++){
            this.listPreviousRawValues.add(0.0);
        }
        for (int i = 0; i < NUMBER_OF_SAMPLE ; i++){
            this.listPreviousAveragedValues.add(0.0);
        }
    }

    public StretchSensePeripheralConnected(String id, float[] value){
        this.id = id;
        this.value = value;
        this.state = STATE_DISCONNECTED;
    }

    ////////////////
    // FUNCTIONS
    ////////////////

    public String toString(){
        return (id + " " + value.toString() + " " + state + " " );
    }

    public void setId(String idString){
        this.id = idString;
    }

    public void setValue(float[] valueDouble){
        this.value = valueDouble;
    }


}