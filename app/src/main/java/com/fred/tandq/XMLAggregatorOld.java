package com.fred.tandq;


import android.util.Log;

import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Fred Stein on 25/04/2017.
 * Current aggregator places content of sQReader.run
 * into anonymous inner class in XML Aggregator constructor to solve concurrency issue
 * with msgStack access.  Have not unpacked why this works & so this version
 * saved against later need.
 */

class XMLAggregatorOld implements Runnable {
    //tag for logging
    private static final String TAG = XMLAggregatorOld.class.getSimpleName();
    //flag for logging
    private boolean mLogging = false;

    private qReader rQs;
    private LinkedBlockingQueue udpQ = new LinkedBlockingQueue();
    private udpWriteQ udpWQ;
    private SortedMap<String, MessageXML> msgStack = new TreeMap<>();
    private udpSender udpS = new udpSender(udpQ);

    XMLAggregatorOld(HashMap<Integer,LinkedBlockingQueue> readQs) {
        rQs = new qReader(readQs);
        udpWQ = new udpWriteQ(udpQ);
    }

    @Override
    public void run() {
        new Thread(rQs).start();
        new Thread(udpS).start();
        new Thread(udpWQ).start();
    }

    public class qReader implements Runnable {
        private HashMap<Integer,LinkedBlockingQueue> queues;
        public qReader(HashMap<Integer,LinkedBlockingQueue> qMap) {
            this.queues = qMap;
        }

        @Override
        public void run() {
            if (mLogging){
                String logString = " sQReader Started";
                Log.i(TAG, logString);
            }

            while (true) {                                      //Multiple queues
                for (int sensor : queues.keySet()) {
                    try {
                        HashMap<String,String> msg = (HashMap<String,String>) queues.get(sensor).take();
                        String ts = new String(msg.get("Timestamp"));
                        if (!msgStack.containsKey(ts)){
                            MessageXML local = new MessageXML();
                            for (String item : msg.keySet()){
                                if (item != "Timestamp") {
                                    local.setVal(sensor, item, msg.get(item));
                                }
                                local.setTimeStamp(ts);
                            }
                            msgStack.put(ts, local);
                        }else {
                            for (String item : msg.keySet()){
                                if (item != "Timestamp") {
                                    msgStack.get(ts).setVal(sensor, item, msg.get(item));
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                for (String item: msgStack.keySet()){
                    if (msgStack.get(item).isComplete()){
                        try {
                            udpWQ.queue.put(msgStack.remove(item));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
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
                Log.i(TAG, logString);
            }
        }
    }
}
