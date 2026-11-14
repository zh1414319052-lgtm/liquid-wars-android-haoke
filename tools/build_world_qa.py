"""Rebuild this QA with existing local tools and process-only environment."""
from pathlib import Path
import os,subprocess,sys
R=Path(__file__).resolve().parents[1]
env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot';env['ANDROID_HOME']='C:/Users/Administrator/AppData/Local/Android/Sdk';env['ANDROID_SDK_ROOT']=env['ANDROID_HOME'];env['PATH']=env['JAVA_HOME']+'/bin;'+env['PATH']
tasks=[':app:packageDebug','--rerun'] if '--package-only' in sys.argv else ['assembleDebugAndroidTest'] if '--helper-only' in sys.argv else ['assembleDebug','assembleDebugAndroidTest']
cache_flags=[] if '--restore-cache' in sys.argv else ['--offline']
raise SystemExit(subprocess.run([str(R/'gradlew.bat'),*tasks,*cache_flags,'--no-daemon','--no-watch-fs','-Dorg.gradle.native=false'],cwd=R,env=env).returncode)
