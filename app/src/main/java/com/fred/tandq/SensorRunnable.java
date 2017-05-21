package com.fred.tandq;
/*
 * Created by Fred Stein on 14/04/2017.
 * Creates a thread to read a sensorType of TYPE_? and return binned data
 * This iteration averages data received during the bin length (Default: 500 ms)
 * SensorRunnable explicitly sets the sensor listener hint (from appState) rather than using an android constant (Default: 20000 microseconds-unit of hint)
 * bin and listenHint are available for future use as parameters to the SensorRunnable constructor
 * Display and udp data queue handles are obtained directly from appState
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


class SensorRunnable implements Runnable {
    //tag for logging
    private static final String TAG = SensorRunnable.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private static final boolean mLogging = true;

    private SensorActivity.sensorHandler sHandler;
    private int sensorType;
    private mySensor fSensor;
    private udpQWriter dataQW;
    private SensorManager sM;
    private Sensor sensor;
    private SensorEventListener mListener;
    private long mEpoch;
    private int counts = 1;
    private float[] acc;
    private long tickLength;
    private long halfTick;
    private int listenHint;
    private boolean upDateData = false;
    private boolean sendData = false;

    private final BroadcastReceiver txListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case SensorActivity.TOGGLE_DISPLAY:
                    upDateData = !upDateData;
                    break;
                case SensorActivity.TOGGLE_SEND: // USB DISCONNECTED
                    sendData = !sendData;
                    break;
            }
        }
    };

    SensorRunnable(mySensor mSensor, SensorManager sMgr, LinkedBlockingQueue dataQ, appState aState ) {
        fSensor = mSensor;
        sensorType = mSensor.getType();
        sM = sMgr;
        sensor = sM.getDefaultSensor(sensorType);
        mEpoch = aState.getEpoch();
        tickLength = aState.getTick();
        halfTick = aState.getHalfTick();
        listenHint = aState.getHint();
        sHandler = aState.getsHandler();
        dataQW = new udpQWriter(dataQ);
        setFilters(txListener,aState.getContext());

        mListener = new SensorEventListener() {        //TODO: Look at where declared wrt usbRunnable. In constructor vs as field
            @Override
            public void onSensorChanged(SensorEvent event) {
                int nValues = event.values.length;
                if (acc == null) {
                    acc = new float[nValues];
                }
                long ts = (event.timestamp-event.timestamp%10000000)/1000000;       //Get event Timestamp and convert to milliseconds

                if (ts >= mEpoch){
                    if ( ts < mEpoch + tickLength) {
                        for (int i = 0; i < nValues; i++) {
                            acc[i] += event.values[i];
                            }
                        counts += 1;
                    } else {
                        long binTs = mEpoch + halfTick;
                        publishEpoch(acc, counts, binTs);                           //Send total values, counts, timestamp (for middle of bin) to publisher
                        for (int i = 0; i < nValues; i++) {                         //Reinitialise accumulator registers and reset counter
                            acc[i] = event.values[i];
                        }
                        counts = 1;
                        mEpoch = mEpoch + tickLength;
                    }
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // do nothing
            }
        };
    }

    @Override
    public void run() {                                                                 //TODO Need thread stop condition
        if (mLogging) {
            String logString = fSensor.getName()+" Thread Started";
            Log.d(TAG, logString);
        }
        new Thread(dataQW).start();                                                         //TODO: Need thread stop condition             refer to publishEpoch -> one of these is not needed!
        sM.registerListener(mListener, sensor, listenHint);                             // listenHint set in appState. default = 50ms
    }

    private void publishEpoch(float[] sData, int counts, final long ts) {
        int nValues = sData.length;
        final float[] avData = new float[nValues];
        for (int i = 0; i < nValues; i++){
            avData[i] = sData[i] / counts;
        }
        if (sHandler != null && upDateData);
            sHandler.obtainMessage(sensorType,displayFormat(avData, ts)).sendToTarget();
        if (sendData){
            try {
                dataQW.queue.put(udpFormat(avData,ts));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private HashMap<String,String> displayFormat(float sData[], long timestamp){
        int nValues = sData.length;
        HashMap<String,String> dispPkt = new HashMap<>();
        fSensor.getDim();
        for (int i = 0; i < nValues; i++){
            dispPkt.put(fSensor.getDim()[i],String.format ("%.3f", sData[i]));
        }
        String time = String.format ("%d", timestamp);
        dispPkt.put("Timestamp",time.substring(time.length() - 5, time.length()));        //right most 5 ms of timestamp
        return dispPkt;
    }

    private HashMap<String, String> udpFormat(float sData[], long timestamp){
        int nValues = sData.length;
        HashMap<String,String> udpPkt = new HashMap<>();
        fSensor.getDim();
        for (int i = 0; i < nValues; i++){
            udpPkt.put(fSensor.getDim()[i],String.format ("%.3f", sData[i]));
        }
        udpPkt.put("Timestamp", Long.toString(timestamp));
        udpPkt.put("Sensor",fSensor.getName());
        return udpPkt;
    }

    public class udpQWriter implements Runnable {
        private LinkedBlockingQueue queue;

        public udpQWriter(LinkedBlockingQueue q) {
            this.queue = q;
        }

        public boolean isRunning(){
            return true;
        }

        @Override
        public void run() {                                                             //TODO Need thread stop condition
            if (mLogging) {
                String logString = " udpQWriter started";
                Log.d(TAG, logString);
            }
        }
    }
    private void setFilters(BroadcastReceiver mReciever, Context context) {                     //USB Filter configuration and reciever registration
        IntentFilter filter = new IntentFilter();
        filter.addAction(SensorActivity.TOGGLE_DISPLAY);
        filter.addAction(SensorActivity.TOGGLE_SEND);
        context.registerReceiver(mReciever, filter);
    }
}


//    public void cleanThread(){
//        //Unregister the listener
//        if(mSensorManager != null) {
//            mSensorManager.unregisterListener(mListener);
//        }
//
//        if(mHandlerThread.isAlive())
//            mHandlerThread.quitSafely();
//    }

