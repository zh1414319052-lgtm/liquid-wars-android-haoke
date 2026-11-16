"""Reproducible local Godot data + native host QA build. Never installs or publishes."""
from pathlib import Path
import argparse, os, subprocess, sys

ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument("--godot",required=True)
parser.add_argument("--java-home",required=True)
parser.add_argument("--android-sdk",required=True)
parser.add_argument("--abi",choices=["all","x86_64","arm64-v8a"],default="all")
parser.add_argument("--label",required=True,help="Unique evidence directory name")
parser.add_argument("--evidence-dir",default="docs/game/godot-1.90")
args=parser.parse_args()
out=ROOT/args.evidence_dir/args.label
out.mkdir(parents=True,exist_ok=False)
env=os.environ.copy()
env.update(JAVA_HOME=args.java_home,ANDROID_HOME=args.android_sdk,ANDROID_SDK_ROOT=args.android_sdk)
env["PATH"]=str(Path(args.java_home)/"bin")+os.pathsep+env["PATH"]
def run(name,cmd):
    with (out/(name+".log")).open("w",encoding="utf-8") as log:
        p=subprocess.run(cmd,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=600)
    print(name,p.returncode,flush=True)
    if p.returncode:raise SystemExit(p.returncode)
    if name=='scene-contract':
        text=(out/(name+'.log')).read_text(encoding='utf-8')
        assert 'GAME_TEST_PASS' in text and 'SCRIPT ERROR' not in text,text
run("geometry",[sys.executable,str(ROOT/"tools/export_garden190.py")])
base=[args.godot,"--headless","--path",str(ROOT/"app/godot")]
run("import",base+["--editor","--import"])
run("scene-contract",base+["--script","res://tests/scene_contract.gd","--quit-after","180"])
run("pack",base+["--export-pack","Android data",str(ROOT/"app/src/main/assets/battle.pck")])
gradle=[str(ROOT/"gradlew.bat"),":app:assembleDebug","-PgardenQa186","--no-daemon","--no-watch-fs","-Dorg.gradle.native=false"]
if args.abi!="all":gradle+=["-PgodotQaAbi="+args.abi]
run("gradle",gradle)
