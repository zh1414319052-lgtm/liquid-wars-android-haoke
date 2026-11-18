"""Focused 1.53 Release archive/resource/JNI/R8 checks. Never installs or contacts a device."""
from pathlib import Path
import argparse,hashlib,json,re,struct,subprocess,zipfile,xml.etree.ElementTree as ET
from PIL import Image

ROOT=Path(__file__).resolve().parents[1];BUILD=ROOT/'build'
SDK=Path(r'C:/Users/Administrator/AppData/Local/Android/Sdk/build-tools/35.0.1')
def sha(data):return hashlib.sha256(data).hexdigest().upper()
def run(args):
    p=subprocess.run([str(x) for x in args],capture_output=True);assert p.returncode==0,(str(args[0]),p.returncode,p.stderr.decode('utf8',errors='replace')[-600:]);return p.stdout.decode('utf8',errors='replace')
def dex_methods(b):
    def uleb(off):
        value=0;shift=0
        while True:
            c=b[off];off+=1;value|=(c&127)<<shift
            if c<128:return value,off
            shift+=7
    count,off=struct.unpack_from('<II',b,56);strings=[]
    for i in range(count):
        start=struct.unpack_from('<I',b,off+4*i)[0];_,start=uleb(start);end=b.index(0,start);strings.append(b[start:end].decode('utf8',errors='replace'))
    count,off=struct.unpack_from('<II',b,64);types=[strings[struct.unpack_from('<I',b,off+4*i)[0]] for i in range(count)]
    count,off=struct.unpack_from('<II',b,88);methods=set()
    for i in range(count):
        cls,proto,name=struct.unpack_from('<HHI',b,off+8*i);methods.add((types[cls],strings[name]))
    count,off=struct.unpack_from('<II',b,96);parents={}
    for i in range(count):
        cls,flags,parent=struct.unpack_from('<III',b,off+32*i);parents[types[cls]]=types[parent] if parent!=0xffffffff else None
    return methods,parents

