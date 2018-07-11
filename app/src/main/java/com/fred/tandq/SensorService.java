package com.fred.tandq;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.util.Log;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Map;

import static com.fred.tandq.nodeController.ACTION_CDC_DRIVER_NOT_WORKING;
import static com.fred.tandq.nodeController.ACTION_NO_USB;
import static com.fred.tandq.nodeController.ACTION_USB_ATTACHED;
import static com.fred.tandq.nodeController.ACTION_USB_DETACHED;
import static com.fred.tandq.nodeController.ACTION_USB_DEVICE_NOT_WORKING;
import static com.fred.tandq.nodeController.ACTION_USB_NOT_SUPPORTED;
import static com.fred.tandq.nodeController.ACTION_USB_PERMISSION;
import static com.fred.tandq.nodeController.ACTION_USB_PERMISSION_NOT_GRANTED;
import static com.fred.tandq.nodeController.ACTION_USB_READY;
import static com.fred.tandq.nodeController.BAUD_RATE;


public class SensorService extends Service {
    //tag for logging
    private static final String TAG = SensorService.class.getSimpleName()+"SF2Debug";
    //flag for logging
    private static boolean mLogging = false;

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private boolean serialPortConnected;
    private nodeController nC;
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
                        connection = usbManager.openDevice(device);
                        serialPortConnected = true;
                        new ConnectionThread(nC.getusbCallback()).start();
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

    @Override
    public void onCreate() {
        if (mLogging){
            String logstring = "Sensor Service Created";
            Log.d(TAG, logstring);
        }
        nC = (nodeController) getApplicationContext();
        serialPortConnected = false;
        setFilters();
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        findSerialPortDevice();                               //TODO: Sort out usb initialisation
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLogging){
            String logstring = "Sensor Service Started";
            Log.d(TAG, logstring);
        }
        initUSB();                                                          //TODO:  See previous
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
        registerReceiver(usbReciever, filter);
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