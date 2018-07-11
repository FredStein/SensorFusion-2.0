package com.fred.tandq;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import static com.fred.tandq.R.string.No_USB;
import static com.fred.tandq.nodeController.ACTION_CDC_DRIVER_NOT_WORKING;
import static com.fred.tandq.nodeController.ACTION_SENSORSERVICE_INITIALISED;
import static com.fred.tandq.nodeController.ACTION_USB_DETACHED;
import static com.fred.tandq.nodeController.ACTION_USB_DEVICE_NOT_WORKING;
import static com.fred.tandq.nodeController.ACTION_USB_NOT_SUPPORTED;
import static com.fred.tandq.nodeController.ACTION_USB_READY;

public class MainActivity extends AppCompatActivity {
    //tag for logging
    private static final String TAG = MainActivity.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private boolean mLogging = false;

    private nodeController nC;
    private Menu appBarMenu;
    private TextView present;
    private TextView absent;
    private String appBarTitle;

    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_USB_READY:
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    appBarTitle = getString(R.string.USB);
                    appBarMenu.getItem(0).setTitle(appBarTitle);         // Change indicator at top right
                    break;
                case ACTION_USB_DETACHED:
                    Toast.makeText(context, "USB Disconnected", Toast.LENGTH_SHORT).show();
                    appBarTitle = getString(R.string.No_USB);
                    appBarMenu.getItem(0).setTitle(appBarTitle);         // Change indicator at top right
                    break;
                case ACTION_USB_NOT_SUPPORTED:
                    Toast nsToast = Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT);
                    nsToast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 0);
                    nsToast.show();
                    break;
                case ACTION_CDC_DRIVER_NOT_WORKING:
                    Toast cdcToast = Toast.makeText(context, "CDC Driver not working", Toast.LENGTH_SHORT);
                    cdcToast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 0);
                    cdcToast.show();
                    break;
                case ACTION_USB_DEVICE_NOT_WORKING:
                    Toast nwToast = Toast.makeText(context, "USB device not working", Toast.LENGTH_SHORT);
                    nwToast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 0);
                    nwToast.show();
                    break;
    /*
                case SensorService.ACTION_SENSORSERVICE_INITIALISED:
                    SensorService.setNC(nC);
                    break;
    */
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nC = (nodeController) getApplicationContext();
        setContentView(R.layout.activity_main);
        if (mLogging){
            String logstring = "Main Activity Created";
            Log.d(TAG, logstring);
        }
        appBarTitle = getString(No_USB);
        ActionBar ab = getSupportActionBar();
        ab.setLogo(R.mipmap.sn_launcher);
        ab.setDisplayUseLogoEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(R.id.SettingsFrag, new SettingsFragment()).commit();

        present = findViewById(R.id.sp);
        absent = findViewById(R.id.sa);
        String[] sensorStatus = nC.getSensorStatus();
        present.setText(sensorStatus[0]);
        absent.setText(sensorStatus[1]);

        //What internal sensors have we got?
        if(!nC.wasStatusDisplayed()) {
            switch (nC.getSensorPresence()) {
                case 0:
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.noSensors), Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.absent), Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.present), Toast.LENGTH_LONG).show();
                    break;
            }
            nC.setStatusDisplayed(true);
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    public void onResume(){
        super.onResume();
        setFilters(mUsbReceiver,this);
        if (appBarMenu != null){
            appBarMenu.getItem(0).setTitle(appBarTitle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.a_bar_menu, menu);             // Inflate the menu; this adds items to the action bar
        appBarMenu = menu;
        menu.getItem(0).setTitle(appBarTitle);
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

    private void setFilters(BroadcastReceiver usbReciever, Context context) {                     //USB Filter configuration and reciever registration
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_READY);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_NOT_SUPPORTED);
        filter.addAction(ACTION_CDC_DRIVER_NOT_WORKING);
        filter.addAction(ACTION_USB_DEVICE_NOT_WORKING);
        filter.addAction(ACTION_SENSORSERVICE_INITIALISED);
        context.registerReceiver(usbReciever, filter);
    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(mUsbReceiver);
    }
}
