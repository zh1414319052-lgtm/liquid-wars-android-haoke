"""Current user gates only: exact coverage/connected floor/diverse placement.
No spawn seats, forbidden LOS, detour/fairness/thickness/contact-time gates.
GPL-3.0-or-later. Native and Android evidence are separate.
"""
from pathlib import Path
import json,hashlib,xml.etree.ElementTree as ET
import numpy as np,cv2
from PIL import Image,ImageDraw,ImageFont
from scipy import ndimage as ndi
R=Path(__file__).resolve().parents[1];O=R/'docs/game/strategy-1.60';S=R/'art/source/free-maps-1.60'
def main():
 plans=json.loads((S/'maps.json').read_text('utf8'))['maps'];assert len(plans)==50;assert (O/'blind-review-directed-final.md').exists()
 yy,xx=np.mgrid[0:480,0:800].astype('float32');rows=[];samples=[];hist=[0]*4
 for p in plans:
  i=p['id'];im=Image.open(R/f'app/src/main/assets/maps/{i}-map.png');a=np.asarray(im);assert im.size==(800,480) and a.shape==(480,800,4);assert set(np.unique(a[:,:,3]))=={0,255};wall=a[:,:,3]>3
  assert ndi.label(~wall)[1]==1 and (~wall).sum()>=27000,(i,'invalid connected capacity')
  geo=Image.open(S/f'geometry/{i:02d}.png');art=Image.open(R/f'app/src/main/assets/maps/{i}-image.png');assert art.size==geo.size==(1980,900)
  cover=cv2.remap(np.asarray(geo),(xx+.5)*1980/800-.5,(yy+.5)*900/480-.5,cv2.INTER_LINEAR)>127;assert np.array_equal(cover,wall),(i,'visible coverage/collision mismatch')
  inside=wall[3:477,3:787];fraction=float(inside.mean());band=0 if fraction<.03 else 1 if fraction<.10 else 2 if fraction<.25 else 3;assert fraction<.45;hist[band]+=1;assert band==p['cover_band'];ys,xs=np.where(inside)
  rows.append(dict(id=i,wall_fraction=fraction,floor_cells=int((~wall).sum()),centroid_x=float((xs.mean()+3)/800) if len(xs) else None,centroid_y=float((479-ys.mean()-3)/480) if len(ys) else None,exact_coverage=True,sha256=hashlib.sha256((R/f'app/src/main/assets/maps/{i}-map.png').read_bytes()).hexdigest()));samples.append(inside)
 freq=np.stack(samples).mean(axis=0);selected=[r for r in rows if r['wall_fraction']>=.03];xr=[min(r['centroid_x'] for r in selected),max(r['centroid_x'] for r in selected)];yr=[min(r['centroid_y'] for r in selected),max(r['centroid_y'] for r in selected)];maximum=float(freq.max())
 # "About0.2..0.8" is reported as observed range, not silently replaced by a
 # hard invented bound. Only the user's explicit maximum frequency is gated.
 assert maximum<=.45,maximum;assert all(hist),hist
 locales=[]
 for path in (R/'app/src/main/res').glob('values*/strings.xml'):
  array=ET.parse(path).getroot().find("string-array[@name='maps_array']")
  if array is not None:assert len(array)==51;locales.append(path.parent.name)
 report=dict(status='PASS',maps=50,wall_bands=hist,centroid_x_range=xr,centroid_y_range=yr,maximum_wall_frequency_excluding_frame=maximum,frame_exclusion='raw orientation: rows3..476, columns3..786; HUD/frame excluded. Per-game flips separately verified in native core.',rows=rows,locales=locales,not_proved=['Fun/human experience','Native passage/capacity/battles: separate checks','Android rendering/performance: separate checks'])
 (O/'free-map-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
 font=S/'fonts/NotoSansSC-VF.ttf';assert font.exists();font=ImageFont.truetype(str(font),21);heat=np.zeros((*freq.shape,3),np.uint8);heat[:,:,0]=(freq*255).astype('uint8');heat[:,:,2]=((1-freq)*110).astype('uint8');page=Image.new('RGB',(1200,770),(25,32,36));page.paste(Image.fromarray(heat).resize((1200,720),Image.Resampling.NEAREST),(0,50));d=ImageDraw.Draw(page);d.text((12,10),f'50图墙频率 · 红色越亮越常见 · 最高{maximum:.0%} · 不含外框',font=font,fill=(204,218,216));page.save(O/'free-wall-frequency.png')
 print('PASS50 exact masks/connected floors/locales; bands',hist,'centroids',xr,yr,'max wall frequency',maximum)
if __name__=='__main__':main()
