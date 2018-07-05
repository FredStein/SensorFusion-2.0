package com.fred.tandq;

import android.util.Log;

import java.util.HashMap;
import java.util.Set;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_GRAVITY;
import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.hardware.Sensor.TYPE_LINEAR_ACCELERATION;
import static android.hardware.Sensor.TYPE_ROTATION_VECTOR;
import static android.os.SystemClock.elapsedRealtime;
import static java.lang.System.currentTimeMillis;


/**
 * Created by Fred Stein on 14/04/2017.
 */

public class MessageXML {
    //tag for logging
    private static final String TAG = MessageXML.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private boolean mLogging = false ;

    private static final int TYPE_USB = 70000;                                                //TODO: Populate from e_sensors.xml
    private String nodeID;
    private HashMap<Integer,mySensor> activeSensors;
    private Set <Integer> sTypes;
    private HashMap<String, String> Accelerometer = new HashMap<>();
    private HashMap<String, String> Gravity = new HashMap<>();
    private HashMap<String, String> Gyroscope = new HashMap<>();
    private HashMap<String, String> LinearAcceleration = new HashMap<>();
    private HashMap<String, String> RotationVector = new HashMap<>();
    private HashMap<String, String> USB = new HashMap<>();
    private String TimeStamp;                                                                      //Converted to clockTime on XML Build
    public String getTimeStamp() {
        return TimeStamp;
    }
    public void setTimeStamp(String timeStamp) {
        TimeStamp = timeStamp;
    }

    MessageXML() {
        this.activeSensors = nodeController.getNodeCtrl().getSensors();
        this.sTypes = nodeController.getNodeCtrl().getSensors().keySet();
        this.nodeID = nodeController.getNodeCtrl().getNodeID();
    }

    public void setVal(HashMap<String, String> sMsg) {
        if (mLogging) {
            for (String item: sMsg.keySet()){
                Log.d(TAG, item + "," + sMsg.get(item));
            }
        }
        switch (nodeController.getNodeCtrl().getSensorType(sMsg.get("Sensor"))) {
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

    public boolean isComplete() {
        Boolean complete = true;
        HashMap<Integer, Boolean> status = new HashMap<>();
        for(Integer type : sTypes) {
            switch (type) {
                case TYPE_ACCELEROMETER:
                    status.put(type, !Accelerometer.keySet().isEmpty());
                    break;
                case TYPE_GRAVITY:
                    status.put(type, !Gravity.keySet().isEmpty());
                    break;
                case TYPE_GYROSCOPE:
                    status.put(type, !Gyroscope.keySet().isEmpty());
                    break;
                case TYPE_LINEAR_ACCELERATION:
                    status.put(type, !LinearAcceleration.keySet().isEmpty());
                    break;
                case TYPE_ROTATION_VECTOR:
                    status.put(type, !RotationVector.keySet().isEmpty());
                    break;
                case TYPE_USB:
                    status.put(type, !USB.keySet().isEmpty());
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
        StringBuilder xml = new StringBuilder("<\u003Fxml version=\"1.0\" encoding=\"utf-8\"\u003F>");
        xml.append("<Node Name=\"").append(nodeID).append("\">");
        for (int item : activeSensors.keySet()) {
            switch (item) {
                case TYPE_ACCELEROMETER:
                    StringBuilder aStr = new StringBuilder("<Sensor Type=\"" + activeSensors.get(item).getName() + "\"");
                    for (String sVar : Accelerometer.keySet()){
                        aStr.append(" ").append(sVar).append("=\"").append(Accelerometer.get(sVar)).append("\"");
                    }
                    element.put(item, aStr + "/>");
                    break;
                case TYPE_GRAVITY:
                    StringBuilder bStr = new StringBuilder("<Sensor Type=\"" + activeSensors.get(item).getName() + "\"");
                    for (String sVar : Gravity.keySet()){
                        bStr.append(" ").append(sVar).append("=\"").append(Gravity.get(sVar)).append("\"");
                    }
                    element.put(item, bStr + "/>");
                    break;
                case TYPE_GYROSCOPE:
                    StringBuilder cStr = new StringBuilder("<Sensor Type=\"" + activeSensors.get(item).getName() + "\"");
                    for (String sVar : Gyroscope.keySet()){
                        cStr.append(" ").append(sVar).append("=\"").append(Gyroscope.get(sVar)).append("\"");
                    }
                    element.put(item, cStr + "/>");
                    break;
                case TYPE_LINEAR_ACCELERATION:
                    StringBuilder dStr = new StringBuilder("<Sensor Type=\"" + activeSensors.get(item).getName() + "\"");
                    for (String sVar : LinearAcceleration.keySet()){
                        dStr.append(" ").append(sVar).append("=\"").append(LinearAcceleration.get(sVar)).append("\"");
                    }
                    element.put(item, dStr + "/>");
                    break;
                case TYPE_ROTATION_VECTOR:
                    StringBuilder eStr = new StringBuilder("<Sensor Type=\"" + activeSensors.get(item).getName() + "\"");
                    for (String sVar : RotationVector.keySet()){
                        eStr.append(" ").append(sVar).append("=\"").append(RotationVector.get(sVar)).append("\"");
                    }
                    element.put(item, eStr + "/>");
                    break;
                case TYPE_USB:
                    StringBuilder fStr = new StringBuilder("<Sensor Type=\"" + activeSensors.get(item).getName() + "\"");
                    for (String sVar : USB.keySet()){
                        fStr.append(" ").append(sVar).append("=\"").append(USB.get(sVar)).append("\"");
                    }
                    element.put(item, fStr + "/>");
                    break;
            }
        }
        for (int item : element.keySet()){
            xml.append(element.get(item));
        }
        xml.append("<Timestamp Time= \"").append(clockTime(TimeStamp)).append("\"/>");
        //Close for well formed xml
        return xml + "</Node>";
    }

    private String clockTime(String ts){
        Long dt = currentTimeMillis() - elapsedRealtime();                                          //currentTimeMillis IS NOT monotonic
        String cTime = String.valueOf(Long.parseLong(ts)+dt);
        return cTime;                                                                               //TODO: Consider adding dt to output to assess system clock drift
    }                                                                                               //      abd monitor jumps in currentTimeMillis
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