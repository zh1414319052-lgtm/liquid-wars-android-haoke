from pathlib import Path
import subprocess
R=Path(__file__).resolve().parents[1];O=R/'build/controls-1.78/java';O.mkdir(parents=True,exist_ok=True);J=Path('C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin')
subprocess.run([J/'javac.exe','-encoding','UTF-8','-d',O,R/'app/src/main/java/com/xenris/liquidwarsos/WorldCamera.java',R/'tools/world-check/Comfort178Check.java'],check=True)
p=subprocess.run([J/'java.exe','-cp',O,'com.xenris.liquidwarsos.Comfort178Check'],capture_output=True);out=p.stdout+p.stderr;print(out.decode());target=R/'docs/game/controls-1.78/java-final.log';assert not target.exists();target.write_bytes(out);raise SystemExit(p.returncode)
