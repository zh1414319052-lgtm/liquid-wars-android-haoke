from pathlib import Path
import subprocess
R=Path(__file__).resolve().parents[1];O=R/'build/world-1.69/classes';O.mkdir(parents=True,exist_ok=True)
J=Path('C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin');src=R/'app/src/main/java/com/xenris/liquidwarsos'
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-d',str(O),str(src/'TouchCursor.java'),str(src/'net/GameWire.java'),str(R/'tools/joystick-check/Check.java')],check=True)
subprocess.run([str(J/'java.exe'),'-cp',str(O),'com.xenris.liquidwarsos.Check'],check=True)
