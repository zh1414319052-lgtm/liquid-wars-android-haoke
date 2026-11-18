// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class TcpTransport {
    private TcpTransport() {}
    public static GameTransport.Connection wrap(Socket socket) throws IOException {
        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();
        return new GameTransport.Connection() {
            @Override public InputStream input() { return input; }
            @Override public OutputStream output() { return output; }
            @Override public void close() throws IOException { socket.close(); }
        };
    }

    public static GameTransport.ListenerFactory listen(int port) {
        return () -> {
            ServerSocket server = new ServerSocket();
            try {
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress(port));
                return new GameTransport.Listener() {
                    @Override public GameTransport.Connection accept() throws IOException {
                        while (true) {
                            Socket socket = server.accept();
                            try {
                                if (LanPeer.isOnLink(socket.getInetAddress())) return wrap(socket);
                            } catch (IOException failed) {
                                GameTransport.close(socket);
                                throw failed;
                            }
                            GameTransport.close(socket);
                        }
                    }
                    @Override public void close() throws IOException { server.close(); }
                };
            } catch (IOException failed) { GameTransport.close(server); throw failed; }
        };
    }

    public static final class Connector implements GameTransport.Connector {
        private final String address;
        private final int port;
        private Socket socket;
        private boolean closed;
        public Connector(String address, int port) { this.address = address; this.port = port; }
        @Override public GameTransport.Connection connect() throws IOException {
            InetAddress peer = LanPeer.parse(address);
            if (!LanPeer.isOnLink(peer)) throw new IOException("Peer is outside the local network");
            Socket pending = new Socket();
            synchronized (this) {
                if (closed) { pending.close(); throw new IOException("Connection cancelled"); }
                socket = pending;
            }
            try {
                pending.connect(new InetSocketAddress(peer, port), 3000);
                return wrap(pending);
            } catch (IOException failure) {
                GameTransport.close(pending);
                throw failure;
            }
        }
        @Override public synchronized void close() throws IOException {
            closed = true;
            if (socket != null) socket.close();
        }
    }
}
