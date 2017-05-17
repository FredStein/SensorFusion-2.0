package com.fred.tandq;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
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
    private static final String TAG = appState.class.getSimpleName()+"SF 2.0";
    //flag for logging
    private static boolean mLogging = true;

    private static Context appContext;
    private static View mainView;
    private static Context dispContext;
    private static View dispView;
    private static TextView present;
    private static TextView absent;
    private static String sPresent = "SENSORS PRESENT\n";
    private static String sAbsent = "SENSORS ABSENT\n";
    private static long epoch;
    public static boolean sendUDP = false;
    public static boolean displayOn = false;
    private static boolean USB = false;
    public static boolean getUSB() {
        return USB;
    }
    public static void setUSB(boolean USBstatus) {
        USB = USBstatus;
    }
    public static String nodeID;
    public static long tickLength;                              //In milliseconds
    public static long halfTick;                                //Also milliseconds
    public static int hubPort;
    public static String hubIP;
    public static int listenHint;                               //Preference accepts value in ms which is converted to hint native unit (microseconds)
    public static SharedPreferences sharedPref;
    private static HashMap<Integer, mySensor> mSensors = new HashMap<>();
    public static HashMap<Integer, mySensor> getSensors(){
        return mSensors;
    }
    public static Integer getSensorType(String name){
        for (mySensor sensor : mSensors.values())
            if (sensor.getName().equals(name)){
                return sensor.getType();
            }
        return 0;
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
    public void initNode(Context mContext, View view) {
        appContext = mContext;
        mainView = view;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        tickLength = Long.parseLong(sharedPref.getString("reportFrequency",mContext.getString(R.string.defaultReport_Freq)));
        halfTick = tickLength / 2;
        setEpoch();
        nodeID = sharedPref.getString("NodeID",mContext.getString(R.string.defaultNodeID));
        hubPort = Integer.parseInt(sharedPref.getString("Port",mContext.getString(R.string.defaultPort)));
        hubIP = sharedPref.getString("hubAddress",mContext.getString(R.string.defaultHubIP));
        listenHint = Integer.parseInt(sharedPref.getString("sampleFrequency",mContext.getString(R.string.defaultSample_Freq)));
        listenHint = listenHint * 1000;         //Convert to microseconds (See above)

        present = (TextView) view.findViewById(R.id.sp);
        absent = (TextView) view.findViewById(R.id.sa);

        Resources AppRes = mContext.getResources();
        setInternalSensors(AppRes);
        if (USB){
            setExtSensors(AppRes);
        }
    }

    public static void initDisplay(Context mContext, View view){

    }

    public static void setEpoch(){
        epoch = elapsedRealtime()+150;                              //Allow time for all threads to start - may nead tweaking - check on real device
    }
    public static long getEpoch(){
        return epoch;
    }

    private static void setInternalSensors(Resources appRes){                                       //TODO: Generalise for internal / external sensor discovery / setup
        SensorManager iSM = (SensorManager) appContext.getSystemService(SENSOR_SERVICE);
        XmlResourceParser sN = appRes.getXml(R.xml.i_sensors);
        int presence = 0;
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
                                mSensors.put(type,new mySensor(name, type, abbr));
                                sPresent = sPresent + name +"\n";
                            }else{
                                sAbsent = sAbsent + name +"\n";
                                presence = 1;
                            }
                        }else if (sN.getName().equals("dimensions")){
                            int nAtts = sN.getAttributeCount();
                            dims = new String[nAtts];
                            for (int k = 0; k < nAtts; k++){
                                dims[k] = sN.getAttributeValue(k);
//                                Log.d(TAG, dims[k]);
                            }
                            if (iSM.getDefaultSensor(temp) != null){
                                mSensors.get(temp).setDim(dims);
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

        //What internal sensors have we got?

        if (mSensors.size() == 0){
            presence = 2;
        }
        present = (TextView) mainView.findViewById(R.id.sp);
        absent = (TextView) mainView.findViewById(R.id.sa);

        present.setText(sPresent);
        absent.setText(sAbsent);

        switch (presence) {
            case 0:
                Toast.makeText(appContext.getApplicationContext(),
                        appRes.getString(R.string.present), Toast.LENGTH_LONG).show();
                break;
            case 1:
                Toast.makeText(appContext.getApplicationContext(),
                        appRes.getString(R.string.absent), Toast.LENGTH_LONG).show();
                break;
            case 2:
                Toast.makeText(appContext.getApplicationContext(),
                        appRes.getString(R.string.noSensors), Toast.LENGTH_LONG).show();
                break;
        }
    }

    private static void setExtSensors(Resources appRes){
        //External Sensor configured here
        //This runs when USB == true or usbService signals ACTION_USB_PERMISSION_GRANTED to? TODO: establish which service does this.
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
                            mSensors.put(type,new mySensor(name, type, abbr));
                        }else if (sN.getName().equals("dimensions")){
                            int nAtts = sN.getAttributeCount();
                            dims = new String[nAtts];
                            for (int k = 0; k < nAtts; k++){
                                dims[k] = sN.getAttributeValue(k);
//                                Log.d(TAG, dims[k]);
                            }
                            mSensors.get(temp).setDim(dims);
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
        //TODO: Code to set USB indicator in AppBar R to "USB"
    }
}