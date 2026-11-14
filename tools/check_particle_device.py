"""Bound MuMu2 only. No configuration/key access; all fixture model lanes zero."""
from pathlib import Path
import argparse,json,subprocess,time
from PIL import Image
R=Path(__file__).resolve().parents[1]
O=R/'docs/game/particles-1.63'; D=O/'device'
ADB=Path('C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe')
PKG='com.xenris.liquidwarsos.haoke.debug'
def adb(*args):
    p=subprocess.run([str(ADB),'-s','127.0.0.1:16448',*map(str,args)],capture_output=True)
    if p.returncode:
        message=(p.stdout+p.stderr).decode('utf8','replace')
        (O/f'device-command-error-{time.time_ns()}.txt').write_text(message,encoding='utf8')
        raise RuntimeError(f'ADB exit {p.returncode}: {message}')
    return p
def main():
    global O,D
    p=argparse.ArgumentParser();p.add_argument('--phase',required=True,choices=['before','after']);p.add_argument('--maps',default='28,5');p.add_argument('--dots',type=int,default=1000);p.add_argument('--install-test',action='store_true');p.add_argument('--capture-set');a=p.parse_args()
    if a.capture_set:
        assert a.capture_set.replace('-','').isalnum()
        O=O/'captures'/a.capture_set;D=O/'device'
    D.mkdir(exist_ok=True,parents=True)
    if a.install_test:
        text=adb('install','-r',R/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk').stdout.decode('utf8','replace');assert 'Success' in text
    for m in map(int,a.maps.split(',')):
        assert 0<=m<50 and a.dots in [0,100,400,1000,4500]
        name=f'particles-{a.phase}-{m}-{a.dots}'
        log=O/f'{name}.log';assert not log.exists(),'Keep old result; use a separate attempt log before retry'
        result=adb('shell','am','instrument','-w','-r','-e','class','com.xenris.liquidwarsos.ParticleVisualProbeTest','-e','map',m,'-e','seed','1628','-e','dots',a.dots,'-e','phase',a.phase,PKG+'.test/androidx.test.runner.AndroidJUnitRunner')
        text=result.stdout.decode('utf8','replace')+result.stderr.decode('utf8','replace');log.write_text(text,encoding='utf8')
        assert 'OK (1 test)' in text and 'FAILURES' not in text,text[-2000:]
        for ext in ['.png','-repeat.png','-recovered.png','.json']:
            dst=D/(name+ext);assert not dst.exists();dst.write_bytes(adb('exec-out','run-as',PKG,'cat','files/'+name+ext).stdout)
        data=json.loads((D/(name+'.json')).read_text('utf8'));assert data['models']==0 and data['pause_pixel_stability'] and data['egl_recovery_pixel_identical']
        if a.phase=='after':
            old=D/f'particles-before-{m}-{a.dots}.json'
            if old.exists():
                prev=json.loads(old.read_text('utf8'))
                assert prev['snapshot_trajectory_sha256']==data['snapshot_trajectory_sha256'],'Real Android native trajectory differs from1.62'
                assert prev['frozen_state_sha256']==data['frozen_state_sha256']
                assert Image.open(old.with_suffix('.png')).size==Image.open(D/(name+'.png')).size,'Comparison resolution differs; preserve and recapture both versions'
        print('PASS',name,'120-step Android snapshots, stable/recreated GLES, zero model lanes',flush=True)
if __name__=='__main__':main()
