from pathlib import Path
import concurrent.futures,subprocess,json,sys,time,hashlib
R=Path(__file__).resolve().parents[1];O=R/'docs/game/local-ai-1.79';label=sys.argv[1];full='--full' in sys.argv;exe=sys.argv[sys.argv.index('--exe')+1] if '--exe' in sys.argv else 'core';seeds=int(sys.argv[sys.argv.index('--seeds')+1]) if '--seeds' in sys.argv else (8 if full else 2)
out=O/(label+'.jsonl');assert not out.exists(),'Retain every experiment'
maps=[1,8,17,28,41,49,50,54] if full else [1,17,49,54]
cases=[(m,17900+m+k*71,mode,swap,90 if full else 45,1000 if full else 400) for m in maps for k in range(seeds) for mode in ([0,1,2] if full else [0,1]) for swap in [0,1]]
source=R/'app/src/main/cpp/application/local_tactics.cpp'
(O/(label+'-identity.json')).write_text(json.dumps(dict(source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),exe_sha256=hashlib.sha256((R/f'build/local-ai-1.79/{exe}.exe').read_bytes()).hexdigest(),cases=len(cases),workers=4,measured_cpu_times_under_concurrency=True,baseline='unmodified1.78 AiTactics plus port of Java item selection; baseline has global enemy locations except invisible',new_observation='144px with walls and invisibility; no global enemy positions'),indent=2),encoding='utf8')
def run(c):
 t=time.monotonic();p=subprocess.run([str(R/f'build/local-ai-1.79/{exe}.exe'),str(R/'app/src/main/assets'),*map(str,c)],capture_output=True,text=True,timeout=600)
 if p.returncode:raise RuntimeError((c,p.stdout,p.stderr))
 r=json.loads(p.stdout);r['host_wall_s']=time.monotonic()-t;return r
with out.open('w',encoding='utf8') as f,concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
 for n,r in enumerate(pool.map(run,cases)):
  f.write(json.dumps(r)+'\n');f.flush()
  if n%16==15:print(label,n+1,'/',len(cases),flush=True)
rows=[json.loads(x) for x in out.read_text().splitlines()];summary={}
for mode in sorted(set(r['mode'] for r in rows)):
 a=[r for r in rows if r['mode']==mode];w=sum(r['new_win']==1 for r in a);loss=sum(r['new_win']==0 for r in a);summary[str(mode)]=dict(n=len(a),wins=w,losses=loss,ties=len(a)-w-loss,win_rate=w/max(1,w+loss),new_pickups=sum(r['new_pickups'] for r in a),old_pickups=sum(r['old_pickups'] for r in a))
(O/(label+'-summary.json')).write_text(json.dumps(summary,indent=2),encoding='utf8');print(json.dumps(summary),flush=True)
