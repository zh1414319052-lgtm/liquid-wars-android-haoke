"""Build the native online QA client with the project's pinned toolchain."""
from pathlib import Path
import os, subprocess, sys
r=Path(__file__).resolve().parents[1]
out=r/'build/online-1.84';out.mkdir(parents=True,exist_ok=True)
label=sys.argv[1];log=out/(label+'.log')
assert not log.exists(), 'Choose a fresh build log label'
env=os.environ.copy()
env['JAVA_HOME']='C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot'
env['ANDROID_HOME']='C:/Users/Administrator/AppData/Local/Android/Sdk'
env['ANDROID_SDK_ROOT']=env['ANDROID_HOME'];env['PATH']=env['JAVA_HOME']+'/bin;'+env['PATH']
tasks=sys.argv[2:] or ['assembleDebug','assembleDebugAndroidTest','assembleRelease','lintVitalRelease']
with log.open('w',encoding='utf-8') as f:
    p=subprocess.run([str(r/'gradlew.bat'),*tasks,'--no-daemon','--no-watch-fs','-Dorg.gradle.native=false'],cwd=r,env=env,stdout=f,stderr=subprocess.STDOUT)
print('EXIT',p.returncode,'LOG',log)
sys.exit(p.returncode)
