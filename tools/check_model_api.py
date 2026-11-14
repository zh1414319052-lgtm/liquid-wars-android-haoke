"""Compile actual pure Java protocol/scheduler code against an existing host JSON library."""
from pathlib import Path
import subprocess
R=Path(__file__).resolve().parents[1];O=R/'build/world-1.69/cloud-check';O.mkdir(parents=True,exist_ok=True)
J=Path(r'C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin')
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/org.json/json/20241224').rglob('*.jar'))
for name,text in {'StaticBits':'public static int[] alliances={0,1,2,3,4,5};','NativeInterface':'public static int teamScore(int p){return 400;} public static boolean areAllied(int a,int b){return a==b;} public static int allianceScore(int p){return 400;}'}.items():
 (O/f'{name}.java').write_text('package com.xenris.liquidwarsos; public class '+name+' {'+text+'}',encoding='utf8')
src=R/'app/src/main/java/com/xenris/liquidwarsos'
files=[*(src/'cloud'/f'{n}.java' for n in ['ModelAction','SnapshotItems','ModelTerrain','ModelStream','ModelConfig','ModelProtocol','ModelHttp','ModelMatch','ModelNavigation','ModelWorld','ModelFacts','ModelRouteExecutor','QaModelBudget']),src/'AllianceRules.java',src/'net/GameWire.java',R/'tools/cloud-check/Check.java',R/'tools/strategy-check/StrategyCheck.java',O/'StaticBits.java',O/'NativeInterface.java']
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',str(jar),'-d',str(O),*[str(p) for p in files]],check=True)
subprocess.run([str(J/'java.exe'),'-cp',str(O)+';'+str(jar),'com.xenris.liquidwarsos.cloud.Check'],check=True)
subprocess.run([str(J/'java.exe'),'-cp',str(O)+';'+str(jar),'com.xenris.liquidwarsos.cloud.StrategyCheck'],check=True)
