package com.fred.tandq;
/*
 * Created by Fred Stein on 14/04/2017.
 * Creates a thread to read a sensor of TYPE_USB and return binned data
 * This iteration averages data received during the bin length (Default: 500 ms)
 * usbRunnable explicitly sets the sensor listener hint (from nodeControlle) rather than using an android constant (Default: 20000 microseconds-unit of hint)
 * bin and listenHint are available for future use as parameters to the USBThread constructor
 * Display and udp data queue handles are obtained directly fromnodeController
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
    private static boolean mLogging = false ;

    private  boolean runThread = false;
    public boolean isRunning(){
        return runThread;
    }
    public void setRunning() {
        runThread = true;
    }
    public void stopRunning(){
        runThread = false;
    }
    private SensorActivity.sensorHandler sHandler;
    private int sensorType;
    private mySensor fSensor;
    private LinkedBlockingQueue dQ;
    private long epoch;
    private int counts = 1;
    private float acc;
    private long tickLength;
    private long halfTick;
    private boolean upDateData = false;
    private boolean sendData = false;
    private String usbStr;
    private usbListener mListener;
    private nodeController nC;

    private final BroadcastReceiver txListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case SensorActivity.TOGGLE_DISPLAY:
                    upDateData = !upDateData;
                    if (sHandler == null) sHandler = nC.getsHandler();
                    break;
                case SensorActivity.TOGGLE_SEND: // USB DISCONNECTED
                    sendData = !sendData;
//                    Log.d(TAG, "USB Runnable kill Message");
//                    Log.d(TAG, "StopThread = " + Boolean.toString(startThread.get()));
                    break;
            }
        }
    };

    usbRunnable(mySensor mSensor, LinkedBlockingQueue dataQ, long mEpoch, long tikLen, long halfTik, nodeController nC){
        if (mLogging) {
            String logString = "USB Thread created";
            Log.d(TAG, logString);
        }
        fSensor = mSensor;
        sensorType = mSensor.getType();
        epoch = mEpoch;
        tickLength = tikLen;
        halfTick = halfTik;
//        dataQW = new udpQWriter(dataQ);
        this.dQ = dataQ;
        setFilters(txListener,nC);
        usbStr = "";
        mListener = new usbListener();
        nC.setusbCallback(mListener);
    }

    @Override
    public void run() {
        if (mLogging) {
            String logString = "USB Thread Started";
            Log.d(TAG, logString);
        }

        while(runThread){

        }
    }

    private void publishEpoch(float sData, int counts, final long ts) {
        float avData = sData / counts;
        if (sHandler != null) {
            if (upDateData){
                sHandler.obtainMessage(sensorType, displayFormat(avData, ts)).sendToTarget();
            }
        }
        if (sendData){
            try {
                if(mLogging) {
                    Log.d(TAG, Float.toString(avData));
                }
                dQ.put(udpFormat(avData,ts));
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

    private void setFilters(BroadcastReceiver mReciever, Context context) {                     //USB Filter configuration and reciever registration
        IntentFilter filter = new IntentFilter();
        filter.addAction(SensorActivity.TOGGLE_DISPLAY);
        filter.addAction(SensorActivity.TOGGLE_SEND);
        context.registerReceiver(mReciever, filter);
    }

    /*
    *  Data from serial port received here. byte stream is processed here and send to
    *  PublishEpoch for transmission to UI and XMLAggregator.
    */

    public class usbListener implements UsbSerialInterface.UsbReadCallback {

        @Override
        public void onReceivedData(byte[] arg0) {
            try {
                String data = new String(arg0, "UTF-8");
                if (!data.contains("%")){
                    usbStr = usbStr + data;
                } else{
                    usbStr = usbStr + data;
                    String val = usbStr.split("m")[0].trim();
                    long ts = elapsedRealtime();
                    if (ts >= epoch){
                        if ( ts < epoch + tickLength) {
                            try{
                                acc += Float.parseFloat(val);
                            } catch (NumberFormatException e){
                                acc = 0;  // First read may deliver invalid char
                            }
                            counts += 1;
                        } else {
                            long binTs = epoch + halfTick;
                            publishEpoch(acc, counts, binTs);                           //Send total values, counts, timestamp (for middle of bin) to publisher
                            acc = Float.parseFloat(val);
                            counts = 1;
                            epoch = epoch + tickLength;
                        }
                    }
                    usbStr = "";
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }
}
