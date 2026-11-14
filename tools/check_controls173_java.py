from pathlib import Path
import subprocess
R=Path(__file__).resolve().parents[1];O=R/'build/world-1.73/cloud-check';J=Path('C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin');jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/org.json/json/20241224').rglob('*.jar'));cp=str(O)+';'+str(jar);S=R/'app/src/main/java/com/xenris/liquidwarsos'
files=[S/f'{n}.java' for n in ['WorldCamera','TouchCursor']]+[R/'tools/world-check/Controls173Check.java']
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(O),*map(str,files)],check=True)
subprocess.run([str(J/'java.exe'),'-cp',cp,'com.xenris.liquidwarsos.cloud.Controls173Check'],check=True)
