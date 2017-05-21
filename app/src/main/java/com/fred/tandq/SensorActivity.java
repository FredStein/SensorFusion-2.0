package com.fred.tandq;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.HashMap;

public class SensorActivity extends AppCompatActivity {
    //tag for logging
    private static final String TAG = SensorActivity.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private static boolean mLogging = true;

    public static final String TOGGLE_SEND = "toggle_display";
    public static final String TOGGLE_DISPLAY = "toggle_send";
    private usbService USBService;                                              //Initialise as null or is null default?
    private appState state;
    private ToggleButton displayData;
    private ToggleButton udpSend;
    private Menu appBarMenu;
    private TextView tDisp;
    private sensorHandler sHandler;
    private HashMap<Integer, mySensor> activeSensors;

    /*  This activity only responds to connected / disconnected / not supported signals
    */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case com.fred.tandq.usbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    appBarMenu.getItem(0).setTitle(getResources().getString(R.string.USB));
                    break;
                case com.fred.tandq.usbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    appBarMenu.getItem(0).setTitle(getResources().getString(R.string.No_USB));
                    break;
                case com.fred.tandq.usbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            USBService = ((com.fred.tandq.usbService.UsbBinder) arg1).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            USBService = null;
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

        ActionBar ab = getSupportActionBar();
        ab.setLogo(R.mipmap.sn_launcher);
        ab.setDisplayUseLogoEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        sHandler = new sensorHandler();
        state = new appState(this);
        state.initNode();
        state.setsHandler(sHandler);
        tDisp = (TextView) findViewById(R.id.time);
        activeSensors = state.getSensors();
        for (Integer item : activeSensors.keySet()) {
                setTVC(activeSensors.get(item));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.a_bar_menu_sa, menu);                  // Inflate the menu; this adds items to the action bar
        this.appBarMenu = menu;
        return true;
    }                                                                                               //TODO Sort out ActionBar contents. Back arrow + USB indicator

    @Override
    public void onStart() {
        if (mLogging){
            String logstring = "Sensor Activity Started";
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
        startService(new Intent(this, SensorService.class));
        ((ToggleButton) findViewById(R.id.SendData)).setChecked(state.isSendUDP());
    }

    @Override
    protected void onPause() {
        if (mLogging){
            String logstring = "Sensor Activity onPause";
            Log.d(TAG, logstring);
        }        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
        if (state.isDisplayData()){
            state.setDisplayData(false);
            ((ToggleButton) findViewById(R.id.DisplayData)).setChecked(false);
            this.sendBroadcast(new Intent(TOGGLE_DISPLAY));
        }
    }

    public void upDateDisplay(View view) {
        boolean checked = ((ToggleButton)view).isChecked();
        this.sendBroadcast(new Intent(TOGGLE_DISPLAY));
        if (checked) {
            state.setDisplayData(true);
        } else {
            state.setDisplayData(false);
        }
    }

    public void sendXML(View view) {
        boolean checked = ((ToggleButton)view).isChecked();
        this.sendBroadcast(new Intent(TOGGLE_SEND));
        if (checked) {
            state.setSendUDP(true);
        } else {
            state.setSendUDP(false);
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
                    activeSensors.get(sensor).getTextView(key).setText(sData.get(key));
                }
            }
            tDisp.setText(sData.get("Timestamp"));
        }
    }

    public static void setFilters(BroadcastReceiver usbReciever, Context context) {                     //USB Filter configuration
        IntentFilter filter = new IntentFilter();
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_DISCONNECTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_NOT_SUPPORTED);
        context.registerReceiver(usbReciever, filter);
    }

/*    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!com.fred.tandq.usbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }*/

    private void setTVC(mySensor sensor){
        for (String tvID : sensor.getFieldIdx().keySet()) {
            int tvi = getResources().getIdentifier(tvID, "id", getPackageName());
            if (tvi == 0){
                Log.d(TAG, "Sensor Field index value not recognised");
            }
            Log.d(TAG, tvID);
            Log.d(TAG, Integer.toString(tvi));
            sensor.setTextField(tvID, (TextView) findViewById(tvi));
        }
    }
}
