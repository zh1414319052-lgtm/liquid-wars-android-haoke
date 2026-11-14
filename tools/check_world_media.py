"""Inspect unaltered App screen recordings; extraction only, no enhancement."""
from pathlib import Path
import subprocess,json
from PIL import Image
R=Path(__file__).resolve().parents[1];O=R/'docs/game/world-1.69';D=O/'device/final';P=R/'work/compressed-images/world169';P.mkdir(parents=True,exist_ok=True)
F=Path('C:/Users/Administrator/.claude/skills/CR/ffmpeg');rows=[]
for name in ['world169-joystick-live.mp4','world169-map50-live.mp4']:
 data=json.loads(subprocess.check_output([str(F/'ffprobe.exe'),'-v','error','-show_streams','-show_format','-of','json',str(D/name)]));s=next(s for s in data['streams'] if s['codec_type']=='video');duration=float(data['format']['duration']);assert duration>8 and s['width']==1920 and s['height']==872
 rows.append(dict(file=name,duration=duration,width=s['width'],height=s['height'],codec=s['codec_name'],raw_screens=True));
for sec in [6,14]:subprocess.run([str(F/'ffmpeg.exe'),'-v','error','-ss',str(sec),'-i',str(D/'world169-map50-live.mp4'),'-frames:v','1','-vf','scale=1024:-2','-q:v','3','-y',str(P/f'video-{sec}.jpg')],check=True)
for name in ['world169-six-items.jpg','world169-50-full.jpg','world169-51-near.jpg']:
 im=Image.open(D/name);im.thumbnail((1536,1536));im.save(P/name,quality=84)
(O/'video-metadata.json').write_text(json.dumps(dict(status='PASS',videos=rows),indent=2),encoding='utf8');print(json.dumps(rows))
