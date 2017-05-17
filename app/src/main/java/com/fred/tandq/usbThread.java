package com.fred.tandq;

import android.util.Log;

import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static android.os.SystemClock.elapsedRealtime;
import static com.fred.tandq.SensorActivity.getsHandler;
import static com.fred.tandq.SensorService.getsDataQ;
import static com.fred.tandq.appState.getEpoch;
import static com.fred.tandq.appState.halfTick;
import static com.fred.tandq.appState.sendUDP;
import static com.fred.tandq.appState.tickLength;

/**
 * Created by Fred Stein on 10/05/2017.
 */

class usbThread implements Runnable {
    //tag for logging
    private static final String TAG = usbThread.class.getSimpleName()+"SF 2.0";
    //flag for logging
    private static boolean mLogging = true ;

    private static SensorActivity.sensorHandler sHandler = getsHandler();
    private static qWriter USBDataQW;
    private static int counts = 1;
    private static float acc;
    private static long mEpoch;
    private static String usbStr = "";
    private LinkedBlockingQueue sDataQ;
    private static mySensor USBSensor;

    usbThread(mySensor sensor){
        mEpoch = getEpoch();
        sDataQ = getsDataQ();
        USBDataQW = new qWriter(sDataQ);
        USBSensor = sensor;
    }
    /*
     *  Data received from serial port will be received here. Just populate onReceivedData with your code
     *  In this particular example. byte stream is converted to String and send to UI thread to
     *  be treated there.
     */
    public static UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
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
                        long binTs = mEpoch + halfTick;                             //TODO: Check - multiple threads resetting epoch No No No
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
        new Thread(USBDataQW).start();
    }

    public class qWriter implements Runnable {
        private LinkedBlockingQueue queue;

        public qWriter(LinkedBlockingQueue q) {
            this.queue = q;
        }

        public boolean isRunning(){
            return true;
        }

        @Override
        public void run() {                                                             //TODO Need thread stop condition
            if (mLogging) {
                String logString = " USBqWriter up";
                Log.v(TAG, logString);
            }
        }
    }

    private static void publishEpoch(float sData, int counts, final long ts) {
        float avData = sData / counts;
        if (sHandler != null)
            sHandler.obtainMessage(USBSensor.getType(),displayFormat(avData, ts)).sendToTarget();
        if (sendUDP){
            try {
                USBDataQW.queue.put(udpFormat(avData,ts));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static HashMap<String,String> displayFormat(float sData, long timestamp){
        HashMap<String,String> dPkt = new HashMap<>();
        dPkt.put("d",String.format ("%.3f", sData));                                //TODO: generalise with use of mySensor class
        String time = String.format ("%d", timestamp);
        dPkt.put("Timestamp", time.substring(time.length() - 5, time.length()));    //right most 5 ms of timestamp
        return dPkt;
    }

    private static HashMap<String, String> udpFormat(float sData, long timestamp){
        HashMap<String,String> udpPkt = new HashMap<>();
        udpPkt.put("d",Float.toString(sData));
        udpPkt.put("Timestamp", Long.toString(timestamp));
        udpPkt.put("Sensor",USBSensor.getName());
        return udpPkt;
    }
}
