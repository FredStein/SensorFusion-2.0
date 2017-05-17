package com.fred.tandq;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.HashMap;

import static com.fred.tandq.appState.displayOn;
import static com.fred.tandq.appState.getUSB;
import static com.fred.tandq.appState.sendUDP;
import static com.fred.tandq.appState.setUSB;

public class SensorActivity extends AppCompatActivity {
    //tag for logging
    private static final String TAG = SensorActivity.class.getSimpleName()+"SF 2.0";
    //flag for logging
    private static boolean mLogging = true;

    private ToggleButton displayData;
    private ToggleButton udpSend;
    private static TextView tDisp;
    private static sensorHandler sHandler;
    public static sensorHandler getsHandler() {
        return sHandler;
    }
    private static HashMap<Integer, mySensor> liveSensors = appState.getSensors();

    /*  This activity only responds to connected / disconnected / not supported signals
    */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case com.fred.tandq.usbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    if (!getUSB()){
                        setUSB(true);
                        //Add USB to list of available sensors
                        //Start usb related threads
                    }
                    break;
                case com.fred.tandq.usbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    if (!getUSB()){
                        // Remove from list of available sensors
                        // Stop usb related threads
                    }
                    break;
                case com.fred.tandq.usbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (mLogging){
            String logstring = "Sensor Activity Created";
            Log.d(TAG, logstring);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        getSupportActionBar().setDisplayUseLogoEnabled(true);
        sHandler = new sensorHandler();
        displayData = (ToggleButton) findViewById(R.id.DisplayData);
        udpSend = (ToggleButton) findViewById(R.id.SendData);
        tDisp = (TextView) findViewById(R.id.time);
        for (Integer item : liveSensors.keySet()) {
                setTVC(liveSensors.get(item));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.a_bar_menu_sa, menu);                  // Inflate the menu; this adds items to the action bar
        return true;
    }                                                                                               //TODO Sort out ActionBar contents. Back arrow + USB indicator

    @Override
    public void onStart() {
        if (mLogging){
            String logstring = "Sensor Activity Started";
            Log.d(TAG, logstring);
        }
        super.onStart();
        setFilters(mUsbReceiver,this);  // Start listening for notifications from UsbService
        startService(new Intent(this, SensorService.class));
    }

    @Override
    protected void onResume() {
        if (mLogging){
            String logstring = "Sensor Activity onResume";
            Log.d(TAG, logstring);
        }
        super.onResume();                               //Included for completness. Reciever runs in background
    }                                                                                               //TODO Do not reinitialise Display / Create new sensorHandler

    @Override
    protected void onPause() {
        if (mLogging){
            String logstring = "Sensor Activity onPause";
            Log.d(TAG, logstring);
        }        super.onPause();                                //Included for completness. Reciever runs in background
//        unregisterReceiver(mUsbReceiver);
//        unbindService(usbConnection);
    }

    public void upDateDisplay(View view) {
        boolean checked = ((ToggleButton)view).isChecked();
        if (checked) {
            displayOn = true;
        } else {
            displayOn = false;
        }
    }

    public void sendXML(View view) {
        boolean checked = ((ToggleButton)view).isChecked();
        if (checked) {
            sendUDP = true;
        } else {
            sendUDP = false;
        }
    }

    public static class sensorHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            //:Param msg.obj element expected to be array of CORRECTLY FORMATTED formatted strings
            //:Param msg.what is type of sensor
            HashMap<String, String> sData = (HashMap<String, String>) msg.obj;
            int sensor = msg.what;
            if (displayOn) {
                for (String key : sData.keySet()) {
                    if (!key.equals("Timestamp")){
                        liveSensors.get(sensor).getTextView(key).setText(sData.get(key));
                    }
                }
                tDisp.setText(sData.get("Timestamp"));
            }
        }
    }

    public static void setFilters(BroadcastReceiver usbReciever, Context context) {                     //USB Filter configuration
        IntentFilter filter = new IntentFilter();
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_DISCONNECTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_NOT_SUPPORTED);
        context.registerReceiver(usbReciever, filter);
    }

    private void setTVC(mySensor sensor){
        HashMap<String,String> tvCodes = sensor.getFieldIdx();
        for (String tvID : tvCodes.keySet()) {
            int tvi = getResources().getIdentifier(tvID, "id", getPackageName());
            Log.d(TAG, tvID);
            Log.d(TAG, Integer.toString(tvi));
            sensor.setTextField(tvID, (TextView) findViewById(tvi));
        }
    }
}
