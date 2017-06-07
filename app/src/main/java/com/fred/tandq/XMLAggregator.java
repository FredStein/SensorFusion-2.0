package com.fred.tandq;


import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Fred Stein on 25/04/2017.
 */

class XMLAggregator implements Runnable {
    //tag for logging
    private static final String TAG = XMLAggregator.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private boolean mLogging = false;

    private AtomicBoolean startThread = new AtomicBoolean(false);
    public boolean isRunning(){
        return startThread.get();
    }
    public void setRunning(boolean run) {
        this.startThread.set(run);
    }

    public boolean stopMe(){
        while (sDataQReader.isRunning()){}
        while (udpQWriter.isRunning()){}
        udpS.stopMe();
        startThread.set(false);
        return true;
    }
    private LinkedBlockingQueue udpQ = new LinkedBlockingQueue();
    private LinkedBlockingQueue sensorDataQ;
    private final SortedMap<String, MessageXML> msgStack = new TreeMap<>();
    private udpQWrite udpQWriter;
    private Thread udpWriterThread;
    private dataQReader sDataQReader;
    private Thread sDataReaderThread;
    private udpSender udpS;
    private Thread udpSenderThread;

    XMLAggregator(LinkedBlockingQueue dataQ) {
        if (mLogging) {
            String logString = " XMLAggregator created";
            Log.d(TAG, logString);
        }
        sensorDataQ = dataQ;
        sDataQReader = new dataQReader(dataQ);
        udpQWriter = new udpQWrite(udpQ);
        udpS = new udpSender(udpQ,nodeController.getNodeCtrl().getIP(),nodeController.getNodeCtrl().getPort());
        udpSenderThread = new Thread(udpS);
    }

    @Override
    public void run() {
        if (mLogging){
            String logString = "XMLAggregator Started";
            Log.d(TAG, logString);
        }
        udpS.setRunning(true);
        udpSenderThread.start();
        while(startThread.get()){
            if (!sDataQReader.isRunning()){
                if (sensorDataQ.size() > 0){
                    sDataQReader.setRunning(true);
                    sDataReaderThread = new Thread(sDataQReader);
                    sDataReaderThread.start();
                }
            }
            if (!udpQWriter.isRunning()){
                synchronized (msgStack){
                    if(msgStack.size() > 0){
                        udpQWriter.setRunning(true);
                        udpWriterThread = new Thread(udpQWriter);
                        udpWriterThread.start();
                    }
                }
            }
        }
    }

    private class dataQReader implements Runnable {
        LinkedBlockingQueue queue;
        private AtomicBoolean running = new AtomicBoolean(false);
        public boolean isRunning(){
            return running.get();
        }
        public void setRunning(boolean running) {
            this.running.set(running);
        }
        public dataQReader(LinkedBlockingQueue q) {
            this.queue = q;
            if (mLogging) {
                String logString = " dataQReader created";
                Log.d(TAG, logString);
            }
        }

        @Override
        public void run() {
            if (mLogging){
                String logString = "dataQReader started";
                Log.d(TAG, logString);
            }
            while (running.get()) {
                try {
                    HashMap<String, String> msg = (HashMap<String, String>) queue.take();
                    for (String item: msg.keySet()){
                        if (mLogging){
                            String logstring =  item + "," + msg.get(item);
                            Log.d(TAG, logstring);
                        }
                    }
                    String ts = msg.get("Timestamp");
                    synchronized (msgStack){
                        if (!msgStack.containsKey(ts)) {
                            MessageXML local = new MessageXML();
                            local.setTimeStamp(ts);
                            local.setVal(msg);
                            msgStack.put(ts, local);
                        }else {
                            msgStack.get(ts).setVal(msg);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (queue.size() > 0){
                    running.set(true);
                }else{
                    running.set(false);
                }
            }
        }
    }

/*
    private void publishXML(){
        for (String item: msgStack.keySet()){
            MessageXML msg = msgStack.get(item);
            if (msg.isComplete());
            try {
                udpWQ.queue.put(udpMsg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
*/


    public class udpQWrite implements Runnable {
        private AtomicBoolean running = new AtomicBoolean(false);
        public boolean isRunning(){
            return running.get();
        }
        public void setRunning(boolean running) {
            this.running.set(running);
        }
        LinkedBlockingQueue queue;
        udpQWrite(LinkedBlockingQueue q) {
            this.queue  = q;
            if (mLogging) {
                String logString = " udpQWrite created";
                Log.d(TAG, logString);
            }
        }
        @Override
        public void run() {
            if (mLogging) {
                String logString = " udpQWrite started";
                Log.d(TAG, logString);
            }
            while (running.get()){
                synchronized (msgStack){
                    for(Iterator<Map.Entry<String, MessageXML>> it = msgStack.entrySet().iterator(); it.hasNext(); ) {
                        Map.Entry<String, MessageXML> entry = it.next();
                        if(entry.getValue().isComplete()) {
                            MessageXML msg = entry.getValue();
                            try {
                                queue.put(msg);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            it.remove();
                        }
                    }
                    if (msgStack.size() > 0){
                        running.set(true);
                    }else{
                        running.set(false);
                    }
                }
            }
        }
    }
}
