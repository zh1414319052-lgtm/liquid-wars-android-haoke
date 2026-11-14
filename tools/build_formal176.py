from pathlib import Path
import os,subprocess,sys
R=Path(__file__).resolve().parents[1]
e=os.environ.copy();e['JAVA_HOME']='C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot';e['ANDROID_HOME']='C:/Users/Administrator/AppData/Local/Android/Sdk';e['ANDROID_SDK_ROOT']=e['ANDROID_HOME'];e['PATH']=e['JAVA_HOME']+'/bin;'+e['PATH']
if '--sign' in sys.argv:
 from release_signing import sign
 os.environ.update({k:e[k] for k in ['JAVA_HOME','ANDROID_HOME','ANDROID_SDK_ROOT','PATH']})
 output=Path(sys.argv[sys.argv.index('--output')+1]).resolve() if '--output' in sys.argv else R/'build/formal-1.76/liquidwars-1.76.apk'
 sign(R/'app/build/outputs/apk/release/app-release-unsigned.apk',output)
else:
 tasks=['assembleDebugAndroidTest'] if '--helper-only' in sys.argv else ['assembleRelease','assembleDebug','assembleDebugAndroidTest','lintVitalRelease']
 p=R/('build/formal-1.76/helper-final.log' if '--helper-only' in sys.argv else 'build/formal-1.76/build-online.log' if '--restore-cache' in sys.argv else 'build/formal-1.76/build.log');p.parent.mkdir(parents=True,exist_ok=True)
 with p.open('w',encoding='utf8') as f:r=subprocess.run([str(R/'gradlew.bat'),*tasks,*([] if '--restore-cache' in sys.argv else ['--offline']),'--no-daemon','--no-watch-fs','-Dorg.gradle.native=false'],cwd=R,env=e,stdout=f,stderr=subprocess.STDOUT)
 print('Build exit',r.returncode,'log',p);sys.exit(r.returncode)
