package com.fred.tandq;
/*
 * Created by Fred Stein on 14/04/2017.
 * Creates a thread to read a sensor of TYPE_USB and return binned data
 * This iteration averages data received during the bin length (Default: 500 ms)
 * usbRunnable explicitly sets the sensor listener hint (from appState) rather than using an android constant (Default: 20000 microseconds-unit of hint)
 * bin and listenHint are available for future use as parameters to the USBThread constructor
 * Display and udp data queue handles are obtained directly from appState
 * :param  mySensor sensor:     Configuration data for this sensor
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static android.os.SystemClock.elapsedRealtime;


class usbRunnable implements Runnable {
    //tag for logging
    private static final String TAG = usbRunnable.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private static boolean mLogging = true ;

    private SensorActivity.sensorHandler sHandler;
    private int sensorType;
    private mySensor fSensor;
    private udpQWriter dataQW;
    private long mEpoch;
    private int counts = 1;
    private float acc;
    private String usbStr = "";
    private long tickLength;
    private long halfTick;
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

    usbRunnable(mySensor mSensor, LinkedBlockingQueue dataQ, appState aState){
        fSensor = mSensor;
        sensorType = mSensor.getType();
        mEpoch = aState.getEpoch();
        tickLength = aState.getTick();
        halfTick = aState.getHalfTick();
        sHandler = aState.getsHandler();
        dataQW = new udpQWriter(dataQ);
        setFilters(txListener,aState.getContext());
    }
    /*
     *  Data received from serial port will be received here. Just populate onReceivedData with your code
     *  In this particular example. byte stream is converted to String and send to UI thread to
     *  be treated there.
     */
    public UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
        try {
            String data = new String(arg0, "UTF-8");
            if (!data.contains("%")){
                usbStr = usbStr + data;
            } else{
                usbStr = usbStr + data;
                String val = usbStr.split("m")[0].trim();
                if(mLogging) {
                    Log.d(TAG, val);
                }
                long ts = elapsedRealtime();
                if (ts >= mEpoch){
                    if ( ts < mEpoch + tickLength) {
                        acc += Float.parseFloat(val);
                        counts += 1;
                    } else {
                        long binTs = mEpoch + halfTick;
                        publishEpoch(acc, counts, binTs);                           //Send total values, counts, timestamp (for middle of bin) to publisher
                        acc = Float.parseFloat(val);
                        counts = 1;
                        mEpoch = mEpoch + tickLength;
                    }
                }
                usbStr = "";
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        }
    };

    @Override
    public void run() {
        if (mLogging) {
            String logString = "USB Thread Started";
            Log.d(TAG, logString);
        }
        new Thread(dataQW).start();
    }

    private void publishEpoch(float sData, int counts, final long ts) {
        float avData = sData / counts;
        if (sHandler != null && upDateData)
            sHandler.obtainMessage(fSensor.getType(),displayFormat(avData, ts)).sendToTarget();
        if (sendData){
            try {
                dataQW.queue.put(udpFormat(avData,ts));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private HashMap<String,String> displayFormat(float sData, long timestamp){
        HashMap<String,String> dPkt = new HashMap<>();
        dPkt.put("d",String.format ("%.3f", sData));                                //TODO: generalise with use of mySensor class
        String time = String.format ("%d", timestamp);
        dPkt.put("Timestamp", time.substring(time.length() - 5, time.length()));    //right most 5 ms of timestamp
        return dPkt;
    }

    private HashMap<String, String> udpFormat(float sData, long timestamp){
        HashMap<String,String> udpPkt = new HashMap<>();
        udpPkt.put("d",Float.toString(sData));
        udpPkt.put("Timestamp", Long.toString(timestamp));
        udpPkt.put("Sensor", fSensor.getName());
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
                String logString = " USBqWriter up";
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
