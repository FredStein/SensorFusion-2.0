package com.fred.tandq;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by Fred Stein on 25/04/2017.
 */

class udpSender implements Runnable {
    //tag for logging
    private static final String TAG = udpSender.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private boolean mLogging = true;

    private AtomicBoolean startThread = new AtomicBoolean(false);
    public boolean stopMe(){
        while (udpR.isRunning()){}
        while (udpW.isRunning()){}
        this.startThread.set(false);
        return this.startThread.get();
    }
    public void setRunning(boolean runState){
        this.startThread.set(runState);
    }
    private final SortedMap<String, MessageXML> udpStack = new TreeMap<>();             //TODO: Check if this is effective in ordering timestamps
    private LinkedBlockingQueue udpRxQ;
    private InetAddress IPout;
    private DatagramSocket socket;
    private String hubIP;
    private int hubPort;
    private udpQReader udpR;
    private Thread udpRxThread;
    private udpTx udpW;
    private Thread udpTxThread;

    udpSender(LinkedBlockingQueue udpQ, String IP, int Port){
        if (false) {
            String logString = " udpSender created";
            Log.d(TAG, logString);
        }
        this.hubIP = IP;
        this.hubPort = Port;
        try {
            this.socket = new DatagramSocket();

        } catch (SocketException e) {
            e.printStackTrace();
        }
        try {
            this.IPout = InetAddress.getByName(hubIP);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.udpRxQ = udpQ;
        this.udpR = new udpQReader(udpQ);
        this.udpW = new udpTx();
    }

    @Override
    public void run() {
        if (false) {
            String logString = " udpSender started";
            Log.d(TAG, logString);
        }
        while (startThread.get()){
            if (!udpR.isRunning()){
                if(udpRxQ.size() > 0){
                    udpR.setRunning(true);
                    udpRxThread = new Thread(udpR);
                    udpRxThread.start();
                }
            }
            if(!udpW.isRunning()){
                synchronized (udpStack){
                    if(udpStack.size()>0) {
                        udpW.setRunning(true);
                        udpTxThread = new Thread(udpW);
                        udpTxThread.start();
                    }
                }
            }
        }
    }

    private class udpQReader implements Runnable {
        LinkedBlockingQueue queue;
        private AtomicBoolean running = new AtomicBoolean(false);
        public boolean isRunning(){
            return running.get();
        }
        public void setRunning(boolean running) {
            this.running.set(running);
        }
        udpQReader(LinkedBlockingQueue q) {
            this.queue = q;
            if (false) {
                String logString = " udpQReader created";
                Log.d(TAG, logString);
            }
        }
        @Override
        public void run() {
            if (false) {
                String logString = " udpQReader started";
                Log.d(TAG, logString);
            }
            while (running.get()) {
                try {
                    MessageXML msg = (MessageXML) queue.take();
                    synchronized(udpStack){
                        udpStack.put(msg.getTimeStamp(),msg);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (queue.size() > 0){
                    running.set(true);
                } else{
                    running.set(false);
                }
            }
        }
    }

    private class udpTx implements Runnable {
        private AtomicBoolean running = new AtomicBoolean(false);
        public boolean isRunning(){
            return running.get();
        }
        public void setRunning(boolean running) {
            this.running.set(running);
        }
        udpTx() {
            if (mLogging) {
                String logString = " udpTx created";
                Log.d(TAG, logString);
            }
        }
        @Override
        public void run(){
            if (false) {
                String logString = " udpTx started";
                Log.d(TAG, logString);
            }
            while (running.get()){
                synchronized(udpStack){
                    String msgStr = udpStack.remove(udpStack.firstKey()).getXmlString();
                    byte[] message = msgStr.getBytes();
                    int msg_length = msgStr.length();
                    DatagramPacket p = new DatagramPacket(message, msg_length, IPout, hubPort);
                    try {
                        socket.send(p);
                        if (mLogging) {
                            Log.d(TAG, "Timestamp : " + msgStr.substring(msgStr.length()-23,msgStr.length()-10));
                        }
                    }catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                    if (udpStack.size() > 0){
                        running.set(true);
                    } else{
                        running.set(false);
                    }
                }
            }
        }
    }
}
