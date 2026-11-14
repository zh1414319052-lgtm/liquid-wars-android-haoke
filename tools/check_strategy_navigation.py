from pathlib import Path
import subprocess,sys
R=Path(__file__).resolve().parents[1];O=R/'build/strategy-check';O.mkdir(parents=True,exist_ok=True)
J=Path('C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin')
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/org.json/json/20241224').rglob('*.jar'))
src=R/'app/src/main/java/com/xenris/liquidwarsos/cloud'
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',str(jar),'-d',str(O),*[str(src/(n+'.java')) for n in ['ModelAction','ModelNavigation','ModelWorld']],str(R/'tools/strategy-check/NavigationCheck.java')],check=True)
subprocess.run([str(J/'java.exe'),'-cp',str(O)+';'+str(jar),'com.xenris.liquidwarsos.cloud.NavigationCheck',str(R/'app/src/main/assets/maps'),*sys.argv[1:]],check=True)
