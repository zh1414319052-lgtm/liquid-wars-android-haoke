// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pure data: merges transports; NSD loss does not discard a fresh UDP room. */
public final class RoomDirectory {
    public static final long UDP_TTL_MILLIS = 6500;
    private static final int MAX_ROOMS = 64;
    private final Map<String, LanRoom> nsd = new LinkedHashMap<>();
    private final Map<String, LanRoom> udp = new HashMap<>();
    private final Map<String, Long> lastUdp = new HashMap<>();

    public synchronized void foundNsd(String service, LanRoom room) {
        if (nsd.containsKey(service) || nsd.size() < MAX_ROOMS) {
            nsd.remove(service);
            nsd.put(service, room); // The newest registration wins during rename/withdraw overlap.
        }
    }

    public synchronized void lostNsd(String service) { nsd.remove(service); }

    public synchronized void foundUdp(LanRoom room, long now) {
        if (udp.containsKey(room.key()) || udp.size() < MAX_ROOMS) {
            udp.put(room.key(), room);
            lastUdp.put(room.key(), now);
        }
    }

    public synchronized List<LanRoom> snapshot(long now) {
        for (String key : new ArrayList<>(udp.keySet())) {
            if (now - lastUdp.get(key) >= UDP_TTL_MILLIS) {
                udp.remove(key);
                lastUdp.remove(key);
            }
        }
        Map<String, LanRoom> merged = new HashMap<>(udp);
        for (LanRoom room : nsd.values()) merged.put(room.key(), room);
        ArrayList<LanRoom> rooms = new ArrayList<>(merged.values());
        rooms.sort((a, b) -> {
            int value = a.name.compareToIgnoreCase(b.name);
            return value == 0 ? a.key().compareTo(b.key()) : value;
        });
        if (rooms.size() > MAX_ROOMS) rooms.subList(MAX_ROOMS, rooms.size()).clear();
        return rooms;
    }
}
