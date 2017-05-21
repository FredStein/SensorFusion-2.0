package com.fred.tandq;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

import static android.content.Context.SENSOR_SERVICE;
import static android.os.SystemClock.elapsedRealtime;
import static android.util.Log.getStackTraceString;

/**
 * Created by Fred Stein on 2/05/2017.
 */

class appState {
    //tag for logging
    private final String TAG = appState.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private boolean mLogging = true;

    private Context stateContext;
    public Context getContext(){
        return stateContext;
    }
    private Resources appRes;

    appState(Context localContext){
        stateContext = localContext;
        appRes = stateContext.getResources();
    }
    private static boolean initialised = false;
    public boolean isInitialised() {
        return initialised;
    }
    private static boolean sensorStatusDisplayed = false;
    private static SensorActivity.sensorHandler sHandler;
    public void setsHandler(SensorActivity.sensorHandler sensorPipe){
        sHandler = sensorPipe;
    }
    public SensorActivity.sensorHandler getsHandler(){
        return sHandler;
    }
    private static long epoch;
    public void setEpoch(){
        epoch = elapsedRealtime()+150;                              //Allow time for all threads to start - may nead tweaking - check on real device
    }
    public long getEpoch(){
        return epoch;
    }
    private static boolean USB = false;
    public boolean getUSB() {
        return USB;
    }
    public void setUSB(boolean USBstatus) {
        USB = USBstatus;
    }
    private static String nodeID;
    public String getNodeID(){
        return nodeID;
    }
    private static long tickLength;                              //In milliseconds
    public long getTick(){
        return tickLength;
    }
    private static long halfTick;                                //Also milliseconds
    public long getHalfTick(){
        return tickLength;
    }
    private static int listenHint;                               //Preference accepts value in ms which is converted to hint native unit (microseconds)
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
    private static SharedPreferences sharedPref;
    public SharedPreferences getSpref(){
        return sharedPref;
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
    private static boolean displayData = false;
    public boolean isDisplayData() {
        return displayData;
    }
    public void setDisplayData(boolean dData) {
        displayData = dData;
    }
    private static boolean sendUDP = false;
    public boolean isSendUDP() {
        return sendUDP;
    }
    public void setSendUDP(boolean sUDP) {
        sendUDP = sUDP;
    }

    /*TODO: Create Config page(S) to configure sensors used by node
    * Sensors available to app added here - presently the suite in which the app is 'interested' read from i_sensorss.xml and e_sensorss.xml
    * e_sensors is assumed to contain one sensor
    * Display page is VERY static and app will crash if expected sensors and
    * display page do not match. Sensor re-configuration requires reconfiguration of
    * display page and TextView naming convention (Abb+dimension name) must be exactly observed
    * OK for Proof-of-Concept but not elegant.  Note also app 'expects' data of specific
    * /type and content from USB
    * */
    public void initNode() {
        initialised = true;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(stateContext);
        tickLength = Long.parseLong(sharedPref.getString("reportFrequency",stateContext.getString(R.string.defaultReport_Freq)));
        halfTick = tickLength / 2;
        setEpoch();
        nodeID = sharedPref.getString("NodeID",stateContext.getString(R.string.defaultNodeID));
        hubPort = Integer.parseInt(sharedPref.getString("Port",stateContext.getString(R.string.defaultPort)));
        hubIP = sharedPref.getString("hubAddress",stateContext.getString(R.string.defaultHubIP));
        listenHint = Integer.parseInt(sharedPref.getString("sampleFrequency",stateContext.getString(R.string.defaultSample_Freq)));
        listenHint = listenHint * 1000;         //Convert to microseconds (See above)

        setInternalSensors();
        if (USB){
            setExtSensors();
        }
        Log.d(TAG, "Node initialised.  Initialised is: " + Boolean.toString(initialised));
    }

    private void setInternalSensors(){                                       //TODO: Generalise for internal / external sensor discovery / setup
        SensorManager iSM = (SensorManager) stateContext.getSystemService(SENSOR_SERVICE);
        XmlResourceParser sN = appRes.getXml(R.xml.i_sensors);
        Integer temp = 0;
        String[] dims;
        try{
            while ( sN.getEventType() != XmlResourceParser.END_DOCUMENT) {
                switch(sN.getEventType()){
                    case XmlResourceParser.START_DOCUMENT:
                        break;
                    case XmlResourceParser.START_TAG:
                        if (sN.getName().equals("sensor")) {
                            String name = sN.getAttributeValue(null, "name");
                            Integer type = Integer.parseInt(sN.getAttributeValue(null, "type"));
                            String abbr = sN.getAttributeValue(null, "abbr");
//                            Log.d(TAG, name);
//                            Log.d(TAG, String.valueOf(type));
//                            Log.d(TAG, abbr);
                            temp = type;
                            if (iSM.getDefaultSensor(temp) != null){
                                activeSensors.put(type,new mySensor(name, type, abbr));
                            }
                        }else if (sN.getName().equals("dimensions")){
                            int nAtts = sN.getAttributeCount();
                            dims = new String[nAtts];
                            for (int k = 0; k < nAtts; k++){
                                dims[k] = sN.getAttributeValue(k);
//                                Log.d(TAG, dims[k]);
                            }
                            if (iSM.getDefaultSensor(temp) != null){
                                activeSensors.get(temp).setDim(dims);
                            }
                        }
                        break;
                    case XmlResourceParser.END_TAG:
                        break;
                }
                sN.next();
            }
        }catch (Exception e){
            Log.e(TAG, getStackTraceString(e));
        }finally{
            sN.close();
        }
    }

    public void setExtSensors(){
        //External Sensor configured here
        //This runs when USB == true
        XmlResourceParser sN = appRes.getXml(R.xml.e_sensors);
        Integer temp = 0;
        String[] dims;
        try{
            while ( sN.getEventType() != XmlResourceParser.END_DOCUMENT) {
                switch(sN.getEventType()){
                    case XmlResourceParser.START_DOCUMENT:
                        break;
                    case XmlResourceParser.START_TAG:
                        if (sN.getName().equals("sensor")) {
                            String name = sN.getAttributeValue(null, "name");
                            Integer type = Integer.parseInt(sN.getAttributeValue(null, "type"));
                            String abbr = sN.getAttributeValue(null, "abbr");
//                            Log.d(TAG, name);
//                            Log.d(TAG, String.valueOf(type));
//                            Log.d(TAG, abbr);
                            temp = type;
                            activeSensors.put(type,new mySensor(name, type, abbr));
                        }else if (sN.getName().equals("dimensions")){
                            int nAtts = sN.getAttributeCount();
                            dims = new String[nAtts];
                            for (int k = 0; k < nAtts; k++){
                                dims[k] = sN.getAttributeValue(k);
//                                Log.d(TAG, dims[k]);
                            }
                            activeSensors.get(temp).setDim(dims);
                        }
                        break;
                    case XmlResourceParser.END_TAG:
                        break;
                }
                sN.next();
            }
        }catch (Exception e){
            Log.e(TAG, getStackTraceString(e));
        }finally{
            sN.close();
        }
    }

    public void removeSensor (Integer type){
        activeSensors.remove(type);
    }

    public String[] sensorStatus(SensorManager sm, XmlResourceParser maX){
        String[] availability = new String[2];
        String present = stateContext.getString(R.string.sensors_present);
        String absent = stateContext.getString(R.string.sensors_absent);
        int presence = 0;
        try{
            while ( maX.getEventType() != XmlResourceParser.END_DOCUMENT) {
                switch(maX.getEventType()){
                    case XmlResourceParser.START_DOCUMENT:
                        break;
                    case XmlResourceParser.START_TAG:
                        if (maX.getName().equals("sensor")) {
                            String name = maX.getAttributeValue(null, "name");
                            Integer type = Integer.parseInt(maX.getAttributeValue(null, "type"));
                            if (sm.getDefaultSensor(type) != null){
                                availability[0] = present + name +"\n";
                                if (presence != 1){
                                    presence = 2;
                                }
                            }else{
                                availability[1] = absent + name +"\n";
                                presence = 1;
                            }
                        }
                        break;
                    case XmlResourceParser.END_TAG:
                        break;
                }
                maX.next();
            }
        }catch (Exception e){
            Log.e(TAG, getStackTraceString(e));
        }finally{
            maX.close();
        }

        //What internal sensors have we got?
        if(!sensorStatusDisplayed) {
             switch (presence) {
                case 0:
                    Toast.makeText(stateContext.getApplicationContext(),
                            stateContext.getString(R.string.noSensors), Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    Toast.makeText(stateContext.getApplicationContext(),
                            stateContext.getString(R.string.absent), Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    Toast.makeText(stateContext.getApplicationContext(),
                            stateContext.getString(R.string.present), Toast.LENGTH_LONG).show();
                    break;
            }
            sensorStatusDisplayed = true;
        }
        return availability;
    }
}