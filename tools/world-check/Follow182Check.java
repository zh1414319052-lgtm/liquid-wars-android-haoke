package com.xenris.liquidwarsos;
import java.util.*;
public final class Follow182Check {
 public static void main(String[] args)throws Exception {
  Operations180Check.main(args);OrderProtocol c=new OrderProtocol();OrderQueue q=new OrderQueue();short[][] x=new short[6][5],y=new short[6][5];short[][] last=null;
  for(int k=0;k<1000;k++){last=c.packet(OrderProtocol.FOLLOW,1,4,Collections.singletonList(new int[]{k,200}),0);q.offer(2,last[0],last[1],x,y);}
  Operations180Check.check(q.size(2)==1,"1000 pending moves coalesce to latest target");q.apply(x,y);Operations180Check.check(x[2][2]==999,"latest finger target survives");
  short[][] stop=c.packet(OrderProtocol.STOP,1,4,null,0);q.offer(2,stop[0],stop[1],x,y);q.offer(2,last[0],last[1],x,y);Operations180Check.check(q.size(2)==2,"stop is chronological boundary");q.apply(x,y);Operations180Check.check(y[2][0]/4==OrderProtocol.STOP,"stop cannot be dropped");q.apply(x,y);
  for(int group=0;group<3;group++){short[][] p=c.packet(OrderProtocol.FOLLOW,group,4,Collections.singletonList(new int[]{20,40}),0);q.offer(2,p[0],p[1],x,y);}Operations180Check.check(q.size(2)==3,"different squads never coalesce");q.clear();
  PlayerHistory history=new PlayerHistory();history.savePlayerPositions(x,y);int[] data=new int[61];data[0]=0x51;history.serialiseHistoricalPlayerState(0,data,1);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();com.xenris.liquidwarsos.net.GameWire.write(bytes,data.length,data);int[] received=com.xenris.liquidwarsos.net.GameWire.read(new java.io.ByteArrayInputStream(bytes.toByteArray()));Operations180Check.check(Arrays.equals(data,received)&&received[21]==-5&&received[24]==4&&received[25]==999,"FOLLOW fields survive loss-recovery history and real wire codec");
  System.out.println("PASS "+Operations180Check.n+" command/history/wire checks including FOLLOW coalescing");
 }
}
