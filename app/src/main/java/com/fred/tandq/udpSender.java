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


/**
 * Created by Fred Stein on 25/04/2017.
 */

class udpSender implements Runnable {
    //tag for logging
    private static final String TAG = udpSender.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private boolean mLogging = true;

    private udpReader udpR;
    private SortedMap<String, MessageXML> udpStack = new TreeMap<>();
    private InetAddress IPout;
    private DatagramSocket socket;
    private String hubIP;
    private int hubPort;

    udpSender(LinkedBlockingQueue udpQ, String IP, int Port){
        hubIP = IP;
        hubPort = Port;
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
        udpR = new udpReader(udpQ);
    }

    @Override
    public void run() {
        new Thread(udpR).start();
    }

    private class udpReader implements Runnable {
        private LinkedBlockingQueue queue;

        public udpReader(LinkedBlockingQueue q) {
            this.queue = q;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    MessageXML msg = (MessageXML) queue.take();
                    udpStack.put(msg.getTimeStamp(),msg);
                    udpTx(msg.getXmlString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void udpTx(String msgStr){
        int msg_length = msgStr.length();
        byte[] message = msgStr.getBytes();
        DatagramPacket p = new DatagramPacket(message, msg_length, IPout, hubPort);
        try {
            socket.send(p);
            if (mLogging) {
                Log.d(TAG, "XML Out : " + msgStr);
            }
        }catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
