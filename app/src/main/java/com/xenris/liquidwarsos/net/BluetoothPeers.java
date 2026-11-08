// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.net;

import com.xenris.liquidwarsos.server.LanRoom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BluetoothPeers {
    public static final class Peer {
        public final String address, name;
        public final boolean paired;
        public Peer(String address, String name, boolean paired) {
            if (address == null || !address.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"))
                throw new IllegalArgumentException("Invalid Bluetooth address");
            this.address = address.toUpperCase(Locale.ROOT);
            this.name = name == null || name.trim().isEmpty() ? this.address : LanRoom.cleanName(name);
            this.paired = paired;
        }
        @Override public String toString() { return name; }
    }
    private final Map<String, Peer> peers = new HashMap<>();
    public void put(Peer peer) {
        if (peers.containsKey(peer.address) || peers.size() < 64) peers.put(peer.address, peer);
    }
    public void clear() { peers.clear(); }
    public List<Peer> snapshot() {
        ArrayList<Peer> list = new ArrayList<>(peers.values());
        list.sort((a, b) -> {
            if (a.paired != b.paired) return a.paired ? -1 : 1;
            int name = a.name.compareToIgnoreCase(b.name);
            return name == 0 ? a.address.compareTo(b.address) : name;
        });
        return list;
    }
}
