"""Compile fixture only into instrumentation APK; no client test commands."""
from pathlib import Path
import os,subprocess
r=Path(__file__).resolve().parents[1]
ndk=Path(os.environ['LOCALAPPDATA'])/'Android/Sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64/bin'
lib=max((r/'app/build/intermediates/cxx/Debug').rglob('x86_64/libnativeinterface.so'),key=lambda p:p.stat().st_mtime)
out=r/'app/src/androidTest/expedition192Jni/x86_64/libexpedition192probe.so';out.parent.mkdir(parents=True,exist_ok=True)
subprocess.run([str(ndk/'x86_64-linux-android26-clang++.cmd'),'-shared','-fPIC','-std=c++17','-O2','-Wl,-z,max-page-size=16384','-I'+str(r/'app/src/main/cpp'),'-I'+str(r/'app/src/main/cpp/aclib'),str(r/'tools/expedition192_probe.cpp'),'-L'+str(lib.parent),'-lnativeinterface','-o',str(out)],check=True)
print('TEST-ONLY native fixture compiled')
