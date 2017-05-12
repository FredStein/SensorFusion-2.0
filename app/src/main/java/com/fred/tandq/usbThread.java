package com.fred.tandq;

import android.os.Handler;
import android.util.Log;

import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static android.os.SystemClock.elapsedRealtime;
import static com.fred.tandq.appState.TYPE_USB;
import static com.fred.tandq.appState.getEpoch;
import static com.fred.tandq.appState.getSensorName;
import static com.fred.tandq.appState.halfTick;
import static com.fred.tandq.appState.sendUDP;
import static com.fred.tandq.appState.tickLength;
import static com.fred.tandq.usbService.MESSAGE_FROM_SERIAL_PORT;

/**
 * Created by Fred Stein on 10/05/2017.
 */

class usbThread implements Runnable {
    //tag for logging
    private static final String TAG = usbService.class.getSimpleName();
    //flag for logging
    private boolean mLogging = false ;

    private static Handler mHandler = SensorActivity.getuHandler();
    private static qWriter USBDataQW;
    private static int counts = 1;
    private static float acc;
    private static long mEpoch;
    private static String usbStr = "";

    usbThread(LinkedBlockingQueue usbDataQ){
        mEpoch = getEpoch();
        USBDataQW = new qWriter(usbDataQ);
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
                Log.d(TAG, val);
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
        if (mHandler != null)
            mHandler.obtainMessage(MESSAGE_FROM_SERIAL_PORT,displayFormat(avData, ts)).sendToTarget();
        if (sendUDP){
            try {
                USBDataQW.queue.put(udpFormat(avData,ts));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static String[] displayFormat(float sData, long timestamp){
        String[] dispPkt = new String[2];
        dispPkt[0] = String.format ("%.3f", sData);
        String time = String.format ("%d", timestamp);
        dispPkt[1] = time.substring(time.length() - 5, time.length());        //right most 5 ms of timestamp
        return dispPkt;
    }

    private static HashMap<String, String> udpFormat(float sData, long timestamp){
        HashMap<String,String> udpPkt = new HashMap<>();
        udpPkt.put("d",Float.toString(sData));
        udpPkt.put("Timestamp", Long.toString(timestamp));
        udpPkt.put("Sensor",getSensorName(TYPE_USB));
        return udpPkt;
    }
}
