package com.xenris.liquidwarsos;
/** Test-only read-only JNI inspector; never packaged into the release APK. */
public final class Online184StateProbe {
    static {System.loadLibrary("online184probe");}
    public static native long[] hashes();
}
