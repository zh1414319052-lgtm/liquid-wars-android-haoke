//    This file is part of Liquid Wars.
//
//    Copyright (C) 2013 Henry Shepperd (hshepperd@gmail.com)
//
//    Liquid Wars is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Liquid Wars is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Liquid Wars.  If not, see <http://www.gnu.org/licenses/>.

package com.xenris.liquidwarsos;

import com.xenris.liquidwarsos.net.GameTransport;
import com.xenris.liquidwarsos.net.GameWire;
import com.xenris.liquidwarsos.net.TcpTransport;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Client extends Thread {
    private volatile ClientCallbacks clientCallbacks;
    private final String address;
    private final GameTransport.Connector connector;
    private volatile GameTransport.Connection connection;
    private volatile boolean destroyCalled;

    public Client(ClientCallbacks callbacks, String ip, int port) {
        this(callbacks, ip, new TcpTransport.Connector(ip, port));
    }

    public Client(ClientCallbacks callbacks, String address, GameTransport.Connector connector) {
        super("Liquid Wars client");
        this.clientCallbacks = callbacks;
        this.address = address;
        this.connector = connector;
    }

    @Override public void run() {
        boolean connected = false;
        ScheduledExecutorService deadline = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "Connection deadline");thread.setDaemon(true);return thread;
        });
        ScheduledFuture<?> timeout = deadline.schedule(() -> GameTransport.close(connector),
                connector.timeoutMillis(), TimeUnit.MILLISECONDS);
        try {
            if (destroyCalled) return;
            connection = connector.connect();
            if (destroyCalled) return;
            InputStream input = connection.input();
            int id = input.read();
            if (id < 1) throw new IOException("Missing peer handshake");
            timeout.cancel(false);
            connected = true;
            ClientCallbacks callbacks = clientCallbacks;
            if (callbacks != null && !destroyCalled) callbacks.onServerConnectionMade(id, address);
            while (!destroyCalled) {
                int[] args = GameWire.read(input);
                if (args == null) break;
                callbacks = clientCallbacks;
                if (callbacks != null && !destroyCalled) callbacks.onServerMessageReceived(args.length, args);
            }
        } catch (IOException failed) {
            // One terminal callback below distinguishes failed setup from a lost connection.
        } finally {
            timeout.cancel(false);
            deadline.shutdownNow();
            GameTransport.close(connection);
            GameTransport.close(connector);
            ClientCallbacks callbacks = clientCallbacks;
            if (callbacks != null && !destroyCalled) {
                if (connected) callbacks.onServerConnectionClosed(address);
                else callbacks.onServerConnectionFailed(address);
            }
        }
    }

    public void send(int argc, int[] args) {
        GameTransport.Connection active = connection;
        if (destroyCalled || active == null) return;
        try { GameWire.write(active.output(), argc, args); }
        catch (IOException failed) { GameTransport.close(active); }
    }

    public void send(int first, int second) { send(2, new int[]{first, second}); }

    public void destroy() {
        destroyCalled = true;
        clientCallbacks = null;
        GameTransport.close(connector); // Also cancels a pending RFCOMM connect/handshake.
        GameTransport.close(connection);
    }

    public void setCallbacks(ClientCallbacks callbacks) { clientCallbacks = callbacks; }

    public interface ClientCallbacks {
        void onServerMessageReceived(int argc, int[] args);
        void onServerConnectionMade(int id, String address);
        void onServerConnectionFailed(String address);
        void onServerConnectionClosed(String address);
    }
}
