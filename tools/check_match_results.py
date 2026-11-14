"""Focused local checks; no Android runtime, device, emulator or rendering. GPL-3.0-or-later."""
from pathlib import Path
import hashlib,json,subprocess,xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/game/strategy-1.60'
BUILD=ROOT/'build/match-result-check'
JAVA=Path(r'C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin')
def method(s,name):
    a=s.index('void '+name+'(');a=s.index('{',a);b=a+1;depth=1
    while depth:
        if s[b]=='{':depth+=1
        if s[b]=='}':depth-=1
        b+=1
    return s[a:b]
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()

# Historical 1.49 whole-app whitelist no longer describes versions1.50-1.59.
# Compare result-path files to the frozen current1.59 baseline. Package/rule
# invariants are checked separately; do not overwrite historical evidence.
baseline=json.loads((OUT/'baseline-1.59.json').read_text(encoding='utf8'))
protected={'app/src/main/java/com/xenris/liquidwarsos/MatchResultDialog.java',*(f'app/src/main/res/{lang}/match_result.xml' for lang in ['values','values-zh','values-b+zh+Hant'])}
changed=[]
for rel in protected:
    assert rel in baseline and digest(ROOT/rel)==baseline[rel],('result path changed from1.59',rel)
reskeys=None
for lang in ['values','values-zh','values-b+zh+Hant']:
    resources=ET.parse(ROOT/f'app/src/main/res/{lang}/match_result.xml').getroot()
    keys={n.attrib['name'] for n in resources}
    assert all(n.text and n.text.strip() for n in resources)
    if reskeys is None:reskeys=keys
    assert keys==reskeys
for role in ['GameActivity','GameServerActivity','GameClientActivity']:
    s=(ROOT/f'app/src/main/java/com/xenris/liquidwarsos/activities/{role}.java').read_text(encoding='utf8')
    rel=f'app/src/main/java/com/xenris/liquidwarsos/activities/{role}.java'
    previous=subprocess.check_output(['git','show','c7c320f:'+rel],cwd=ROOT).decode('utf8')
    for name in ['checkIfLost','checkForWinner','showResult', 'replayMatch' if role=='GameActivity' else 'leaveMatch']:
        current=method(s,name)
        if role=='GameActivity' and name=='replayMatch':
            # The only production replay change forwards explicit debug-only
            # QA guards, preventing saved API lanes from being enabled by QA.
            start=current.index('        // Keep explicit debug QA guards')
            end=current.index('        finish();',start)
            guard=current[start:end]
            assert 'FLAG_DEBUGGABLE' in guard and 'qa_model_teams' in guard and 'qa_paid_budget' in guard
            assert 'ModelStore' not in guard and 'SharedPreferences' not in guard
            current=current[:start]+current[end:]
        assert current==method(previous,name),('result behavior changed',role,name)
    assert 'makeDialogDismissIn' not in s and 'makeDialogCancelableIn' not in s
    assert method(s,'checkIfLost').index('gameFinished || lostGame')<method(s,'checkIfLost').index('showResult')
    winner=method(s,'checkForWinner')
    assert winner.index('gameFinished = true')<winner.index('showResult')
    assert 'isDestroyed()' in method(s,'showResult') and '(spectating && gameFinished)' in method(s,'showResult')
    if role=='GameActivity':
        retry=method(s,'replayMatch')
        assert retry.index('myGLSurfaceView.onPause')<retry.index('stopMatch()')<retry.index('StaticBits.newSeed()')<retry.index('startActivity(next)')
        assert 'if (leavingMatch' in retry
    else:
        run=method(s,'run')
        assert run.index('myGLSurfaceView.onPause')<run.index('NativeInterface.destroyGame()')<run.index('.setCallbacks(')<run.index('if (!isFinishing()) finish()')
        assert 'leaveMatch(false)' in s and 'leaveMatch(true)' in s
        if role=='GameServerActivity':
            assert 'gameFinished ? StaticBits.BACK_TO_MENU : StaticBits.KILL_GAME' in s
            assert 'while(running)' in method(s,'waitForEveryoneToBeReady')
        else:
            assert 'synchronized (nativeStateLock)' in method(s,'onServerMessageReceived')
            assert 'if (!running) return;' in method(s,'onServerMessageReceived')

# Run the real shared dialog adapter with tiny Java API doubles. This verifies
# callback wiring/labels and persistence options only, never Android UI/lifecycle.
# Reuse the existing1.52 custom-view fixture, not the retired platform buttons.
import ast
fixture=ast.parse((ROOT/'tools/check_ui_refresh.py').read_text('utf8'))
assignment=next(n for n in fixture.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='sources' for t in n.targets))
sources=ast.literal_eval(assignment.value)
for name,text in sources.items():
    p=BUILD/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf8')
classes=BUILD/'classes';classes.mkdir(parents=True,exist_ok=True)
files=[str(BUILD/name) for name in sources]+[str(ROOT/'app/src/main/java/com/xenris/liquidwarsos/MatchResultDialog.java')]
subprocess.run([str(JAVA/'javac.exe'),'-encoding','UTF-8','-d',str(classes),*files],check=True)
subprocess.run([str(JAVA/'java.exe'),'-cp',str(classes),'DialogCheck'],check=True)
report=dict(status='PASS',baseline='c7c320f/1.59-QA',baseline_files=len(baseline),protected_result_files=sorted(protected),changed_files=changed,
            source_contracts='PASS',shared_dialog_java_api_double_check='PASS',unchanged_result_paths=True,
            localized_resources=sorted(reskeys),device='NOT_TESTED',actual_android_buttons='NOT_TESTED',
            actual_replay_lifecycle='NOT_TESTED',multiplayer='NOT_TESTED',performance='NOT_TESTED')
(OUT/'focused-check.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print('PASS: source contracts, localized resources, baseline invariants. Actual Android UI/lifecycle/LAN NOT_TESTED.')
