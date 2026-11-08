// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Legacy discovery datagrams only. Gameplay TCP bytes/ports remain unchanged. */
public final class LegacyLanDiscovery {
    static final byte[] QUERY = "9287349213846".getBytes(StandardCharsets.US_ASCII);
    static final byte[] RESPONSE = "2138479287348".getBytes(StandardCharsets.US_ASCII);
    private LegacyLanDiscovery() {}

    public static byte[] response(String name) {
        byte[] text = LanRoom.cleanName(name).getBytes(StandardCharsets.UTF_8);
        byte[] packet = Arrays.copyOf(RESPONSE, RESPONSE.length + text.length);
        System.arraycopy(text, 0, packet, RESPONSE.length, text.length);
        return packet;
    }

    public static LanRoom decode(byte[] bytes, int length, String ip, int gamePort) {
        if (length <= RESPONSE.length || length > bytes.length || length > 1024) return null;
        for (int i = 0; i < RESPONSE.length; i++) if (bytes[i] != RESPONSE[i]) return null;
        try {
            String name = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, RESPONSE.length, length - RESPONSE.length)).toString();
            return new LanRoom(name, ip, gamePort);
        } catch (CharacterCodingException | IllegalArgumentException invalid) {
            return null;
        }
    }

    public static final class Publisher implements Closeable {
        private final DatagramSocket socket;
        private volatile boolean closed;

        public Publisher(int discoveryPort, String name) throws IOException {
            socket = new DatagramSocket(discoveryPort);
            byte[] reply = response(name);
            Thread worker = new Thread(() -> {
                byte[] bytes = new byte[128];
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                try {
                    while (!closed) {
                        packet.setLength(bytes.length);
                        socket.receive(packet);
                        if (packet.getLength() != QUERY.length) continue;
                        boolean matches = true;
                        for (int i = 0; i < QUERY.length; i++) matches &= bytes[i] == QUERY[i];
                        if (matches) socket.send(new DatagramPacket(reply, reply.length,
                                packet.getAddress(), packet.getPort()));
                    }
                } catch (IOException stopped) {
                    // Closing this instance's socket releases receive immediately.
                } finally { socket.close(); }
            }, "LAN room publisher");
            worker.setDaemon(true);
            worker.start();
        }

        public int localPort() { return socket.getLocalPort(); }
        @Override public void close() { closed = true; socket.close(); }
    }

    public static final class Browser implements Closeable {
        private final DatagramSocket socket;
        private volatile boolean closed;

        public Browser(int discoveryPort, int gamePort, Supplier<List<InetAddress>> destinations,
                       Consumer<LanRoom> onRoom) throws IOException {
            socket = new DatagramSocket(); // Ephemeral client port: never conflicts with a host.
            socket.setBroadcast(true);
            socket.setSoTimeout(200);
            Thread worker = new Thread(() -> {
                byte[] bytes = new byte[1024];
                DatagramPacket incoming = new DatagramPacket(bytes, bytes.length);
                long nextQuery = 0;
                try {
                    while (!closed) {
                        long now = System.nanoTime() / 1000000;
                        if (now >= nextQuery) {
                            nextQuery = now + 1000;
                            for (InetAddress address : destinations.get()) {
                                try { socket.send(new DatagramPacket(QUERY, QUERY.length, address, discoveryPort)); }
                                catch (IOException unavailableInterface) { /* Try the other local broadcasts. */ }
                            }
                        }
                        try {
                            incoming.setLength(bytes.length); // A short packet must not truncate the next one.
                            socket.receive(incoming);
                            if (closed) break;
                            LanRoom room = decode(bytes, incoming.getLength(),
                                    incoming.getAddress().getHostAddress(), gamePort);
                            if (room != null) onRoom.accept(room);
                        } catch (SocketTimeoutException waiting) { }
                    }
                } catch (IOException stopped) {
                    // A stop/restart cannot close any newer session's socket.
                } finally { socket.close(); }
            }, "LAN room browser");
            worker.setDaemon(true);
            worker.start();
        }

        @Override public void close() { closed = true; socket.close(); }
    }
}
