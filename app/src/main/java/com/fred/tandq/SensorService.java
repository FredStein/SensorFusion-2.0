package com.fred.tandq;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Map;


public class SensorService extends Service {
    //tag for logging
    private static final String TAG = SensorService.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private static boolean mLogging = true;

    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    public static final String ACTION_CDC_DRIVER_NOT_WORKING = "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int BAUD_RATE = 115200; // BaudRate. Change this value if you need
//    public static boolean SERVICE_CONNECTED = false;

//    private IBinder binder = new UsbBinder();

    private Context context;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;

    private boolean serialPortConnected;
    //Read callback is in usbRunnable.  Service is not recieving data and passing to activity - usb runnable is

//    private LinkedBlockingQueue sDataQ;
    /*
     * Different notifications from OS will be received here (USB attached, detached, permission responses...)
     */
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
//                        Intent pIntent = new Intent(ACTION_USB_PERMISSION_GRANTED);   More interested in USB Ready sent by Connection Thread
//                        context.sendBroadcast(pIntent);
                        connection = usbManager.openDevice(device);
                        serialPortConnected = true;
                        new ConnectionThread().start();
                        nodeController.getNodeCtrl().setExtSensors();
                        nodeController.getThread(70000).start();           //Start thread
                    }
                    else {          //USB connection not accepted. Send an Intent to the Activities
                        Log.d(TAG, "ACTION PERMISSION NOT GRANTED");
                        context.sendBroadcast(new Intent(ACTION_USB_PERMISSION_NOT_GRANTED));
                    }
                    break;
                case ACTION_USB_ATTACHED: // USB CONNECTED
                    if (!serialPortConnected) findSerialPortDevice();        // A USB device has been attached. Try to open it as a Serial port
                    break;
/*                case ACTION_NO_USB:
                    Log.d(TAG, "No USB MSG?");
 //                   nodeController.removeActiveSensor(70000);
                    serialPortConnected = false;
 //                   serialPort.close();
                    break;*/
                case ACTION_USB_DETACHED: // USB DISCONNECTED
                    Log.d(TAG, "ACTION USB DISCONNECTED");
                    context.sendBroadcast(new Intent(ACTION_USB_DISCONNECTED));
                    nodeController.removeActiveSensor(70000);
                    serialPortConnected = false;
                    serialPort.close();
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        if (mLogging){
            String logstring = "Sensor Service Created";
            Log.d(TAG, logstring);
        }
        SensorManager sMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.context = this;
        this.serialPortConnected = false;
//        SensorService.SERVICE_CONNECTED = true;
        setFilters();
        Log.d(TAG, "USB Status: " + Boolean.toString(serialPortConnected));
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
//        findSerialPortDevice();

/*        for (mySensor item : nodeController.getSensors().values()){
            Log.d(TAG, "Sensors: " + item.getName());
            if (!item.getName().equals("USB")) {
                SensorRunnable sr = new SensorRunnable(item, sMgr,sDataQ);
                sr.setRunning(true);
                SensorThreads.put(item.getType(), new Thread(sr));     //Instantiate thread
            }else if(item.getName().equals("USB")){
                usbRunnable ur = new usbRunnable(item,sDataQ);
                ur.setRunning(true);
                SensorThreads.put(item.getType(), new Thread(ur));
            }
        }*/
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLogging){
            String logstring = "Sensor Service Started";
            Log.d(TAG, logstring);
        }
        initUSB();

        Log.d(TAG, "USB Status: " + Boolean.toString(serialPortConnected));

        nodeController.getNodeCtrl().getAggregator().setRunning(true);
        new Thread(nodeController.getNodeCtrl().getAggregator()).start();

        for (Integer item : nodeController.getNodeCtrl().getSensors().keySet()) {
            nodeController.getNodeCtrl().getThread(item).start();           //Start thread
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mLogging){
            String logstring = "Sensor Service Bound";
            Log.d(TAG, logstring);
        }
        return null;
    }

    /*USB Elements*/

    public void setFilters() {                     //USB Filter configuration
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        filter.addAction(ACTION_NO_USB);
        registerReceiver(usbReciever, filter);
    }

    public class UsbBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
    }

    private void findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        Log.d(TAG,"fSPD USB Devices present: " + Boolean.toString(!usbDevices.isEmpty()));
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();
                if (deviceVID != 0x1d6b && (devicePID != 0x0001 || devicePID != 0x0002 || devicePID != 0x0003)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestUserPermission();
                    Log.d(TAG, "fSPD Ping");
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep)
                    break;
            }
            if (!keep) {
                // There is no USB device connected (but usb hosts were listed). Send an intent to MainActivity.
                Log.d(TAG, "fSPD. No USB device connected (but usb hosts were listed)");
/*                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);*/
            }
        } else {
            // There is no USB device connected. Send an intent to MainActivity
            Log.d(TAG, "fSPD. No USB device connected.");
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }

    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    private class ConnectionThread extends Thread {
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
                    serialPort.read(nodeController.getusbCallback());
                    // Everything went as expected. Send an intent to MainActivity
                    Intent intent = new Intent(ACTION_USB_READY);
                    Log.d(TAG, "ACTION USB READY");
                    context.sendBroadcast(intent);
                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if (serialPort instanceof CDCSerialDevice) {
                        Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                        Log.d(TAG, "ACTION CDC NOT WORKING");
                        context.sendBroadcast(intent);
                    } else {
                        Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                        Log.d(TAG, "ACTION USB NOT WORKING");
                        context.sendBroadcast(intent);
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                Log.d(TAG, "ACTION USB NOT SUPPORTED");
                context.sendBroadcast(intent);
            }
        }
    }

    private void initUSB(){
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        Log.d(TAG,"iUSB USB Devices present: " + Boolean.toString(!usbDevices.isEmpty()));
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();
                if (deviceVID != 0x1d6b && (devicePID != 0x0001 || devicePID != 0x0002 || devicePID != 0x0003)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestUserPermission();
                    Log.d(TAG, "iUSB Ping");
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
                // There is no USB device connected (but usb hosts were listed). Send an intent to MainActivity.
                Log.d(TAG, "iUSB. No USB device connected (but usb hosts were listed)");
/*                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);*/
            }
        } else {
            // There is no USB device connected. Send an intent to MainActivity
            Log.d(TAG, "iUSB. No USB device connected");
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }
}