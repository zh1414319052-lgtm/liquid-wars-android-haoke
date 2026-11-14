from pathlib import Path
import subprocess
R=Path(__file__).resolve().parents[1];O=R/'build/controls-1.74/java';O.mkdir(parents=True,exist_ok=True)
J=Path('C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin');S=R/'app/src/main/java/com/xenris/liquidwarsos'
files=[S/(n+'.java') for n in ['WorldCamera','TouchCursor','StickLayout']]+[R/'tools/world-check/Controls174Check.java']
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-d',str(O),*map(str,files)],check=True)
subprocess.run([str(J/'java.exe'),'-cp',str(O),'com.xenris.liquidwarsos.Controls174Check'],check=True)
