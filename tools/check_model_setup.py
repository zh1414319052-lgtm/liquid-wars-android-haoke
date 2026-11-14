"""Focused setup/discovery checks; no API credentials, Android device or alternate renderer."""
from pathlib import Path
import subprocess
R=Path(__file__).resolve().parents[1];O=R/'build/model-setup-check';O.mkdir(parents=True,exist_ok=True)
J=Path(r'C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin')
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/org.json/json/20241224').rglob('*.jar'))
for name,text in {'StaticBits':'public static int[] alliances={0,1,2,3,4,5};','NativeInterface':'public static int teamScore(int p){return 400;}'}.items():
 (O/f'{name}.java').write_text('package com.xenris.liquidwarsos; public class '+name+' {'+text+'}',encoding='utf8')
src=R/'app/src/main/java/com/xenris/liquidwarsos'
files=[*(src/'cloud'/f'{n}.java' for n in ['ModelAction','ModelTerrain','ModelStream','ModelConfig','ModelProtocol','ModelHttp','ModelMatch','ModelProviders','ModelDirectory','ModelSetupDraft','ModelNavigation','ModelWorld','ModelFacts','ModelRouteExecutor','QaModelBudget']),src/'AllianceRules.java',src/'net/GameWire.java',R/'tools/cloud-check/Check.java',R/'tools/cloud-check/SetupCheck.java',O/'StaticBits.java',O/'NativeInterface.java']
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',str(jar),'-d',str(O),*[str(p) for p in files]],check=True)
for cls in ['SetupCheck','Check']:
 subprocess.run([str(J/'java.exe'),'-cp',str(O)+';'+str(jar),'com.xenris.liquidwarsos.cloud.'+cls],check=True)
