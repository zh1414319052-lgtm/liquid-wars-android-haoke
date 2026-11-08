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

import android.content.Context;
import android.util.Log;
import com.xenris.liquidwarsos.StaticBits;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/** Independent lifecycle-owned publishers and browsers; no shared UDP socket. */
public final class ServerFinder {
    private static NsdRoomDiscovery.Publisher nsdPublisher;
    private static LegacyLanDiscovery.Publisher udpPublisher;
    private static NsdRoomDiscovery.Browser nsdBrowser;
    private static LegacyLanDiscovery.Browser udpBrowser;
    private static String sharedName;
    private ServerFinder() {}

    public static synchronized void share(Context context, int discoveryPort, String name) {
        String safeName = LanRoom.cleanName(name);
        if (safeName.equals(sharedName)) return;
        stopSharing();
        sharedName = safeName;
        nsdPublisher = new NsdRoomDiscovery.Publisher(context, StaticBits.PORT_NUMBER, safeName);
        try { udpPublisher = new LegacyLanDiscovery.Publisher(discoveryPort, safeName); }
        catch (IOException unavailable) { Log.w("LiquidWarsLAN", "UDP publication unavailable", unavailable); }
    }

    public static synchronized void stopSharing() {
        if (udpPublisher != null) udpPublisher.close();
        if (nsdPublisher != null) nsdPublisher.close();
        udpPublisher = null;
        nsdPublisher = null;
        sharedName = null;
    }

    public static synchronized void search(Context context, Consumer<List<LanRoom>> callback) {
        stopSearching();
        NsdRoomDiscovery.Browser browser = new NsdRoomDiscovery.Browser(context, callback);
        nsdBrowser = browser;
        Context application = context.getApplicationContext();
        try {
            udpBrowser = new LegacyLanDiscovery.Browser(StaticBits.PORT_NUMBER + 1,
                    StaticBits.PORT_NUMBER, () -> NetInfo.broadcastAddresses(application), browser::acceptUdp);
        } catch (IOException unavailable) { Log.w("LiquidWarsLAN", "UDP discovery unavailable", unavailable); }
    }

    public static synchronized void stopSearching() {
        if (udpBrowser != null) udpBrowser.close();
        if (nsdBrowser != null) nsdBrowser.close();
        udpBrowser = null;
        nsdBrowser = null;
    }
}
