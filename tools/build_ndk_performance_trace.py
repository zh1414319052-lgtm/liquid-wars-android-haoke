"""Build Android x86_64 baseline O0/current O2 same-input full-state hash probes."""
from pathlib import Path
import subprocess,zipfile,io,shutil,json
R=Path(__file__).resolve().parents[1];B=R/'build/performance-1.68';O=R/'docs/game/performance-1.68'
N=Path('C:/Users/Administrator/AppData/Local/Android/Sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64')
base=B/'ndk-baseline';base.mkdir(exist_ok=True)
with zipfile.ZipFile(io.BytesIO(subprocess.check_output(['git','archive','--format=zip','563bf33','app/src/main/cpp/application'],cwd=R))) as z:z.extractall(base)
parts=['state','items','move','map','ai','ai_tactics','dot','player','random','spiral','info','visual_texture']
for name,core,opt in [('baseline',base/'app/src/main/cpp/application','-O0'),('optimized',R/'app/src/main/cpp/application','-O2')]:
 files=[R/'tools/performance_core_trace.cpp']+[core/(n+'.cpp') for n in parts]+[R/'app/src/main/cpp/aclib/lodepng.cpp']
 cmd=[str(N/'bin/clang++.exe'),'--target=x86_64-linux-android26','--sysroot='+str(N/'sysroot'),'-std=c++17',opt,'-fno-fast-math','-ffp-contract=off','-static-libstdc++','-DLODEPNG_COMPILE_DISK','-I'+str(R/'tools/difficulty-check/stubs'),'-I'+str(core),'-I'+str(R/'app/src/main/cpp/aclib'),*map(str,files),'-o',str(B/f'core-{name}')]
 with (O/f'ndk-{name}-build.log').open('wb') as log:subprocess.run(cmd,cwd=R,stdout=log,stderr=subprocess.STDOUT,check=True)
 print('Built',name,opt)
maps=B/'ndk-assets/maps';maps.mkdir(parents=True,exist_ok=True)
for p in (R/'app/src/main/assets/maps').glob('*-map.png'):shutil.copy2(p,maps/p.name)
