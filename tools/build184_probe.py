from pathlib import Path
import os,subprocess
r=Path(__file__).resolve().parents[1];ndk=Path(os.environ['LOCALAPPDATA'])/'Android/Sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64/bin'
libs=list((r/'app/build/intermediates/cxx/Debug').glob('*/obj/x86_64/libnativeinterface.so'))
if not libs:libs=list((r/'app/build/intermediates/cxx/Debug').rglob('x86_64/libnativeinterface.so'))
assert libs
lib=max(libs,key=lambda p:p.stat().st_mtime);out=r/'app/src/androidTest/jniLibs/x86_64/libonline184probe.so';out.parent.mkdir(parents=True,exist_ok=True)
cmd=[str(ndk/'x86_64-linux-android26-clang++.cmd'),'-shared','-fPIC','-std=c++17','-O2','-Wl,-z,max-page-size=16384','-I'+str(r/'app/src/main/cpp'),'-I'+str(r/'app/src/main/cpp/aclib'),str(r/'tools/online184_probe.cpp'),'-L'+str(lib.parent),'-lnativeinterface','-o',str(out)]
subprocess.run(cmd,check=True);print('Read-only instrumentation probe compiled; release APK unchanged.')
