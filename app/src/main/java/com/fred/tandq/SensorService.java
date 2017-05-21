package com.fred.tandq;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;


public class SensorService extends Service {
    //tag for logging
    private static final String TAG = SensorService.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private static boolean mLogging = true;

    private appState state;
    private LinkedBlockingQueue sDataQ;
    private HashMap<Integer, mySensor> activeSensors;
    private HashMap<Integer, Thread> SensorThreads = new HashMap<>();
    private XMLAggregator XMLC;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case com.fred.tandq.usbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    if (!activeSensors.containsKey(70000)){                   //TODO: Define a constant for the USB Sensor Type - ideally get from eSensors.xml
                            state.setExtSensors();
                    } else{
                        Log.d(TAG, "Why? It should have been destroyed and removed");
                    }
                    if (!SensorThreads.containsKey(70000)){
                        SensorThreads.put(70000, new Thread(new usbRunnable(activeSensors.get(70000),sDataQ, state)));                 //Instantiate USB runnable
                    } else{
                        Log.d(TAG, "Why? It should have been destroyed and removed");
                    }
                    SensorThreads.get(70000).start();           //Start thread
                    break;
                case com.fred.tandq.usbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Log.d(TAG, "Need to trace thread activity");

                    //Stop usbWriter and usbRunnable.  Check for existance each step.
                    // TODO Destroy usbWriter and usbRunnable(?) - why? can sit idle.
                    state.removeSensor(70000);
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        if (mLogging){
            String logstring = "Sensor Service Created";
            Log.d(TAG, logstring);
        }
        SensorManager sMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        state = new appState(this);
        setFilters(mUsbReceiver,this);
        activeSensors = state.getSensors();
        sDataQ = new LinkedBlockingQueue();
        for (mySensor item : activeSensors.values()){
            if (!item.getName().equals("USB")) {
                SensorThreads.put(item.getType(), new Thread(new SensorRunnable(item, sMgr,sDataQ, state)));     //Instantiate thread
            }else if(item.getName().equals("USB")){
                SensorThreads.put(item.getType(), new Thread(new usbRunnable(item,sDataQ,state)));
            }
        }
        XMLC = new XMLAggregator(sDataQ, state);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLogging){
            String logstring = "Sensor Service Started";
            Log.d(TAG, logstring);
        }
        for (Integer item : SensorThreads.keySet()) {
            SensorThreads.get(item).start();           //Start thread
        }
        new Thread(XMLC).start();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static void setFilters(BroadcastReceiver usbReciever, Context context) {                     //USB Filter configuration
        IntentFilter filter = new IntentFilter();
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_DISCONNECTED);
        context.registerReceiver(usbReciever, filter);
    }
}