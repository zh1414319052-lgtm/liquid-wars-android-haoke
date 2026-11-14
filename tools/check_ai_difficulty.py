"""Focused JVM checks of the shipped controller. No Android runtime/device."""
from pathlib import Path
import json, subprocess, sys, xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/difficulty-check'
JAVA=Path(r'C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin')
SRC=ROOT/'app/src/main/java/com/xenris/liquidwarsos'

STUB='''package com.xenris.liquidwarsos;
import java.util.*;
public final class NativeInterface {
 public static List<Integer> calls=new ArrayList<>(); public static int plans; public static boolean available=true;
 public static int getNearestDot(int team,short x,short y){calls.add(team);return ((x+team+1)<<16)|(y+team+2);}
 public static boolean updateAiTargets(short[] p){plans++;for(int i=0;i<60;i++)p[i]=(short)(100+i);return available;}
 public static boolean updateStrategicTargets(short[] p,int[] strategy){return updateAiTargets(p);}
 public static void reset(){calls.clear();plans=0;available=true;}
}'''
TEST='''package com.xenris.liquidwarsos;
import java.util.*;import java.nio.charset.StandardCharsets;import java.io.*;
import com.xenris.liquidwarsos.ui.DifficultyRoomName;
import com.xenris.liquidwarsos.net.GameWire;
public final class Check {
 static int checks;static void ok(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static short[][] initial(){short[][] v=new short[6][5];for(int i=0;i<6;i++){Arrays.fill(v[i],(short)-1);v[i][0]=(short)(20+i);}return v;}
 static boolean equal(short[][] a,short[][] b){return Arrays.deepEquals(a,b);}
 public static void main(String[] args)throws Exception{
  for(int mask:new int[]{0,1,0x3e,0x2a,0x3f})for(int level:new int[]{1,-1,99}){
   NativeInterface.reset();AiController c=new AiController(level);short[][] x=initial(),y=initial(),oldX=initial(),oldY=initial();
   for(int tick=0;tick<100;tick++){
    NativeInterface.calls.clear();c.update(x,y,mask);List<Integer> order=new ArrayList<>(NativeInterface.calls);NativeInterface.calls.clear();
    for(int p=0;p<6;p++)if((mask&(1<<p))!=0){int n=NativeInterface.getNearestDot(p,oldX[p][0],oldY[p][0]);oldX[p][0]=(short)(n>>>16);oldY[p][0]=(short)n;}
    ok(order.equals(NativeInterface.calls),"classic RNG call order");ok(equal(x,oldX)&&equal(y,oldY),"classic exact input sequence");
   }ok(NativeInterface.plans==0,"classic never plans");
  }
  NativeInterface.reset();AiController relaxed=new AiController(0);short[][] x=initial(),y=initial();
  for(int tick=0;tick<100;tick++){short before=x[1][0];relaxed.update(x,y,0x3e);ok((x[1][0]!=before)==(tick>=64&&tick%12==0),"relaxed response");ok(x[0][0]==20,"human untouched");}
  ok(NativeInterface.calls.size()==500&&NativeInterface.plans==0,"relaxed keeps RNG calls");
  NativeInterface.reset();AiController hard=new AiController(2);x=initial();y=initial();
  for(int tick=0;tick<17;tick++){hard.update(x,y,0x2a);for(int p=0;p<6;p++)for(int s=0;s<5;s++){ok(x[p][s]==((p%2==1)?100+p*10+s:(s==0?20+p:-1)),"hard targets/human isolation");}}
  ok(NativeInterface.plans==3&&NativeInterface.calls.size()==51,"hard cadence and old calls");
  NativeInterface.available=false;hard=new AiController(2);hard.update(x,y,0x2a);ok(x[1][1]==-1&&y[1][1]==-1,"legacy fallback clears flank points");
  NativeInterface.reset();new AiController(2).update(initial(),initial(),0x3e);ok(NativeInterface.plans==1,"replay fresh planner cadence");
  NativeInterface.reset();new AiController(2).update(initial(),initial(),0);ok(NativeInterface.plans==0&&NativeInterface.calls.isEmpty(),"six humans no AI work");
  NativeInterface.reset();AiController direct=new AiController(1);x=initial();y=initial();short[] points=new short[60];Arrays.fill(points,(short)-1);points[10]=300;points[15]=240;points[14]=500;points[19]=300;
  direct.setModelInputs(2,0,points);direct.applyModelInputs(x,y,0x3e);ok(x[1][0]==-2&&y[1][0]==-2,"startup waits motionless for model");
  direct.setModelInputs(2,2,points);direct.update(x,y,0x3e);ok(x[1][0]==300&&x[1][4]==500&&y[1][4]==300,"model controls all five fingers");ok(NativeInterface.calls.equals(Arrays.asList(2,3,4,5)),"model team never calls local AI");ok(x[0][0]==20,"model cannot control human");
  direct.setModelInputs(2,0,points);direct.applyModelInputs(x,y,0x3e);ok(x[1][0]==-2&&y[1][0]==-2,"expired command becomes idle before wire capture");
  Arrays.fill(points,(short)-1);direct.setModelInputs(2,2,points);direct.applyModelInputs(x,y,0x3e);ok(x[1][0]==-2&&y[1][4]==-1,"empty model action explicitly stops");
  direct.setModelInputs(2,0,points);direct.applyModelInputs(x,y,0x3e);ok(x[1][0]==-2,"stop never invokes local fallback");
  NativeInterface.reset();AiController onlyModels=new AiController(2);onlyModels.setModelInputs(63,0,points);onlyModels.update(x,y,63);ok(NativeInterface.calls.isEmpty()&&NativeInterface.plans==0,"six model teams never call local AI/planner even on Hard");
  PlayerHistory idleHistory=new PlayerHistory();idleHistory.savePlayerPositions(x,y);int[] idleFrame=new int[63];idleHistory.serialiseCurrentPlayerState(idleFrame,3);ByteArrayOutputStream idleBytes=new ByteArrayOutputStream();GameWire.write(idleBytes,63,idleFrame);int[] idleRead=GameWire.read(new ByteArrayInputStream(idleBytes.toByteArray()));for(int p=0;p<6;p++)ok(idleRead[3+p*10]==-2&&idleRead[4+p*10]==-2,"idle sentinel survives host/client signed int wire");
  PlayerHistory history=new PlayerHistory();x=initial();y=initial();new AiController(2).update(x,y,0x3e);history.savePlayerPositions(x,y);
  int[] packet=new int[63];packet[0]=0x72;packet[1]=0x73;packet[2]=123;history.serialiseCurrentPlayerState(packet,3);
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();GameWire.write(bytes,63,packet);int[] decoded=GameWire.read(new ByteArrayInputStream(bytes.toByteArray()));
  ok(Arrays.equals(packet,decoded),"five-target full STEP_GAME frame");
  history.savePlayerPositions(initial(),initial());int[] retry=new int[63];history.serialiseHistoricalPlayerState(1,retry,3);
  for(int i=3;i<63;i++)ok(retry[i]==packet[i],"resend retains all AI target slots");
  for(String name:new String[]{"Room", "房间".repeat(100),"🌊".repeat(40),"", "a\\nb", null})for(String label:new String[]{"Hard AI","困难 AI","休闲 AI","Classic AI"}){
   String room=DifficultyRoomName.format(name,label);ok(room.getBytes(StandardCharsets.UTF_8).length<=60,"wire byte bound");ok(room.endsWith(" · "+label),"visible suffix intact");ok(!room.contains("\\n")&&!room.contains("�"),"UTF8 codepoint boundary");
   byte[] utf8=room.getBytes(StandardCharsets.UTF_8);int[] message=new int[utf8.length+1];message[0]=0x7d;for(int i=0;i<utf8.length;i++)message[i+1]=utf8[i];
   bytes.reset();GameWire.write(bytes,message.length,message);decoded=GameWire.read(new ByteArrayInputStream(bytes.toByteArray()));for(int i=0;i<utf8.length;i++)utf8[i]=(byte)decoded[i+1];
   ok(room.equals(new String(utf8,StandardCharsets.UTF_8)),"existing room-name wire round trip");
  }
  System.out.println("PASS: "+checks+" JVM assertions; real controller + room formatter; JNI is a call-recording stub, not Android.");
 }
}'''

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    for name,data in [('NativeInterface.java',STUB),('Check.java',TEST),('MyRenderer.java','package com.xenris.liquidwarsos; public class MyRenderer {public static final int WIDTH=800,HEIGHT=480;}')]: (OUT/name).write_text(data,encoding='utf8')
    sources=[SRC/'AiDifficulty.java',SRC/'AiController.java',SRC/'ui/DifficultyRoomName.java',SRC/'server/LanRoom.java',SRC/'net/GameWire.java',SRC/'PlayerHistory.java',OUT/'NativeInterface.java',OUT/'MyRenderer.java',OUT/'Check.java']
    subprocess.run([str(JAVA/'javac.exe'),'-encoding','UTF-8','-d',str(OUT),*[str(p) for p in sources]],check=True)
    result=subprocess.run([str(JAVA/'java.exe'),'-cp',str(OUT),'com.xenris.liquidwarsos.Check'],check=True,capture_output=True,text=True)
    ns='{http://schemas.android.com/apk/res/android}'
    for layout in ['single_game_setup','multi_game_setup']:
        ids=[n.get(ns+'id') for n in ET.parse(ROOT/f'app/src/main/res/layout/{layout}.xml').iter()]
        assert ids.count('@+id/difficulty_spinner')==1 and ids.count('@+id/difficulty_description')==1
    translations=[]
    for lang in ['values','values-zh']:
        tree=ET.parse(ROOT/f'app/src/main/res/{lang}/ai_difficulty.xml')
        translations.append({n.tag+':'+n.get('name') for n in tree.getroot()})
        for array in tree.iter('string-array'):assert len(array)==3
    assert translations[0]==translations[1]
    assert 'AiController' not in (SRC/'activities/GameClientActivity.java').read_text('utf8')
    for activity in ['GameActivity','GameServerActivity']:
        assert 'AiController(StaticBits.aiDifficulty)' in (SRC/f'activities/{activity}.java').read_text('utf8')
    print(result.stdout.strip())
    (ROOT/(sys.argv[1] if len(sys.argv)>1 else 'docs/game/difficulty-1.54')/'java-check.json').write_text(json.dumps(dict(status='PASS',detail=result.stdout.strip(),localizedLayouts=2,clientPlanner=False,androidRuntime='NOT_TESTED'),ensure_ascii=False,indent=2)+'\n',encoding='utf8')

if __name__=='__main__':main()
