package com.fred.tandq;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static android.content.Context.SENSOR_SERVICE;
import static android.os.SystemClock.elapsedRealtime;

/**
 * Created by Fred Stein on 2/05/2017.
 */

class appState {
    //tag for logging
    private static final String TAG = appState.class.getSimpleName()+"SF 2.0";
    //flag for logging
    private static boolean mLogging = true;

    public static final int TYPE_USB = 20000;

    private TextView present;
    private TextView absent;
    private static String sPresent = "SENSORS PRESENT\n";
    private static String sAbsent = "SENSORS ABSENT\n";
    private static long epoch;
    private static HashMap<Integer, String> sMapNames = new HashMap<>();
    private static HashMap<Integer, String> sMapAbb = new HashMap<>();
    private static HashMap<Integer, List> textViewCodes = new HashMap<>();
    public static Set<Integer> getSensors() {
        return sMapNames.keySet();
    }
    public static String getSensorName(int sensorType) {
        return sMapNames.get(sensorType);
    }
    public static List getTextViewCodes(Integer sensorType ) {
        return textViewCodes.get(sensorType);
    }
    private static HashMap<String, Integer> sTypeIdx;
    public static Integer getSensorType(String sensorName) {
        return sTypeIdx.get(sensorName);
    }
    public static String[] varNames;
    public static String nodeID;
    public static long tickLength;                              //In milliseconds
    public static long halfTick;
    public static int hubPort;
    public static String hubIP;
    public static int listenHint;                               //Preference accepts value in ms converted to hint native unit (microseconds)
    public static SharedPreferences sharedPref;
    public static boolean sendUDP = false;
    public static boolean displayOn = false;


    public int initNode(Context mContext, View view) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        tickLength = Long.parseLong(sharedPref.getString("reportFrequency",mContext.getString(R.string.defaultReport_Freq)));
        halfTick = tickLength / 2;
        setEpoch();
        nodeID = sharedPref.getString("NodeID",mContext.getString(R.string.defaultNodeID));
        hubPort = Integer.parseInt(sharedPref.getString("Port",mContext.getString(R.string.defaultPort)));
        hubIP = sharedPref.getString("hubAddress",mContext.getString(R.string.defaultHubIP));
        listenHint = Integer.parseInt(sharedPref.getString("sampleFrequency",mContext.getString(R.string.defaultSample_Freq)));
        listenHint = listenHint * 1000;         //Convert to microseconds (See above)

        SensorManager mSensorManager = (SensorManager) mContext.getSystemService(SENSOR_SERVICE);

        Resources AppRes = mContext.getResources();

        int[] resTypes = AppRes.getIntArray(R.array.S_TYPE);
        String[] resNames = AppRes.getStringArray(R.array.S_NAME);
        String[] resAbb = AppRes.getStringArray(R.array.S_ABB);

        HashMap<Integer,String> sNameIdx = new HashMap<>();
        HashMap<String,Integer> sTypeIdx = new HashMap<>();
        for (int i=0; i < resTypes.length; i++){
            sNameIdx.put(resTypes[i],resNames[i]);
            sTypeIdx.put(resNames[i],resTypes[i]);
        }
        this.sTypeIdx = sTypeIdx;

        HashMap<Integer,String> sAbbIdx = new HashMap<>();
        for (int i=0; i < resTypes.length; i++){
            sAbbIdx.put(resTypes[i],resAbb[i]);
        }

        present = (TextView) view.findViewById(R.id.sp);
        absent = (TextView) view.findViewById(R.id.sa);

        int gotAll = 0;
        for (int item : resTypes) {
            if (mSensorManager.getDefaultSensor(item) == null) {
                // We do not have this sensor
                sAbsent = sAbsent + sNameIdx.get(item) +"\n";
                gotAll = 1;
            }
            else if(item == TYPE_USB){
                sMapNames.put(item, sNameIdx.get(item));
                sMapAbb.put(item, sAbbIdx.get(item));                           //TODO: Add code to pick up USB if plugged in
            }
            else if(mSensorManager.getDefaultSensor(item) != null){
                sMapNames.put(item, sNameIdx.get(item));
                sMapAbb.put(item, sAbbIdx.get(item));
                sPresent = sPresent + sNameIdx.get(item) +"\n";
            }
        }
        present.setText(sPresent);
        absent.setText(sAbsent);
        if (sMapNames.size() == 0){gotAll = 2;}
        return gotAll;
    }

    public static void initDisplay(Context mContext, View view){
        Resources AppRes = mContext.getResources();
        varNames = AppRes.getStringArray(R.array.S_VAR);

        for (Integer item : sMapAbb.keySet()) {
            String pref = sMapAbb.get(item);
            List<TextView> tvCodes = new ArrayList<>();
            for (String element : varNames) {
                String tvID = pref + element;
                int tvi = AppRes.getIdentifier(tvID, "id", mContext.getPackageName());
                if (tvi != 0) {                                                                         // tvi = 0: no resource with that value
                    tvCodes.add((TextView) view.findViewById(tvi));
                }
            }
            textViewCodes.put(item, tvCodes);
        }
    }

    public static void setEpoch(){
        epoch = elapsedRealtime()+150;                              //Allow time for all threads to start - may nead tweaking - check on real device
    }
    public static long getEpoch(){
        return epoch;
    }

    public static void setFilters(BroadcastReceiver usbReciever, Context SAcontext) {                     //USB Filter configuration
        IntentFilter filter = new IntentFilter();
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_NO_USB);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_DISCONNECTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(com.fred.tandq.usbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        SAcontext.registerReceiver(usbReciever, filter);
    }

}