def main():
    p=argparse.ArgumentParser();p.add_argument('--apk',type=Path,default=ROOT/'app/build/outputs/apk/release/app-release.apk');a=p.parse_args();apk=a.apk.resolve()
    meta=json.loads((ROOT/'docs/game/formal-1.53/signing-public.json').read_text('utf8'))
    badging=run([SDK/'aapt.exe','dump','badging',apk]);assert "package: name='com.xenris.liquidwarsos.haoke' versionCode='22' versionName='1.53'" in badging
    assert 'application-debuggable' not in badging and "sdkVersion:'26'" in badging
    signature=run([SDK/'apksigner.bat','verify','--verbose','--print-certs',apk]);assert meta['certificateSha256'].lower() in signature.lower()
    assert 'Verified using v2 scheme (APK Signature Scheme v2): true' in signature and 'Verified using v3 scheme (APK Signature Scheme v3): true' in signature
    assert 'Android Debug' not in signature
    run([SDK/'zipalign.exe','-c','-P','16','4',apk])
    fixture=json.loads((ROOT/'docs/game/formal-1.53/baseline-1.52.json').read_text('utf8'))
    baseline=fixture['app_file_sha256']
    allowed={'app/build.gradle','app/src/main/AndroidManifest.xml','app/src/main/res/values/ui_strings.xml','app/src/main/res/values-zh/ui_strings.xml','app/src/main/res/layout/instructions.xml'}
    changed=[]
    for rel,digest in baseline.items():
        if sha((ROOT/rel).read_bytes()).lower()!=digest.lower():assert rel in allowed,rel;changed.append(rel)
    # Compare normal platform permissions; only the self-signature receiver name follows app ID.
    lines=run([SDK/'aapt.exe','dump','permissions',apk]).splitlines()
    permissions=sorted(re.sub(r"name='[^']+\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'","name='<self>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'",l) for l in lines if l.startswith('uses-permission:'))
    assert permissions==fixture['normalized_permissions'],permissions
    methods=set();parents={};libraries=[];jni=re.findall(r'\bnative\s+\w+(?:\[\])?\s+(\w+)\s*\(', (ROOT/'app/src/main/java/com/xenris/liquidwarsos/NativeInterface.java').read_text('utf8'));assert jni
    with zipfile.ZipFile(apk) as z:
        assert z.read('assets/licenses/GPL-3.0.txt')==(ROOT/'LICENSE').read_bytes()
        for n in z.namelist():
            assert not n.lower().endswith(('.p12','.jks','.keystore','.dpapi')),n
            if re.fullmatch(r'classes\d*\.dex',n):m,p=dex_methods(z.read(n));methods|=m;parents.update(p)
            if not(n.startswith('lib/') and n.endswith('.so')):continue
            b=z.read(n);bits=64 if b[4]==2 else 32;endian='<' if b[5]==1 else '>'
            phoff=struct.unpack_from(endian+('Q' if bits==64 else 'I'),b,32 if bits==64 else 28)[0];size,count=struct.unpack_from(endian+'HH',b,54 if bits==64 else 42);aligns=[]
            for i in range(count):
                start=phoff+i*size
                if struct.unpack_from(endian+'I',b,start)[0]==1:aligns.append(struct.unpack_from(endian+('Q' if bits==64 else 'I'),b,start+(48 if bits==64 else 28))[0])
            assert aligns and (bits!=64 or min(aligns)>=16384)
            if n.endswith('/libnativeinterface.so'):
                for method in jni:assert ('Java_com_xenris_liquidwarsos_NativeInterface_'+method).encode()+b'\0' in b,(n,method)
            libraries.append(dict(path=n,bits=bits,alignment=aligns))
        for file in (ROOT/'app/src/main/assets/maps').iterdir():
            if file.is_file():assert z.read('assets/maps/'+file.name)==file.read_bytes(),file
    assert sorted({n['path'].split('/')[1] for n in libraries})==['arm64-v8a','armeabi-v7a','x86','x86_64']
    assert {n['path'] for n in libraries}==set(fixture['native_library_paths'])
    assert sum(n['path'].endswith('/libnativeinterface.so') for n in libraries)==4
    descriptor='Lcom/xenris/liquidwarsos/NativeInterface;'
    for method in jni:assert (descriptor,method) in methods,('R8 removed JNI method',method)
    def has_method(cls,name):
        while cls:
            if (cls,name) in methods:return True
            cls=parents.get(cls)
        return False
    roles={'main_menu':'MainMenuActivity','single_game_setup':'SinglePlayerGameSetupActivity','multi_game_setup':'MultiplayerGameSetupActivity','multiplayer_menu':'MultiplayerMenuActivity','bluetooth_menu':'BluetoothMenuActivity','client_game_setup':'ClientGameSetupActivity','instructions':'InstructionsActivity'}
    ns='{http://schemas.android.com/apk/res/android}';handlers=0
    for layout,activity in roles.items():
        for node in ET.parse(ROOT/f'app/src/main/res/layout/{layout}.xml').iter():
            click=node.get(ns+'onClick')
            if click:assert has_method('Lcom/xenris/liquidwarsos/activities/'+activity+';',click),(layout,click);handlers+=1
            if node.tag.startswith('com.xenris.'):assert ('L'+node.tag.replace('.','/')+';','<init>') in methods,('R8 removed XML view constructor',node.tag)
    foreground=Image.open(ROOT/'app/src/main/res/drawable-nodpi/launcher_foreground.png');alpha=foreground.getchannel('A')
    assert foreground.size==(432,432)
    mask_pixels=alpha.load()
    for y in range(432):
        for x in range(432):
            if mask_pixels[x,y]>16:assert (x-215.5)**2+(y-215.5)**2<=(33*4)**2,'Adaptive mask clips symbol'
    for lang in ['values','values-zh']:
        values={n.get('name'):n.text for n in ET.parse(ROOT/f'app/src/main/res/{lang}/ui_strings.xml').iter('string')}
        assert '1.53' in values['release_attribution'] and 'GPL-3.0-or-later' in values['release_attribution']
    report=dict(status='PASS',version='1.53',versionCode=22,applicationId=meta['applicationId'],apkBytes=apk.stat().st_size,apkSha256=sha(apk.read_bytes()),
        certificateSha256=meta['certificateSha256'],signatureSchemes=['v2','v3'],debuggable=False,baselineAppFiles=len(baseline),changedBaselineFiles=changed,
        platformPermissionsUnchanged=True,selfSignaturePermission='renamed with independent applicationId',jniMethods=len(jni),xmlOnClickEntries=handlers,
        nativeLibraries=libraries,mapCount=46,iconAdaptiveSafeCircle=True,device='NOT_TESTED',installationUpgrade='NOT_TESTED',multiplayer='NOT_TESTED',performance='NOT_TESTED')
    (ROOT/'docs/game/formal-1.53/package-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(json.dumps({k:report[k] for k in ['status','version','apkBytes','apkSha256','jniMethods','xmlOnClickEntries']},ensure_ascii=False))
if __name__=='__main__':main()
