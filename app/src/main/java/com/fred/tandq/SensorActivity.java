package com.fred.tandq;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.HashMap;

import static com.fred.tandq.nodeController.getsHandler;
import static com.fred.tandq.nodeController.isDisplayData;
import static com.fred.tandq.nodeController.isSendUDP;
import static com.fred.tandq.nodeController.setDisplayData;
import static com.fred.tandq.nodeController.setSendUDP;
import static com.fred.tandq.nodeController.setsHandler;

public class SensorActivity extends AppCompatActivity {
    //tag for logging
    private static final String TAG = SensorActivity.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private static boolean mLogging = false;

    public static final String TOGGLE_SEND = "toggle_display";
    public static final String TOGGLE_DISPLAY = "toggle_send";
    private ToggleButton displayData;
    private ToggleButton udpSend;
    private Menu appBarMenu;
    private TextView tDisp;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case SensorService.ACTION_USB_READY:
                    nodeController.getNodeCtrl().setAppBarTitle(getString(R.string.USB));
                    appBarMenu.getItem(0).setTitle(nodeController.getNodeCtrl().getAppBarTitle());         // Change indicator at top right
                    for (Integer item : nodeController.getNodeCtrl().getSensors().keySet()) {
                        setTVC(nodeController.getNodeCtrl().getSensors().get(item));
                    }
                    break;
                case SensorService.ACTION_USB_DISCONNECTED:
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    nodeController.getNodeCtrl().setAppBarTitle(getString(R.string.No_USB));
                    appBarMenu.getItem(0).setTitle(nodeController.getNodeCtrl().getAppBarTitle());         // Change indicator at top right
                    break;
                case SensorService.ACTION_USB_NOT_SUPPORTED:
                    Toast nsToast = Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT);
                    nsToast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 0);
                    nsToast.show();
                    break;
                case SensorService.ACTION_CDC_DRIVER_NOT_WORKING:
                    Toast cdcToast = Toast.makeText(context, "CDC Driver not working", Toast.LENGTH_SHORT);
                    cdcToast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 0);
                    cdcToast.show();
                    break;
                case SensorService.ACTION_USB_DEVICE_NOT_WORKING:
                    Toast nwToast = Toast.makeText(context, "USB device not working", Toast.LENGTH_SHORT);
                    nwToast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 0);
                    nwToast.show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (mLogging){
            String logstring = "Sensor Activity onCreate";
            Log.d(TAG, logstring);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        ActionBar ab = getSupportActionBar();
        ab.setLogo(R.mipmap.sn_launcher);
        ab.setDisplayUseLogoEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        if (getsHandler() == null)
        {
            setsHandler(new sensorHandler());
        }

        tDisp = findViewById(R.id.time);
        for (Integer item : nodeController.getNodeCtrl().getSensors().keySet()) {
                setTVC(nodeController.getNodeCtrl().getSensors().get(item));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.a_bar_menu_sa, menu);                  // Inflate the menu
        this.appBarMenu = menu;
        menu.getItem(0).setTitle(nodeController.getNodeCtrl().getAppBarTitle());;
        return true;
    }

    @Override
    public void onStart() {
        if (mLogging){
            String logstring = "Sensor Activity onStart";
            Log.d(TAG, logstring);
        }
        super.onStart();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        if (mLogging){
            String logstring = "Sensor Activity onResume";
            Log.d(TAG, logstring);
        }
        super.onResume();                                                                       // Start listening for notifications from UsbService
        setFilters(mUsbReceiver,this);
        ((ToggleButton) findViewById(R.id.SendData)).setChecked(isSendUDP());
    }

    @Override
    protected void onPause() {
        if (mLogging){
            String logstring = "Sensor Activity onPause";
            Log.d(TAG, logstring);
        }        super.onPause();
        unregisterReceiver(mUsbReceiver);
        if (isDisplayData()){
            setDisplayData(false);
            ((ToggleButton) findViewById(R.id.DisplayData)).setChecked(false);
            this.sendBroadcast(new Intent(TOGGLE_DISPLAY));
        }
    }

    public void upDateDisplay(View view) {
        boolean checked = ((ToggleButton)view).isChecked();
        Intent dispIntent = new Intent(TOGGLE_DISPLAY);
        this.sendBroadcast(dispIntent);
        if (checked) {
            setDisplayData(true);
        } else {
            setDisplayData(false);
        }
    }

    public void sendXML(View view) {
        boolean checked = ((ToggleButton)view).isChecked();
        this.sendBroadcast(new Intent(TOGGLE_SEND));
        if (checked) {
            setSendUDP(true);
        } else {
            setSendUDP(false);
        }
    }

    public class sensorHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            //:Param msg.obj    HashMap of CORRECTLY FORMATTED formatted strings
            //:Param msg.what   Type of sensor
            HashMap<String, String> sData = (HashMap<String, String>) msg.obj;
            int sensor = msg.what;
            for (String key : sData.keySet()) {
                if (!key.equals("Timestamp")){
                    TextView tv = nodeController.getNodeCtrl().getSensors().get(sensor).getTextView(key);
                    if (tv != null){
                        tv.setText(sData.get(key));
                    }
                }
            }
            tDisp.setText(sData.get("Timestamp"));
        }
    }

    private void setFilters(BroadcastReceiver usbReciever, Context context) {                     //USB Filter configuration and receiver registration
        IntentFilter filter = new IntentFilter();
        filter.addAction(SensorService.ACTION_USB_READY);
        filter.addAction(SensorService.ACTION_USB_DISCONNECTED);
        filter.addAction(SensorService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(SensorService.ACTION_CDC_DRIVER_NOT_WORKING);
        filter.addAction(SensorService.ACTION_USB_DEVICE_NOT_WORKING);
        context.registerReceiver(usbReciever, filter);
    }

    public void setTVC(mySensor sensor){
        for (String tvID : sensor.getFieldIdx().keySet()) {
            int tvi = getResources().getIdentifier(tvID, "id", getPackageName());
            sensor.setTextField(tvID, (TextView) findViewById(tvi));
        }
    }
}
