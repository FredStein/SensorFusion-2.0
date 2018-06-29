package com.fred.tandq;
/*
 * Created by Fred Stein on 14/04/2017.
 * Creates a thread to read a sensorType of TYPE_? and return binned data
 * This iteration averages data received during the bin length (Default: 500 ms)
 * SensorRunnable explicitly sets the sensor listener hint (from nodeController) rather than using an android constant (Default: 20000 microseconds-unit of hint)
 * bin and listenHint are available for future use as parameters to the SensorRunnable constructor
 * Display and udp data queue handles are obtained directly from nodeController
 * :param  mySensor sensor:     Configuration data for this sensor
   :param  Context mContext:    Context of the SensorManager
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.os.SystemClock.elapsedRealtime;


class SensorRunnable implements Runnable {
    //tag for logging
    private static final String TAG = SensorRunnable.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private static final boolean mLogging = false;

    private AtomicBoolean startThread = new AtomicBoolean(false);
    public boolean isRunning(){
        return startThread.get();
    }
    public void setRunning(boolean running) {
        this.startThread.set(running);
    }

    public boolean stopMe(){
        sM.unregisterListener(mListener, sensor);
//        dataQW.stop();
//        startThread.set(false);
        return startThread.get();
    }
    private SensorActivity.sensorHandler sHandler;
    private int sensorType;
    private mySensor fSensor;
//    private udpQWriter dataQW;
//    private Thread sensorDataTxThread;
    private long mEpoch;
    private int counts = 1;
    private float[] vAcc;
    private long dtAcc = 0;
    private long tickLength;
    private long halfTick;
    private int listenHint;
    private boolean upDateData = false;
    private boolean sendData = false;
    private SensorManager sM;
    private Sensor sensor;
    private SensorEventListener mListener;
    private LinkedBlockingQueue dQ;

    private final BroadcastReceiver txListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case SensorActivity.TOGGLE_DISPLAY:
                    upDateData = !upDateData;
                    if (sHandler == null) sHandler = nodeController.getsHandler();
                    break;
                case SensorActivity.TOGGLE_SEND:
                    sendData = !sendData;
                    break;
            }
        }
    };

    SensorRunnable(mySensor mSensor, SensorManager sMgr, LinkedBlockingQueue dataQ ) {
        fSensor = mSensor;
        sensorType = mSensor.getType();
        mEpoch = nodeController.getNodeCtrl().getEpoch();
        tickLength = nodeController.getNodeCtrl().getTick();
        halfTick = nodeController.getNodeCtrl().getHalfTick();
//        dataQW = new udpQWriter(dataQ);
        this.dQ = dataQ;
        setFilters(txListener,nodeController.getNodeCtrl());
        listenHint = nodeController.getNodeCtrl().getHint();
        sM = sMgr;
        sensor = sM.getDefaultSensor(sensorType);
        mListener = new sListener();
    }

    @Override
    public void run(){                                                                 //TODO Need thread stop condition
        if (mLogging){
            String logString = fSensor.getName()+" Thread Started";
            Log.d(TAG, logString);
        }
        sM.registerListener(mListener, sensor, listenHint);                             // listenHint set in nodeController. default = 50ms
        while(startThread.get()){
/*            if (!dataQW.isRunning()){
                    dataQW.setRunning(true);
                    sensorDataTxThread = new Thread(dataQW);
                    sensorDataTxThread.start();
            }*/
        }
    }

    private void publishEpoch(float[] sData, int counts, final long dT, final long ts) {
        int nValues = sData.length;
        final float[] avData = new float[nValues];
        for (int i = 0; i < nValues; i++){
            avData[i] = sData[i] / counts;
        }
        final long dTm = dT/ counts;

        if (sHandler != null){
            if(upDateData) sHandler.obtainMessage(sensorType,displayFormat(avData, ts)).sendToTarget();
        }
        if (sendData){
            try {
                dQ.put(udpFormat(avData,ts, dTm));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private HashMap<String,String> displayFormat(float sData[], long timestamp){
        int nValues = sData.length;
        HashMap<String,String> dispPkt = new HashMap<>();
        for (int i = 0; i < nValues; i++){
            dispPkt.put(fSensor.getDim()[i],String.format ("%.3f", sData[i]));
        }
        String time = String.format ("%d", timestamp);
        dispPkt.put("Timestamp",time.substring(time.length() - 5, time.length()));        //right most 5 ms of timestamp
        return dispPkt;
    }

    private HashMap<String, String> udpFormat(float sData[], long timestamp, long dTm){
        int nValues = sData.length;
        HashMap<String,String> udpPkt = new HashMap<>();
        fSensor.getDim();
        for (int i = 0; i < nValues; i++){
            udpPkt.put(fSensor.getDim()[i],String.format ("%.3f", sData[i]));
        }
        udpPkt.put("Timestamp", Long.toString(timestamp));
        udpPkt.put("dT",Long.toString(dTm));
        udpPkt.put("Sensor",fSensor.getName());
        return udpPkt;
    }

/*    public class udpQWriter implements Runnable {
        private AtomicBoolean running = new AtomicBoolean(false);
        public boolean isRunning(){
            return running.get();
        }
        public void setRunning(boolean running) {
            this.running.set(running);
        }
        public void stop(){
            running.set(false);
        }
        private LinkedBlockingQueue queue;
        udpQWriter(LinkedBlockingQueue q) {
            queue = q;
            if (mLogging) {
                String logString = fSensor.getName()+" udpQWriter created";
                Log.d(TAG, logString);
            }
        }

        @Override
        public void run() {
            if (mLogging) {
                String logString = fSensor.getName()+" udpQWriter started";
                Log.d(TAG, logString);
            }
            while (running.get()) {
            }
            running.set(false);
        }
    }*/

    private void setFilters(BroadcastReceiver mReciever, Context context) {                     //USB Filter configuration and reciever registration
        IntentFilter filter = new IntentFilter();
        filter.addAction(SensorActivity.TOGGLE_DISPLAY);
        filter.addAction(SensorActivity.TOGGLE_SEND);
        context.registerReceiver(mReciever, filter);
    }

    private class sListener implements SensorEventListener {

        public void onSensorChanged(SensorEvent event) {
            long tRx = elapsedRealtime();
            int nValues = event.values.length;
            if (vAcc == null) {
                vAcc = new float[nValues];
            }
            long ts = (event.timestamp-event.timestamp%10000000)/1000000;       //Get event Timestamp and convert to milliseconds

            if (ts >= mEpoch){
                if ( ts < mEpoch + tickLength) {
                    for (int i = 0; i < nValues; i++) {
                        vAcc[i] += event.values[i];
                    }
                    dtAcc = dtAcc + (tRx - ts);
                    counts += 1;
                } else {
                    long binTs = mEpoch + halfTick;
                    publishEpoch(vAcc, counts, dtAcc, binTs);                           //Send total values, counts, dT total, timestamp (for middle of bin) to publisher
                    for (int i = 0; i < nValues; i++) {                         //Reinitialise accumulator registers and reset counter
                        vAcc[i] = event.values[i];
                    }
                    dtAcc = (tRx - ts);
                    counts = 1;
                    mEpoch = mEpoch + tickLength;
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }
    }
}

