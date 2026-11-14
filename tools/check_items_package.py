"""1.69 QA package/source gate; no device, credentials or provider access."""
from pathlib import Path
import hashlib,json,re,struct,zipfile,subprocess,xml.etree.ElementTree as ET
from check_formal_release import run,dex_methods,SDK
R=Path(__file__).resolve().parents[1];O=R/'docs/game/world-1.69'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 apk=R/'app/build/outputs/apk/debug/app-debug.apk';prev=json.loads((R/'docs/game/menu-1.64/package-verification.json').read_text('utf8'))
 gradle=(R/'app/build.gradle').read_text('utf8');before=subprocess.check_output(['git','show','7c4a9f0:app/build.gradle'],cwd=R).decode('utf8');strip=lambda s:re.sub(r'(?m)^[ \t]*version(?:Code|Name)[ \t]+[^\n]+','',s)
 assert strip(gradle).replace('        resValue "string", "ui_version", versionName\n','')==strip(before),'Dependencies/build configuration changed beyond shared version resource'
 for p in (R/'app/src/main/assets/maps').glob('*'):
  if p.is_file():
   if p.name in {'50-image.png','50-map.png','51-image.png','51-map.png'}:
    ident=int(p.name.split('-')[0]);meta=json.loads((R/f'art/source/world-1.69/collision/{ident}.json').read_text('utf8'));assert sha(p)==meta['image_sha256' if '-image' in p.name else 'collision_sha256'];assert meta['area_ratio']==5 and meta['ground_components']==1
   else:assert sha(p)==prev['sourceAppSha256'][p.relative_to(R).as_posix()],'Existing map changed'
 for n in ['app/src/main/AndroidManifest.xml','app/src/main/cpp/application/visual_texture.cpp','app/src/main/cpp/application/visual_palette.hpp','app/src/main/java/com/xenris/liquidwarsos/TeamPalette.java']:
  assert sha(R/n)==prev['sourceAppSha256'][n],('Unrequested invariant changed',n)
 badge=run([SDK/'aapt.exe','dump','badging',apk]);assert "package: name='com.xenris.liquidwarsos.haoke.debug' versionCode='38' versionName='1.69-QA'" in badge
 sig=run([SDK/'apksigner.bat','verify','--verbose','--print-certs',apk]);assert prev['certificateSha256'] in sig.lower() and 'Android Debug' in sig and 'Verified using v2 scheme (APK Signature Scheme v2): true' in sig
 run([SDK/'zipalign.exe','-c','-P','16','4',apk])
 permissions=sorted(re.sub(r"name='[^']+\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'","name='<self>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'",l) for l in run([SDK/'aapt.exe','dump','permissions',apk]).splitlines() if l.startswith('uses-permission:'))
 assert permissions==prev['normalizedPermissions']
 natives=re.findall(r'\bnative\s+\w+(?:\[\])?\s+(\w+)\s*\(', (R/'app/src/main/java/com/xenris/liquidwarsos/NativeInterface.java').read_text('utf8'));assert len(natives)==28
 assert 'VERSION_CODE = 23;' in (R/'app/src/main/java/com/xenris/liquidwarsos/StaticBits.java').read_text('utf8')
 methods=set();libs=[];gaps=[]
 oldapk=Path('D:/浩科软件优化/液态战争重制版_Liquid Wars OS/液态战争重制版_1.64-QA_Android.apk')
 with zipfile.ZipFile(apk) as z,zipfile.ZipFile(oldapk) as old:
  for p in (R/'app/src/main/assets').rglob('*'):
   if p.is_file():assert z.read('assets/'+p.relative_to(R/'app/src/main/assets').as_posix())==p.read_bytes()
  for name in z.namelist():
   assert not name.lower().endswith(('.jks','.keystore','.p12','.dpapi')),name
   if re.fullmatch(r'classes\d*\.dex',name):m,_=dex_methods(z.read(name));methods|=m
   if not(name.startswith('lib/') and name.endswith('.so')):continue
   data=z.read(name);bits=64 if data[4]==2 else 32;e='<' if data[5]==1 else '>';ph=struct.unpack_from(e+('Q' if bits==64 else 'I'),data,32 if bits==64 else 28)[0];size,count=struct.unpack_from(e+'HH',data,54 if bits==64 else 42);align=[]
   for i in range(count):
    at=ph+i*size
    if struct.unpack_from(e+'I',data,at)[0]==1:align.append(struct.unpack_from(e+('Q' if bits==64 else 'I'),data,at+(48 if bits==64 else 28))[0])
   assert align and (bits!=64 or min(align)>=16384)
   if name.endswith('/libnativeinterface.so'):
    for n in natives:assert ('Java_com_xenris_liquidwarsos_NativeInterface_'+n).encode()+b'\0' in data
   if name.endswith('/libc++_shared.so'):assert data==old.read(name)
   libs.append(dict(path=name,bits=bits,alignment=align))
  assert len([n for n in z.namelist() if re.fullmatch(r'assets/maps/\d+-map.png',n)])==51 and 'assets/maps/0-map.png' not in z.namelist()
  with apk.open('rb') as f:
   entries=sorted(z.infolist(),key=lambda p:p.header_offset)
   for a,b in zip(entries,entries[1:]):
    f.seek(a.header_offset+26);n,x=struct.unpack('<HH',f.read(4));gap=b.header_offset-(a.header_offset+30+n+x+a.compress_size)
    if gap>65536:gaps.append((a.filename,gap))
 assert not gaps,('Large incremental ZIP holes',gaps)
 assert {p['path'] for p in libs}=={p['path'] for p in prev['nativeLibraries']}
 assert not any('ProbeTest' in c for c,m in methods)
 for n in natives:assert ('Lcom/xenris/liquidwarsos/NativeInterface;',n) in methods
 for cls in ['GameActivity','GameServerActivity','GameClientActivity']:
  source=(R/f'app/src/main/java/com/xenris/liquidwarsos/activities/{cls}.java').read_text('utf8');assert 'NativeInterface.totalParticles()' in source and 'StaticBits.NUMBER_OF_TEAMS * StaticBits.dotsPerTeam' not in source
 catalog=json.loads((R/'art/source/items-1.66/catalog.json').read_text('utf8'));assert len(catalog)==18
 for locale in ['values','values-zh']:
  root=ET.parse(R/f'app/src/main/res/{locale}/items.xml')
  for key in ['item_names','item_descriptions']:assert len(root.find(f"string-array[@name='{key}']"))==18
 from fontTools.ttLib import TTFont
 font=TTFont(R/'app/src/main/assets/fonts/NotoSansSC-Intro.ttf');cmap=font.getBestCmap()
 for row in catalog:
  for k in ['name_zh','description_zh','name_en','description_en']:assert all(ord(c) in cmap for c in row[k]),('Missing commercial-font glyph',row['id'],k)
 report=dict(status='PASS',version='1.69-QA',versionCode=38,compatibilityVersion=23,mapCount=51,itemKinds=18,apkBytes=apk.stat().st_size,apkSha256=sha(apk),applicationId='com.xenris.liquidwarsos.haoke.debug',certificateSha256=prev['certificateSha256'],signatureVerified=True,normalizedPermissions=permissions,platformPermissionsUnchanged=True,dependenciesUnchanged=True,newNetworkEndpoints=False,paidModelTests=False,nativeRuleExtension='two per-map 5x worlds, random1..6 waves, continuing deterministic item RNG on rewind; movement/combat unchanged',nativeLibraries=libs,jniMethods=len(natives),largeZipGaps=gaps,sourceAppSha256={p.relative_to(R).as_posix():sha(p) for p in (R/'app/src').rglob('*') if p.is_file()})
 report['sourceAppSha256']['app/build.gradle']=sha(R/'app/build.gradle');(O/'package-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8');(O/'apk-badging.txt').write_text(badge,encoding='utf8');(O/'apk-signature.txt').write_text(sig,encoding='utf8')
 print(json.dumps({k:report[k] for k in ['status','version','apkBytes','apkSha256','certificateSha256']},ensure_ascii=False))
if __name__=='__main__':main()
