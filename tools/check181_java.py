from pathlib import Path
import subprocess
R=Path(__file__).resolve().parents[1];O=R/'build/audio-1.81/java';O.mkdir(parents=True,exist_ok=True);S=R/'app/src/main/java/com/xenris/liquidwarsos';J=Path('C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin')
files=[S/'AudioEvents.java',S/'OperationsSnapshot.java',S/'cloud/SnapshotItems.java',R/'tools/world-check/Audio181Check.java']
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-d',str(O),*map(str,files)],check=True)
subprocess.run([str(J/'java.exe'),'-cp',str(O),'com.xenris.liquidwarsos.Audio181Check'],check=True)
