"""1.64 catalog/menu QA checks against delivered1.63. No device or network."""
from pathlib import Path
import hashlib,json,re,struct,zipfile,xml.etree.ElementTree as ET
from check_formal_release import run,dex_methods,SDK
R=Path(__file__).resolve().parents[1];O=R/'docs/game/menu-1.64'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    apk=R/'app/build/outputs/apk/debug/app-debug.apk'
    prev=json.loads((R/'docs/game/particles-1.63/package-verification.json').read_text('utf8'))
    old=prev['sourceAppSha256'];changed=[n for n,h in old.items() if not(R/n).is_file() or sha(R/n)!=h]
    removed={'app/src/main/assets/maps/0-image.png','app/src/main/assets/maps/0-map.png','app/src/androidTest/java/com/xenris/liquidwarsos/RuinsPreviewTest.java'}
    allowed={'app/build.gradle','app/src/main/cpp/application/state.cpp','app/src/main/cpp/application/info.hpp','app/src/main/java/com/xenris/liquidwarsos/StaticBits.java','app/src/main/java/com/xenris/liquidwarsos/Util.java','app/src/main/res/layout/main_menu.xml'}|removed
    allowed.update('app/src/main/java/com/xenris/liquidwarsos/activities/'+n for n in ['MainMenuActivity.java','SinglePlayerGameSetupActivity.java','MultiplayerGameSetupActivity.java','ClientGameSetupActivity.java'])
    allowed.update('app/src/main/res/'+n for n in ['values/strings.xml','values/ui_strings.xml','values-zh/strings.xml','values-zh/ui_strings.xml','values-de/strings.xml','values-fr/strings.xml','values-it/strings.xml'])
    allowed.add('app/src/androidTest/java/com/xenris/liquidwarsos/FreeMapsProbeTest.java')
    assert set(changed)==allowed,('Unexpected source change',set(changed)^allowed)
    for name in removed:assert not(R/name).exists()
    for p in (R/'app/src/main').rglob('*'):
        if p.is_file() and p.relative_to(R).as_posix() not in old:
            assert p.relative_to(R).as_posix() in {'app/src/main/java/com/xenris/liquidwarsos/MapCatalog.java','app/src/main/cpp/application/map_catalog.hpp'}
    import subprocess
    gradle=(R/'app/build.gradle').read_text('utf8');before=subprocess.check_output(['git','show','76d091e:app/build.gradle'],cwd=R).decode('utf8')
    strip=lambda s:re.sub(r'version(?:Code|Name)\s+[^\n]+','',s)
    assert strip(gradle)==strip(before),'Build/dependencies changed'
    badge=run([SDK/'aapt.exe','dump','badging',apk]);assert "package: name='com.xenris.liquidwarsos.haoke.debug' versionCode='33' versionName='1.64-QA'" in badge and 'application-debuggable' in badge
    sig=run([SDK/'apksigner.bat','verify','--verbose','--print-certs',apk]);assert prev['certificateSha256'] in sig.lower() and 'Android Debug' in sig
    assert 'Verified using v2 scheme (APK Signature Scheme v2): true' in sig
    run([SDK/'zipalign.exe','-c','-P','16','4',apk])
    permissions=sorted(re.sub(r"name='[^']+\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'","name='<self>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'",l) for l in run([SDK/'aapt.exe','dump','permissions',apk]).splitlines() if l.startswith('uses-permission:'))
    assert permissions==prev['normalizedPermissions']
    native=re.findall(r'\bnative\s+\w+(?:\[\])?\s+(\w+)\s*\(', (R/'app/src/main/java/com/xenris/liquidwarsos/NativeInterface.java').read_text('utf8'));assert len(native)==19
    static=(R/'app/src/main/java/com/xenris/liquidwarsos/StaticBits.java').read_text('utf8');assert 'VERSION_CODE = 19;' in static and 'NUMBER_OF_MAPS = MapCatalog.COUNT;' in static
    methods=set();libs=[];gaps=[]
    prior_apk=Path('D:/浩科软件优化/液态战争重制版_Liquid Wars OS/液态战争重制版_1.63-QA_Android.apk')
    with zipfile.ZipFile(apk) as z,zipfile.ZipFile(prior_apk) as oldzip:
        for p in (R/'app/src/main/assets').rglob('*'):
            if p.is_file():assert z.read('assets/'+p.relative_to(R/'app/src/main/assets').as_posix())==p.read_bytes()
        for name in z.namelist():
            assert not name.lower().endswith(('.jks','.keystore','.p12','.dpapi')),name
            if re.fullmatch(r'classes\d*\.dex',name):m,_=dex_methods(z.read(name));methods|=m
            if not(name.startswith('lib/') and name.endswith('.so')):continue
            data=z.read(name);bits=64 if data[4]==2 else 32;e='<' if data[5]==1 else '>'
            ph=struct.unpack_from(e+('Q' if bits==64 else 'I'),data,32 if bits==64 else 28)[0]
            size,count=struct.unpack_from(e+'HH',data,54 if bits==64 else 42);align=[]
            for i in range(count):
                at=ph+i*size
                if struct.unpack_from(e+'I',data,at)[0]==1:align.append(struct.unpack_from(e+('Q' if bits==64 else 'I'),data,at+(48 if bits==64 else 28))[0])
            assert align and (bits!=64 or min(align)>=16384)
            if name.endswith('/libnativeinterface.so'):
                for n in native:assert ('Java_com_xenris_liquidwarsos_NativeInterface_'+n).encode()+b'\0' in data
            if name.endswith('/libc++_shared.so'):assert data==oldzip.read(name)
            libs.append({'path':name,'bits':bits,'alignment':align})
        with apk.open('rb') as f:
            entries=sorted(z.infolist(),key=lambda p:p.header_offset)
            for a,b in zip(entries,entries[1:]):
                f.seek(a.header_offset+26);n,x=struct.unpack('<HH',f.read(4));gap=b.header_offset-(a.header_offset+30+n+x+a.compress_size)
                if gap>65536:gaps.append((a.filename,gap))
    assert not gaps,('Large incremental ZIP holes',gaps)
    assert {p['path'] for p in libs}=={p['path'] for p in prev['nativeLibraries']}
    assert not any('ProbeTest' in c for c,m in methods)
    for n in native:assert ('Lcom/xenris/liquidwarsos/NativeInterface;',n) in methods
    for n in ['GameActivity','GameServerActivity','GameClientActivity']:assert any(c=='Lcom/xenris/liquidwarsos/activities/'+n+';' for c,m in methods)
    catalog=json.loads((R/'art/source/catalog-1.64.json').read_text('utf8'));ids=[m['id'] for m in catalog['maps']];assert ids==list(range(1,50))
    for loc in ['values','values-zh','values-de','values-fr','values-it']:
        names=[n.text for n in ET.parse(R/f'app/src/main/res/{loc}/strings.xml').find("string-array[@name='maps_array']")]
        assert len(names)==50 and all('Moss Ruins' not in n and '苔石遗迹' not in n for n in names)
    with zipfile.ZipFile(apk) as z:
        assert 'assets/maps/0-image.png' not in z.namelist() and 'assets/maps/0-map.png' not in z.namelist()
        assert len([n for n in z.namelist() if re.fullmatch(r'assets/maps/\d+-map.png',n)])==49
    for cls,names in [('MainMenuActivity',['ruinsTrial','consumeRuinsLaunch']),('SinglePlayerGameSetupActivity',['isBiomeTrial','applyTrialGuard'])]:
        for name in names:assert ('Lcom/xenris/liquidwarsos/activities/'+cls+';',name) not in methods
    menu=(R/'app/src/main/res/layout/main_menu.xml').read_text('utf8');assert 'ruins_trial_button' not in menu and 'ruinsTrial' not in menu
    report=dict(status='PASS',version='1.64-QA',versionCode=33,compatibilityVersion=19,mapCount=49,apkBytes=apk.stat().st_size,apkSha256=sha(apk),applicationId='com.xenris.liquidwarsos.haoke.debug',certificateSha256=prev['certificateSha256'],signatureVerified=True,normalizedPermissions=permissions,platformPermissionsUnchanged=True,dependenciesUnchanged=True,newNetworkRequests=False,nativeSimulationAlgorithmsUnchanged=True,mapSelectionChanged=True,nativeGraphicsSourcesUnchanged=True,nativeLibraries=libs,jniMethods=len(native),changedBaselineFiles=changed,largeZipGaps=gaps,sourceAppSha256={p.relative_to(R).as_posix():sha(p) for p in (R/'app/src').rglob('*') if p.is_file()})
    report['sourceAppSha256']['app/build.gradle']=sha(R/'app/build.gradle')
    (O/'package-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
    (O/'apk-badging.txt').write_text(badge,encoding='utf8');(O/'apk-signature.txt').write_text(sig,encoding='utf8')
    print(json.dumps({k:report[k] for k in ['status','version','apkBytes','apkSha256','certificateSha256']},ensure_ascii=False))
if __name__=='__main__':main()
