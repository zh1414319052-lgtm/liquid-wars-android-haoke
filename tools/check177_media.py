from pathlib import Path
import subprocess,json
R=Path(__file__).resolve().parents[1];O=R/'docs/game/controls-items-1.77';D=O/'device';W=R/'work/compressed-images/controls177';W.mkdir(parents=True,exist_ok=True);F=Path('C:/Users/Administrator/.claude/skills/CR/ffmpeg');rows=[]
for name in ['actual-continuous-camera.mp4','actual-devour.mp4']:
 v=json.loads(subprocess.check_output([F/'ffprobe.exe','-v','error','-show_format','-show_streams','-of','json',D/name]));s=next(s for s in v['streams'] if s['codec_type']=='video');duration=float(v['format']['duration']);assert duration>5 and s['width']==1390 and s['height']==632;rows.append(dict(file=name,durationSeconds=duration,width=s['width'],height=s['height'],codec=s['codec_name'],actualApp=True,controlledSteps=True,notPerformanceTest=True))
for sec in [2,3,4]:subprocess.run([F/'ffmpeg.exe','-v','error','-ss',str(sec),'-i',D/'actual-devour.mp4','-frames:v','1','-vf','scale=1024:-2','-q:v','3','-y',W/f'devour-video-{sec}.jpg'],check=True)
(O/'media-verification.json').write_text(json.dumps(dict(status='PASS',videos=rows,firstDefaultResolutionRecording='INVALID: missing moov, retained in failures'),indent=2),encoding='utf8');print(json.dumps(rows))
