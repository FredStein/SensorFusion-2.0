package com.fred.tandq;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static android.os.SystemClock.elapsedRealtime;
import static android.util.Log.getStackTraceString;

/**
 * Created by Fred Stein on 22/05/2017.
 */

public class nodeController extends Application {
    //tag for logging
    private static final String TAG = nodeController.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private boolean mLogging = false;

    private static nodeController nodeCtrl;
    public static nodeController getNodeCtrl()
    {
        return nodeCtrl;
    }

    private static boolean sensorStatusDisplayed = false;
    public static boolean wasStatusDisplayed() {
        return sensorStatusDisplayed;
    }
    public static void setStatusDisplayed(boolean sensorStatusDisplayed) {
        nodeController.sensorStatusDisplayed = sensorStatusDisplayed;
    }

    private static int intSensSuiteStatus = 0;
    public static int getSensorPresence() {
        return intSensSuiteStatus;
    }
    private static String[] sensorStatus = new String[2];
    public static String[] getSensorStatus() {
        return sensorStatus;
    }

    private static boolean initialised = false;
    private static SharedPreferences sharedPref;
    public SharedPreferences getSpref(){
        return sharedPref;
    }
    private static long epoch;
    public void setEpoch(){
        epoch = elapsedRealtime()+150;                              //Allow time for all threads to start - may nead tweaking - check on real device
    }
    public long getEpoch(){
        return epoch;
    }
    private static String appBarTitle = "";
    public void setAppBarTitle (String title){
        appBarTitle = title;
    }
    public String getAppBarTitle(){
        return appBarTitle;
    }
    private static String nodeID;
    public String getNodeID(){
        return nodeID;
    }
    private static long tickLength;                              //In milliseconds
    public long getTick(){
        return tickLength;
    }
    public long getHalfTick(){
        return tickLength / 2;
    }
    private static int listenHint;                               //Preference accepts value in ms which is converted to 'Hint' native unit (microseconds)
    public int getHint(){
        return listenHint;
    }
    private static int hubPort;
    public int getPort(){
        return hubPort;
    }
    private static String hubIP;
    public String getIP(){
        return hubIP;
    }
    private static HashMap<Integer, mySensor> activeSensors = new HashMap<>();
    public HashMap<Integer, mySensor> getSensors(){
        return activeSensors;
    }
    public Integer getSensorType(String name){
        for (mySensor sensor : activeSensors.values())
            if (sensor.getName().equals(name)){
                return sensor.getType();
            }
        return 0;
    }
    public static void removeActiveSensor(Integer type){
        activeSensors.remove(type);
        SensorThreads.remove(type);
    }

    private static SensorActivity.sensorHandler sHandler;
    public static void setsHandler(SensorActivity.sensorHandler sensorPipe){
        sHandler = sensorPipe;
    }
    public static SensorActivity.sensorHandler getsHandler(){
        return sHandler;
    }

    private static boolean displayData = false;
    public static boolean isDisplayData() {
        return displayData;
    }
    public static void setDisplayData(boolean dData) {
        displayData = dData;
    }
    private static boolean sendUDP = false;
    public static boolean isSendUDP() {
        return sendUDP;
    }
    public static void setSendUDP(boolean sUDP) {
        sendUDP = sUDP;
    }
    private static usbRunnable.usbListener mCallback;
    public static usbRunnable.usbListener getusbCallback() {
        return mCallback;
    }
    public static void setusbCallback(usbRunnable.usbListener mCallback) {
        nodeController.mCallback = mCallback;
    }
    LinkedBlockingQueue sDataQ = new LinkedBlockingQueue();
    private static HashMap<Integer, Thread> SensorThreads = new HashMap<>();
    public static Thread getThread (Integer type){
        if (SensorThreads.containsKey(type)){
            return SensorThreads.get(type);
        }else return null;
    }
    private static XMLAggregator XMLC;
    public XMLAggregator getAggregator(){
        return XMLC;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        nodeCtrl = this;
        if (mLogging){
            String logstring = "Node Controller Created";
            Log.d(TAG, logstring);
        }
        setAppBarTitle(getString(R.string.No_USB));
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        initNode();
        setInternalSensors(sm);
        startService(new Intent(this, SensorService.class));
    }

