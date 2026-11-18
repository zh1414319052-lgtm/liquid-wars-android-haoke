package com.xenris.liquidwarsos;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.content.Context;
import org.junit.*;import org.junit.runner.RunWith;
import java.util.*;import java.io.*;import org.json.*;

/** Real Android C++ library and authored map; no fabricated room/AI/render success. */
@RunWith(AndroidJUnit4.class)
public class Expedition192ContractTest {
 final Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
 @Test public void tenAuthoritiesAndFilteredProjection()throws Exception{
  Assert.assertTrue(ctx.getPackageName().endsWith("garden186"));NativeInterface.init(ctx.getAssets());
  Assert.assertTrue(NativeInterface.createExpeditionGame(0,10,192007));Assert.assertEquals(10000,NativeInterface.totalParticles());
  int[][] packets=new int[10][];JSONArray rows=new JSONArray();
  for(int t=0;t<10;t++){
   packets[t]=NativeInterface.expeditionWire(t);int[] d=packets[t];Assert.assertEquals(192,d[1]);Assert.assertEquals(t,d[3]);Assert.assertEquals(10,d[4]);Assert.assertEquals(10000,d[5]);
   OperationsSnapshot ops=new OperationsSnapshot(Arrays.copyOfRange(d,13,13+d[9]));
   ExpeditionSnapshot facts=new ExpeditionSnapshot(Arrays.copyOfRange(d,13+d[9],13+d[9]+d[10]));
   Assert.assertEquals(1200000,facts.duration);Assert.assertEquals(10,ops.teams);Assert.assertEquals(1000,facts.count(t));
   for(int other=0;other<10;other++)if(other!=t){Assert.assertEquals(-1,facts.raw[facts.seat(other)+2]);Assert.assertEquals(0,ops.raw[ops.group(other,0)]);}
   int offset=13+d[9]+d[10]+d[11],own=0;HashSet<Long> ids=new HashSet<>();
   for(int i=0;i<d[12];i++){int b=offset+i*26;long id=(d[b]&0xffffffffL)|((d[b+1]&0xffffffffL)<<32);Assert.assertTrue(ids.add(id));if(d[b+2]==t)own++;}
   Assert.assertEquals(1000,own);
   int mb=13+d[9]+d[10];if(t!=0)for(int i=0;i<d[mb+4];i++)Assert.assertEquals(t,d[mb+5+i*4]);
   rows.put(new JSONObject().put("viewer",t).put("visible_units",d[12]).put("bytes",d.length*4).put("birth_x",d[7]).put("birth_y",d[8]));
  }
  NativeInterface.destroyGame();NativeInterface.createExpeditionProxy(9,10);
  Assert.assertTrue(NativeInterface.expeditionReceive(packets[9]));Assert.assertFalse(NativeInterface.expeditionReceive(packets[9]));
  int[] bad=packets[9].clone();bad[2]=7;bad[4]=11;Assert.assertFalse(NativeInterface.expeditionReceive(bad));
  bad=packets[9].clone();bad[2]=7;bad[12]++;Assert.assertFalse(NativeInterface.expeditionReceive(bad));
  Assert.assertFalse(NativeInterface.expeditionReceive(packets[8]));
  ExpeditionSnapshot guest=new ExpeditionSnapshot(NativeInterface.expeditionSnapshot());Assert.assertEquals(1000,guest.count(9));
  Assert.assertEquals(packets[9][7],NativeInterface.sceneInfo()[4]);
  float[] visual=NativeInterface.godotFrame(0,0,3264,2048,false);Assert.assertEquals(packets[9][12],(int)visual[0]);Assert.assertTrue(visual[14]>0);
  NativeInterface.destroyGame();NativeInterface.createGame(0,1,192,50);Assert.assertEquals(300,NativeInterface.totalParticles());Assert.assertTrue(NativeInterface.itemSnapshot().length>=209);
  try(FileWriter out=new FileWriter(new File(ctx.getExternalFilesDir(null),"expedition192-contract.json"))){out.write(new JSONObject().put("result","PASS").put("views",rows).put("evidence","Native ten-army state, filtered projection, real decoder/renderer-packet and legacy neighbor. Not ten physical devices or balance proof.").toString(2));}
 }
 @After public void close(){NativeInterface.destroyGame();NativeInterface.uninit();}
}
