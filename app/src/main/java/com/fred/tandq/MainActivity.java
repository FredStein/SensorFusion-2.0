package com.fred.tandq;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    //tag for logging
    private static final String TAG = MainActivity.class.getSimpleName();
    //flag for logging
    private boolean mLogging = false;

    private appState state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(R.id.SettingsFrag, new SettingsFragment()).commit();

        this.state = new appState();

        switch (state.initNode(this, findViewById(android.R.id.content))) {
            case 0:
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.present), Toast.LENGTH_LONG).show();
                break;
            case 1:
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.absent), Toast.LENGTH_LONG).show();
                break;
            case 2:
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.noSensors), Toast.LENGTH_LONG).show();
                break;
        }
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
}
