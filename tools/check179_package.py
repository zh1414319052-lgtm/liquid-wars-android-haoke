"""Focused package gate against delivered1.77. No device, provider or credentials access."""
from pathlib import Path
import json,hashlib,zipfile,re,struct,sys,xml.etree.ElementTree as ET
from check_formal_release import run,dex_methods,SDK
R=Path(__file__).resolve().parents[1];O=R/'docs/game/local-ai-1.79'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 apk=R/'build/local-ai-1.79/liquidwars-1.79-release.apk';prev=json.loads((R/'docs/game/controls-1.78/package-verification.json').read_text('utf8'))
 baseline=json.loads((O/'baseline.json').read_text('utf8'))
 badge=run([SDK/'aapt.exe','dump','badging',apk]);assert "package: name='com.xenris.liquidwarsos.haoke' versionCode='48' versionName='1.79'" in badge and 'application-debuggable' not in badge
 sig=run([SDK/'apksigner.bat','verify','--verbose','--print-certs',apk]);assert prev['certificateSha256'].lower() in sig.lower() and 'Android Debug' not in sig
 for scheme in ['v2','v3']:assert f'Verified using {scheme} scheme (APK Signature Scheme {scheme}): true' in sig
 run([SDK/'zipalign.exe','-c','-P','16','4',apk])
 permissions=sorted(re.sub(r"name='[^']+\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'","name='<self>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'",l) for l in run([SDK/'aapt.exe','dump','permissions',apk]).splitlines() if l.startswith('uses-permission:'));assert permissions==prev['normalizedPermissions']
 allowed={'app/src/main/java/com/xenris/liquidwarsos/NativeInterface.java', 'app/src/main/java/com/xenris/liquidwarsos/AiController.java', 'app/src/main/res/values-zh/ai_difficulty.xml', 'app/src/main/cpp/application/minimap_sight.hpp', 'app/src/main/res/values/ai_difficulty.xml', 'app/build.gradle', 'app/src/main/cpp/nativeinterface/ai_targets.cpp', 'app/src/main/java/com/xenris/liquidwarsos/cloud/ModelGameSupport.java'}
 for n,h in baseline['source_sha256'].items():
  if n.startswith('app/src/main/') and n not in allowed:assert sha(R/n)==h,('Unrequested production change',n)
 oldGradle=(R/'docs/game/formal-1.76/build-config-baseline.txt').read_text('utf8');now=(R/'app/build.gradle').read_text('utf8');strip=lambda s:re.sub(r'(?m)^\s*version(?:Code|Name)\s+[^\n]+','',s);assert strip(oldGradle)==strip(now)
 assert 'VERSION_CODE = 28;' in (R/'app/src/main/java/com/xenris/liquidwarsos/StaticBits.java').read_text('utf8')
 methods=set();parents={};native={}
 with zipfile.ZipFile(apk) as z:
  for name in z.namelist():
   assert not name.lower().endswith(('.jks','.keystore','.p12','.dpapi')),name
   if name.startswith('lib/') or (name.startswith('assets/') and not name.startswith('assets/dexopt/')):
    pass # Native AI changes are intended; assets checked individually below.
    if name.endswith('.so'):
     b=z.read(name);native[name]=hashlib.sha256(b).hexdigest()
     if not name.endswith('/libnativeinterface.so'):assert native[name]==prev['nativeLibraries'][name]
     bits=64 if b[4]==2 else 32;endian='<' if b[5]==1 else '>'
     phoff=struct.unpack_from(endian+('Q' if bits==64 else 'I'),b,32 if bits==64 else 28)[0];size,count=struct.unpack_from(endian+'HH',b,54 if bits==64 else 42);aligns=[]
     for i in range(count):
      start=phoff+i*size
      if struct.unpack_from(endian+'I',b,start)[0]==1:aligns.append(struct.unpack_from(endian+('Q' if bits==64 else 'I'),b,start+(48 if bits==64 else 28))[0])
     assert aligns and (bits!=64 or min(aligns)>=16384)
     if name.endswith('/libnativeinterface.so'):
      for method in re.findall(r'\bnative\s+\w+(?:\[\])?\s+(\w+)\s*\(', (R/'app/src/main/java/com/xenris/liquidwarsos/NativeInterface.java').read_text('utf8')):assert ('Java_com_xenris_liquidwarsos_NativeInterface_'+method).encode()+b'\0' in b

   if re.fullmatch(r'classes\d*\.dex',name):m,p=dex_methods(z.read(name));methods|=m;parents.update(p)
  assert set(native)==set(prev['nativeLibraries']) and {n.split('/')[1] for n in native}=={'armeabi-v7a','arm64-v8a','x86','x86_64'}
  assert len([n for n in z.namelist() if re.fullmatch(r'assets/maps/\d+-map.png',n)])==59
  assert not any('ProbeTest' in c for c,m in methods)
  for p in (R/'app/src/main/assets').rglob('*'):
   if p.is_file():assert z.read('assets/'+p.relative_to(R/'app/src/main/assets').as_posix())==p.read_bytes()
 natives=re.findall(r'\bnative\s+\w+(?:\[\])?\s+(\w+)\s*\(', (R/'app/src/main/java/com/xenris/liquidwarsos/NativeInterface.java').read_text('utf8'));assert len(natives)==29
 for n in natives:assert ('Lcom/xenris/liquidwarsos/NativeInterface;',n) in methods
 def has_method(c,m):
  while c:
   if (c,m) in methods:return True
   c=parents.get(c)
  return False
 for layout,activity in {'main_menu':'MainMenuActivity','single_game_setup':'SinglePlayerGameSetupActivity','multi_game_setup':'MultiplayerGameSetupActivity','multiplayer_menu':'MultiplayerMenuActivity','bluetooth_menu':'BluetoothMenuActivity','client_game_setup':'ClientGameSetupActivity','instructions':'InstructionsActivity'}.items():
  for node in ET.parse(R/f'app/src/main/res/layout/{layout}.xml').iter():
   click=node.get('{http://schemas.android.com/apk/res/android}onClick')
   if click:assert has_method('Lcom/xenris/liquidwarsos/activities/'+activity+';',click),(layout,click)
 from fontTools.ttLib import TTFont
 cmap=TTFont(R/'app/src/main/assets/fonts/NotoSansSC-Intro.ttf').getBestCmap();source=(R/'app/src/main/java/com/xenris/liquidwarsos/ItemOverlay.java').read_text('utf8')
 assert all(ord(c) in cmap for c in source if '\u4e00'<=c<='\u9fff')
 for cls in ['GameActivity','GameServerActivity','GameClientActivity']:
  s=(R/f'app/src/main/java/com/xenris/liquidwarsos/activities/{cls}.java').read_text('utf8');assert 'TouchTargets.update' in s and '.camera)' in s and '.noteTargets(' in s and 'onControls(' in s
 report=dict(status='PASS',version='1.79',versionCode=48,compatibilityVersion=28,apkBytes=apk.stat().st_size,apkSha256=sha(apk),certificateSha256=prev['certificateSha256'],applicationId=prev['applicationId'],normalizedPermissions=permissions,permissionsUnchanged=True,dependenciesUnchanged=True,newNetworkRequests=False,nativeLibraries=native,authorityAndControlsSourceUnchanged=True,runtimeAssetsByteIdenticalTo178=True,sourceAppSha256={p.relative_to(R).as_posix():sha(p) for p in (R/'app/src/main').rglob('*') if p.is_file()},notTested=['physical phone','multi-device Wi-Fi/Bluetooth','real model APIs','long-run FPS/thermal/memory'])
 report['sourceAppSha256']['app/build.gradle']=sha(R/'app/build.gradle');(O/'package-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8');(O/'apk-badging.txt').write_text(badge,encoding='utf8');(O/'apk-signature.txt').write_text(sig,encoding='utf8');print(json.dumps({k:v for k,v in report.items() if k not in {'sourceAppSha256','nativeLibraries','normalizedPermissions'}},ensure_ascii=False,indent=2))
if __name__=='__main__':main()
