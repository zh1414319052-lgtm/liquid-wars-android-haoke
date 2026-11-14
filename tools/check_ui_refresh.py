"""Focused 1.52 resource/invariant checks and real result adapter with Java API doubles.
Not Android runtime, screenshots, lifecycle, network or performance tests.
"""
from pathlib import Path
import hashlib,json,subprocess,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'docs/game/ui-1.52';BUILD=ROOT/'build/ui-result-check'
JAVA=Path(r'C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p):return (ROOT/p).read_text('utf8')
baseline=json.loads((OUT/'baseline-files.json').read_text('utf8'))
preserved=[]
for rel,digest in baseline.items():
    if any(s in rel for s in ['/cpp/','/assets/','/net/']) or rel.endswith(('/Client.java','/Server.java','/ServerFinder.java','/StaticBits.java','/GameViewport.java','/MyGLSurfaceView.java','/GameActivity.java','/GameServerActivity.java','/GameClientActivity.java','/ClientGameSetupActivity.java','/BluetoothMenuActivity.java','/MultiplayerMenuActivity.java','/AndroidManifest.xml','/layout/game.xml')):
        assert sha(ROOT/rel)==digest,('invariant modified',rel);preserved.append(rel)
ns='{http://schemas.android.com/apk/res/android}'
layouts=['main_menu','single_game_setup','multi_game_setup','multiplayer_menu','bluetooth_menu','client_game_setup','instructions']
for layout in layouts:
    tree=ET.parse(ROOT/f'app/src/main/res/layout/{layout}.xml').getroot()
    assert tree.get(ns+'id')=='@+id/ui_root'
    assert tree.get(ns+'fitsSystemWindows')=='true'
    assert any(n.tag=='ScrollView' for n in tree.iter()),('scroll fallback',layout)
    if 'game_setup' in layout:
        maps=[n for n in tree.iter('ImageView') if n.get(ns+'id')=='@+id/map_imageview']
        assert len(maps)==1 and maps[0].get(ns+'scaleType')=='fitCenter'
for rel in ['single_game_setup','multi_game_setup']:
    ids={n.get(ns+'id'):n.tag for n in ET.parse(ROOT/f'app/src/main/res/layout/{rel}.xml').iter() if n.get(ns+'id')}
    for id in ['map_spinner','timeout_spinner','teamsize_spinner']:assert ids['@+id/'+id]=='Spinner'
    for id in ['previous_button','next_button']:assert ids['@+id/'+id]=='androidx.appcompat.widget.AppCompatImageButton'
    assert ids['@+id/start_button']=='com.google.android.material.button.MaterialButton'
for activity in ['SinglePlayerGameSetupActivity','MultiplayerGameSetupActivity']:
    s=read('app/src/main/java/com/xenris/liquidwarsos/activities/'+activity+'.java')
    assert 'R.layout.ui_spinner_item' in s and 'R.layout.ui_spinner_dropdown_item' in s and 'setOnLongClickListener' in s
en=ET.parse(ROOT/'app/src/main/res/values/ui_strings.xml').getroot();zh=ET.parse(ROOT/'app/src/main/res/values-zh/ui_strings.xml').getroot()
assert {n.get('name') for n in en if n.get('translatable')!='false'}=={n.get('name') for n in zh}
# Validate native colors against the saved editable project, including text contrast.
design=json.loads((OUT/'design.haoke-ui.json').read_text('utf8'))
colors={n.get('name'):n.text for n in ET.parse(ROOT/'app/src/main/res/values/ui_tokens.xml').getroot() if n.tag=='color'}
for k,v in design['tokens'].items():
    if isinstance(v,str) and v.startswith('#'):assert colors['ui_'+k]==v
def lum(c):
    rgb=[int(c[i:i+2],16)/255 for i in (1,3,5)];return sum(a*b for a,b in zip([.2126,.7152,.0722],[v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in rgb]))
contrast={}
for fg,bg in [('ink','surface'),('muted','bg'),('onAccent','accent')]:
    a,b=sorted([lum(design['tokens'][fg]),lum(design['tokens'][bg])]);ratio=(b+.05)/(a+.05);assert ratio>=4.5;contrast[fg+'/'+bg]=round(ratio,2)
sources={
'android/view/View.java':'''package android.view; import java.util.*; public class View {
 public static final int VISIBLE=0,GONE=8; public int visibility=GONE; public OnClickListener click;
 public final Map<Integer,View> children=new HashMap<>(); public void setVisibility(int v){visibility=v;}
 public interface OnClickListener {void onClick(View v);} public void setOnClickListener(OnClickListener c){click=c;}
 @SuppressWarnings("unchecked") public <T extends View>T findViewById(int id){return (T)children.get(id);}
 public void tap(){if(click!=null)click.onClick(this);}
}''',
'android/widget/TextView.java':'''package android.widget; public class TextView extends android.view.View {public Object text; public void setText(int t){text=t;} public void setText(CharSequence t){text=t;}}''',
'android/view/LayoutInflater.java':'''package android.view; import android.widget.TextView; public class LayoutInflater {public View inflate(int id,Object p){View v=new View();for(int n=1;n<=4;n++)v.children.put(n,new TextView());return v;}}''',
'android/app/Activity.java':'''package android.app; public class Activity {public android.view.LayoutInflater getLayoutInflater(){return new android.view.LayoutInflater();}}''',
'android/app/AlertDialog.java':'''package android.app; public class AlertDialog {
 public boolean cancelable=true,outside=true,showing=false;public android.view.View view;
 public void setCanceledOnTouchOutside(boolean v){outside=v;} public boolean isShowing(){return showing;}
 public void show(){showing=true;}public void dismiss(){showing=false;}
 public static class Builder {final AlertDialog d=new AlertDialog();public Builder(Activity a){} public Builder setView(android.view.View v){d.view=v;return this;} public Builder setCancelable(boolean v){d.cancelable=v;return this;}public AlertDialog create(){return d;}}
}''',
'com/xenris/liquidwarsos/R.java':'''package com.xenris.liquidwarsos; public class R {public static class id {public static final int result_message=1,result_primary=2,result_exit=3,result_spectate=4;}public static class layout {public static final int match_result=1;}public static class string {public static final int continue_spectating=999;}}''',
'DialogCheck.java':'''import android.app.*;import android.widget.TextView;import com.xenris.liquidwarsos.*;
public class DialogCheck {public static void main(String[] args){int[] calls={0,0,0};int checks=0;
 for(int role=0;role<3;role++)for(int outcome=0;outcome<3;outcome++)for(int action=0;action<3;action++){
  boolean spectate=outcome==2;if(action==2&&!spectate)continue;
  AlertDialog d=MatchResultDialog.show(new Activity(),"result",100+role,()->calls[0]++,200+role,()->calls[1]++,spectate?()->calls[2]++:null);
  if(d.cancelable||d.outside||!d.showing)throw new AssertionError("Persistent visible dialog");
  TextView p=d.view.findViewById(R.id.result_primary),e=d.view.findViewById(R.id.result_exit),s=d.view.findViewById(R.id.result_spectate);
  if(!p.text.equals(100+role)||!e.text.equals(200+role)||(s.visibility==0)!=spectate)throw new AssertionError("Labels/option");
  android.view.View b=action==0?p:action==1?e:s;b.tap();b.tap();if(d.showing)throw new AssertionError("Dismiss before navigation");checks++;
 }
 if(calls[0]!=9||calls[1]!=9||calls[2]!=3)throw new AssertionError("Callback/repeated tap routing");
 System.out.println("PASS: 21 callback scenarios, 3 roles x 3 outcomes, repeated taps ignored; Java API doubles only.");}}
'''}
for name,s in sources.items():p=BUILD/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
classes=BUILD/'classes';classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(JAVA/'javac.exe'),'-encoding','UTF-8','-d',str(classes),*[str(BUILD/n) for n in sources],str(ROOT/'app/src/main/java/com/xenris/liquidwarsos/MatchResultDialog.java')],check=True)
subprocess.run([str(JAVA/'java.exe'),'-cp',str(classes),'DialogCheck'],check=True)
report=dict(status='PASS',baseline='fa148ab / 1.51-QA',preserved_file_count=len(preserved),preserved_files=preserved,menu_layouts=layouts,translation_keys=len(zh),contrast=contrast,result_callback_scenarios=21,device='NOT_TESTED',android_layout_rendering='NOT_TESTED',native_ui_interaction='NOT_TESTED',performance='NOT_TESTED')
(OUT/'focused-check.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8');print('PASS: resources, tokens, translations, map fit, core/network invariants. No device or emulator used.')
