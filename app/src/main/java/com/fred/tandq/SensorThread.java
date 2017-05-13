package com.fred.tandq;
/*
 * Created by Fred Stein on 14/04/2017.
 * Creates a thread to read a sensorType of TYPE_? and return binned data
 * This iteration averages data received during the bin length (Default: 50 ms)
 * SensorThread explicitly sets the sensor listener hint (from appState) rather than using an android constant (Default: 20000 microseconds-unit of hint)
 * bin and listenHint are available for future use as parameters to the SensorThread constructor
 * :param  int sensorType:      any android sensor
   :param  Context mContext:    the context starting the thread
   :param  Queue uQueue:        the queue holding each binned sensor reading (taken off async and built to XML)
 */

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static android.content.Context.SENSOR_SERVICE;
import static com.fred.tandq.SensorActivity.getsHandler;
import static com.fred.tandq.SensorService.getsDataQ;
import static com.fred.tandq.appState.getEpoch;
import static com.fred.tandq.appState.getSensorName;
import static com.fred.tandq.appState.halfTick;
import static com.fred.tandq.appState.listenHint;
import static com.fred.tandq.appState.sendUDP;
import static com.fred.tandq.appState.tickLength;
import static com.fred.tandq.appState.varNames;


class SensorThread implements Runnable {
    //tag for logging
    private static final String TAG = SensorThread.class.getSimpleName()+"SF 2.0";
    //flag for logging
    private static final boolean mLogging = false;

    private int sensorType;
    private SensorActivity.sensorHandler sHandler;
    private dataQWriter dataQW;
    private SensorManager sM;
    private Sensor sensor;
    private SensorEventListener mListener;
    private long mEpoch;
    private int counts = 1;
    private float[] acc;
    private LinkedBlockingQueue sDataQ;

    SensorThread( final int sensorType, Context mContext ) {
        this.sensorType = sensorType;
        Log.d(TAG, appState.getSensorName(sensorType)+", " + Integer.toString(sensorType));
        sHandler = getsHandler();
        this.sM = (SensorManager) mContext.getSystemService(SENSOR_SERVICE);
        this.sensor = sM.getDefaultSensor(sensorType);
        sDataQ = getsDataQ();
        mEpoch = getEpoch();

        dataQW = new dataQWriter(sDataQ);
        this.mListener = new SensorEventListener() {
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
            String logString = getSensorName(sensorType)+" Thread Started";
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
        sHandler.obtainMessage(sensorType,displayFormat(avData, ts)).sendToTarget();
        if (sendUDP){
            try {
                dataQW.queue.put(udpFormat(avData,ts));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String[] displayFormat(float sData[], long timestamp){
        int nValues = sData.length;
        String[] dispPkt = new String[nValues+1];
        for (int i = 0; i < nValues; i++){
            dispPkt[i] = String.format ("%.3f", sData[i]);
        }
        String time = String.format ("%d", timestamp);
        dispPkt[nValues] = time.substring(time.length() - 5, time.length());        //right most 5 ms of timestamp
        return dispPkt;
    }

    private HashMap<String, String> udpFormat(float sData[], long timestamp){
        int nValues = sData.length;
        HashMap<String,String> udpPkt = new HashMap<>();
        for (int i = 0; i < nValues; i++){
            udpPkt.put(varNames[i],Float.toString(sData[i]));
        }
        udpPkt.put("Timestamp", Long.toString(timestamp));
        udpPkt.put("Sensor",appState.getSensorName(sensorType));
        return udpPkt;
    }

    public class dataQWriter implements Runnable {
        private LinkedBlockingQueue queue;

        public dataQWriter(LinkedBlockingQueue q) {
            this.queue = q;
        }

        public boolean isRunning(){
            return true;
        }

        @Override
        public void run() {                                                             //TODO Need thread stop condition
            if (mLogging) {
                String logString = " dataQWriter started";
                Log.v(TAG, logString);
            }
        }
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

