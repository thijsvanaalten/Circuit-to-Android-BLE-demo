package com.stretchsense.ten_channel_ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.stretchsense.ten_channel_ble.Graphing.LineGraph;
import com.stretchsense.ten_channel_ble.ble.StretchSenseLibraryManager;
import com.stretchsense.ten_channel_ble.ble.StretchSensePeripheralAvailable;
import com.stretchsense.ten_channel_ble.ble.StretchSensePeripheralConnected;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends BaseActivity implements StretchSenseLibraryManager.StretchSenseLibraryManagerListener {

    // PARAMETERS

    // Manager Bluetooth
    private StretchSenseLibraryManager mBleManager;
    private int REQUEST_ENABLE_BT = 1;

    //Graph View
    private ImageView mImageView;
    private LineGraph mlineGraph;
    private TextView mGraphMax;
    private TextView mGraphMin;

    //Data Logging
    private BufferedWriter outputFile;
    private boolean LogData = false;
    private boolean DataLoggingStarted = false;

    private long startTime = System.currentTimeMillis();

    // Graph initialisation
    private boolean Initiate = true;

    //Filtering
    private int filter_current = 8;

    private boolean deviceConnected = false;

    public ListView UIdevicesList ;
    ArrayAdapter<String> btListadapter;
    private AlertDialog alert;
    LinearLayout valuesParentView;

    int[] idConnected = new int[100];
    int connectedCount = 0;

    @Override
    public void onBackPressed()
    {
        // Make more complex, request second press to disconnect?
        // code here to show dialog
        super.onBackPressed();  // optional depending on your needs


        if (mBleManager.getConnectedCount()>0){
            Toast.makeText(this,"Disconnecting...",Toast.LENGTH_SHORT).show();
            try {
                mBleManager.disconnectAll();
            }
            catch(Exception e){}
        }
    }


    //FUNCTION CREATION
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Set File Writer
        setFileWriter();

        UIdevicesList = new ListView(this);

        ArrayList<String> names = new ArrayList<String>();
        btListadapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,names){

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);

                return view;
            }
        };
        btListadapter.setNotifyOnChange(true);
        UIdevicesList.setAdapter(btListadapter);

        UIdevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,int position, long arg3)
            {

                if(!mBleManager.isTheAddressOnTheListConnected(btListadapter.getItem(position).toString())) {
                    Log.e("Debug", "Connect pressed");

                    Toast.makeText(MainActivity.this, "Connecting...", Toast.LENGTH_SHORT).show();
                    mBleManager.connectWithAddress(btListadapter.getItem(position).toString());

                    try {
                        if (alert.isShowing()) alert.dismiss();
                    }
                    catch(Exception e){}
                }

                else{

                 Log.e("Debug", "Disconnect pressed");
                    mBleManager.disconnectWithAddress(btListadapter.getItem(position).toString());
                }
            }

        });

        // Create the object of the Bluetooth manager
        mBleManager = new StretchSenseLibraryManager(this);
        // Element of the UI
        //textValue = new TextView[20];
        valuesParentView = (LinearLayout) findViewById(R.id.linView1);


        mGraphMax = (TextView) findViewById(R.id.graph_max);
        mGraphMin = (TextView) findViewById(R.id.graph_min);
        mImageView = (ImageView)findViewById(R.id.graph);
        final ViewTreeObserver observer = mImageView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){

                                               @Override
                                               public void onGlobalLayout() {
                                                   if(Initiate){

                                                       //Initiate LineGraph
                                                       mlineGraph = new LineGraph(mImageView, getBaseContext());
                                                       graphUpdateTimer();

                                                       Initiate=false;
                                                   }
                                               }


                                           }
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        // If the bluetooth is enable, initialize the manager and scan
        if (mBleManager.isTheBleEnable(this)) {
            mBleManager.initialiseTheManager(this);
        }
        else{
            //Notify user that Bluetooth is required and prompt enable
            enableBT();

        }
    }

    public void enableBT(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()){
            Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // The REQUEST_ENABLE_BT constant passed to startActivityForResult() is a locally defined integer (which must be greater than 0), that the system passes back to you in your onActivityResult()
            // implementation as the requestCode parameter.

            startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);
        }
    }

    private void graphUpdateTimer(){

        //UpdateGraph
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms

                try {
                    graphUpdateTimer();
                    mlineGraph.Update_Graph();

                    if (LogData){

                        //Append data to file


                        try {

                            if (connectedCount>0) {
                                //TimeStamp
                                outputFile.write(System.currentTimeMillis() - startTime + ",");

                                //Sensor Data
                                for (int count = 0; count < connectedCount; count++) {

                                    for (int count_ch = 0; count_ch < 10; count_ch++) {

                                        outputFile.write(mlineGraph.getSensorValue(idConnected[count] + count_ch) + ",");
                                    }
                                }
                            }
                            outputFile.newLine();
                            outputFile.flush();
                        }
                        catch(Exception e){

                            //Data Logging Failed
                        }

                    }


                }
                catch(Exception e ){}


                }
        }, 40);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK){
                //Bluetooth enabled successfully
                Toast.makeText(this,"Bluetooth has been enabled",Toast.LENGTH_SHORT).show();
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this,"Bluetooth is require to use this application",Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    // FUNCTIONS APPLICATION
    public void changeTheTextOf(final int id, final String text){

        // Do the action on the UI thread to change the text of the UI element
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
             try {

                 ((TextView) findViewById(id)).setText(text);

             }
             catch(Exception e){}
             }
        });

    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        if (DataLoggingStarted){
            Uri uri = Uri.fromFile(logFile);
            Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
            sendBroadcast(scanFileIntent);
        }
        else{
        //Delete log file
            Log.e("Debug","delete");
            boolean deleted = logFile.delete();
        }

        try {
            mBleManager.disconnectAll();
            mBleManager.stopScanning();
        }
        catch (Exception e)
        {

        }

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bluetoothscan:

                    //Update to show connection options when scannign
                    //Pop-up window with listview
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    LayoutInflater inflater = LayoutInflater.from(this);
                    View layout = inflater.inflate(R.layout.connection_dialog, null);

                    //Update the list
                    for (int count = 0; count < UIdevicesList.getCount(); count++) {
                        if (mBleManager.isTheAddressOnTheListConnected(btListadapter.getItem(count).toString())) {
                            try {
                                UIdevicesList.getChildAt(count).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                            } catch (Exception e) {
                            }
                        } else {
                            try {
                                UIdevicesList.getChildAt(count).setBackgroundColor(Color.TRANSPARENT);
                            } catch (Exception e) {
                            }
                        }
                    }

                    LinearLayout ConnectionListView = (LinearLayout) layout.findViewById(R.id.connection_list);
                    try {
                        ConnectionListView.addView(UIdevicesList);
                    } catch (Exception e) {
                        ((ViewGroup) UIdevicesList.getParent()).removeAllViews();
                        ConnectionListView.addView(UIdevicesList);
                    }

                //Start scanning again if timeout has been reached
                TextView scanner = (TextView) layout.findViewById(R.id.scanner);
                scanner.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        //If already scanning notify user
                        if(mBleManager.isScanning()) {
                            Toast.makeText(MainActivity.this, "Already scanning...", Toast.LENGTH_SHORT).show();
                        }
                        //Else remove all non-connected view and start scanning
                        else{
                            Toast.makeText(MainActivity.this, "Scanning...", Toast.LENGTH_SHORT).show();
                            for (int count = (UIdevicesList.getCount()-1); count >=0 ; count--) {
                                if (!mBleManager.isTheAddressOnTheListConnected(btListadapter.getItem(count).toString())) {
                                    mBleManager.listStretchSensePeripheralsAvailable.remove(count);
                                    btListadapter.remove(btListadapter.getItem(count).toString());
                                }
                            }
                                    mBleManager.startScanningForAPeriod();
                        }
                    }
                });


                builder.setTitle("Connect").setView(layout).setCancelable(true);

                    builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Close dialog
                        }
                    });

                    alert = builder.create();
                    alert.show();

                    // On rescan pressed
                    //mBleManager.startScanningForAPeriod();

                break;

            case R.id.dataLogging:

                // Start/Stop Data Logging
                onLogData();

                break;

            case R.id.Filtering:

                onFilteringClicked();

                break;

            case R.id.Share:

                onShareClicked();

                break;


        }


        return true;
    }



    public void onShareClicked(){

        if(DataLoggingStarted) {
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);

            if (logFile.exists()) {
                intentShareFile.setType("application/csv");
                intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile));

                intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                        "StretchSense Data");
                intentShareFile.putExtra(Intent.EXTRA_TEXT, "");

                startActivity(Intent.createChooser(intentShareFile, "Share File"));
            }
        }
        else{

            Toast.makeText(MainActivity.this,"No data recorded",Toast.LENGTH_SHORT).show();

        }

    }

    // FUNCTION LISTENER STRETCHSENSE MANAGER
    @Override
    public void onReadRemoteRssi(int rssi) {

    }

    boolean firstBluetoothDevicePrompt = true;

    @Override
    public void onPeripheralDiscovered(String deviceNameToScanFor, BluetoothDevice device, int rssi, byte[] scanRecord) {


        //ToDo will need some sort of blocking code to prevent spamming and repeating of circuit ID's.

            // Expend the list of the sensors available and display the address of the first sensor
            for (StretchSensePeripheralAvailable myPeripheral : mBleManager.listStretchSensePeripheralsAvailable) {
                if (!IsRepeatedInList(myPeripheral.device.getAddress())) {
                    // Loading information for the user
                    Toast.makeText(this, "A StretchSense sensor has been detected", Toast.LENGTH_SHORT).show();
                    if (firstBluetoothDevicePrompt) {
                        Toast.makeText(this, "Click on the Bluetooth icon to connect", Toast.LENGTH_SHORT).show();
                        firstBluetoothDevicePrompt=false;
                    }

                    btListadapter.add(myPeripheral.device.getAddress());
                    return;
            }
        }
    }

    public boolean IsRepeatedInList(String address){

        for (int count=0;count<UIdevicesList.getCount();count++) {
            if (address == btListadapter.getItem(count).toString()) {
                return true;
            }
        }
        return false;

    }

    @Override
    public void onConnected(final String address) {

        // Loading feedback information
        mBleManager.writeCharacteristicFilter(filter_current);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                boolean isNew = true;

                for (int count=0;count<connectedCount;count++){
                    if (idConnected[count]==hexToInt(address)){
                        isNew = false;
                    }
                }

                if(isNew) {

                    idConnected[connectedCount] = hexToInt(address);
                    connectedCount++;
                }

                for (int ColorId = 0; ColorId < 10; ColorId++) {

                int id = hexToInt(address)+ColorId;

                    createNewTextValue(id,ColorId);
                    mlineGraph.addSensor(id,ColorId);

                }
            }});
    }

    @Override
    public void onConnecting() {

        // Loading feedback information
        deviceConnected=true;
    }


    @Override
    public void onDisconnected(final String address) {

        deviceConnected=false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                for (int count = 0; count < UIdevicesList.getCount(); count++) {
                    if (!mBleManager.isTheAddressOnTheListConnected(btListadapter.getItem(count).toString())) {
                        UIdevicesList.getChildAt(count).setBackgroundColor(Color.TRANSPARENT);
                    }
                }


                for (int ColorId = 0; ColorId < 10; ColorId++) {

                    int id = hexToInt(address) + ColorId;
                    mlineGraph.removeSensor(id);
                    valuesParentView.removeView((TextView) findViewById(id));
                    TextView view = (TextView) findViewById(id);
                    view = null;
                }

        }});

        Toast.makeText(this,"Device disconnected",Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        // Loading feedback information

    }

    @Override
    public void onCharacteristicDiscovered(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        // Loading feedback information

    }

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic) {

        // Loading feedback information

    }


    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        // Expend the list of the sensors connect and display the address of the first sensor
        for (StretchSensePeripheralConnected myPeripheral: mBleManager.listStretchSensePeripheralsConnected) {
            // change the text of the textView after a peripheral is found

            for (int count=0;count<10;count++){
                 //Update TextViews
                int id = hexToInt(myPeripheral.gatt.getDevice().getAddress().toString())+count;

                changeTheTextOf(id, (myPeripheral.value[count] + "pF"));
                UpdateGraph(myPeripheral.value[count],id);

            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mGraphMax.setText(mlineGraph.global_max+"pf");
                    mGraphMin.setText(mlineGraph.global_min+"pf");

                }
            });

            //           return;
        }

    }

    public static int hexToInt(String hex) {
        String removeBits;

        removeBits = hex.substring(0,2)+hex.substring(3,5)+hex.substring(6,8)+hex.substring(9,10);

        return Integer.parseInt(removeBits, 16);
    }

    //Start stop logging data
    public void onLogData(){

        LogData = !LogData;
        if (LogData){
            if (!DataLoggingStarted) {
                Toast.makeText(this, "Data Logging Started", Toast.LENGTH_SHORT).show();
            }
            else{

                //Ask
                Toast.makeText(this, "Data appending to file", Toast.LENGTH_SHORT).show();


            }
        }
        else{
            Toast.makeText(this,"Data Logging Stopped",Toast.LENGTH_SHORT).show();
        }
        DataLoggingStarted = true;

    }


    //Update filtering characteristic
    public void onFilteringClicked(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.filter_dialog, null);

        final SeekBar filteringSeekBar = (SeekBar) layout.findViewById(R.id.filtering_seekbar);
        final TextView filteringTextView = (TextView) layout.findViewById(R.id.filtering_text);

        filteringSeekBar.setMax(255);
        filteringSeekBar.setProgress(filter_current);
        filteringTextView.setText(filter_current+" pt");

        filteringSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                filteringTextView.setText(progress+" pt");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        builder.setTitle("Filtering")
                .setView(layout).setCancelable(true);



        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Update filter
                filter_current = filteringSeekBar.getProgress();
                mBleManager.writeCharacteristicFilter(filter_current);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Close dialog
            }
        });

        final AlertDialog alert = builder.create();
        alert.show();

    }

    File logFile = null;

    // -- Log File Create -- //
    private void setFileWriter(){
        try {
            boolean notDone =true;
            File logDir;
            int fileCounter = 0;
            while (notDone) {
                logDir = new File(Environment.getExternalStorageDirectory().getPath() + "/StretchSense 10 Channel BLE Data/");

                if (!logDir.exists()){
                    logDir.mkdir();
                }
                logFile = new File(logDir,"StretchSense log "+fileCounter+".csv");
                MediaScannerConnection.scanFile(this, new String[] {logFile.toString()}, null, null);
                if (logFile.exists()){
                    fileCounter++;
                }
                else{
                    notDone = false;
                }
            }
            outputFile = new BufferedWriter(new FileWriter(logFile));

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "No External Storage Detected, Please insert SD card to Record Data",Toast.LENGTH_LONG).show();
        }
    }


    private void UpdateGraph(final float value, final int id){


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mlineGraph.addPoint(value, id);
            }
        });
    }

    private void createNewTextValue(int id, final int ColorId){

            TextView textValue = new TextView(MainActivity.this);
            textValue.setText(0+".0pF");
            textValue.setGravity(Gravity.CENTER);
            textValue.setHeight(150);
            textValue.setId(id);
            textValue.setBackgroundColor(getBaseContext().getResources().getColor(mlineGraph.raw_colors[ColorId]));

        //Set onclick listener for each button
            textValue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(mlineGraph.toggleChannel(view.getId()))
                    {
                        view.setBackgroundColor(getBaseContext().getResources().getColor(mlineGraph.raw_colors[ColorId]));
                    }
                    else{
                        view.setBackgroundColor(Color.WHITE);
                    }
                    }
            });

            valuesParentView.addView(textValue);

        }
}
