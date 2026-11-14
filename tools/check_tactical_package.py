"""1.59 QA archive checks. No device, network or alternate renderer."""
from pathlib import Path
import argparse, hashlib, json, re, struct, zipfile, xml.etree.ElementTree as ET
from PIL import Image
from check_formal_release import run, dex_methods, SDK

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/game/tactical-1.59'
CERT='a1519f2f24b603f1cd5ab4e76ec440c5275036a8dfcc439eb1ab868f35d0ddb2'
ALLOWED={'app/src/main/java/com/xenris/liquidwarsos/activities/GameServerActivity.java', 'app/src/main/assets/maps/35-map.png', 'app/src/main/assets/maps/39-image.png', 'app/src/main/assets/maps/19-image.png', 'app/src/main/cpp/application/state.hpp', 'app/src/main/assets/maps/33-image.png', 'app/src/main/assets/maps/0-map.png', 'app/src/main/assets/maps/6-image.png', 'app/src/main/assets/maps/2-map.png', 'app/src/main/assets/maps/44-image.png', 'app/src/main/assets/maps/37-image.png', 'app/src/main/assets/maps/25-image.png', 'app/src/main/assets/maps/18-image.png', 'app/src/main/assets/maps/31-map.png', 'app/src/main/assets/maps/27-map.png', 'app/src/main/assets/maps/31-image.png', 'app/src/main/assets/maps/21-image.png', 'app/src/main/cpp/application/state.cpp', 'app/src/main/assets/maps/13-map.png', 'app/src/main/assets/maps/2-image.png', 'app/src/main/java/com/xenris/liquidwarsos/NativeInterface.java', 'app/src/main/assets/maps/37-map.png', 'app/src/main/assets/maps/34-image.png', 'app/src/main/assets/maps/4-image.png', 'app/src/main/assets/maps/9-map.png', 'app/src/main/assets/maps/22-map.png', 'app/src/main/assets/maps/1-map.png', 'app/src/main/assets/maps/5-map.png', 'app/src/main/assets/maps/19-map.png', 'app/src/main/assets/maps/27-image.png', 'app/src/main/assets/maps/21-map.png', 'app/src/main/res/values-zh/strings.xml', 'app/src/main/assets/maps/0-image.png', 'app/src/main/assets/maps/23-map.png', 'app/src/main/assets/maps/34-map.png', 'app/src/main/assets/maps/12-map.png', 'app/src/main/assets/maps/14-image.png', 'app/src/main/assets/maps/36-map.png', 'app/src/main/assets/maps/43-map.png', 'app/src/main/assets/maps/3-map.png', 'app/src/main/assets/maps/41-map.png', 'app/src/main/cpp/application/info.hpp', 'app/src/main/assets/maps/41-image.png', 'app/src/main/assets/maps/20-map.png', 'app/src/main/assets/maps/38-map.png', 'app/src/main/assets/maps/22-image.png', 'app/src/main/assets/maps/26-image.png', 'app/src/main/assets/maps/33-map.png', 'app/src/main/assets/maps/12-image.png', 'app/src/main/assets/maps/30-map.png', 'app/src/main/assets/maps/36-image.png', 'app/src/main/cpp/nativeinterface/ai_targets.cpp', 'app/src/main/assets/maps/7-image.png', 'app/src/main/assets/maps/10-image.png', 'app/src/main/assets/maps/8-image.png', 'app/src/main/assets/maps/7-map.png', 'app/src/main/assets/maps/29-image.png', 'app/src/main/assets/maps/43-image.png', 'app/src/main/assets/maps/16-map.png', 'app/src/main/assets/maps/32-map.png', 'app/src/main/assets/maps/24-image.png', 'app/src/main/assets/maps/32-image.png', 'app/src/main/res/values/ui_strings.xml', 'app/src/main/assets/maps/24-map.png', 'app/src/main/java/com/xenris/liquidwarsos/activities/GameClientActivity.java', 'app/src/main/assets/maps/14-map.png', 'app/src/main/assets/maps/8-map.png', 'app/src/main/assets/maps/30-image.png', 'app/src/main/assets/maps/28-map.png', 'app/src/main/assets/maps/18-map.png', 'app/src/main/assets/maps/6-map.png', 'app/src/main/assets/maps/42-image.png', 'app/src/main/assets/maps/11-image.png', 'app/src/main/assets/maps/23-image.png', 'app/src/main/cpp/application/info.cpp', 'app/src/main/assets/maps/45-image.png', 'app/src/main/assets/maps/4-map.png', 'app/src/main/assets/maps/28-image.png', 'app/src/main/java/com/xenris/liquidwarsos/StaticBits.java', 'app/src/main/assets/maps/13-image.png', 'app/src/main/res/values-zh/ui_strings.xml', 'app/src/main/res/values-fr/strings.xml', 'app/src/main/assets/maps/15-map.png', 'app/src/main/assets/maps/17-image.png', 'app/src/main/assets/maps/10-map.png', 'app/build.gradle', 'app/src/main/assets/maps/40-image.png', 'app/src/main/assets/maps/11-map.png', 'app/src/main/assets/maps/25-map.png', 'app/src/main/res/values-de/strings.xml', 'app/src/main/assets/maps/40-map.png', 'app/src/main/res/values-it/strings.xml', 'app/src/main/assets/maps/20-image.png', 'app/src/main/assets/maps/9-image.png', 'app/src/main/assets/maps/44-map.png', 'app/src/main/assets/maps/17-map.png', 'app/src/main/assets/maps/1-image.png', 'app/src/main/assets/maps/26-map.png', 'app/src/main/assets/maps/42-map.png', 'app/src/main/assets/maps/3-image.png', 'app/src/main/assets/maps/39-map.png', 'app/src/main/assets/maps/15-image.png', 'app/src/main/assets/maps/16-image.png', 'app/src/main/assets/maps/35-image.png', 'app/src/main/assets/maps/29-map.png', 'app/src/main/assets/maps/38-image.png', 'app/src/main/res/values/strings.xml', 'app/src/main/java/com/xenris/liquidwarsos/activities/GameActivity.java', 'app/src/main/assets/maps/5-image.png', 'app/src/main/java/com/xenris/liquidwarsos/Util.java', 'app/src/main/assets/maps/45-map.png'}


