// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.net;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/** Only accept numeric peers on an attached local subnet. */
public final class LanPeer {
    private LanPeer() {}

    public static InetAddress parse(String address) throws UnknownHostException {
        if (address != null && address.indexOf(':') >= 0
                && address.matches("[0-9A-Fa-f:.]+(%[A-Za-z0-9_.-]+)?")) {
            return InetAddress.getByName(address);
        }
        if (address == null || !address.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            throw new UnknownHostException("Expected a numeric LAN address");
        }
        String[] parts = address.split("\\.");
        byte[] bytes = new byte[4];
        for (int i = 0; i < bytes.length; i++) {
            int value = Integer.parseInt(parts[i]);
            if (value > 255) throw new UnknownHostException("Invalid IPv4 address");
            bytes[i] = (byte) value;
        }
        return InetAddress.getByAddress(bytes);
    }

    public static boolean isOnLink(InetAddress peer) throws SocketException {
        if (peer.isAnyLocalAddress() || peer.isMulticastAddress()) return false;
        if (peer.isLoopbackAddress()) return true;
        Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
        if (networks == null) return false;
        while (networks.hasMoreElements()) {
            NetworkInterface network = networks.nextElement();
            if (!network.isUp() || network.isLoopback() || network.isPointToPoint()) continue;
            for (InterfaceAddress local : network.getInterfaceAddresses()) {
                if (!peer.equals(local.getBroadcast())
                        && sharesPrefix(peer.getAddress(), local.getAddress().getAddress(), local.getNetworkPrefixLength())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean sharesPrefix(byte[] peer, byte[] local, int prefix) {
        if (peer.length != local.length || prefix <= 0 || prefix > peer.length * 8) return false;
        int wholeBytes = prefix / 8;
        for (int i = 0; i < wholeBytes; i++) {
            if (peer[i] != local[i]) return false;
        }
        int remainder = prefix % 8;
        return remainder == 0 || ((peer[wholeBytes] ^ local[wholeBytes]) & (0xff << (8 - remainder))) == 0;
    }
}
