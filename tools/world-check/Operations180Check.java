package com.xenris.liquidwarsos;
import java.util.*;
public final class Operations180Check {
 static int n;static void check(boolean yes,String message){n++;if(!yes)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  OrderProtocol codec=new OrderProtocol();List<int[]> line=new ArrayList<>();for(int k=0;k<600;k++)line.add(new int[]{k,100+(int)(60*Math.sin(k/40.0))});List<int[]> reduced=OrderProtocol.simplify(line);check(reduced.size()<=24,"bounded route");check(Arrays.equals(reduced.get(0),line.get(0))&&Arrays.equals(reduced.get(reduced.size()-1),line.get(599)),"both ends preserved");
  List<short[][]> packets=codec.route(1,7,line);OrderQueue q=new OrderQueue();short[][] x=new short[6][5],y=new short[6][5];for(short[][] p:packets)check(q.offer(2,p[0],p[1],x,y),"accepted packet");check(q.size(2)==packets.size(),"no overwrite from rapid submissions");
  for(short[][] p:packets){q.apply(x,y);check(Arrays.equals(x[2],p[0])&&Arrays.equals(y[2],p[1]),"one chronological packet per host frame");}check(q.size(2)==0,"queue drained");check(y[2][0]/4==OrderProtocol.COMMIT,"last command commits");
  short[] retained=x[2].clone();q.apply(x,y);check(Arrays.equals(x[2],retained),"repeated lockstep frame preserves dedup token");
  for(short[][] p:packets)q.offer(0,p[0],p[1],x,y);short[] normal={123,-1,-1,-1,-1},ny={456,-1,-1,-1,-1};q.offer(0,normal,ny,x,y);check(q.size(0)==0&&x[0][0]==123,"legacy replacement cancels uncommitted route");
  q.offer(1,packets.get(0)[0],packets.get(0)[1],x,y);q.offer(0,packets.get(0)[0],packets.get(0)[1],x,y);q.clear(0);check(q.size(0)==0&&q.size(1)==1,"host finger changes preserve remote client's pending route");q.clear();check(q.size(1)==0,"rewind discards pending packets");
  for(int i=0;i<40000;i++){short[][] p=codec.packet(OrderProtocol.STOP,3,0,null,0);check(p[0][1]>0&&p[0][1]<=32767,"serial wraps without negative values");}
  PlayerHistory history=new PlayerHistory();for(short[][] packet:packets){System.arraycopy(packet[0],0,x[2],0,5);System.arraycopy(packet[1],0,y[2],0,5);history.savePlayerPositions(x,y);}
  for(int back=0;back<packets.size();back++){int[] data=new int[61];data[0]=0x51;history.serialiseHistoricalPlayerState(back,data,1);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();com.xenris.liquidwarsos.net.GameWire.write(out,data.length,data);int[] received=com.xenris.liquidwarsos.net.GameWire.read(new java.io.ByteArrayInputStream(out.toByteArray()));check(Arrays.equals(data,received),"historical authoritative frame survives real wire codec");short[][] packet=packets.get(packets.size()-1-back);for(int i=0;i<5;i++)check(received[21+i*2]==packet[0][i]&&received[22+i*2]==packet[1][i],"missed-frame history preserves signed command fields and order");}
  System.out.println("PASS "+n+" codec and lockstep queue checks");
 }
}
