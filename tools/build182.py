from pathlib import Path
import os,subprocess,sys
R=Path(__file__).resolve().parents[1];O=R/'build/follow-1.82';O.mkdir(parents=True,exist_ok=True)
e=os.environ.copy();e['JAVA_HOME']='C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot';e['ANDROID_HOME']='C:/Users/Administrator/AppData/Local/Android/Sdk';e['ANDROID_SDK_ROOT']=e['ANDROID_HOME'];e['PATH']=e['JAVA_HOME']+'/bin;'+e['PATH']
label=sys.argv[1] if len(sys.argv)>1 else 'build';log=O/(label+'.log');assert not log.exists()
tasks=sys.argv[2:] or ['assembleDebug','assembleDebugAndroidTest','assembleRelease','lintVitalRelease']
with log.open('w',encoding='utf8') as f:p=subprocess.run([str(R/'gradlew.bat'),*tasks,'--offline','--no-daemon','--no-watch-fs','-Dorg.gradle.native=false'],cwd=R,env=e,stdout=f,stderr=subprocess.STDOUT)
print('EXIT',p.returncode,'LOG',log);sys.exit(p.returncode)
