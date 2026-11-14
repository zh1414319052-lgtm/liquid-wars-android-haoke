"""Actual unchanged C++ core, resume-safe per-image evidence; no API/device."""
from pathlib import Path
import subprocess,json,concurrent.futures as cf,time,hashlib
R=Path(__file__).resolve().parents[1];O=R/'docs/game/biomes-1.62/core';O.mkdir(exist_ok=True)
S=R/'art/source/biomes-1.62/collision';A=R/'build/biome-assets'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
core={q.name:sha(q) for q in (R/'app/src/main/cpp/application').glob('*') if q.is_file() and q.suffix in ['.cpp','.hpp']}
core['free-spawn-check.exe']=sha(R/'build/free-spawn-check.exe')
(O/'core-identity.json').write_text(json.dumps(core,indent=2),encoding='utf8')
def check(i,maskhash):
 dest=O/f'{i:02}-{maskhash[:10]}';dest.mkdir(exist_ok=True);result=dest/'result.json'
 if result.exists():return json.loads(result.read_text('utf8'))
 started=time.time()
 with (dest/'run.log').open('w',encoding='utf8') as log:
  code=subprocess.run([str(R/'build/free-spawn-check.exe'),str(A),str(i),str(i),'128',str(dest/'placement.csv')],cwd=R,stdout=log,stderr=subprocess.STDOUT).returncode
 assert sha(A/'maps'/f'{i}-map.png')==maskhash,'Input changed while checking'
 row=dict(id=i,status='PASS' if code==0 else 'FAIL',exit=code,mask_sha256=maskhash,seeds=128,scales=[0,100,400,1000,4500],elapsed_s=round(time.time()-started,2),evidence=dest.relative_to(R).as_posix())
 result.write_text(json.dumps(row,indent=2),encoding='utf8');return row
pending={};completed={}
with cf.ThreadPoolExecutor(max_workers=8) as pool:
 while True:
  available={}
  generation=json.loads((R/'docs/game/biomes-1.62/generation.json').read_text('utf8'))
  for q in S.glob('*.json'):
   try:r=json.loads(q.read_text('utf8'))
   except (json.JSONDecodeError,OSError):continue
   if generation['maps'][str(r['id'])]['status'] not in ['GENERATED','EXISTING_APPROVED_PREVIEW'] or r['source_sha256']!=generation['maps'][str(r['id'])]['sha256']:continue
   available[r['id']]=r['collision_sha256']
  for i,h in available.items():
   if i in pending or completed.get(i,{}).get('mask_sha256')==h:continue
   pending[i]=pool.submit(check,i,h)
  for i,f in list(pending.items()):
   if f.done():
    completed[i]=f.result();print(json.dumps(completed[i]),flush=True);del pending[i]
  (O/'progress.json').write_text(json.dumps(dict(completed=len(completed),pending=sorted(pending),results=list(completed.values())),indent=2),encoding='utf8')
  if len(available)==50 and len(completed)==50 and not pending and generation['new_calls']==49:break
  if not json.loads((R/'docs/game/biomes-1.62/CONTROL.json').read_text('utf8')).get('generation_enabled',True) and not pending:break
  time.sleep(3)
print('FINAL',len(completed),'maps, failures',[i for i,v in completed.items() if v['status']!='PASS'],flush=True)
