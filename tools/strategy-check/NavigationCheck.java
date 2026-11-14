package com.xenris.liquidwarsos.cloud;
import java.nio.file.*;import java.awt.image.*;import javax.imageio.*;import java.util.*;
public final class NavigationCheck {
 public static void main(String[] args)throws Exception{int checks=0;for(int map=(args.length>1?Integer.parseInt(args[1]):0);map<(args.length>1?Integer.parseInt(args[1])+1:50);map++)for(int flip=0;flip<4;flip++){
  BufferedImage image=ImageIO.read(Path.of(args[0],map+"-map.png").toFile());byte[] mask=new byte[384000];for(int y=0;y<480;y++)for(int x=0;x<800;x++)mask[y*800+x]=(byte)((image.getRGB((flip&1)!=0?799-x:x,(flip&2)!=0?y:479-y)>>>24)>3?1:0);
  ModelNavigation nav=new ModelNavigation(mask);if(nav.rooms.isEmpty())throw new AssertionError("empty map");
  for(ModelNavigation.Door door:nav.doors){checks++;if(nav.wall(door.x,door.y)||!nav.clear(door.ax,door.ay,door.bx,door.by))throw new AssertionError("door geometry "+map);for(int[][] path:new int[][][]{door.pathA,door.pathB})for(int k=1;k<path.length;k++)if(!nav.clear(path[k-1][0],path[k-1][1],path[k][0],path[k][1]))throw new AssertionError("portal path crosses wall");}
  for(ModelNavigation.Room room:nav.rooms){checks++;if(nav.routes(0,room.id,1).isEmpty())throw new AssertionError("graph disconnected "+map+" room"+room.id+" area="+room.area+" xy="+room.x+","+room.y);}
  System.out.println(map+" flip="+flip+" rooms="+nav.rooms.size()+" doors="+nav.doors.size()+" prefix="+nav.description.length()+" buildMs="+nav.buildMs+" connectedRepair="+nav.connectedRepair);
 }System.out.println("PASS "+checks+" navigation assertions on "+(args.length>1?"map"+args[1]:"all50maps")+" x4 flips; host JVM, not MuMu");}
}
