// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
/** Absolute deadlines prevent per-step sleep overshoot accumulating. No simulation state. */
public final class StepPacer {
 private long deadline,lastBegin,period;
 public long delay(long before,long now,int micros){
  long p=micros*1000L;if(p<=0){reset();return 0;}
  if(deadline==0||period!=p||before<lastBegin||before-lastBegin>p*8||now-deadline>p*4)deadline=before+p;else deadline+=p;
  period=p;lastBegin=before;return Math.max(0,deadline-now);
 }
 public void reset(){deadline=lastBegin=period=0;}
}
