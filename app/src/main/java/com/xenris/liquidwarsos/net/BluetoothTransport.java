// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.net;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public final class BluetoothTransport {
    public static final UUID SERVICE_UUID = UUID.fromString("c83b0510-5655-4b9c-94d8-9b13808340a0");
    public static final String EXTRA_MODE = "bluetooth";
    private BluetoothTransport() {}

    private static GameTransport.Connection wrap(BluetoothSocket socket) throws IOException {
        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();
        return new GameTransport.Connection() {
            @Override public InputStream input() { return input; }
            @Override public OutputStream output() { return output; }
            @Override public void close() throws IOException { socket.close(); }
        };
    }

    public static GameTransport.ListenerFactory listen(BluetoothAdapter adapter) {
        return new GameTransport.ListenerFactory() {
            @Override public int clientLimit() { return 1; }
            @Override public GameTransport.Listener open() throws IOException {
                try {
                    if (adapter == null || !adapter.isEnabled()) throw new IOException("Bluetooth disabled");
                    BluetoothServerSocket server = adapter.listenUsingRfcommWithServiceRecord("Liquid Wars OS", SERVICE_UUID);
                    return new GameTransport.Listener() {
                        @Override public GameTransport.Connection accept() throws IOException {
                            BluetoothSocket socket = server.accept();
                            try { return wrap(socket); }
                            catch (IOException failed) { GameTransport.close(socket); throw failed; }
                        }
                        @Override public void close() throws IOException { server.close(); }
                    };
                } catch (SecurityException denied) { throw new IOException("Bluetooth permission denied", denied); }
            }
        };
    }

    public static final class Connector implements GameTransport.Connector {
        private final BluetoothAdapter adapter;
        private final String address;
        private BluetoothSocket socket;
        private boolean closed;
        public Connector(BluetoothAdapter adapter, String address) { this.adapter = adapter; this.address = address; }
        @Override public long timeoutMillis() { return 30000; }
        @Override public GameTransport.Connection connect() throws IOException {
            try {
                if (adapter == null || !adapter.isEnabled()) throw new IOException("Bluetooth disabled");
                BluetoothSocket pending = adapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(SERVICE_UUID);
                synchronized (this) {
                    if (closed) { pending.close(); throw new IOException("Connection cancelled"); }
                    socket = pending;
                }
                pending.connect(); // Secure RFCOMM: Android handles pairing confirmation.
                return wrap(pending);
            } catch (SecurityException | IllegalArgumentException denied) {
                throw new IOException("Bluetooth unavailable or permission denied", denied);
            }
        }
        @Override public synchronized void close() throws IOException {
            closed = true;
            if (socket != null) socket.close();
        }
    }
}
