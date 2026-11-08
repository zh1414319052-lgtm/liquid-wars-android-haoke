// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.net;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;

public final class BluetoothAccess {
    private BluetoothAccess() {}
    public static BluetoothAdapter adapter(Context context) {
        BluetoothManager manager = (BluetoothManager) context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        return manager == null ? null : manager.getAdapter();
    }
    public static String[] missing(Context context, BluetoothPermissions.Action action) {
        ArrayList<String> missing = new ArrayList<>();
        for (String permission : BluetoothPermissions.required(Build.VERSION.SDK_INT, action))
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                missing.add(permission);
        return missing.toArray(new String[0]);
    }
    public static boolean has(Context context, BluetoothPermissions.Action action) { return missing(context, action).length == 0; }
}
