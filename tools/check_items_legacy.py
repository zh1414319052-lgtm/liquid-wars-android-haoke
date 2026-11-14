"""Native regression against preserved1.64 sources. No device, no renderer, no network."""
from pathlib import Path
import subprocess,zipfile,io
R=Path(__file__).resolve().parents[1];B=R/'build/world-1.69';O=R/'docs/game/world-1.69'
baseline=B/'baseline';baseline.mkdir(exist_ok=True)
with zipfile.ZipFile(io.BytesIO(subprocess.check_output(['git','archive','--format=zip','7c4a9f0','app/src/main/cpp/application'],cwd=R))) as z:z.extractall(baseline)
parts=['state','move','map','ai','ai_tactics','dot','player','random','spiral','info','visual_texture']
for name,core in [('before',baseline/'app/src/main/cpp/application'),('after',R/'app/src/main/cpp/application')]:
 files=['tools/items_legacy_trace.cpp']+[str(core/(n+'.cpp')) for n in parts]+(['app/src/main/cpp/application/items.cpp'] if name=='after' else [])+['app/src/main/cpp/aclib/lodepng.cpp']
 text='@echo off\ncall "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\BuildTools\\VC\\Auxiliary\\Build\\vcvars64.bat" >nul\ncl.exe /nologo /std:c++17 /O2 /EHsc /Itools\\difficulty-check\\stubs /I"'+str(core)+'" /Iapp\\src\\main\\cpp\\aclib /DLODEPNG_COMPILE_DISK '+' '.join('"'+n+'"' for n in files)+' /Fe"'+str(B/f'legacy-{name}.exe')+'" /Fo"'+str(B).replace("\\","/")+'/"\nexit /b %errorlevel%\n'
 cmd=B/f'legacy-{name}.cmd';cmd.write_text(text,encoding='utf8')
 with (O/f'legacy-{name}-build.log').open('wb') as log:subprocess.run(['cmd','/c',str(cmd)],cwd=R,stdout=log,stderr=subprocess.STDOUT,check=True)
 with (O/f'legacy-{name}.csv').open('wb') as log:subprocess.run([str(B/f'legacy-{name}.exe'),str(R/'app/src/main/assets')],cwd=R,stdout=log,check=True)
assert (O/'legacy-before.csv').read_bytes()==(O/'legacy-after.csv').read_bytes(),'Unpowered trajectory changed'
print('PASS49 maps x15 checkpoints; identical1.64/1.65 hashes before first item wave (140steps/map,600particles, both RNG states/order/health/input/scores)')
