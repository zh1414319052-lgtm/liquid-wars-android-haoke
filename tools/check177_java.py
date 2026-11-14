from pathlib import Path
import subprocess
R=Path(__file__).resolve().parents[1];O=R/'build/controls-items-1.77/java';O.mkdir(parents=True,exist_ok=True);J=Path('C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin');S=R/'app/src/main/java/com/xenris/liquidwarsos'
subprocess.run([J/'javac.exe','-encoding','UTF-8','-d',O,S/'WorldCamera.java',R/'tools/world-check/Continuous177Check.java'],check=True);subprocess.run([J/'java.exe','-cp',O,'com.xenris.liquidwarsos.Continuous177Check'],check=True)
