from pathlib import Path
import subprocess,json,hashlib,sys
R=Path(__file__).resolve().parents[1];O=R/'docs/game/world-1.70';ADB='C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe';S='127.0.0.1:16448';P='com.xenris.liquidwarsos.haoke.debug';phase=sys.argv[1]
def adb(*args):return subprocess.check_output([ADB,'-s',S,*args])
base=adb('shell','pm','path',P).decode().strip().removeprefix('package:');digest=adb('shell','sha256sum',base).decode().split()[0]
expected=json.loads((R/('docs/game/world-1.69/package-verification.json' if phase=='baseline' else 'docs/game/world-1.70/package-verification.json')).read_text('utf8'))['apkSha256'];assert digest==expected
subprocess.run([ADB,'-s',S,'install','-r',str(R/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')],check=True)
D=O/'device'/phase;D.mkdir(parents=True,exist_ok=True)
for map,dots in [(50,1000),(28,1000),(28,4500)]:
 p=subprocess.run([ADB,'-s',S,'shell','am','instrument','-w','-e','class','com.xenris.liquidwarsos.Tiled170ProbeTest#movingSample','-e','phase',phase,'-e','map',str(map),'-e','dots',str(dots),P+'.test/androidx.test.runner.AndroidJUnitRunner'],capture_output=True);(D/f'{map}-{dots}.log').write_bytes(p.stdout+p.stderr);assert p.returncode==0 and b'OK (1 test)' in p.stdout,p.stdout.decode('utf8',errors='replace')
 for ext in ['json','jpg']:
  name=f'world170-{phase}-{map}-{dots}.{ext}';raw=adb('exec-out','run-as',P,'cat','files/'+name);(D/name).write_bytes(raw)
  assert hashlib.sha256(raw).hexdigest()==adb('shell','run-as',P,'sha256sum','files/'+name).decode().split()[0]
 print(phase,map,dots,'PASS',flush=True)
budget=json.loads(adb('exec-out','run-as',P,'cat','files/qa-strategy160-budget.json'));numeric={k:v for k,v in budget.items() if isinstance(v,(int,float)) and not isinstance(v,bool)};assert numeric['requests']==573 and numeric['reserved_or_peak_cost_micros']==8923724
(D/'identity.json').write_text(json.dumps(dict(installed_sha256=digest,budget_numeric=numeric,models=0),indent=2),encoding='utf8')
