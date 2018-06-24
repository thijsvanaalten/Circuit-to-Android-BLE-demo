package com.stretchsense.ten_channel_ble.ble;


/**
 * Constants Class
 *
 * Define all the constants of the API
 *
 * @author StretchSense
 * @version 1.0
 * @since 07/2016
 * @see ' www.stretchsense.com
 */
class Constants {

    // Enumerations Type of the average

    private static final int NO_AVERAGE = 0;
    private static final int AVERAGE_IIR = 1;
    private static final int AVERAGE_FIR = 2;

    // User Parameter

    /**
     * Boolean to connect automatically when a sensor is detected or wait for the connect function
     */
    public static boolean AUTOCONNECT = false;
    /**
     * Time period to scan for a sensor
     */
    public static final long SCAN_PERIOD = 20 * 1000; // 20 * 1000ms
    /**
     * Initial value of sampling time between two sample ((value + 1)*40ms)
     */
    public static int VALUE_SAMPLING_TIME = 0; // (value + 1)*40ms
    /**
     * Type of average used (NO_AVERAGE, AVERAGE_IIR or AVERAGE_FIR)
     */
    public static int TYPE_OF_AVERAGE = NO_AVERAGE;

    // CONSTRUCTOR


    public Constants() {
    }
}
