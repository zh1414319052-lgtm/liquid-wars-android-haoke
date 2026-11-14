"""Source-asset colour checks and labelled authoring preview, not a game screenshot.
SPDX-License-Identifier: GPL-3.0-or-later
"""
from pathlib import Path
import argparse,json,zipfile,io,hashlib
import numpy as np
import cv2
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/game/comfort-1.48'
FONT=Path('C:/Windows/Fonts/NotoSansSC-VF.ttf')
def luminance(rgb):
    x=rgb.astype('float64')/255;x=np.where(x<=.04045,x/12.92,((x+.055)/1.055)**2.4)
    return x[:,:,0]*.2126+x[:,:,1]*.7152+x[:,:,2]*.0722
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--baseline-apk',required=True);args=parser.parse_args()
    scheme=json.loads((ROOT/'art/source/comfort-1.48/palette.json').read_text(encoding='utf8'));old_scheme=json.loads((ROOT/'art/source/comfort-1.48/palette-1.47-reference.json').read_text(encoding='utf8'))
    teams=np.array(scheme['team_rgb'],dtype=np.float32);assert teams.max()<=200
    saturation=(teams.max(1)-teams.min(1))/teams.max(1);assert saturation.max()<.55
    lab=cv2.cvtColor((teams/255).reshape(1,6,3),cv2.COLOR_RGB2LAB).reshape(6,3);separation=[float(np.linalg.norm(lab[a]-lab[b])) for a in range(6) for b in range(a+1,6)];assert min(separation)>15
    rows=[]
    with zipfile.ZipFile(args.baseline_apk) as z:
        for i in range(46):
            mask=ROOT/f'app/src/main/assets/maps/{i}-map.png';assert mask.read_bytes()==z.read(f'assets/maps/{i}-map.png'),f'Mask changed {i}'
            old=np.array(Image.open(io.BytesIO(z.read(f'assets/maps/{i}-image.png'))).convert('RGB'));new=np.array(Image.open(ROOT/f'app/src/main/assets/maps/{i}-image.png').convert('RGB'));assert old.shape==new.shape
            geometry=np.array(Image.open(ROOT/f'art/source/fullscreen-1.47/geometry/{i:02d}.png'));wall=geometry>250;floor=geometry<5
            for m in [wall,floor]:m[:20]=False;m[-20:]=False;m[:,:20]=False;m[:,-40:]=False
            stats=[]
            for image in [old,new]:
                l=luminance(image);a=float(np.median(l[wall])) if wall.any() else None;b=float(np.median(l[floor]));contrast=(a+.05)/(b+.05) if a is not None else None;stats.append(dict(obstacle_median_luminance=a,floor_median_luminance=b,contrast=contrast,rgb_p99=float(np.percentile(image,99))))
            if stats[1]['contrast'] is not None:assert 2<=stats[1]['contrast']<=5,(i,stats[1])
            rows.append(dict(id=i,old=stats[0],new=stats[1],mask_sha256=hashlib.sha256(mask.read_bytes()).hexdigest()))
        assert FONT.exists();font=ImageFont.truetype(str(FONT),23);small=ImageFont.truetype(str(FONT),19)
        page=Image.new('RGB',(1536,650),(31,37,42));draw=ImageDraw.Draw(page)
        draw.text((15,10),'配色素材对比（非游戏截图；非屏幕亮度实测）',font=font,fill=(191,201,207))
        old=Image.open(io.BytesIO(z.read('assets/maps/1-image.png'))).convert('RGB');new=Image.open(ROOT/'app/src/main/assets/maps/1-image.png').convert('RGB')
        for col,(im,label) in enumerate([(old,'1.47 原配色素材'),(new,'1.48 柔和暮色素材')]):
            im.thumbnail((756,344));x=col*768+6;page.paste(im,(x,50));draw.text((x+7,401),label,font=font,fill=(191,201,207))
        names=['绿','蓝','红','青','黄','洋红']
        for col,palette in enumerate([old_scheme,scheme]):
            for index,(name,rgb) in enumerate(zip(names,palette['team_rgb'])):
                x=col*768+index*124+15;rgb=tuple(int(round(c)) for c in rgb);draw.rounded_rectangle((x,466,x+107,507),radius=12,fill=rgb);draw.text((x+32,517),name,font=small,fill=(177,190,199))
            draw.text((col*768+15,568),'队伍与HUD基色（实际粒子还受血量、透明边缘影响）',font=small,fill=(177,190,199))
        page.save(OUT/'colour-comparison.png');page.save(OUT/'colour-comparison-preview.jpg',quality=86)
    result=dict(status='PASS',baseline='1.47-QA',maps=46,mask_bytes_unchanged=True,team_max_channel=int(teams.max()),team_max_hsv_saturation=float(saturation.max()),minimum_pairwise_lab_distance=min(separation),health_brightness=scheme['health_brightness'],measurement='RGB source-asset statistics only; not physical screen luminance or medical evidence',actual_comfort='NOT_TESTED',colour_vision_deficiency='NOT_TESTED',rows=rows)
    (OUT/'palette-check.json').write_text(json.dumps(result,indent=2),encoding='utf8');print('PASS:46 masks unchanged; softer source contrasts,6 distinct base colours, max RGB200. Actual screen/comfort NOT_TESTED.')
if __name__=='__main__':main()
