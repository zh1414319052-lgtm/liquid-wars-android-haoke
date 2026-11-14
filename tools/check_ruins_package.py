"""1.61 ruins-preview QA archive checks. No device, network or alternate renderer."""
from pathlib import Path
import argparse, hashlib, json, re, struct, zipfile, xml.etree.ElementTree as ET
from PIL import Image
from check_formal_release import run, dex_methods, SDK

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/game/ruins-1.61'
CERT='a1519f2f24b603f1cd5ab4e76ec440c5275036a8dfcc439eb1ab868f35d0ddb2'
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--apk',type=Path,default=ROOT/'app/build/outputs/apk/debug/app-debug.apk');args=parser.parse_args();apk=args.apk.resolve()
    badge=run([SDK/'aapt.exe','dump','badging',apk]);assert "package: name='com.xenris.liquidwarsos.haoke.debug' versionCode='30' versionName='1.61-QA'" in badge
    assert 'application-debuggable' in badge and "sdkVersion:'26'" in badge
    signature=run([SDK/'apksigner.bat','verify','--verbose','--print-certs',apk]);assert CERT in signature.lower() and 'Android Debug' in signature
    assert 'Verified using v2 scheme (APK Signature Scheme v2): true' in signature
    run([SDK/'zipalign.exe','-c','-P','16','4',apk])
    (OUT/'apk-badging.txt').write_text(badge,encoding='utf8');(OUT/'apk-signature.txt').write_text(signature,encoding='utf8')
    baseline=json.loads((OUT/'baseline-1.60.json').read_text('utf8'));changed=[];preserved=[]
    for rel,digest in baseline.items():
        assert (ROOT/rel).is_file(),('removed baseline file',rel)
        (preserved if sha(ROOT/rel)==digest.lower() else changed).append(rel)
    protected=['app/src/main/AndroidManifest.xml','app/src/main/java/com/xenris/liquidwarsos/AiController.java','app/src/main/java/com/xenris/liquidwarsos/AiDifficulty.java','app/src/main/java/com/xenris/liquidwarsos/AllianceRules.java','app/src/main/java/com/xenris/liquidwarsos/NativeInterface.java']
    protected += ['app/src/main/cpp/application/'+name for name in ['move.cpp','move.hpp','dot.cpp','dot.hpp','ai.cpp','ai.hpp','ai_tactics.cpp','ai_tactics.hpp','random.cpp','random.hpp','spiral.cpp','spiral.hpp','player.cpp','player.hpp']]
    protected += [rel for rel in baseline if rel.startswith('app/src/main/cpp/') or rel.startswith('app/src/main/java/com/xenris/liquidwarsos/cloud/')]
    protected += [f'app/src/main/assets/maps/{i}-{kind}.png' for i in range(50) for kind in ['image','map']]
    for rel in protected:assert rel in preserved,('protected behavior/regular map changed',rel)
    allowed={'app/build.gradle','app/src/main/assets/fonts/NotoSansSC-Intro.ttf','app/src/main/java/com/xenris/liquidwarsos/StaticBits.java','app/src/main/java/com/xenris/liquidwarsos/activities/MainMenuActivity.java','app/src/main/java/com/xenris/liquidwarsos/activities/GameActivity.java','app/src/main/res/layout/main_menu.xml','app/src/main/res/values/ui_strings.xml','app/src/main/res/values-zh/ui_strings.xml'}
    assert set(changed)<=allowed,('unexpected modified baseline files',set(changed)-allowed)
    import subprocess
    before=subprocess.check_output(['git','show','893bfa9:app/build.gradle'],cwd=ROOT).decode('utf8')
    strip_versions=lambda t:re.sub(r'version(?:Code|Name)\s+[^\n]+','',t)
    assert strip_versions(before)==strip_versions((ROOT/'app/build.gradle').read_text('utf8')),'Build dependencies/settings changed'
    urls=lambda t:set(re.findall(r'https?://[a-zA-Z0-9.-]+',t))
    for rel in changed:
        if not rel.endswith('.java'):continue
        before=subprocess.check_output(['git','show','893bfa9:'+rel],cwd=ROOT).decode('utf8')
        assert urls((ROOT/rel).read_text('utf8'))<=urls(before),('new network destination',rel)
    for p in (ROOT/'app/src/main/java').rglob('*.java'):
        if p.relative_to(ROOT).as_posix() not in baseline:
            text=p.read_text('utf8')
            if p.name=='QaModelBudget.java':
                # Citation stored in local numeric evidence; never requested.
                text=text.replace('.put("price_source","https://api-docs.deepseek.com/zh-cn/quick_start/pricing/")','')
                assert all(token not in text for token in ['openConnection(', 'HttpURLConnection', 'ModelHttp'])
            assert not urls(text),('new class with endpoint',p)
    fixture=json.loads((ROOT/'docs/game/formal-1.53/baseline-1.52.json').read_text('utf8'))
    permissions=sorted(re.sub(r"name='[^']+\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'","name='<self>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'",line) for line in run([SDK/'aapt.exe','dump','permissions',apk]).splitlines() if line.startswith('uses-permission:'))
    assert permissions==fixture['normalized_permissions']
    java=ROOT/'app/src/main/java/com/xenris/liquidwarsos'
    static=(java/'StaticBits.java').read_text('utf8');assert 'VERSION_CODE = 17;' in static and 'NUMBER_OF_MAPS = 50;' in static
    native=re.findall(r'\bnative\s+\w+(?:\[\])?\s+(\w+)\s*\(', (java/'NativeInterface.java').read_text('utf8'));assert len(native)==19
    methods=set();parents={};libs=[];gaps=[]
    native_baseline=json.loads((ROOT/'docs/game/realtime-1.57/native-baseline-1.56.json').read_text('utf8'))
    with zipfile.ZipFile(apk) as z:
        prior=json.loads((OUT/'native-libraries-1.60.json').read_text('utf8'))
        for name,digest in prior.items():assert hashlib.sha256(z.read(name)).hexdigest()==digest,('native binary changed',name)
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
        assert z.read('assets/fonts/NotoSansSC-Intro.ttf')==(ROOT/'app/src/main/assets/fonts/NotoSansSC-Intro.ttf').read_bytes()
        assert z.read('assets/licenses/NotoSansSC-OFL.txt')==(ROOT/'app/src/main/assets/licenses/NotoSansSC-OFL.txt').read_bytes()
        assert z.read('assets/maps/50-image.png')==(ROOT/'app/src/main/assets/maps/50-image.png').read_bytes()
        assert z.read('assets/maps/50-map.png')==(ROOT/'app/src/main/assets/maps/50-map.png').read_bytes()
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
    for cls in ['AiController','AiDifficulty','ui/DifficultyPicker','ui/DifficultyRoomName','cloud/ModelAction','cloud/ModelTerrain','cloud/ModelStream','cloud/ModelConfig','cloud/ModelProtocol','cloud/ModelHttp','cloud/ModelMatch','cloud/ModelStore','cloud/ModelProviders','cloud/ModelDirectory','cloud/ModelSetupDraft','activities/ModelSettingsActivity','AllianceRules','AllianceSettings','MatchIntro','MatchIntroView','cloud/ModelNavigation','cloud/ModelFacts','cloud/ModelWorld','cloud/ModelRouteExecutor','cloud/QaModelBudget']:
        assert any(c=='Lcom/xenris/liquidwarsos/'+cls+';' for c,m in methods),cls
    assert not any('ProbeTest' in c for c,m in methods), 'Test probe must not ship in game APK'
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
    for node in ET.parse(ROOT/'app/src/main/res/layout/game.xml').iter():
        if node.tag.startswith('com.xenris.'):assert ('L'+node.tag.replace('.','/')+';','<init>') in methods
    for lang in ['values','values-zh']:
        values={n.get('name'):n.text for n in ET.parse(ROOT/f'app/src/main/res/{lang}/ui_strings.xml').iter('string')}
        assert '1.61-QA' in values['release_attribution']
    report=dict(status='PASS',version='1.61-QA',versionCode=30,applicationId='com.xenris.liquidwarsos.haoke.debug',
        apkBytes=apk.stat().st_size,apkSha256=sha(apk),certificateSha256=CERT,debuggable=True,signatureVerified=True,
        platformPermissionsUnchanged=True,normalizedPermissions=permissions,compatibilityVersion=17,mapCount=50,
        baselineAppFiles=len(baseline),changedBaselineFiles=changed,preservedAppFiles=len(preserved),
        jniMethods=len(native),xmlOnClickEntries=handlers,nativeLibraries=libs,largeZipGaps=gaps,
        sourceAppSha256={p.relative_to(ROOT).as_posix():sha(p) for p in (ROOT/'app/src').rglob('*') if p.is_file()},
        nativeRuntimeUnchanged=True,combatRuleVersion=14,modelIdleInputVersion=15,actualModelApi='NOT_TESTED; all API lanes disabled for this preview',
        device='MuMu instance2 / 127.0.0.1:16448 / Android15',actualGraphics='MuMu snapshots; device-smoke.json',multiplayer='NOT_TESTED',performance='NOT_TESTED')
    report['sourceAppSha256']['app/build.gradle']=sha(ROOT/'app/build.gradle')
    (OUT/'package-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(json.dumps({k:report[k] for k in ['status','version','apkBytes','apkSha256','preservedAppFiles','jniMethods','xmlOnClickEntries']},ensure_ascii=False))

if __name__=='__main__':main()
