package com.studio.artaban.anaglyph3d.transfer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by pascal on 20/03/16.
 * Bluetooth Helper
 */
public class Bluetooth {

    public enum Status {
        DISABLED,   // Bluetooth not supported or disabled
        READY,      // Bluetooth ready (enabled & registered)
        LISTENING,  // Master mode
        CONNECTING, // Slave mode
        CONNECTED   // Processing connection
    }

    private BluetoothAdapter mAdapter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) // Not already paired
                    synchronized (mDevices) {
                        mDevices.add(device.getName() + Constants.BLUETOOTH_DEVICES_SEPARATOR +
                                device.getAddress());
                    }
            }
        }
    };

    private final ArrayList<String> mDevices = new ArrayList<>();
    private final ByteArrayOutputStream mReceived = new ByteArrayOutputStream();
    private Status mStatus = Status.DISABLED;
    private String mRemoteDevice;

    private class ListenThread extends Thread { //////

        private final boolean mSecure;
        private final BluetoothServerSocket mSocket;

        public ListenThread(boolean secure, String uuid, String name) {

            mSecure = secure;
            BluetoothServerSocket socket = null;
            try {
                if (secure) socket = mAdapter.listenUsingRfcommWithServiceRecord(name, UUID.fromString(uuid));
                else socket = mAdapter.listenUsingInsecureRfcommWithServiceRecord(name, UUID.fromString(uuid));
            }
            catch (IOException e) {
                Logs.add(Logs.Type.W, "Failed to listen " + ((secure) ? "secure" : "insecure") +
                        " socket: " + e.toString());
            }
            mSocket = socket;
        }

        @Override public void run() {

            if (mSocket == null)
                return;

            while (mStatus == Status.LISTENING) {

                try {
                    BluetoothSocket socket = mSocket.accept();
                    if (socket != null) {

                        BluetoothDevice device = socket.getRemoteDevice();
                        mRemoteDevice = device.getName() + Constants.BLUETOOTH_DEVICES_SEPARATOR +
                                device.getAddress();
                        if (mProcessing != null) {

                            mProcessing.cancel();
                            mProcessing = null;
                        }
                        mProcessing = new ProcessThread(socket);
                        mStatus = Status.CONNECTED;
                        mProcessing.start();
                    }
                }
                catch (IOException e) {

                    Logs.add(Logs.Type.W, "Listening - " + mSocket.toString() + " - error: " + e.toString());
                    mStatus = Status.READY;
                    break;
                }
            }
        }
        public void cancel() {

            try {
                if (mSocket != null)
                    mSocket.close();
            }
            catch (IOException e) {
                Logs.add(Logs.Type.W, "Failed to close " + ((mSecure) ? "secure" : "insecure") +
                        " socket (listen): " + e.toString());
            }
        }
    };
    private class ConnectThread extends Thread { //////

        private final boolean mSecure;
        private final BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, boolean secure, String uuid) {

            mSecure = secure;
            BluetoothSocket socket = null;
            try {
                if (secure) socket = device.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
                else socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid));
            }
            catch (IOException e) {
                Logs.add(Logs.Type.W, "Failed to create " + ((secure) ? "secure" : "insecure") +
                        " socket: " + e.toString());
            }
            mSocket = socket;
            mRemoteDevice = device.getName() + Constants.BLUETOOTH_DEVICES_SEPARATOR + device.getAddress();
        }

        @Override public void run() {

            if (mSocket == null)
                return;

            mAdapter.cancelDiscovery();
            try {
                mSocket.connect();
                if (mProcessing != null) {

                    mProcessing.cancel();
                    mProcessing = null;
                }
                mProcessing = new ProcessThread(mSocket);
                mStatus = Status.CONNECTED;
                mProcessing.start();
            }
            catch (IOException e) {

                Logs.add(Logs.Type.W, "Connectivity - " + mSocket.toString() + " - failed: " + e.toString());
                mStatus = Status.READY; // Not connected
            }
        }
        public void cancel() {

            try {
                if (mSocket != null)
                    mSocket.close();
            }
            catch (IOException e) {
                Logs.add(Logs.Type.W, "Failed to close " + ((mSecure) ? "secure" : "insecure") + " socket (connect): " +
                        e.toString());
            }
        }
    };
    private class ProcessThread extends Thread { //////

        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ProcessThread(BluetoothSocket socket) {

            mSocket = socket;
            InputStream in = null;
            OutputStream out = null;
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }
            catch (IOException e) {
                Logs.add(Logs.Type.E, "Failed to create I/O stream: " + e.toString());
            }
            mInStream = in;
            mOutStream = out;
        }

        @Override public void run() {

            byte[] buffer = new byte[1024];
            while (mStatus == Status.CONNECTED) {
                try {
                    int bytes = mInStream.read(buffer);
                    if (bytes > 0)
                        synchronized (mReceived) { mReceived.write(buffer, 0, bytes); }
                }
                catch (IOException e) {

                    Logs.add(Logs.Type.W, "Socket disconnected: " + e.toString());
                    mStatus = Status.READY;
                    break;
                }
            }
        }
        public void cancel() {

            try { mSocket.close(); }
            catch (IOException e) {
                Logs.add(Logs.Type.W, "Failed to close socket (process): " + e.toString());
            }
        }

        public void write(byte[] buffer, int len) {

            if (mOutStream == null)
                return;

            try { mOutStream.write(buffer, 0, len); }
            catch (IOException e) {
                Logs.add(Logs.Type.E, "Failed to write: " + e.toString());
            }
        }
    };

    //////
    public Status getStatus() { return mStatus; }
    public String getRemoteDevice() { return mRemoteDevice; }

    //
    public void discover() {

        if (mStatus == Status.DISABLED)
            return;

        if (mAdapter.isDiscovering())
            mAdapter.cancelDiscovery();
        mAdapter.startDiscovery();
    }
    public boolean isDiscovering() {
        return (mAdapter != null) && (mAdapter.isDiscovering());
    }
    public String getDevice(short index) {
        synchronized (mDevices) {

            if ((mStatus == Status.DISABLED) || (index >= mDevices.size()))
                return null;

            return mDevices.get(index);
        }
    }

    //
    private ListenThread mListening;
    private ConnectThread mConnecting;
    private ProcessThread mProcessing;

    public boolean listen(boolean secure, String uuid, String name) {

        if (mStatus != Status.READY) {
            Logs.add(Logs.Type.W, "Failed to listen: Wrong " + mStatus + " status");
            return false;
        }
        if (mListening != null) {

            synchronized (mListening) {
                mListening.cancel();
                mListening = null;
            }
        }
        mListening = new ListenThread(secure, uuid, name);
        mStatus = Status.LISTENING;
        mListening.start();
        return true;
    }
    public boolean connect(boolean secure, String uuid, String address) {

        if (mStatus != Status.READY) {
            Logs.add(Logs.Type.W, "Failed to connect: Wrong " + mStatus + " status");
            return false;
        }
        if (mConnecting != null) {

            synchronized (mConnecting) {
                mConnecting.cancel();
                mConnecting = null;
            }
        }
        mConnecting = new ConnectThread(mAdapter.getRemoteDevice(address), secure, uuid);
        mStatus = Status.CONNECTING;
        mConnecting.start();
        return true;
    }
    public void reset() {

        switch (mStatus) {
            case LISTENING: {

                mStatus = Status.READY;
                synchronized (mListening) {
                    mListening.cancel();
                    mListening = null;
                }
                break;
            }
            case CONNECTING: {

                synchronized (mConnecting) {
                    mConnecting.cancel();
                    mConnecting = null;
                }
                mStatus = Status.READY;
                break;
            }
            case CONNECTED: {

                mStatus = Status.READY;
                synchronized (mProcessing) {
                    mProcessing.cancel();
                    mProcessing = null;
                }
                break;
            }
        }
    }

    //
    public boolean write(byte[] buffer, int len) {

        if (mStatus != Status.CONNECTED) {
            Logs.add(Logs.Type.W, "Failed to write: Wrong " + mStatus + " status");
            return false;
        }
        ProcessThread process;
        synchronized (mProcessing) { process = mProcessing; }
        process.write(buffer, len);
        return true;
    }
    public synchronized int read(ByteArrayOutputStream buffer) {

        if (mStatus != Status.CONNECTED) {
            Logs.add(Logs.Type.W, "Failed to read: Wrong " + mStatus + " status");
            return Constants.NO_DATA;
        }
        int bytes = mReceived.size();
        if (bytes > 0) {

            try {
                buffer.write(mReceived.toByteArray());
                mReceived.reset();
            }
            catch (IOException e) {
                Logs.add(Logs.Type.E, "Failed to read: " + e.toString());
            }
        }
        return bytes;
    }

    //
    public boolean initialize() {

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Logs.add(Logs.Type.W, "Bluetooth not supported");
            return false;
        }
        if (!enable())
            return false;

        mDevices.clear();

        Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
        if (devices.size() > 0)
            for (BluetoothDevice device : devices)
                mDevices.add(device.getName() + "\n" + device.getAddress());

        mStatus = Status.READY;
        return true;
    }
    public boolean enable() {
        if (mAdapter == null)
            return false; // Not initialized

        if ((!mAdapter.isEnabled()) && (!mAdapter.enable())) {
            Logs.add(Logs.Type.E, "Failed to enable Bluetooth");
            return false;
        }
        return true;
    }
    public void register(Context context) {
        context.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }
    public void unregister(Context context) { context.unregisterReceiver(mReceiver); }
    public void release() {
        if (mStatus == Status.DISABLED)
            return;

        reset();
        if (mAdapter.isDiscovering())
            mAdapter.cancelDiscovery();

        mStatus = Status.DISABLED; // Needed when recreating activity (static)
    }
}
