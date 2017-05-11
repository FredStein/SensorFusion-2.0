package com.fred.tandq;

import android.app.Service;
import android.content.Intent;

import android.os.IBinder;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import static com.fred.tandq.appState.TYPE_USB;


public class SensorService extends Service {
    //tag for logging
    private static final String TAG = SensorService.class.getSimpleName();
    //flag for logging
    private static boolean mLogging = false;

    private Set<Integer> sensors;
    private static LinkedBlockingQueue[] sDataQ;
    public static HashMap<Integer, LinkedBlockingQueue> udpDataQ = new HashMap<>();
    private static SensorThread[] sensorThreads;
    private static XMLAggregator XMLC;

    @Override
    public void onCreate() {
        sensors = appState.getSensors();
        sDataQ = new LinkedBlockingQueue[sensors.size()];
        sensorThreads = new SensorThread[sensors.size()];
        int i = 0;
        for (Integer item : sensors){
            if (item != TYPE_USB) {                                                         //TODO: May be possible to start USB thread here but will need to check connectivity first
                sDataQ[i] = new LinkedBlockingQueue();
                udpDataQ.put(item, sDataQ[i]);
                sensorThreads[i] = new SensorThread(sDataQ[i], item, this);
                i += 1;
            }
        }
        XMLC = new XMLAggregator(udpDataQ);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        for (SensorThread item : sensorThreads) {
            new Thread(item).start();
        }
        new Thread(XMLC).start();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}