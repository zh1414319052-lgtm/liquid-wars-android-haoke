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

import android.net.wifi.WifiManager;
import android.content.Context;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.InterfaceAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NetInfo {
    public static String getIPAddress(Context context) {
        try {
            WifiManager wm = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return wm == null || wm.getConnectionInfo() == null ? "0.0.0.0" : format(wm.getConnectionInfo().getIpAddress());
        } catch (RuntimeException unavailable) { return "0.0.0.0"; }
    }

    /** Local broadcasts only; never probes each address or scans ports. Recomputed on every query. */
    public static List<InetAddress> broadcastAddresses(Context context) {
        Set<InetAddress> addresses = new LinkedHashSet<>();
        try { addresses.add(InetAddress.getByName("255.255.255.255")); }
        catch (java.net.UnknownHostException impossible) { }
        try {
            WifiManager wifi = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null && wifi.getDhcpInfo() != null) {
                android.net.DhcpInfo dhcp = wifi.getDhcpInfo();
                if (dhcp.ipAddress != 0 && dhcp.netmask != 0)
                    addresses.add(InetAddress.getByName(format(dhcp.ipAddress | ~dhcp.netmask)));
            }
        } catch (Exception unavailableWifiInfo) { }
        try {
            java.util.Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) for (NetworkInterface network : Collections.list(interfaces)) {
                if (!network.isUp() || network.isLoopback()) continue;
                for (InterfaceAddress address : network.getInterfaceAddresses()) {
                    if (address.getBroadcast() != null && addresses.size() < 8)
                        addresses.add(address.getBroadcast());
                }
            }
        } catch (Exception unavailableInterfaces) { }
        return new ArrayList<>(addresses);
    }

    public static String getNetMask(Context context) {
        WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        int i = wm.getDhcpInfo().netmask;
        return format(i);
    }

    public static String getBroadcastAddress(Context context) {
        WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        int ip = wm.getConnectionInfo().getIpAddress();
        int nm = wm.getDhcpInfo().netmask;
        return format(ip | (~nm));
    }

    public static String getSSID(Context context) {
        WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        return wm.getConnectionInfo().getSSID();
    }

    private static String format(int i) {
        return String.format("%d.%d.%d.%d",
            (i & 0xFF),
            ((i >> 8) & 0xFF),
            ((i >> 16) & 0xFF),
            ((i >> 24) & 0xFF));
    }
}
