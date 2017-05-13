package com.fred.tandq;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

import static com.fred.tandq.appState.displayOn;
import static com.fred.tandq.appState.getTextViewCodes;
import static com.fred.tandq.appState.initDisplay;
import static com.fred.tandq.appState.sendUDP;

public class SensorActivity extends AppCompatActivity {
    //tag for logging
    private static final String TAG = SensorActivity.class.getSimpleName()+"SF 2.0";
    //flag for logging
    private static boolean mLogging = true;

    private ToggleButton displayData;
    private ToggleButton udpSend;
    private static TextView tDisp;
    private static TextView usbDisp;
    private static sensorHandler sHandler;
    public static sensorHandler getsHandler() {
        return sHandler;
    }
    private static USBHandler usbHandler;
    public static USBHandler getuHandler(){
        return usbHandler;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case com.fred.tandq.usbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case com.fred.tandq.usbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case com.fred.tandq.usbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case com.fred.tandq.usbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case com.fred.tandq.usbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private usbService USBService;

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
        usbHandler = new USBHandler(this);

        displayData = (ToggleButton) findViewById(R.id.DisplayData);
        udpSend = (ToggleButton) findViewById(R.id.SendData);
        tDisp = (TextView) findViewById(R.id.time);
        usbDisp = (TextView) findViewById(R.id.USBr);
        initDisplay(this,this.findViewById(android.R.id.content));
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
        startService(new Intent(this, SensorService.class));
    }

    @Override
    protected void onResume() {
        if (mLogging){
            String logstring = "Sensor Activity onResume";
            Log.d(TAG, logstring);
        }
        super.onResume();                               //Included for completness. Reciever runs in background
        appState.setFilters(mUsbReceiver,this);  // Start listening notifications from UsbService
        startService(usbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it

    }                                                                                               //TODO Do not reinitialise Display / Create new sensorHandler

    @Override
    protected void onPause() {
        if (mLogging){
            String logstring = "Sensor Activity onPause";
            Log.d(TAG, logstring);
        }        super.onPause();                                //Included for completness. Reciever runs in background
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
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
            String[] sData = (String[]) msg.obj;
            int sensor = msg.what;
            List<TextView> tvc = getTextViewCodes(sensor);
            if (displayOn) {
                int i = 0;
                for (TextView item : tvc) {
                    item.setText(sData[i]);
                    i++;
                }
                tDisp.setText(sData[sData.length-1]);
            }
        }
    }

    private static class USBHandler extends Handler {                    //TODO: Merge handlers
        private final WeakReference<SensorActivity> sActivity;

        public USBHandler(SensorActivity activity) {
            sActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            //:Param msg.obj element expected to be array of CORRECTLY FORMATTED formatted strings
            //:Param msg.what is type of sensor
            String[] sData = (String[]) msg.obj;
            int sensor = msg.what;                                      //Unused. Indicates USB as source
            if (displayOn) {
                usbDisp.setText(sData[0]);
                tDisp.setText(sData[1]);                                //TODO: Does this add anything?  Issue goes away if handlers merged.
            }
        }
    }

    // Included for completeness.  Service should not stop.  May be able to use this to start service explicitly.
    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
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
    }

    // Included for completeness.  Service should not stop.  May be able to use this to start service explicitly.
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
}
