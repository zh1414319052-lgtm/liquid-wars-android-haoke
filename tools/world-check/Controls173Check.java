package com.xenris.liquidwarsos.cloud;
import com.xenris.liquidwarsos.*;import java.util.*;
public final class Controls173Check {
 static int n;static void ok(boolean b,String why){n++;if(!b)throw new AssertionError(why);}
 public static void main(String[]args)throws Exception{
  WorldCamera c=new WorldCamera();c.world(1600,960,50,0,false,800,480);c.beginTouch();int anchored=c.touchX(700,1000),ay=c.touchY(200,500);float before=c.left;c.touchTarget(1100,650);c.frame(1000000000L);for(int k=1;k<=60;k++)c.frame(1000000000L+k*8333333L);ok(c.left>before+200,"camera follows target");ok(c.touchX(700,1000)==anchored&&c.touchY(200,500)==ay,"stationary finger projection not fed by camera");
  c.beginTouch(7);ok(c.touchX(400,1000,7)==c.mapX(400,1000),"new finger uses current visible map");ok(c.touchX(700,1000,0)==anchored,"new finger does not rebase held finger");c.endTouch(7);ok(c.touchX(700,1000)==anchored,"lifting secondary preserves primary");
  c.endTouch();ok(c.mapX(700,1000)!=anchored,"live screen projection moved");float held=c.left;c.pan(200,400);float p=c.left;c.followTarget(1400,500);c.frame(2000000000L);ok(c.left==p&&!c.following,"minimap inspection stays manual");
  c.touchTarget(1100,500);ok(c.following&&!c.overview,"new move resumes follow");c.toggle(1100,500);ok(c.overview,"overview retained");c.touchTarget(900,500);ok(!c.overview,"movement exits overview");c.touchTarget(0,0);c.frame(3000000000L);ok(c.left>=0&&c.bottom>=0,"edge clamp");c.world(800,480,1,0,false,500,300);c.touchTarget(100,200);c.frame(4000000000L);ok(c.left==0&&c.bottom==0,"small maps full view unchanged");
  TouchCursor cursor=new TouchCursor();cursor.bounds(1600,960);cursor.target(1200,800);cursor.followMass(700,300);ok(cursor.mapX()==1200&&cursor.mapY()==800,"inactive target cannot be replaced by mass");cursor.grab(1200,800);cursor.axis(1,0);cursor.advance(.08f);short x=cursor.mapX();cursor.release();cursor.followMass(1200,800);ok(cursor.mapX()==x,"release retains nearby target");
  int[] s=new int[209+1+17+62+22];s[0]=1000;int at=210;s[at]=0x4c573137;s[at+1]=1;s[at+2]=0;s[at+3]=1;s[at+4]=9000;for(int t=0;t<6;t++){s[147+t*10+8]=t;s[13+t]=100;}s[3]=600;s[4]=108000;ok(SnapshotItems.group(s,1)==0&&s[165]==1,"temporary group leaves permanent group");ok(SnapshotItems.rewindSpawned(s),"single reset flag");s[0]=9000;ok(SnapshotItems.group(s,1)==1,"temporary group expires");s[0]=1000;s[at+4]=0;
  int[] alliances={0,1,2,3,4,5};s[25+1*20+18]=6000;int mask=ModelVisibility.hiddenMask(0,s,alliances);ok(mask==2&&ModelVisibility.hiddenMask(1,s,alliances)==0,"enemy hidden own visible");int[] raw=new int[217+600];raw[216]=600;for(int t=0;t<6;t++){raw[t*8]=100;raw[t*8+4]=100+t*180;raw[t*8+5]=200;raw[48+t*12]=1234;raw[50+t*12]=100;raw[156+t*10]=900;for(int k=0;k<100;k++)raw[217+t*100+k]=(100+t*180+k%10)|((200+k/10)<<11)|(t<<21)|(5<<24);}
  int[] original=raw.clone(),filtered=ModelVisibility.filter(raw,1600,mask);ok(filtered.length==717&&filtered[216]==500&&filtered[8]==0&&filtered[60]==0&&filtered[166]==-1,"hidden particle/header/cluster/input removed");for(int k=217;k<filtered.length;k++)ok(((filtered[k]>>>21)&7)!=1,"no hidden particle word");ok(Arrays.equals(raw,original),"authority unchanged");ok(ModelVisibility.filter(raw,1600,0)==raw,"no-cloak view zero-copy");s[25+19]=8000;ok(ModelVisibility.hiddenMask(0,s,alliances)==0,"reveal defeats hidden view");s[25+19]=0;alliances[1]=0;ok(ModelVisibility.hiddenMask(0,s,alliances)==0,"ally can see cloak");alliances[1]=1;
  ModelTerrain terrain=new ModelTerrain(new byte[1600*960]);ModelWorld world=new ModelWorld(filtered,terrain.navigation);ok(world.massCount[1]==0&&world.population[1][0]==0,"hidden model has no current mass/route traffic");

  java.util.concurrent.atomic.AtomicReference<Check.Connection> request=new java.util.concurrent.atomic.AtomicReference<>();ModelConfig[] configs={Check.conf(0,"fixture"),ModelConfig.empty(),ModelConfig.empty(),ModelConfig.empty(),ModelConfig.empty(),ModelConfig.empty()};
  try(ModelMatch match=new ModelMatch(configs,new int[]{0,1,2,3,4,5},()->1000,()->new ModelHttp(url->{Check.Connection conn=new Check.Connection(url,200,Check.body(0,Check.ACTION));request.set(conn);return conn;}))){
   match.terrain(new byte[1600*960]);match.itemState(s);match.pause(false);match.update(1,raw);Check.idle(match);java.lang.reflect.Field sent=ModelMatch.class.getDeclaredField("sentObservation");sent.setAccessible(true);String observation=((String[])sent.get(match))[0];org.json.JSONObject obj=new org.json.JSONObject(observation);ok(obj.getJSONArray("hidden_enemy_ids").getInt(0)==1,"actual match marks hidden enemy");for(Object row:obj.getJSONArray("ENEMIES"))ok(((org.json.JSONObject)row).getInt("id")!=1,"actual API observation omits hidden mass");ok(request.get()!=null&&request.get().sent.size()>0,"fakeHTTPS carried real filtered request");
  }


  try(ModelMatch match=new ModelMatch(configs,new int[]{0,1,2,3,4,5},()->1000,()->new ModelHttp(url->new Check.Connection(url,401,"fixture")))){
   match.terrain(new byte[1600*960]);match.itemState(s);match.pause(false);match.update(1,raw);Check.idle(match);ok(!match.due(1),"fatal provider error stops lane");s[43]=0;match.itemState(s);ok(!match.due(1)&&match.inputs().mask==0,"cloak transition cannot restart fatal lane or local fallback");
  }

  System.out.println("PASS "+n+" shared camera/anchored gesture/target retention/balance trailer/per-team cloak assertions");
 }
}
