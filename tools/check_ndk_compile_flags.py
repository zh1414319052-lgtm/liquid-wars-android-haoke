"""Inspect ndk-build dry-run effective ordering; never compiles or installs software."""
from pathlib import Path
import subprocess,json,re
R=Path(__file__).resolve().parents[1];O=R/'docs/game/performance-1.68';report={}
for p in (R/'app/build/.cxx/Debug/2z6a2ur4').glob('*/android_gradle_build.json'):
 d=json.loads(p.read_text('utf8'));v=next(v for v in d['libraries'].values() if v['artifactName']=='application');cmd=v['buildCommandComponents'][:-1]+['nativeinterface','-n','-B','V=1']
 raw=subprocess.check_output(cmd,cwd=R/'app',stderr=subprocess.STDOUT).decode('utf8','replace');(O/f'ndk-dry-run-{p.parent.name}.log').write_text(raw,encoding='utf8');rows=[]
 for name in ['state.cpp','move.cpp','items.cpp','graphics.cpp']:
  matches=[line for line in raw.splitlines() if re.search(r'[/\\]'+re.escape(name)+r'(?:\s|\")',line) and ' -c ' in line and 'clang++' in line]
  assert len(matches)==1,(p.parent.name,name,len(matches));line=matches[0];opts=re.findall(r'(?:^|\s)(-O\S+)',line)
  assert opts[-1]=='-O2' and '-fno-fast-math' in line and '-ffp-contract=off' in line,(name,line)
  rows.append(dict(file=name,optimization_flags=opts,effective_last_optimization=opts[-1],no_fast_math=True,no_fp_contract=True))
 report[p.parent.name]=rows
assert len(report)==4,report
(O/'compile-flags.json').write_text(json.dumps(report,indent=2),encoding='utf8');print('PASS effective strict -O2, four source modules across all four ABIs')
