// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

/** Local AI input policy. Not a simulation or wire-protocol parameter. */
public final class AiDifficulty {
    public static final int RELAXED=0, CLASSIC=1, HARD=2;
    private AiDifficulty() {}
    public static int valid(int value) { return value>=RELAXED && value<=HARD ? value : CLASSIC; }
}
