// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.server;

import java.util.Objects;

public final class LanRoom {
    public final String name;
    public final String ip;
    public final int port;

    public LanRoom(String name, String ip, int port) {
        if (ip == null || ip.isEmpty() || port < 1 || port > 65535)
            throw new IllegalArgumentException("Invalid room endpoint");
        this.name = cleanName(name);
        this.ip = ip;
        this.port = port;
    }

    public String key() { return ip + "#" + port; }

    public static String cleanName(String name) {
        if (name == null) return "Liquid Wars";
        StringBuilder result = new StringBuilder();
        int bytes = 0;
        for (int offset = 0; offset < name.length();) {
            int cp = name.codePointAt(offset);
            offset += Character.charCount(cp);
            if (Character.isISOControl(cp) || cp == 0x2028 || cp == 0x2029) continue;
            String next = new String(Character.toChars(cp));
            int size = next.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (bytes + size > 60) break; // DNS instance label must fit 63 UTF-8 bytes.
            result.append(next);
            bytes += size;
        }
        String value = result.toString().trim();
        return value.isEmpty() ? "Liquid Wars" : value;
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof LanRoom)) return false;
        LanRoom room = (LanRoom) other;
        return port == room.port && ip.equals(room.ip) && name.equals(room.name);
    }

    @Override public int hashCode() { return Objects.hash(name, ip, port); }
    @Override public String toString() { return name; }
}
