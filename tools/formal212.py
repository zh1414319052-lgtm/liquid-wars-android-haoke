"""2.12 local release tools. Explicit commands only; no publication or credentials export."""
from pathlib import Path
import argparse, hashlib, json, os, re, shutil, zipfile, xml.etree.ElementTree as ET
import release_signing as signing
from check_formal_release import dex_methods, run, SDK

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/formal-2.12'
DOC=ROOT/'docs/game/formal-2.12'
DELIVERY=Path('D:/浩科软件优化/液态战争重制版_Liquid Wars OS')
APK=OUT/'liquidwars-2.12.apk'
NAME='液态战争重制版_2.12_Android.apk'

def sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        while b:=f.read(2**20): h.update(b)
    return h.hexdigest()

def save(name,data):
    DOC.mkdir(parents=True,exist_ok=True)
    (DOC/name).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def sign():
    OUT.mkdir(parents=True,exist_ok=True)
    signing.sign(ROOT/'app/build/outputs/apk/release/app-release-unsigned.apk',APK)
    cert=json.loads(signing.PUBLIC.read_text('utf-8'))['certificateSha256'].lower()
    record=dict(version='2.12',versionCode=85,applicationId=signing.APP_ID,certificateSha256=cert,
                apkSha256=sha(APK),bytes=APK.stat().st_size,debuggable=False)
    save('package.json',record)
    print(json.dumps(record))

def check():
    meta=json.loads((DOC/'package.json').read_text('utf-8'))
    assert sha(APK)==meta['apkSha256']
    badging=run([SDK/'aapt.exe','dump','badging',APK])
    assert "package: name='com.xenris.liquidwarsos.haoke' versionCode='85' versionName='2.12'" in badging
    assert 'application-debuggable' not in badging
    certs=run([SDK/'apksigner.bat','verify','--verbose','--print-certs',APK])
    assert meta['certificateSha256'] in certs.lower()
    old=DELIVERY/'液态战争重制版_2.07_Android.apk'
    assert meta['certificateSha256'] in run([SDK/'apksigner.bat','verify','--print-certs',old]).lower()
    for n in [2,3]: assert re.search(r'Verified using v'+str(n)+r' scheme.*true',certs)
    run([SDK/'zipalign.exe','-c','-P','16','4',APK])
    jni=re.findall(r'\bnative\s+\w+(?:\[\])?\s+(\w+)\s*\(', (ROOT/'app/src/main/java/com/xenris/liquidwarsos/NativeInterface.java').read_text('utf-8'))
    methods=set();parents={};libs=[];assets=0
    with zipfile.ZipFile(APK) as z:
        for n in z.namelist():
            assert not n.lower().endswith(('.jks','.keystore','.p12','.dpapi','.mp4','.log')),n
            assert 'guest193probe' not in n and 'expedition192-seed' not in n,n
            if re.fullmatch(r'classes\d*\.dex',n):
                b=z.read(n);m,p=dex_methods(b);methods|=m;parents.update(p)
                for forbidden in [b'cleanbizai.com',b'hkai_',b'PublicLobbyActivity',b'PlatformClient;']:assert forbidden not in b,forbidden
            if n.endswith('/libnativeinterface.so'):
                libs.append(n);b=z.read(n)
                for method in jni: assert ('Java_com_xenris_liquidwarsos_NativeInterface_'+method).encode()+b'\0' in b,(n,method)
        assert sorted(n.split('/')[1] for n in libs)==['arm64-v8a','armeabi-v7a','x86','x86_64']
        assert 'assets/audio/swallow.wav' not in z.namelist()
        for p in (ROOT/'app/src/main/assets').rglob('*'):
            if p.is_file(): assert z.read('assets/'+p.relative_to(ROOT/'app/src/main/assets').as_posix())==p.read_bytes(),str(p);assets+=1
    for method in jni: assert ('Lcom/xenris/liquidwarsos/NativeInterface;',method) in methods,method
    roles={'main_menu':'MainMenuActivity','single_game_setup':'SinglePlayerGameSetupActivity','multi_game_setup':'MultiplayerGameSetupActivity','multiplayer_menu':'MultiplayerMenuActivity','bluetooth_menu':'BluetoothMenuActivity','client_game_setup':'ClientGameSetupActivity','instructions':'InstructionsActivity'}
    handlers=0
    for layout,activity in roles.items():
        for node in ET.parse(ROOT/f'app/src/main/res/layout/{layout}.xml').iter():
            click=node.get('{http://schemas.android.com/apk/res/android}onClick')
            if click:
                cls='Lcom/xenris/liquidwarsos/activities/'+activity+';'
                while cls and (cls,click) not in methods: cls=parents.get(cls)
                assert cls,(layout,click);handlers+=1
    suites=[ET.parse(p).getroot() for p in (ROOT/'app/build/test-results/testReleaseUnitTest').glob('TEST-*.xml')]
    counts={k:sum(int(t.get(k,0)) for t in suites) for k in ['tests','errors','failures']}
    assert counts['tests']>=60 and counts['errors']==counts['failures']==0,counts
    permissions=run([SDK/'aapt.exe','dump','permissions',APK])
    (DOC/'permissions.txt').write_text(permissions,encoding='utf-8')
    (DOC/'apk-signature.txt').write_text(certs,encoding='utf-8')
    report=dict(status='PASS',**meta,abis=[n.split('/')[1] for n in libs],jniMethods=len(jni),xmlHandlers=handlers,
                exactSourceAssets=assets,releaseTests=counts,releaseR8=True,certificateMatches207=True,
                notification='NOT_SENT',securityScope='FOCUSED artifact, identity and updater; not full audit')
    save('archive-check.json',report);print(json.dumps(report))

def deliver():
    report=json.loads((DOC/'archive-check.json').read_text('utf-8'));assert report['status']=='PASS' and sha(APK)==report['apkSha256']
    target=DELIVERY/NAME
    if target.exists(): assert sha(target)==sha(APK),'Do not replace a different existing release'
    else: shutil.copyfile(APK,target)
    assert sha(target)==report['apkSha256']
    save('pending-notification.json',dict(notificationPublished=False,version='2.12',versionCode=85,
        applicationId=signing.APP_ID,softwareId='f2a585b84fbf5b85',platform='android',channel='stable',
        status='WAITING_USER_UPLOAD_AND_EXPLICIT_NOTIFICATION_INSTRUCTION',sizeBytes=report['bytes'],
        sha256=report['apkSha256'],certificateSha256=report['certificateSha256'],apk=str(target)))
    print(str(target))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('action',choices=['sign','check','deliver'],default='check',nargs='?')
    globals()[p.parse_args().action]()
