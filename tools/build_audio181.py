"""Original procedural Liquid Wars sound design. GPL-3.0-or-later. No samples/models."""
from pathlib import Path
import numpy as np, wave, json, hashlib
R=Path(__file__).resolve().parents[1];OUT=R/'app/src/main/assets/audio';SRC=R/'art/source/audio-1.81';OUT.mkdir(exist_ok=True);SRC.mkdir(parents=True,exist_ok=True)
SR=48000
def noise(n,seed,slow=9):
 rng=np.random.default_rng(seed);a=rng.normal(0,1,n);return np.convolve(a,np.ones(slow)/slow,mode='same')
def drop(d,f,end,tau=.055):
 t=np.arange(round(d*SR))/SR;hz=end+(f-end)*np.exp(-t/.025);phase=2*np.pi*np.cumsum(hz)/SR
 return (1-np.exp(-t/.002))*np.exp(-t/tau)*(np.sin(phase)+.16*np.sin(phase*2.13))*np.minimum(1,np.maximum(0,(d-t)/.018))
def layer(a,b,sec,gain=1):
 pos=round(sec*SR);n=min(len(b),len(a)-pos)
 if n>0:a[pos:pos+n]+=b[:n]*gain
def blank(d):return np.zeros(round(d*SR))
clips={}
# One close, clean droplet: very short confirmation, no sustained chime.
a=blank(.145);layer(a,drop(.14,1100,460,.031),0);clips['order']=(a,.075,'Single damped droplet; immediate point/stop/split confirmation')
# A soft stream sweep carries a route gesture; texture dominates pitched content.
a=blank(.32);t=np.arange(len(a))/SR;a+=noise(len(a),18102,17)*np.sin(np.pi*t/.32)**1.7*.7
for at,f in [(0.025,830),(.080,990),(.155,1150),(.22,1260)]:layer(a,drop(.095,f,f*.64,.023),at,.27)
clips['route']=(a,.055,'Airy water sweep with four irregular beads; route commitment')
# Fine bubbles are intentionally dry, quiet and irregular, not a repeated metronome.
a=blank(.19)
for at,f,g in [(0,770,.8),(.022,1360,.3),(.073,590,.6),(.116,1090,.25)]:layer(a,drop(.07,f,f*.5,.018),at,g)
clips['devour']=(a,.047,'Irregular close microbubbles for HP loss while devouring a pickup')
# A wet impact opens into an inharmonic glass resonance; distinct from capture's melody.
a=blank(.52);layer(a,drop(.23,720,310,.07),0,.8);t=np.arange(len(a))/SR
for f,g in [(930,.22),(1473,.09),(2071,.035)]:a+=g*np.sin(2*np.pi*f*t+1.5*np.exp(-t/.06))*np.exp(-t/.15)*(1-np.exp(-t/.016))
clips['item']=(a,.095,'Wet impact and delicate glass afterglow; item gained/activated')
# Capture gets a rounded rising triad and a low foundation, never an alarm.
a=blank(.78)
for at,f,g in [(0,330,.7),(.13,440,.65),(.26,550,.6),(.39,660,.55)]:layer(a,drop(.36,f*1.5,f,.115),at,g)
clips['capture']=(a,.108,'Four rounded rising droplets, spaced in a complete phrase; site captured')
# Fixed-income production is a subdued pouring texture, below command sounds.
a=blank(.37);t=np.arange(len(a))/SR;a+=noise(len(a),18106,35)*(.5+.5*np.sin(t*35))*np.sin(np.pi*t/.37)**2
for at,f in [(.045,260),(.09,420),(.16,315),(.235,505)]:layer(a,drop(.105,f*1.6,f,.032),at,.3)
clips['produce']=(a,.052,'Low pouring/rippling texture; actual newly generated particles')
# Warning is a sustained, low bowed-water resonance rather than more bubble impacts.
a=blank(.62);t=np.arange(len(a))/SR;phase=2*np.pi*np.cumsum(196-48*(t/.62)**2)/SR
envelope=np.sin(np.pi*t/.62)**.8
a=envelope*(.65*np.sin(phase+.72*np.sin(2*np.pi*7*t))+.22*np.sin(phase*1.47)+.07*np.sin(phase*3.71)+.12*noise(len(a),18107,23))
clips['alert']=(a,.10,'One low bowed-water resonance with downward bend; distinct warning without a siren')
# Rewind is a reverse suction followed by a final soft closure, not a reversed victory tune.
a=blank(.74);t=np.arange(len(a))/SR;a+=noise(len(a),18108,22)*np.sin(np.pi*t/.74)**1.2*.45
for at,f in [(0,450),(.13,610),(.23,800)]:layer(a,drop(.28,f*1.7,f,.075)[::-1],at,.5)
layer(a,drop(.22,650,190,.055),.50,.7);clips['rewind']=(a,.08,'Reverse liquid suction and a falling closing bubble; rewind reset')
catalog=[];audition=[]
for name,(a,rms,intent) in clips.items():
 # Remove DC, normalize gently, leave ample peak headroom, and silence both boundaries.
 a-=a.mean();a*=min(rms/max(1e-9,float(np.sqrt(np.mean(a*a)))),.40/max(1e-9,float(abs(a).max())))
 edge=min(480,len(a)//4);a[:edge]*=np.linspace(0,1,edge);a[-edge:]*=np.linspace(1,0,edge)
 pcm=(a*32767).round().astype('<i2');p=OUT/(name+'.wav')
 with wave.open(str(p),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(SR);w.writeframes(pcm.tobytes())
 catalog.append(dict(name=name,intent=intent,seconds=len(a)/SR,peak_dbfs=20*np.log10(max(abs(a).max(),1e-9)),rms_dbfs=20*np.log10(max(float(np.sqrt(np.mean(a*a))),1e-9)),sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
 audition.extend([a,np.zeros(round(SR*.65))])
with wave.open(str(SRC/'sound-preview.wav'),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(SR);w.writeframes((np.concatenate(audition)*32767).round().astype('<i2').tobytes())
(SRC/'catalog.json').write_text(json.dumps(dict(sample_rate=SR,channels=1,license='GPL-3.0-or-later; original procedural sounds, no third-party recording',source='tools/build_audio181.py',audition_order=list(clips),clips=catalog),ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(catalog,indent=2))
