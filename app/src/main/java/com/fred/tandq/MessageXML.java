package com.fred.tandq;

import android.util.Log;

import java.util.HashMap;
import java.util.Set;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_GRAVITY;
import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.hardware.Sensor.TYPE_LINEAR_ACCELERATION;
import static android.hardware.Sensor.TYPE_ROTATION_VECTOR;
import static com.fred.tandq.appState.TYPE_USB;
import static com.fred.tandq.appState.getSensorName;
import static com.fred.tandq.appState.getSensorType;
import static com.fred.tandq.appState.getSensors;
import static com.fred.tandq.appState.nodeID;

/**
 * Created by Fred Stein on 14/04/2017.
 */

class MessageXML {
    //tag for logging
    private static final String TAG = MessageXML.class.getSimpleName();
    //flag for logging
    private boolean mLogging = false ;

    private static Set<Integer> liveSensors;
    private static HashMap<String, String> Accelerometer = new HashMap<>();
    private static HashMap<String, String> Gravity = new HashMap<>();
    private static HashMap<String, String> Gyroscope = new HashMap<>();
    private static HashMap<String, String> LinearAcceleration = new HashMap<>();
    private static HashMap<String, String> RotationVector = new HashMap<>();
    private static HashMap<String, String> USB = new HashMap<>();

    MessageXML() {

        liveSensors = getSensors();
    }

    public void setVal(HashMap<String, String> sMsg) {
        for (String item: sMsg.keySet()){
            Log.i(TAG, item + "," + sMsg.get(item));
        }
        switch (getSensorType(sMsg.get("Sensor"))) {
            case TYPE_ACCELEROMETER:
                for (String item : sMsg.keySet()) {
                    if (item != "Timestamp" && item != "Sensor") {
                        Accelerometer.put(item, sMsg.get(item));
                    }
                }break;
            case TYPE_GRAVITY:
                for (String item : sMsg.keySet()) {
                    if (item != "Timestamp" && item != "Sensor") {
                        Gravity.put(item, sMsg.get(item));
                    }
                }break;
            case TYPE_GYROSCOPE:
                for (String item : sMsg.keySet()) {
                    if (item != "Timestamp" && item != "Sensor") {
                        Gyroscope.put(item, sMsg.get(item));
                    }
                }break;
            case TYPE_LINEAR_ACCELERATION:
                for (String item : sMsg.keySet()) {
                    if (item != "Timestamp" && item != "Sensor") {
                        LinearAcceleration.put(item, sMsg.get(item));
                    }
                }break;
            case TYPE_ROTATION_VECTOR:
                for (String item : sMsg.keySet()) {
                    if (item != "Timestamp" && item != "Sensor") {
                        RotationVector.put(item, sMsg.get(item));
                    }
                }break;
            case TYPE_USB:
                for (String item : sMsg.keySet()) {
                    if (item != "Timestamp" && item != "Sensor") {
                        USB.put(item, sMsg.get(item));
                    }
                }break;
        }
    }
    private String TimeStamp;                               //TODO: Outward ts should be clock time
                                                            //Presently elapsedRealtime
    public String getTimeStamp() {
        return TimeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        TimeStamp = timeStamp;
    }

    private HashMap<Integer, Boolean> status = new HashMap<>();

    public boolean isComplete() {
        Boolean complete = true;
        for (int item : liveSensors) {
            switch (item) {
                case TYPE_ACCELEROMETER:
                    status.put(item, !Accelerometer.keySet().isEmpty());
                    break;
                case TYPE_GRAVITY:
                    status.put(item, !Gravity.keySet().isEmpty());
                    break;
                case TYPE_GYROSCOPE:
                    status.put(item, !Gyroscope.keySet().isEmpty());
                    break;
                case TYPE_LINEAR_ACCELERATION:
                    status.put(item, !LinearAcceleration.keySet().isEmpty());
                    break;
                case TYPE_ROTATION_VECTOR:
                    status.put(item, !RotationVector.keySet().isEmpty());
                    break;
                case TYPE_USB:
                    status.put(item, !USB.keySet().isEmpty());
                    break;
            }
            for (Boolean val : status.values()) {
                complete = complete && val;
            }
        }
        return complete;
    }

    public String getXmlString(){
        HashMap<Integer,String> element = new HashMap<>();
        String xml = "<\u003Fxml version=\"1.0\" encoding=\"utf-8\"\u003F>";
        xml = xml + "<Node Name=\"" + nodeID +"\">";
        for (int item : liveSensors) {
            switch (item) {
                case TYPE_ACCELEROMETER:
                    String aStr = "<Sensor Type=\""+ getSensorName(item)+ "\"";
                    for (String sVar : Accelerometer.keySet()){
                        aStr = aStr + " " + sVar + "=\"" + Accelerometer.get(sVar)+"\"";
                    }
                    element.put(item, aStr + "/>");
                    break;
                case TYPE_GRAVITY:
                    String bStr = "<Sensor Type=\""+ getSensorName(item)+ "\"";
                    for (String sVar : Gravity.keySet()){
                        bStr = bStr + " " +sVar + "=\"" + Gravity.get(sVar)+"\"";
                    }
                    element.put(item, bStr + "/>");
                    break;
                case TYPE_GYROSCOPE:
                    String cStr = "<Sensor Type=\""+ getSensorName(item)+ "\"";
                    for (String sVar : Gyroscope.keySet()){
                        cStr = cStr + " " + sVar + "=\"" + Gyroscope.get(sVar)+"\"";
                    }
                    element.put(item, cStr + "/>");
                    break;
                case TYPE_LINEAR_ACCELERATION:
                    String dStr = "<Sensor Type=\""+ getSensorName(item)+ "\"";
                    for (String sVar : LinearAcceleration.keySet()){
                        dStr = dStr + " " + sVar + "=\"" + LinearAcceleration.get(sVar)+"\"";
                    }
                    element.put(item, dStr + "/>");
                    break;
                case TYPE_ROTATION_VECTOR:
                    String eStr = "<Sensor Type=\""+ getSensorName(item)+ "\"";
                    for (String sVar : RotationVector.keySet()){
                        eStr = eStr + " " + sVar + "=\"" + RotationVector.get(sVar)+"\"";
                    }
                    element.put(item, eStr + "/>");
                    break;
                case TYPE_USB:
                    String fStr = "<Sensor Type=\""+ getSensorName(item)+ "\"";
                    for (String sVar : USB.keySet()){
                        fStr = fStr + " " + sVar + "=\"" + USB.get(sVar)+"\"";
                    }
                    element.put(item, fStr + "/>");
                    break;
            }
        }
        for (int item : element.keySet()){
            xml = xml + element.get(item);
        }
        xml = xml + "<Timestamp Time= \"" + TimeStamp + "\"/>";
        //Close for well formed xml
        return xml + "</Node>";
    }
}
/*
<Sensor
        Type="Accelerometer"
                x="123"
                y="123"
                z="123" />

<Sensor
        Type="Gyroscope"
                x="123"
                y="123"
                z="123" />

<Sensor
        Type="Gravity"
                x="123"
                y="123"
                z="123" />

<Sensor
        Type="Linear Acceleration"
                x="123"
                y="123"
                z="123" />

<Sensor
        Type="Rotation Vector"
                x="123"
                y="123"
                z="123"
                w="123"
                r="123"/>

<Timestamp Time="timeval" />
</Node>
*/