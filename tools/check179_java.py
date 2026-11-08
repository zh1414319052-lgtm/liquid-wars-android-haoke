"""Focused production-controller/wire/camera checks. JNI is explicitly a stub."""
from pathlib import Path
import subprocess,json
R=Path(__file__).resolve().parents[1];O=R/'build/local-ai-1.79/java';O.mkdir(parents=True,exist_ok=True)
J=Path('C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin');S=R/'app/src/main/java/com/xenris/liquidwarsos'
stub='''package com.xenris.liquidwarsos; import java.util.*;
public class NativeInterface {
 static List<Integer> calls=new ArrayList<>(),masks=new ArrayList<>();static boolean available=true;
 public static int getNearestDot(int t,short x,short y){calls.add(t);return (777<<16)|444;}
 public static boolean updateAiTargets(short[] a,int mask,int level){masks.add(mask);Arrays.fill(a,(short)-1);for(int t=0;t<6;t++)if((mask&(1<<t))!=0){a[t*10]=(short)(100+t);a[t*10+5]=(short)(200+t);a[t*10+3]=2;a[t*10+8]=0;a[t*10+4]=-3;a[t*10+9]=123;}return available;}
 public static int[] itemSnapshot(){return null;}public static byte[] modelTerrain(){return null;}
}'''
test='''package com.xenris.liquidwarsos;import java.util.*;import java.io.*;import com.xenris.liquidwarsos.net.GameWire;
public class Local179Check {
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 static short[][] initial(){short[][] a=new short[6][5];for(int t=0;t<6;t++){Arrays.fill(a[t],(short)-1);a[t][0]=(short)(20+t);}return a;}
 public static void main(String[] args)throws Exception{
 for(int level=0;level<3;level++)for(int control:new int[]{0,1,42,62,63})for(int model:new int[]{0,2,21,63}){
  AiController a=new AiController(level);short[][] x=initial(),y=initial();a.setModelInputs(model,0,null);
  for(int tick=0;tick<100;tick++){NativeInterface.calls.clear();NativeInterface.masks.clear();a.update(x,y,control);List<Integer> want=new ArrayList<>();for(int t=0;t<6;t++){
   if((control&~model&(1<<t))!=0)want.add(t);
   if((control&(1<<t))==0){ok(x[t][0]==20+t,"human position unchanged");for(int k=1;k<5;k++)ok(x[t][k]==-1,"human secondary unchanged");}
   if((control&model&(1<<t))!=0){ok(x[t][0]==-2&&y[t][0]==-2,"configured model stays idle");for(int k=1;k<5;k++)ok(x[t][k]==-1&&y[t][k]==-1,"model secondary/commands cleared");}
   if(level>0&&tick>=8&&(control&~model&(1<<t))!=0)ok(x[t][0]==100+t&&x[t][4]==-3&&y[t][4]==123,"all eligible teams get complete planned command");
  }ok(NativeInterface.calls.equals(want),"legacy RNG-call order and count preserved");for(int mask:NativeInterface.masks){ok((mask&~(control&~model&63))==0,"no planning human/model teams");ok(Integer.bitCount(mask)<=2,"bounded teams per worker tick");}if(level==0)ok(NativeInterface.masks.isEmpty(),"relaxed retains old pacing");}
 }
 AiController a=new AiController(2);short[][] x=initial(),y=initial();for(int k=0;k<8;k++)a.update(x,y,63);
 short[] point=new short[60];Arrays.fill(point,(short)-1);point[10]=333;point[15]=222;point[14]=-3;point[19]=44;point[13]=4;point[18]=0;a.setModelInputs(2,2,point);a.update(x,y,63);ok(x[1][0]==333&&x[1][4]==-3&&y[1][4]==44,"model command keeps all five slots");a.setModelInputs(2,0,point);a.applyModelInputs(x,y,63);ok(x[1][0]==-2&&x[1][4]==-1,"expired model never local fallback");
 NativeInterface.available=false;a=new AiController(2);x=initial();y=initial();a.update(x,y,63);ok(x[0][0]==20&&x[0][1]==-1,"failed plan holds last legal target, never omniscient nearest");NativeInterface.available=true;
 a=new AiController(2);for(int k=0;k<8;k++)a.update(x,y,63);PlayerHistory h=new PlayerHistory();h.savePlayerPositions(x,y);int[] packet=new int[63];h.serialiseCurrentPlayerState(packet,3);ByteArrayOutputStream bytes=new ByteArrayOutputStream();GameWire.write(bytes,63,packet);int[] got=GameWire.read(new ByteArrayInputStream(bytes.toByteArray()));ok(Arrays.equals(packet,got),"host/client five-target signed wire including nonce");h.savePlayerPositions(initial(),initial());int[] retry=new int[63];h.serialiseHistoricalPlayerState(1,retry,3);for(int k=3;k<63;k++)ok(retry[k]==packet[k],"retry history exact");
 System.out.println("PASS "+n+" production Java assertions; JNI stub, not Android or real LAN");
 }
}'''
for name,data in [('NativeInterface.java',stub),('Local179Check.java',test),('MyRenderer.java','package com.xenris.liquidwarsos;public class MyRenderer {public static final int WIDTH=800,HEIGHT=480;}'),('ModelTerrain.java','package com.xenris.liquidwarsos.cloud;public class ModelTerrain {public static int widthForSize(int n){return n>800*480?1600:800;}}')]: (O/name).write_text(data,encoding='utf8')
sources=[S/'AiController.java',S/'AiDifficulty.java',S/'PlayerHistory.java',S/'net/GameWire.java',S/'WorldCamera.java',R/'tools/world-check/Comfort178Check.java',*O.glob('*.java')]
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-d',str(O),*map(str,sources)],check=True)
logs=[]
for cls in ['Local179Check','Comfort178Check']:
 p=subprocess.run([str(J/'java.exe'),'-cp',str(O),'com.xenris.liquidwarsos.'+cls],capture_output=True,text=True);logs.append(p.stdout+p.stderr);print(logs[-1]);assert p.returncode==0
assert 'AiController' not in (S/'activities/GameClientActivity.java').read_text()
for n in ['GameActivity','GameServerActivity']:assert 'AiController(StaticBits.aiDifficulty)' in (S/f'activities/{n}.java').read_text()
(R/'docs/game/local-ai-1.79/java-final.log').write_text('\n'.join(logs),encoding='utf8')
