"""Focused source/timing/package checks; no provider request or device access."""
from pathlib import Path
import subprocess,json,re
R=Path(__file__).resolve().parents[1];O=R/'build/performance-1.68/classes';O.mkdir(parents=True,exist_ok=True)
J=Path('C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin');src=R/'app/src/main/java/com/xenris/liquidwarsos'
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-d',str(O),str(src/'FrameCadence.java'),str(R/'tools/frame-cadence-check/Check.java')],check=True)
subprocess.run([str(J/'java.exe'),'-cp',str(O),'com.xenris.liquidwarsos.Check'],check=True)
# Gameplay inputs, RNG/ordering and network paths stay byte-identical to prior QA.
for n in ['state','move','ai','ai_tactics','dot','player','random','spiral','map']:
 path=f'app/src/main/cpp/application/{n}.cpp';assert (R/path).read_bytes().replace(b'\r\n',b'\n')==subprocess.check_output(['git','show','563bf33:'+path],cwd=R).replace(b'\r\n',b'\n'),path
for n in ['GameActivity','GameServerActivity','GameClientActivity']:
 path=f'app/src/main/java/com/xenris/liquidwarsos/activities/{n}.java';assert (R/path).read_bytes().replace(b'\r\n',b'\n')==subprocess.check_output(['git','show','563bf33:'+path],cwd=R).replace(b'\r\n',b'\n'),path
assert 'VERSION_CODE = 22;' in (src/'StaticBits.java').read_text('utf8')
from fontTools.ttLib import TTFont
cmap=TTFont(R/'app/src/main/assets/fonts/NotoSansSC-Intro.ttf').getBestCmap()
ui=(src/'ItemOverlay.java').read_text('utf8');new=ui[ui.index(' public void showFrameOptions()'):ui.index('    public void showGuide()')]
missing=sorted({c for c in new if '\u4e00'<=c<='\u9fff' and ord(c) not in cmap});assert not missing,('Missing Noto Sans SC glyphs',missing)
print('PASS unchanged authority/three activity paths/handshake + new frame UI commercial font glyphs')