    public void initNode() {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        tickLength = Long.parseLong(sharedPref.getString("reportFrequency",getString(R.string.defaultReport_Freq)));
        setEpoch();
        nodeID = sharedPref.getString("NodeID",getString(R.string.defaultNodeID));
        hubPort = Integer.parseInt(sharedPref.getString("Port",getString(R.string.defaultPort)));
        hubIP = sharedPref.getString("hubAddress",getString(R.string.defaultHubIP));
        listenHint = Integer.parseInt(sharedPref.getString("sampleFrequency",getString(R.string.defaultSample_Freq)));
        listenHint = listenHint * 1000;         //Convert to microseconds (See above)
        XMLC = new XMLAggregator(sDataQ);
        initialised = true;
    }

    private void setInternalSensors(SensorManager sm){                                       //TODO: Generalise for internal / external sensor discovery / setup
        sensorStatus[0] = getString(R.string.sensors_present);
        sensorStatus[1] = getString(R.string.sensors_absent);
        Integer temp = 0;
        String[] dims;
        try (XmlResourceParser sN = getResources().getXml(R.xml.i_sensors)) {
            while (sN.getEventType() != XmlResourceParser.END_DOCUMENT) {
                switch (sN.getEventType()) {
                    case XmlResourceParser.START_DOCUMENT:
                        break;
                    case XmlResourceParser.START_TAG:
                        if (sN.getName().equals("sensor")) {
                            String name = sN.getAttributeValue(null, "name");
                            Integer type = Integer.parseInt(sN.getAttributeValue(null, "type"));
                            String abbr = sN.getAttributeValue(null, "abbr");
                            temp = type;
                            if (sm.getDefaultSensor(type) != null) {
                                activeSensors.put(type, new mySensor(name, type, abbr));
                                SensorRunnable sr = new SensorRunnable(activeSensors.get(type), sm, sDataQ);
                                sr.setRunning(true);
                                SensorThreads.put(type, new Thread(sr));
                                sensorStatus[0] = sensorStatus[0] + name + "\n";
                                if (intSensSuiteStatus != 1) {
                                    intSensSuiteStatus = 2;
                                }
                            } else {
                                sensorStatus[1] = sensorStatus[1] + name + "\n";
                                intSensSuiteStatus = 1;
                            }
                        } else if (sN.getName().equals("dimensions")) {
                            int nAtts = sN.getAttributeCount();
                            dims = new String[nAtts];
                            for (int k = 0; k < nAtts; k++) {
                                dims[k] = sN.getAttributeValue(k);
//                                Log.d(TAG, dims[k]);
                            }
                            if (sm.getDefaultSensor(temp) != null) {
                                activeSensors.get(temp).setDim(dims);
                            }
                        }
                        break;
                    case XmlResourceParser.END_TAG:
                        break;
                }
                sN.next();
            }
        } catch (Exception e) {
            Log.e(TAG, getStackTraceString(e));
        }
    }

    public void setExtSensors(){
        //External Sensor configured here. At the moment *There can be only one*
        Integer temp = 0;
        String[] dims;
        try (XmlResourceParser sN = getResources().getXml(R.xml.e_sensors)) {
            while (sN.getEventType() != XmlResourceParser.END_DOCUMENT) {
                switch (sN.getEventType()) {
                    case XmlResourceParser.START_DOCUMENT:
                        break;
                    case XmlResourceParser.START_TAG:
                        if (sN.getName().equals("sensor")) {
                            String name = sN.getAttributeValue(null, "name");
                            Integer type = Integer.parseInt(sN.getAttributeValue(null, "type"));
                            String abbr = sN.getAttributeValue(null, "abbr");
                            temp = type;
                            activeSensors.put(type, new mySensor(name, type, abbr));
                            usbRunnable ur = new usbRunnable(activeSensors.get(type), sDataQ);
                            ur.setRunning(true);
                            SensorThreads.put(type, new Thread(ur));
                        } else if (sN.getName().equals("dimensions")) {
                            int nAtts = sN.getAttributeCount();
                            dims = new String[nAtts];
                            for (int k = 0; k < nAtts; k++) {
                                dims[k] = sN.getAttributeValue(k);
                            }
                            activeSensors.get(temp).setDim(dims);
                        }
                        break;
                    case XmlResourceParser.END_TAG:
                        break;
                }
                sN.next();
            }
        } catch (Exception e) {
            Log.e(TAG, getStackTraceString(e));
        }
    }
}

