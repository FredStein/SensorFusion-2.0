package com.fred.tandq;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.XmlResourceParser;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

import static com.fred.tandq.usbService.SERVICE_CONNECTED;

public class MainActivity extends AppCompatActivity {
    //tag for logging
    private static final String TAG = MainActivity.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private boolean mLogging = true;

    private usbService USBService;                                              //Initialise as null or is null default?
    private Menu appBarMenu;
    private appState state;
    private TextView present;
    private TextView absent;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case com.fred.tandq.usbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    appBarMenu.getItem(0).setTitle(getResources().getString(R.string.USB));         // Change indicator at top right
                    break;
                case com.fred.tandq.usbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    appBarMenu.getItem(0).setTitle(getResources().getString(R.string.No_USB));         // Change indicator at top right
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (mLogging){
            String logstring = "Main Activity Created";
            Log.d(TAG, logstring);
        }
        ActionBar ab = getSupportActionBar();
        ab.setLogo(R.mipmap.sn_launcher);
        ab.setDisplayUseLogoEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(R.id.SettingsFrag, new SettingsFragment()).commit();

        present = (TextView) findViewById(R.id.sp);
        absent = (TextView) findViewById(R.id.sa);

        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        XmlResourceParser rp = getResources().getXml(R.xml.i_sensors);
        state = new appState(this);

        String[] sensorStatus = state.sensorStatus(sm,rp);  //TODO: Does sensorDisplayStatus test hold up?

        present.setText(sensorStatus[0]);
        absent.setText(sensorStatus[1]);
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    public void onResume(){
        super.onResume();
        setFilters(mUsbReceiver,this);
        startService(usbService.class, usbConnection, null);            // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.a_bar_menu, menu);             // Inflate the menu; this adds items to the action bar
        this.appBarMenu = menu;
        return true;
    }

    public void goDisplay(View view) {
        Intent intent = new Intent(this, SensorActivity.class);
        startActivity(intent);
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    private void setFilters(BroadcastReceiver usbReciever, Context context) {                     //USB Filter configuration and reciever registration
        IntentFilter filter = new IntentFilter();
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_DISCONNECTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_NOT_SUPPORTED);
        context.registerReceiver(usbReciever, filter);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        Log.d(TAG, Boolean.toString(SERVICE_CONNECTED));
        if (!SERVICE_CONNECTED) {
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
        Log.d(TAG, Boolean.toString(SERVICE_CONNECTED));
    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }
}
