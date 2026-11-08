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

import com.xenris.liquidwarsos.net.GameTransport;
import com.xenris.liquidwarsos.net.GameWire;
import java.io.IOException;

public class SubServer extends Thread {
    private final SubServerCallbacks callbacks;
    private final GameTransport.Connection connection;
    public final int id;
    private volatile boolean closed;

    public SubServer(SubServerCallbacks callbacks, GameTransport.Connection connection, int id) {
        super("Liquid Wars peer " + id);
        this.callbacks = callbacks;
        this.connection = connection;
        this.id = id;
    }

    public void handshake() throws IOException {
        connection.output().write(id);
        connection.output().flush();
    }

    @Override public void run() {
        try {
            while (!closed) {
                int[] args = GameWire.read(connection.input());
                if (args == null) break;
                // Keep the original server callback's byte-count convention.
                callbacks.onSubServerMessageReceived(id, args.length * 4, args);
            }
        } catch (IOException disconnected) {
        } finally {
            destroy();
            callbacks.onSubServerDisconnect(id);
        }
    }

    public void destroy() { closed = true; GameTransport.close(connection); }
    public void send(int argc, int[] args) {
        if (closed) return;
        try { GameWire.write(connection.output(), argc, args); }
        catch (IOException failed) { destroy(); }
    }

    public interface SubServerCallbacks {
        void onSubServerMessageReceived(int id, int argc, int[] args);
        void onSubServerDisconnect(int id);
    }
}
