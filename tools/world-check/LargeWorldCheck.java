package com.xenris.liquidwarsos.cloud;
import com.xenris.liquidwarsos.*;import org.json.*;
public final class LargeWorldCheck {
 static int n;static void ok(boolean b){if(!b)throw new AssertionError("large case "+n);n++;}
 public static void main(String[] args)throws Exception{
  TiledWorldCheck.main(args);
  for(int ww:new int[]{800,1600,3200})for(float dp:new float[]{1,1.5f,2})for(boolean aim:new boolean[]{false,true}){
   MiniMapLayout m=new MiniMapLayout(1600,720,dp,ww,ww==800?480:960,aim);ok(Math.abs(m.width/m.height-ww/(float)(ww==800?480:960))<.0001);ok(m.contains(m.left+.1f,m.top+.1f));ok(!m.contains(m.left-1,m.top));
   for(int x=0;x<ww;x+=67)for(int y=0;y<(ww==800?480:960);y+=73){float px=m.left+(x+.5f)/ww*m.width,py=m.top+((ww==800?480:960)-.5f-y)/(ww==800?480:960)*m.height;ok(m.x(px)==x&&m.y(py)==y);}
  }
  ModelTerrain terrain=new ModelTerrain(new byte[3200*960]);ok(terrain.width==3200&&terrain.height==960&&terrain.description.contains("[3200,960]"));
  String action="{\"to\":[3100,900],\"route\":[],\"hold_ms\":1000,\"item_to\":[3000,700]}";
  for(int protocol=0;protocol<4;protocol++){ModelConfig c=Check.conf(protocol,"fixture");final int p=protocol;ModelAction a=new ModelHttp(url->new Check.Connection(url,200,Check.body(p,action))).call(c,"{}",terrain.description,false);ok(a.points()[0]==3100&&a.itemTo[0]==3000);ModelStream stream=new ModelStream(protocol,3200,960);ok(stream.event(Check.event(protocol,action)).points()[5]==900);}
  int[] raw=new int[223];raw[216]=6;for(int t=0;t<6;t++){int x=2500+t*100,y=500+t*50;raw[t*8]=1;raw[t*8+4]=x;raw[t*8+5]=y;raw[217+t]=x|(y<<12)|(t<<22)|(5<<25);}ModelWorld world=new ModelWorld(raw,terrain.navigation);ok(world.massX[5]==3000&&world.team(raw[222])==5&&world.y(raw[222])==750);
  int[] hp=new int[209+1+62+22];hp[209]=0;hp[210]=0x4c574850;hp[211]=6;int trailer=hp.length-22;hp[trailer]=SnapshotItems.MAGIC;hp[trailer+1]=6;for(int k=0;k<6;k++){int at=212+k*10;hp[at]=1200-k*100;hp[at+1]=1200;hp[at+2]=k;hp[at+3]=k;hp[trailer+2+k*3]=k;hp[trailer+3+k*3]=100+k*500;hp[trailer+4+k*3]=500;}
  ok(SnapshotItems.slots(hp)==6&&SnapshotItems.count(hp)==6);for(int k=0;k<6;k++)ok(SnapshotItems.hp(hp,k)==1200-k*100&&SnapshotItems.maxHp(hp,k)==1200&&SnapshotItems.leader(hp,k)==k&&SnapshotItems.devouring(hp,k));
  ModelMatch match=new ModelMatch(new ModelConfig[]{new ModelConfig(false,0,"https://example.invalid","",""),new ModelConfig(false,0,"https://example.invalid","",""),new ModelConfig(false,0,"https://example.invalid","",""),new ModelConfig(false,0,"https://example.invalid","",""),new ModelConfig(false,0,"https://example.invalid","",""),new ModelConfig(false,0,"https://example.invalid","","")},new int[]{0,1,2,3,4,5});match.itemState(hp);java.lang.reflect.Field facts=ModelMatch.class.getDeclaredField("itemFacts");facts.setAccessible(true);JSONObject obj=(JSONObject)facts.get(match);ok(obj.getJSONArray("pickup_kind_x_y_hp_max_leader").getJSONArray(5).getInt(3)==700);match.close();
  ok(MapCatalog.COUNT==52&&MapCatalog.width(52)==3200&&MapCatalog.height(52)==960&&MapCatalog.mapIdForSelection(52)==52);
  WorldCamera c=new WorldCamera();c.world(3200,960,52,3,false,3000,800);ok(c.mapX(c.screenX(3100,1400),1400)==3100&&c.mapY(c.screenY(900,600),600)==900);
  System.out.println("PASS "+n+" large-map/minimap exact taps/health/model facts/12bit/4protocol full+stream assertions; fake HTTPS only");
 }
}
