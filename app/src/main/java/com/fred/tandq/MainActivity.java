package com.fred.tandq;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import java.util.Set;

import static com.fred.tandq.appState.setUSB;

public class MainActivity extends AppCompatActivity {
    //tag for logging
    private static final String TAG = MainActivity.class.getSimpleName()+"SF 2.0";
    //flag for logging
    private boolean mLogging = true;

    private usbService USBService;                                              //Initialise as null or is null default?

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case com.fred.tandq.usbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    setUSB(true);                                                                           //TODO: Change indicator at top right
                    break;
                case com.fred.tandq.usbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    //Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();     //Do nothing as permission is not sought
                    break;
                case com.fred.tandq.usbService.ACTION_NO_USB: // NO USB CONNECTED
                    //Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();               //Do nothing as this is default state & USB MUST signal presence
                    break;
                case com.fred.tandq.usbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED                 //TODO: Change indicator at top right
                    setUSB(false);
                    break;
                case com.fred.tandq.usbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast mToast = Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT);
                    mToast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 0);
                    mToast.show();
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

    private appState state = new appState();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (mLogging){
            String logstring = "Main Activity Created";
            Log.d(TAG, logstring);
        }
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(R.id.SettingsFrag, new SettingsFragment()).commit();
    }

    @Override
    protected void onStart(){
        super.onStart();
        setFilters(mUsbReceiver,this);  // Prepare to listen for notifications from UsbService
        startService(usbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it

        state.initNode(this, findViewById(android.R.id.content));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.a_bar_menu, menu);             // Inflate the menu; this adds items to the action bar
        return true;
    }

    public void goDisplay(View view) {
        startActivity(new Intent(this, SensorActivity.class));
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    public static void setFilters(BroadcastReceiver usbReciever, Context context) {                     //USB Filter configuration
        IntentFilter filter = new IntentFilter();
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_NO_USB);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_DISCONNECTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        context.registerReceiver(usbReciever, filter);
    }

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
}
