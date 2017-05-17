package com.fred.tandq;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static com.fred.tandq.appState.getUSB;


public class SensorService extends Service {
    //tag for logging
    private static final String TAG = SensorService.class.getSimpleName()+"SF 2.0";
    //flag for logging
    private static boolean mLogging = true;

    private HashMap<Integer, mySensor> sensors;
    private static LinkedBlockingQueue sDataQ;
    public static LinkedBlockingQueue getsDataQ() {
        return sDataQ;
    }
    private static SensorThread[] sensorThreads;
    private static XMLAggregator XMLC;
    private static usbThread USBThread;

    @Override
    public void onCreate() {
        sensors = appState.getSensors();
        sDataQ = new LinkedBlockingQueue();
        sensorThreads = new SensorThread[sensors.size()];
        int i = 0;
        for (mySensor item : sensors.values()){
            if (!item.getName().equals("USB")) {
                sensorThreads[i] = new SensorThread(item, this);
                i += 1;
            }
        }
        if(getUSB()){
            USBThread = new usbThread(sensors.get(70000));
        }
        XMLC = new XMLAggregator();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        for (SensorThread item : sensorThreads) {
            new Thread(item).start();
        }
        if (getUSB()){
            new Thread(USBThread).start();
        }
        new Thread(XMLC).start();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}