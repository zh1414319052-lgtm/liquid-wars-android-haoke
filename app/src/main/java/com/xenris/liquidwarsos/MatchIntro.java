// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
/** Existing63-int input frames carry43 warmup batches. Both peers skip native
 * simulation for those indices, including fast replay. No new wire messages. */
public final class MatchIntro {
 public static final int BATCH_MS=70,FRAMES=43,DURATION_MS=BATCH_MS*FRAMES;
 private MatchIntro(){}
 public static boolean warming(int frame){return frame<FRAMES;}
 public static int remaining(int frame){return Math.max(0,FRAMES-frame)*BATCH_MS;}
 public static int seconds(long ms){return Math.min(3,(int)Math.max(0,(ms+999)/1000));}
}
