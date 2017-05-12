package com.fred.tandq;


import android.util.Log;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashMap;

/**
 * Created by Fred Stein on 25/04/2017.
 */

class XMLAggregator implements Runnable {
    //tag for logging
    private static final String TAG = XMLAggregator.class.getSimpleName();
    //flag for logging
    private boolean mLogging = false;

    private dataQReader rQs;
    private LinkedBlockingQueue udpQ = new LinkedBlockingQueue();
    private udpWriteQ udpWQ;
    private SortedMap<String, MessageXML> msgStack = new TreeMap<>();
    private udpSender udpS = new udpSender(udpQ);

    XMLAggregator(LinkedBlockingQueue sDataQ) {
        udpWQ = new udpWriteQ(udpQ);
        rQs = new dataQReader(sDataQ){
            @Override
            public void run(){
                while (true) {
                    try {
                        HashMap<String, String> msg = (HashMap<String, String>) queue.take();
                        for (String item: msg.keySet()){
                            Log.d(TAG, item + "," + msg.get(item));
                        }
                        String ts = new String(msg.get("Timestamp"));
                        if (!msgStack.containsKey(ts)) {
                            MessageXML local = new MessageXML();
                            local.setTimeStamp(ts);
                            local.setVal(msg);
                            msgStack.put(ts, local);
                        }else {
                            msgStack.get(ts).setVal(msg);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (String item: msgStack.keySet()){
                        MessageXML msg = msgStack.get(item);
                        if (msg.isComplete());
                        publishXML(msg);
                    }
                }
            }
        };
    }

    @Override
    public void run() {
        new Thread(rQs).start();
        new Thread(udpS).start();
        new Thread(udpWQ).start();
    }

    private void publishXML(MessageXML udpMsg){
        try {
            udpWQ.queue.put(udpMsg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class dataQReader implements Runnable {
        public LinkedBlockingQueue queue;
        public dataQReader(LinkedBlockingQueue q) {
            this.queue = q;
        }

        @Override
        public void run() {                                     //Overiden will not execute
            if (mLogging){
                String logString = " dataQReader Started";
                Log.d(TAG, logString);
            }
        }
    }

    public class udpWriteQ implements Runnable {
        private LinkedBlockingQueue queue;

        public udpWriteQ(LinkedBlockingQueue q) {
            this.queue = q;
        }

        public boolean isRunning(){
            return true;
        }

        @Override
        public void run() {                             //TODO Thread stop condition
            if (mLogging) {
                String logString = " udpWriteQ Started";
                Log.d(TAG, logString);
            }
        }
    }
}
