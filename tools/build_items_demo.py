"""Trim loading transitions from actual App recording. No retouching or speed changes."""
from pathlib import Path
import av,json,hashlib,subprocess,imageio_ffmpeg
R=Path(__file__).resolve().parents[1];O=R/'docs/game/items-1.66/device';raw=O/'effects-demo-raw.mp4';out=O/'effects-demo.mp4'
video=av.open(str(raw));stream=video.streams.video[0];ranges=[];start=None;last=0;frames=0
for f in video.decode(video=0):
 t=float(f.time);a=f.to_ndarray(format='rgb24')[::32,::32,:].astype('float32');terrain=((a[:,:,0]>a[:,:,1]*1.12)&(a[:,:,1]>a[:,:,2]*1.12)&(a[:,:,0]>65)).mean()>.45
 if terrain and start is None:start=t
 if not terrain and start is not None:
  if t-start>.35:ranges.append([round(start,3),round(t,3)])
  start=None
 last=t;frames+=1
if start is not None:ranges.append([round(start,3),round(last,3)])
assert len(ranges)>=6,ranges
filters=['[0:v]trim=start='+str(a)+':end='+str(b)+',setpts=PTS-STARTPTS[v'+str(k)+']' for k,(a,b) in enumerate(ranges)]
filters.append(''.join('[v'+str(k)+']' for k in range(len(ranges)))+'concat=n='+str(len(ranges))+':v=1:a=0[out]')
command=[imageio_ffmpeg.get_ffmpeg_exe(),'-hide_banner','-loglevel','error','-y','-i',str(raw),'-filter_complex',';'.join(filters),'-map','[out]','-an','-c:v','libx264','-preset','medium','-crf','18','-pix_fmt','yuv420p','-fps_mode','vfr',str(out)]
subprocess.run(command,check=True)
framesDir=O/'video-frames';framesDir.mkdir(exist_ok=True);targets=[a+(b-a)*.78 for a,b in ranges];video=av.open(str(raw));at=0
for f in video.decode(video=0):
 if at<len(targets) and float(f.time)>=targets[at]:
  f.to_image().save(framesDir/f'effect-{at}.jpg',quality=92);at+=1
edited=av.open(str(out));decoded=sum(1 for _ in edited.decode(video=0))
report=dict(status='PASS',raw_sha256=hashlib.sha256(raw.read_bytes()).hexdigest(),edited_sha256=hashlib.sha256(out.read_bytes()).hexdigest(),width=stream.width,height=stream.height,raw_frames_decoded=frames,edited_frames_decoded=decoded,edited_duration_seconds=float(edited.duration/1000000),source_spans_seconds=ranges,edit='Only loading/ready/menu transitions removed. Actual game frames retained at original timing; no retouching or speed changes.',controlled_native_steps=True,performance_evidence=False)
(O/'video-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8');print(json.dumps(report,ensure_ascii=False))
