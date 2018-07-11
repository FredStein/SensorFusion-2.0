package com.fred.tandq;

import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Map;
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
    private static final boolean mLogging = false;

    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_CDC_DRIVER_NOT_WORKING = "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    public static final String ACTION_SENSORSERVICE_INITIALISED = "Sensor Service UP";
    public static final String ACTION_USB_PERMISSION = "USB_PERMISSION";
    public static final int BAUD_RATE = 115200; // BaudRate. Change this value if you need to.  TODO: Settings page for serial port.

    private Context mApp;
    private boolean sendUDP;
    public boolean isSendUDP() {
        return sendUDP;
    }
    public void setSendUDP(boolean sUDP) {
        sendUDP = sUDP;
    }
    private boolean sensorStatusDisplayed;
    public boolean wasStatusDisplayed() {
        return sensorStatusDisplayed;
    }
    public void setStatusDisplayed(boolean sensorStatusDisplayed) {
        this.sensorStatusDisplayed = sensorStatusDisplayed;
    }
    private int suiteStatus;
    public int getSensorPresence() {
        return suiteStatus;
    }
    private boolean displayData;
    public boolean isDisplayData() {
        return displayData;
    }
    public void setDisplayData(boolean dData) {
        displayData = dData;
    }
    private SharedPreferences sharedPref;
    public SharedPreferences getSpref(){
        return sharedPref;
    }
    private long epoch;
    public void setEpoch(){
        epoch = elapsedRealtime()+150;                              //Allow time for all threads to start - may nead tweaking - check on real device
    }
    public long getEpoch(){
        return epoch;
    }
    private long tickLength;                                        //In milliseconds
    public long getTick(){
        return tickLength;
    }
    public long getHalfTick(){
        return tickLength / 2;
    }
    private String nodeID;
    public String getNodeID(){
        return nodeID;
    }
    private int hubPort;
    public int getPort(){
        return hubPort;
    }
    private String hubIP;
    public String getIP(){
        return hubIP;
    }
    private int listenHint;                               //Preference accepts value in ms which is converted to 'Hint' native unit (microseconds)
    public int getHint(){
        return listenHint;
    }
    private LinkedBlockingQueue sDataQ;
    private SensorManager sm;
    private HashMap<Integer, mySensor> activeSensors;
    public HashMap<Integer, mySensor> getSensors(){
        return activeSensors;
    }
    public int getSensorType(String name){
        for (mySensor sensor : activeSensors.values())
            if (sensor.getName().equals(name)){
                return sensor.getType();
            }
        return 0;
    }
    public void removeActiveSensor(int type){
        activeSensors.remove(type);
        SensorThreads.remove(type);
    }
    private HashMap<Integer, Thread> SensorThreads;
    public Thread getSensorThread (Integer type){
        if (SensorThreads.containsKey(type)){
            return SensorThreads.get(type);
        }else return null;
    }
    private String[] sensorStatus;
    public String[] getSensorStatus() {
        return sensorStatus;
    }
    private XMLAggregator xmlA;
    private Thread xmlT;
    public Thread getAggregator(){
        return xmlT;
    }
    private SensorActivity.sensorHandler sHandler;
    public void setsHandler(SensorActivity.sensorHandler sensorPipe){
        sHandler = sensorPipe;
    }
    public SensorActivity.sensorHandler getsHandler(){
        return sHandler;
    }
    private usbRunnable.usbListener usbCallback;
    public usbRunnable.usbListener getusbCallback() {
        return usbCallback;
    }
    public void setusbCallback(usbRunnable.usbListener Callback) {
        usbCallback = Callback;
    }
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private boolean serialPortConnected;

    private final BroadcastReceiver usbReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_USB_PERMISSION: // USB PERMISSION GRANTED
                    boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if (granted){
                        if(mLogging){
                            String logString = "Permission received";
                            Log.d(TAG, logString);
                        }
                        connection = usbManager.openDevice(device);
                        serialPortConnected = true;
                        new ConnectionThread(getusbCallback()).start();
                    }
                    else {          //USB connection not accepted. Send an Intent to the Activities
                        context.sendBroadcast(new Intent(ACTION_USB_PERMISSION_NOT_GRANTED));
                    }
                    break;
                case ACTION_USB_ATTACHED: // USB CONNECTED
                    if (!serialPortConnected) findSerialPortDevice();        // A USB device has been attached. Try to open it as a Serial port
                    break;
                case ACTION_USB_DETACHED: // USB DISCONNECTED
                    serialPortConnected = false;
                    serialPort.close();
                    break;
            }
        }
    };

    private boolean initialised;

    @Override
    public void onCreate() {
        super.onCreate();
        if (mLogging){
            String logstring = "Node Controller Created";
            Log.d(TAG, logstring);
        }
        mApp = this;
        sensorStatusDisplayed = false;
        suiteStatus = 0;
        initialised = false;                                                //TODO: break out into state control callbacks?
        sendUDP = false;
        displayData = false;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(mApp);
        setEpoch();
        tickLength = Long.parseLong(sharedPref.getString("reportFrequency",mApp.getString(R.string.defaultReport_Freq)));
        nodeID = sharedPref.getString("NodeID",mApp.getString(R.string.defaultNodeID));
        hubPort = Integer.parseInt(sharedPref.getString("Port",mApp.getString(R.string.defaultPort)));
        hubIP = sharedPref.getString("hubAddress",mApp.getString(R.string.defaultHubIP));
        listenHint = Integer.parseInt(sharedPref.getString("sampleFrequency",mApp.getString(R.string.defaultSample_Freq)))*1000;      //Convert to microseconds (See above)
        sDataQ  = new LinkedBlockingQueue();
        sm = (SensorManager)mApp.getSystemService(SENSOR_SERVICE);                                  //TODO: Why the cast
        activeSensors  = new HashMap<>();
        SensorThreads = new HashMap<>();
        sensorStatus = new String[2];
        setInternalSensors(sm,sDataQ,activeSensors,SensorThreads,sensorStatus,suiteStatus);         //TODO: Consider separate function to run threads
        xmlA = new XMLAggregator(sDataQ);
        xmlA.setRunning(true);
        xmlT = new Thread(xmlA);
        xmlT.start();
        for (Integer item : getSensors().keySet()) {
            getSensorThread(item).start();                                                          //Start thread. TODO: Look at type of sensorThreads
        }
        BroadcastReceiver signalRX = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case ACTION_USB_PERMISSION: // USB PERMISSION GRANTED
                        boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                        if (granted){
                            if(mLogging){
                                String logString = "Permission received";
                                Log.d(TAG, logString);
                            }
                            setExtSensors(sDataQ,epoch,tickLength,getHalfTick(),activeSensors,SensorThreads);
                            getSensorThread(70000).start();           //Start thread
                        }
                        break;
                    case ACTION_USB_DETACHED: // USB DISCONNECTED
                        removeActiveSensor(70000);
                        break;
                }
            }
        };
        mApp.startService(new Intent(mApp, SensorService.class));

        serialPortConnected = false;
        setFilters();
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        findSerialPortDevice();                                             //TODO: Sort out usb initialisation these overlap
        initUSB();                                                          //TODO:  See previous

        initialised = true;
    }

    private void setInternalSensors(SensorManager sm, LinkedBlockingQueue sQueue,                               //TODO: This does not need epoch ctc. Why not?
                                    HashMap<Integer, mySensor> activSensors,
                                    HashMap<Integer, Thread> sThreads,
                                    String[] sStatus, int status){                                    //TODO: Generalise for internal / external sensor discovery / setup
        sStatus[0] = mApp.getString(R.string.sensors_present);
        sStatus[1] = mApp.getString(R.string.sensors_absent);
        int temp = 0;
        String[] dims;
        try (XmlResourceParser sN = mApp.getResources().getXml(R.xml.i_sensors)) {
            while (sN.getEventType() != XmlResourceParser.END_DOCUMENT) {
                switch (sN.getEventType()) {
                    case XmlResourceParser.START_DOCUMENT:
                        break;
                    case XmlResourceParser.START_TAG:
                        if (sN.getName().equals("sensor")) {
                            String name = sN.getAttributeValue(null, "name");
                            int type = Integer.parseInt(sN.getAttributeValue(null, "type"));
                            String abbr = sN.getAttributeValue(null, "abbr");
                            temp = type;
                            if (sm.getDefaultSensor(type) != null) {
                                activSensors.put(type, new mySensor(name, type, abbr));
                                SensorRunnable sr = new SensorRunnable(activSensors.get(type), sm, sQueue, epoch, tickLength, getHalfTick(),this);
                                sr.setRunning();
                                sThreads.put(type, new Thread(sr));
                                sStatus[0] = sStatus[0] + name + "\n";
                                if (status != 1) {
                                    status = 2;
                                }
                            } else {
                                sStatus[1] = sStatus[1] + name + "\n";
                                status = 1;
                            }
                        } else if (sN.getName().equals("dimensions")) {
                            int nAtts = sN.getAttributeCount();
                            dims = new String[nAtts];
                            for (int k = 0; k < nAtts; k++) {
                                dims[k] = sN.getAttributeValue(k);
//                                Log.d(TAG, dims[k]);
                            }
                            if (sm.getDefaultSensor(temp) != null) {
                                activSensors.get(temp).setDim(dims);
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

    public void setExtSensors(LinkedBlockingQueue sQueue, Long epoch, Long tickLength, Long halfTick,           //TODO: This needs epoch etc. Why?
                              HashMap<Integer, mySensor> activSensors,
                              HashMap<Integer, Thread> sThreads ){
        //External Sensor(s) configured here. At the moment *There can be only one*
        int temp = 0;
        String[] dims;
        try (XmlResourceParser sN = mApp.getResources().getXml(R.xml.e_sensors)) {
            while (sN.getEventType() != XmlResourceParser.END_DOCUMENT) {
                switch (sN.getEventType()) {
                    case XmlResourceParser.START_DOCUMENT:
                        break;
                    case XmlResourceParser.START_TAG:
                        if (sN.getName().equals("sensor")) {
                            String name = sN.getAttributeValue(null, "name");
                            int type = Integer.parseInt(sN.getAttributeValue(null, "type"));
                            String abbr = sN.getAttributeValue(null, "abbr");
                            temp = type;
                            activSensors.put(type, new mySensor(name, type, abbr));
                            usbRunnable ur = new usbRunnable(activSensors.get(type), sQueue,epoch,tickLength,halfTick, this);
                            ur.setRunning();
                            sThreads.put(type, new Thread(ur));
                        } else if (sN.getName().equals("dimensions")) {
                            int nAtts = sN.getAttributeCount();
                            dims = new String[nAtts];
                            for (int k = 0; k < nAtts; k++) {
                                dims[k] = sN.getAttributeValue(k);
                            }
                            activSensors.get(temp).setDim(dims);
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

    public void setFilters() {                     //USB Filter configuration
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(usbReciever, filter);
    }


    private class ConnectionThread extends Thread {
        usbRunnable.usbListener mListener;
        ConnectionThread(usbRunnable.usbListener usbListener){
            mListener = usbListener;
        }
        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort != null) {
                if (serialPort.open()) {
                    serialPort.setBaudRate(BAUD_RATE);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(mListener);
                    // Everything went as expected. Send an intent to self
                    Intent intent = new Intent(ACTION_USB_READY);
                    sendBroadcast(intent);
                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Activities
                    if (serialPort instanceof CDCSerialDevice) {
                        Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                        sendBroadcast(intent);
                    } else {
                        Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                        sendBroadcast(intent);
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                sendBroadcast(intent);
            }
        }
    }

    private void findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();
                if (deviceVID != 0x1d6b && (devicePID != 0x0001 || devicePID != 0x0002 || devicePID != 0x0003)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestUserPermission();
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep)
                    break;
            }
            if (!keep) {
                // There is no USB device connected (but usb hosts were listed)
            }
        } else {
//Default state & USB Attached/Detached so no need?.                                TODO: Check if serial port disconnects with OS thread handling.
// There is no USB device connected. Send an intent to Activities
/*            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);*/
        }
    }

    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    private void initUSB(){                                             //TODO: This needs sorting out
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();
                if (deviceVID != 0x1d6b && (devicePID != 0x0001 || devicePID != 0x0002 || devicePID != 0x0003)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestUserPermission();
                    keep = false;
/*                    connection = usbManager.openDevice(device);
                    serialPortConnected = true;
                    nodeController.getNodeCtrl().setExtSensors();

                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) {
                            serialPort.setBaudRate(BAUD_RATE);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(nodeController.getusbCallback());
                            // Everything went as expected. Send an intent to MainActivity
                            Intent intent = new Intent(ACTION_USB_READY);
                            context.sendBroadcast(intent);
                        } else {
                            // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                            // Send an Intent to Main Activity
                            if (serialPort instanceof CDCSerialDevice) {
                                Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                                context.sendBroadcast(intent);
                            } else {
                                Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                                context.sendBroadcast(intent);
                            }
                        }
                    } else {
                        // No driver for given device, even generic CDC driver could not be loaded
                        Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                        context.sendBroadcast(intent);
                    }*/
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep)
                    break;
            }
            if (!keep) {
                // There is no USB device connected (but usb hosts were listed). Send an intent to Activities
/*                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);*/
            }
        } else {
            // There is no USB device connected. Send an intent to Activities
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }
}

