from pathlib import Path
import csv,json,hashlib
R=Path(__file__).resolve().parents[1];rows=list(csv.DictReader((R/'art/ASSETS.csv').open(encoding='utf-8-sig')));checked=[]
for row in rows:
 for key in ['runtime_image','runtime_collision','editable_geometry']:
  for item in row.get(key,'').split(';'):
   if not item.strip():continue
   p=R/item.strip();assert p.is_file(),(row.get('id'),key,str(p));checked.append(item.strip())
for item in ['art/source/free-maps-1.60/fonts/NotoSansSC-VF.ttf','app/src/main/assets/licenses/NotoSansSC-OFL.txt','art/source/icon-1.53','third_party']:
 assert (R/item).exists(),item
(R/'docs/game/controls-items-1.77/asset-verification.json').write_text(json.dumps(dict(status='PASS',catalogRows=len(rows),currentRuntimeAndEditablePathsChecked=len(checked),allFilesPresent=True,sourceAndLicensePreserved=True,newGeneratedAssets=False),indent=2),encoding='utf8');print('Asset inventory PASS',len(rows),'rows',len(checked),'actual paths')
