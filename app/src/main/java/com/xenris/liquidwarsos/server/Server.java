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

package com.xenris.liquidwarsos.server;

import android.content.Context;
import android.util.Log;
import com.xenris.liquidwarsos.net.GameTransport;
import com.xenris.liquidwarsos.net.TcpTransport;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server implements SubServer.SubServerCallbacks {
    public volatile ServerCallbacks serverCallbacks;
    private final GameTransport.ListenerFactory factory;
    private final CopyOnWriteArrayList<SubServer> peers = new CopyOnWriteArrayList<>();
    private volatile GameTransport.Listener listener;
    private volatile boolean starting;
    private volatile boolean destroyed;
    private long generation;
    private int nextClientId = 1;
    public static int SET_MAP_IMAGE_COMMAND = 0x56;
    public static int TEAM_LIST_COMMAND = 0x57;
    public static int REQUEST_CHANGE_TEAM_COMMAND = 0x58;
    public static int START_GAME_COMMAND = 0x59;

    public Server(Context context, int port) { this(context, TcpTransport.listen(port)); }
    public Server(Context context, GameTransport.ListenerFactory factory) {
        this.serverCallbacks = (ServerCallbacks)context;
        this.factory = factory;
    }

    public synchronized boolean startAccepting() {
        if (destroyed) return false;
        if (listener != null || starting) return true;
        starting = true;
        long ticket = ++generation;
        new Thread(() -> acceptConnections(ticket), "Liquid Wars room listener").start();
        return true; // Ready/failure is asynchronous; callers use onListeningChanged.
    }

    public boolean isStarting() { return starting; }
    public boolean isAccepting() { return listener != null; }
    public int connectedClientCount() { return peers.size(); }
    public int clientCapacity() { return factory.clientLimit(); }

    private void acceptConnections(long ticket) {
        GameTransport.Listener local = null;
        try {
            local = factory.open();
            synchronized (this) {
                if (destroyed || ticket != generation) return;
                listener = local;
                starting = false;
            }
            listeningChanged(true);
            while (!destroyed && listener == local) {
                GameTransport.Connection connection = local.accept();
                if (listener != local || peers.size() >= factory.clientLimit()) {
                    GameTransport.close(connection);
                    continue;
                }
                SubServer peer = new SubServer(this, connection, allocateClientId());
                try {
                    peer.handshake();
                    synchronized (this) {
                        if (destroyed || listener != local) { peer.destroy(); continue; }
                        peers.add(peer);
                    }
                    ServerCallbacks callbacks = serverCallbacks;
                    if (callbacks != null) callbacks.onClientConnected(peer.id);
                    peer.start(); // Initial room messages are sent before inbound callbacks can race them.
                } catch (IOException failedHandshake) {
                    peers.remove(peer);
                    peer.destroy();
                }
            }
        } catch (IOException failed) {
            synchronized (this) {
                if (!destroyed && ticket == generation)
                    Log.w("LiquidWarsLAN", "Room listener stopped/unavailable", failed);
            }
        } finally {
            GameTransport.close(local);
            boolean notify;
            synchronized (this) {
                notify = ticket == generation && !destroyed;
                if (notify) { listener = null; starting = false; }
            }
            if (notify) listeningChanged(false);
        }
    }

    private void listeningChanged(boolean ready) {
        ServerCallbacks callbacks = serverCallbacks;
        if (callbacks != null) callbacks.onListeningChanged(ready);
    }

    private synchronized int allocateClientId() {
        for (int attempt = 0; attempt < 255; attempt++) {
            int candidate = nextClientId;
            nextClientId = candidate == 255 ? 1 : candidate + 1;
            boolean used = false;
            for (SubServer peer : peers) if (peer.id == candidate) used = true;
            if (!used) return candidate;
        }
        throw new IllegalStateException("No free legacy client ID");
    }

    public void stopAccepting() {
        GameTransport.Listener old;
        synchronized (this) {
            generation++;
            starting = false;
            old = listener;
            listener = null;
        }
        GameTransport.close(old);
    }

    public void sendToOne(int id, int argc, int[] args) {
        for (SubServer peer : peers) if (peer.id == id) { peer.send(argc, args); break; }
    }
    public void disconnectPeer(int id) {
        for (SubServer peer : peers) if (peer.id == id) { peer.destroy(); break; }
    }
    public void sendToOne(int id, int first, int second) { sendToOne(id, 2, new int[]{first, second}); }
    public void sendToAll(int argc, int[] args) { for (SubServer peer : peers) peer.send(argc, args); }
    public void sendToAll(int first, int second) { sendToAll(2, new int[]{first, second}); }
    public void sendToAll(int first, int second, int third) { sendToAll(3, new int[]{first, second, third}); }
    @Override public void onSubServerMessageReceived(int id, int argc, int[] args) {
        ServerCallbacks callbacks = serverCallbacks;
        if (callbacks != null) callbacks.onClientMessageReceived(id, argc, args);
    }
    @Override public void onSubServerDisconnect(int id) {
        for (SubServer peer : peers) if (peer.id == id) { peers.remove(peer); break; }
        ServerCallbacks callbacks = serverCallbacks;
        if (callbacks != null) callbacks.onClientDisconnected(id);
    }
    public void destroy() {
        synchronized (this) {
            destroyed = true;
            serverCallbacks = null;
        }
        stopAccepting();
        for (SubServer peer : peers) peer.destroy();
        peers.clear();
    }
    public void setCallbacks(ServerCallbacks callbacks) { serverCallbacks = callbacks; }
    public interface ServerCallbacks {
        void onClientMessageReceived(int id, int argc, int[] args);
        void onClientConnected(int id);
        void onClientDisconnected(int id);
        default void onListeningChanged(boolean ready) {}
    }
}