def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--apk',type=Path,default=ROOT/'app/build/outputs/apk/debug/app-debug.apk');args=parser.parse_args();apk=args.apk.resolve()
    badge=run([SDK/'aapt.exe','dump','badging',apk]);assert "package: name='com.xenris.liquidwarsos.haoke.debug' versionCode='28' versionName='1.59-QA'" in badge
    assert 'application-debuggable' in badge and "sdkVersion:'26'" in badge
    signature=run([SDK/'apksigner.bat','verify','--verbose','--print-certs',apk]);assert CERT in signature.lower() and 'Android Debug' in signature
    assert 'Verified using v2 scheme (APK Signature Scheme v2): true' in signature
    run([SDK/'zipalign.exe','-c','-P','16','4',apk])
    (OUT/'apk-badging.txt').write_text(badge,encoding='utf8');(OUT/'apk-signature.txt').write_text(signature,encoding='utf8')
    baseline=json.loads((OUT/'baseline-1.58.json').read_text('utf8'));changed=[];preserved=[]
    for rel,digest in baseline.items():
        if sha(ROOT/rel)!=digest.lower():assert rel in ALLOWED,rel;changed.append(rel)
        else:preserved.append(rel)
    expected_new={'app/src/main/assets/maps/49-image.png', 'app/src/main/cpp/application/deployment.hpp', 'app/src/androidTest/java/com/xenris/liquidwarsos/TacticalMapsProbeTest.java', 'app/src/main/assets/maps/46-map.png', 'app/src/main/assets/maps/48-image.png', 'app/src/main/assets/maps/49-map.png', 'app/src/main/cpp/application/spawn.hpp', 'app/src/main/assets/maps/46-image.png', 'app/src/main/assets/maps/48-map.png', 'app/src/main/assets/maps/47-map.png', 'app/src/main/assets/maps/47-image.png'}
    current={p.relative_to(ROOT).as_posix() for p in (ROOT/'app/src').rglob('*') if p.is_file()}|{'app/build.gradle'}
    assert current-set(baseline)==expected_new and set(baseline)<=current
    fixture=json.loads((ROOT/'docs/game/formal-1.53/baseline-1.52.json').read_text('utf8'))
    permissions=sorted(re.sub(r"name='[^']+\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'","name='<self>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'",line) for line in run([SDK/'aapt.exe','dump','permissions',apk]).splitlines() if line.startswith('uses-permission:'))
    assert permissions==fixture['normalized_permissions']
    java=ROOT/'app/src/main/java/com/xenris/liquidwarsos'
    static=(java/'StaticBits.java').read_text('utf8');assert 'VERSION_CODE = 16;' in static and 'NUMBER_OF_MAPS = 50;' in static
    native=re.findall(r'\bnative\s+\w+(?:\[\])?\s+(\w+)\s*\(', (java/'NativeInterface.java').read_text('utf8'));assert len(native)==19
    methods=set();parents={};libs=[];gaps=[]
    native_baseline=json.loads((ROOT/'docs/game/realtime-1.57/native-baseline-1.56.json').read_text('utf8'))
    with zipfile.ZipFile(apk) as z:
        assert z.read('assets/licenses/GPL-3.0.txt')==(ROOT/'LICENSE').read_bytes()
        for n in z.namelist():
            assert not n.lower().endswith(('.p12','.jks','.keystore','.dpapi')),n
            if re.fullmatch(r'classes\d*\.dex',n):m,p=dex_methods(z.read(n));methods|=m;parents.update(p)
            if not(n.startswith('lib/') and n.endswith('.so')):continue
            b=z.read(n)
            if n.endswith('/libc++_shared.so'):assert hashlib.sha256(b).hexdigest()==native_baseline[n],('runtime changed',n)
            bits=64 if b[4]==2 else 32;endian='<' if b[5]==1 else '>'
            phoff=struct.unpack_from(endian+('Q' if bits==64 else 'I'),b,32 if bits==64 else 28)[0];size,count=struct.unpack_from(endian+'HH',b,54 if bits==64 else 42);aligns=[]
            for i in range(count):
                start=phoff+i*size
                if struct.unpack_from(endian+'I',b,start)[0]==1:aligns.append(struct.unpack_from(endian+('Q' if bits==64 else 'I'),b,start+(48 if bits==64 else 28))[0])
            assert aligns and (bits!=64 or min(aligns)>=16384)
            if n.endswith('/libnativeinterface.so'):
                for method in native:assert ('Java_com_xenris_liquidwarsos_NativeInterface_'+method).encode()+b'\0' in b,(n,method)
            libs.append(dict(path=n,bits=bits,alignment=aligns))
        for p in (ROOT/'app/src/main/assets/maps').iterdir():
            if p.is_file():assert z.read('assets/maps/'+p.name)==p.read_bytes(),p
        # Record large inter-entry holes without confusing normal native alignment/signing.
        with apk.open('rb') as stream:
            entries=sorted(z.infolist(),key=lambda i:i.header_offset)
            for a,b in zip(entries,entries[1:]):
                stream.seek(a.header_offset+26);name,extra=struct.unpack('<HH',stream.read(4))
                gap=b.header_offset-(a.header_offset+30+name+extra+a.compress_size)
                if gap>65536:gaps.append(dict(after=a.filename,bytes=gap))
    assert not gaps,('unexpected packaging gaps',gaps)
    assert {i['path'] for i in libs}==set(fixture['native_library_paths'])
    for method in native:assert ('Lcom/xenris/liquidwarsos/NativeInterface;',method) in methods
    for cls in ['AiController','AiDifficulty','ui/DifficultyPicker','ui/DifficultyRoomName','cloud/ModelAction','cloud/ModelTerrain','cloud/ModelStream','cloud/ModelConfig','cloud/ModelProtocol','cloud/ModelHttp','cloud/ModelMatch','cloud/ModelStore','cloud/ModelProviders','cloud/ModelDirectory','cloud/ModelSetupDraft','activities/ModelSettingsActivity','AllianceRules','AllianceSettings']:
        assert any(c=='Lcom/xenris/liquidwarsos/'+cls+';' for c,m in methods),cls
    assert not any(('ModelControlProbeTest' in c or 'TacticalMapsProbeTest' in c) for c,m in methods), 'Test probe must not ship in game APK'
    handlers=0;ns='{http://schemas.android.com/apk/res/android}'
    roles={'main_menu':'MainMenuActivity','single_game_setup':'SinglePlayerGameSetupActivity','multi_game_setup':'MultiplayerGameSetupActivity','multiplayer_menu':'MultiplayerMenuActivity','bluetooth_menu':'BluetoothMenuActivity','client_game_setup':'ClientGameSetupActivity','instructions':'InstructionsActivity'}
    def has_method(c,m):
        while c:
            if(c,m) in methods:return True
            c=parents.get(c)
        return False
    for layout,activity in roles.items():
        for node in ET.parse(ROOT/f'app/src/main/res/layout/{layout}.xml').iter():
            click=node.get(ns+'onClick')
            if click:assert has_method('Lcom/xenris/liquidwarsos/activities/'+activity+';',click);handlers+=1
            if node.tag.startswith('com.xenris.'):assert ('L'+node.tag.replace('.','/')+';','<init>') in methods
    for lang in ['values','values-zh']:
        values={n.get('name'):n.text for n in ET.parse(ROOT/f'app/src/main/res/{lang}/ui_strings.xml').iter('string')}
        assert '1.59-QA' in values['release_attribution']
    report=dict(status='PASS',version='1.59-QA',versionCode=28,applicationId='com.xenris.liquidwarsos.haoke.debug',
        apkBytes=apk.stat().st_size,apkSha256=sha(apk),certificateSha256=CERT,debuggable=True,signatureVerified=True,
        platformPermissionsUnchanged=True,normalizedPermissions=permissions,compatibilityVersion=16,mapCount=50,
        baselineAppFiles=len(baseline),changedBaselineFiles=changed,preservedAppFiles=len(preserved),
        jniMethods=len(native),xmlOnClickEntries=handlers,nativeLibraries=libs,largeZipGaps=gaps,
        sourceAppSha256={p.relative_to(ROOT).as_posix():sha(p) for p in (ROOT/'app/src').rglob('*') if p.is_file()},
        nativeRuntimeUnchanged=True,combatRuleVersion=14,modelIdleInputVersion=15,actualModelApi='NOT_TESTED this version; model code unchanged',
        device='MuMu instance2 / 127.0.0.1:16448 / Android15',actualGraphics='MuMu snapshots; device-smoke.json',multiplayer='NOT_TESTED',performance='NOT_TESTED')
    report['sourceAppSha256']['app/build.gradle']=sha(ROOT/'app/build.gradle')
    (OUT/'package-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(json.dumps({k:report[k] for k in ['status','version','apkBytes','apkSha256','preservedAppFiles','jniMethods','xmlOnClickEntries']},ensure_ascii=False))

if __name__=='__main__':main()
