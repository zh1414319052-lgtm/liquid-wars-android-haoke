package com.xenris.liquidwarsos.cloud;
import com.xenris.liquidwarsos.*;import org.json.*;import java.net.*;
public final class WorldCheck {
 static int n;static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[] args)throws Exception{
  WorldCamera camera=new WorldCamera();camera.world(2000,960,50,0,false,1700,800);ok(!camera.overview&&camera.viewWidth==800&&Math.abs(camera.viewWidth/camera.viewHeight-2.2)<.0001);ok(camera.left==1200&&Math.abs(camera.bottom-(960-camera.viewHeight))<.01);
  for(int x=1200;x<2000;x+=17)for(int y=480;y<960;y+=19){ok(camera.mapX(camera.screenX(x,1320),1320)==x);ok(camera.mapY(camera.screenY(y,600),600)==y);}
  camera.toggle(1700,800);ok(camera.overview&&camera.left<=0&&camera.bottom<=0&&camera.viewWidth>=2000&&camera.viewHeight>=960);camera.pan(1100,600);ok(!camera.following);float held=camera.left;camera.follow(100,100);ok(camera.left==held);camera.toggle(1600,800);ok(camera.following&&!camera.overview);
  TouchCursor cursor=new TouchCursor();cursor.bounds(2000,960);cursor.target(1800,800);cursor.grab(1800,800);cursor.axis(1,1);for(int i=0;i<80;i++)cursor.advance(.01f);ok(cursor.mapX()>1800&&cursor.mapY()>800&&cursor.mapX()<2000&&cursor.mapY()<960);
  String action="{\"to\":[1800,800],\"route\":[],\"hold_ms\":1000,\"item_to\":[1700,700]}";
  ModelTerrain terrain=new ModelTerrain(new byte[2000*960]);ok(terrain.width==2000&&terrain.height==960);ok(terrain.navigation.description.contains("[2000,960]"));
  ModelAction parsed=ModelAction.parse(new JSONObject(action),2000,960);ok(parsed.points()[0]==1800&&terrain.allows(parsed));ok(!new ModelTerrain(new byte[800*480]).allows(parsed));
  for(int p=0;p<4;p++){final int protocol=p;ModelConfig config=Check.conf(p,"fixture");ModelHttp h=new ModelHttp(url->new Check.Connection(url,200,Check.body(protocol,action)));ModelAction result=h.call(config,"{}",terrain.description,false);ok(result.points()[0]==1800&&result.itemTo[1]==700);ModelStream stream=new ModelStream(p,2000,960);ok(stream.event(Check.event(p,action)).points()[5]==800);}
  int[] raw=new int[217+6];raw[216]=6;for(int t=0;t<6;t++){int x=1300+t*50,y=600+t*30;raw[t*8]=1;raw[t*8+4]=x;raw[t*8+5]=y;raw[217+t]=x|(y<<11)|(t<<21)|(5<<24);}ModelWorld world=new ModelWorld(raw,terrain.navigation);ok(world.massX[5]==1550&&world.massY[5]==750&&world.team(raw[222])==5);
  int[] items=new int[209+1+22];items[209]=0;items[210]=SnapshotItems.MAGIC;items[211]=6;for(int k=0;k<6;k++){items[212+k*3]=k;items[213+k*3]=100+k*200;items[214+k*3]=700;}ok(SnapshotItems.slots(items)==6&&SnapshotItems.count(items)==6&&items[SnapshotItems.offset(items,5)+1]==1100);
  System.out.println("PASS "+n+" wide world/camera/nearby target/6-slot/full and streaming API assertions; fake HTTPS only");
 }
}
