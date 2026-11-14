// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from Haoke WPGen native updater, 2026.
package com.xenris.liquidwarsos.updates;

import com.xenris.liquidwarsos.BuildConfig;

/** QA and stable use the real installed package and separate server/state channels. */
public final class UpdateAppIdentity {
    public static final String APPLICATION_ID = "com.xenris.liquidwarsos.haoke";
    private UpdateAppIdentity() { }
    public static String channel(String applicationId) {
        return channel(applicationId, BuildConfig.DEBUG);
    }
    static String channel(String applicationId, boolean debug) {
        if (!APPLICATION_ID.equals(applicationId)) return null;
        return debug ? "qa" : "stable";
    }
}
