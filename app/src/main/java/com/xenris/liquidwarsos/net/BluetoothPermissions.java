// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.net;

public final class BluetoothPermissions {
    public enum Action { CONNECT, SCAN, HOST }
    private BluetoothPermissions() {}
    public static String[] required(int sdk, Action action) {
        if (sdk < 31) return new String[0]; // Older devices pair/scan through system Settings.
        String connect = "android.permission.BLUETOOTH_CONNECT";
        if (action == Action.SCAN) return new String[]{connect, "android.permission.BLUETOOTH_SCAN"};
        if (action == Action.HOST) return new String[]{connect, "android.permission.BLUETOOTH_ADVERTISE"};
        return new String[]{connect};
    }
}
