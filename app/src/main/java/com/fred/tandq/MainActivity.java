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

import static com.fred.tandq.nodeController.getSensorPresence;
import static com.fred.tandq.nodeController.getSensorStatus;
import static com.fred.tandq.nodeController.setStatusDisplayed;
import static com.fred.tandq.nodeController.wasStatusDisplayed;

public class MainActivity extends AppCompatActivity {
    //tag for logging
    private static final String TAG = MainActivity.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private boolean mLogging = true;

    private Menu appBarMenu;
    private TextView present;
    private TextView absent;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case SensorService.ACTION_USB_READY:
                nodeController.getNodeCtrl().setAppBarTitle(getString(R.string.USB));
                appBarMenu.getItem(0).setTitle(nodeController.getNodeCtrl().getAppBarTitle());         // Change indicator at top right
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

        String[] sensorStatus = getSensorStatus();
        present.setText(sensorStatus[0]);
        absent.setText(sensorStatus[1]);

        //What internal sensors have we got?
        if(!wasStatusDisplayed()) {
            switch (getSensorPresence()) {
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
            setStatusDisplayed(true);
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
            appBarMenu.getItem(0).setTitle(nodeController.getNodeCtrl().getAppBarTitle());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.a_bar_menu, menu);             // Inflate the menu; this adds items to the action bar
        this.appBarMenu = menu;
        menu.getItem(0).setTitle(nodeController.getNodeCtrl().getAppBarTitle());
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
        filter.addAction(SensorService.ACTION_USB_READY);
        filter.addAction(SensorService.ACTION_USB_DISCONNECTED);
        filter.addAction(SensorService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(SensorService.ACTION_CDC_DRIVER_NOT_WORKING);
        filter.addAction(SensorService.ACTION_USB_DEVICE_NOT_WORKING);
        context.registerReceiver(usbReciever, filter);
    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(mUsbReceiver);
    }
}
