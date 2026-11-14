from pathlib import Path
import subprocess
R=Path(__file__).resolve().parents[1];O=R/'build/follow-1.82/java';O.mkdir(parents=True,exist_ok=True)
J=Path('C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin');S=R/'app/src/main/java/com/xenris/liquidwarsos'
(O/'MyRenderer.java').write_text('package com.xenris.liquidwarsos; public final class MyRenderer { public static final int WIDTH=800,HEIGHT=480; }')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-d',str(O),str(O/'MyRenderer.java'),str(S/'PlayerHistory.java'),str(S/'net/GameWire.java'),str(S/'OrderQueue.java'),str(S/'OrderProtocol.java'),str(S/'OperationsSnapshot.java'),str(R/'tools/world-check/Operations180Check.java'),str(R/'tools/world-check/Follow182Check.java')],check=True)
subprocess.run([str(J/'java.exe'),'-cp',str(O),'com.xenris.liquidwarsos.Follow182Check'],check=True)
