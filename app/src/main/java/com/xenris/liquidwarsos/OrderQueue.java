// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import java.util.ArrayDeque;
/** Guarded by the activity's input lock. Exactly one packet/team/lockstep frame. */
public final class OrderQueue {
 private final ArrayDeque<short[][]>[] pending;
 @SuppressWarnings("unchecked") public OrderQueue(){pending=new ArrayDeque[6];for(int i=0;i<6;i++)pending[i]=new ArrayDeque<>();}
 public boolean offer(int team,short[] x,short[] y,short[][] liveX,short[][] liveY){
  if(team<0||team>=6||x.length!=5||y.length!=5)return false;
  if(x[0]==-5){
   short[][] last=pending[team].peekLast();
   if(y[0]/4==OrderProtocol.FOLLOW&&last!=null&&last[1][0]==y[0]&&last[1][1]==y[1])pending[team].removeLast();
   if(pending[team].size()>=512)return false;pending[team].add(new short[][]{x.clone(),y.clone()});
  }
  else{pending[team].clear();System.arraycopy(x,0,liveX[team],0,5);System.arraycopy(y,0,liveY[team],0,5);}return true;
 }
 public void apply(short[][] x,short[][] y){for(int t=0;t<6;t++)if(!pending[t].isEmpty()){short[][] p=pending[t].remove();System.arraycopy(p[0],0,x[t],0,5);System.arraycopy(p[1],0,y[t],0,5);}}
 public void clear(){for(ArrayDeque<short[][]> p:pending)p.clear();}
 public void clear(int team){if(team>=0&&team<6)pending[team].clear();}
 public int size(int t){return pending[t].size();}
}
