"""Two generated top views -> editable vector artwork + independent 5x collision.
No generator used for any unrequested map; original rasters retained verbatim.
"""
from pathlib import Path
import sys,json,hashlib,html
import numpy as np,cv2
from scipy import ndimage as ndi
from PIL import Image
import build_biome_svg as b
R=Path(__file__).resolve().parents[1];S=R/'art/source/world-1.69';A=R/'app/src/main/assets/maps';O=R/'docs/game/world-1.69';W,H=2000,960
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
for ident,name in [(50,'断冰峡湾'),(51,'丹霞回廊')]:
 raw=S/'originals'/f'{ident}.png';im=Image.open(raw).convert('RGB');b.W=W;b.H=H;art,stats=b.vectorize(im)
 # Classify terrain footprint, never paint a fake bridge over an obstacle.
 small=np.asarray(im.resize((W,H),Image.Resampling.LANCZOS));smooth=cv2.GaussianBlur(small,(5,5),0).astype(np.int16)
 if ident==50:wall=cv2.cvtColor(smooth.astype(np.uint8),cv2.COLOR_RGB2GRAY)>105
 else:wall=(smooth[:,:,0]-smooth[:,:,1]>30)&(smooth[:,:,0]>92)
 wall=cv2.morphologyEx(wall.astype(np.uint8),cv2.MORPH_CLOSE,np.ones((5,5),np.uint8))>0
 labels,_=ndi.label(wall);sizes=np.bincount(labels.ravel());wall&=sizes[labels]>=24
 wall[:4]=wall[-4:]=True;wall[:,:4]=True;wall[:,-13:]=True
 labels,count=ndi.label(~wall);sizes=np.bincount(labels.ravel());sizes[0]=0;main=sizes.argmax();sealed=(labels!=main)&~wall;wall|=sealed
 assert ndi.label(~wall)[1]==1 and (~wall).sum()>500000
 # Floor remains one component after removing a small edge clearance, so a
 # one-pixel accidental bridge cannot be the only connection between regions.
 clear=ndi.binary_erosion(~wall,iterations=3);lab,n=ndi.label(clear);mass=np.bincount(lab.ravel());mass[0]=0;major=[int(v) for v in mass[1:] if v>2000]
 assert not major or max(major)/sum(major)>.98,('Narrow separation',ident,major)
 path=b.run_paths(wall);layer=f'<g id="gameplay_collision" opacity="0" style="display:none"><path fill="white" d="{path}"/></g>'
 svg=f'<svg xmlns="{b.NS}" width="3000" height="1440" viewBox="0 0 {W} {H}"><title>World {ident}</title><desc>Generated top-down geology, traced palette artwork; independent hidden collision. 2000x960 cells, five times 800x480 area.</desc><g id="vector_art">{art}</g>{layer}</svg>'
 target=S/'svg'/f'{ident}.svg';target.write_text(svg,encoding='utf8');output=b.render(svg,3000,1440);assert np.array_equal(np.asarray(output),np.asarray(b.render(svg.replace(layer,""),3000,1440))),"Collision layer became visible";output.save(A/f'{ident}-image.png',optimize=True)
 rgba=np.zeros((H,W,4),np.uint8);rgba[wall]=255;Image.fromarray(rgba).save(A/f'{ident}-map.png',optimize=True)
 mask=f'<svg xmlns="{b.NS}" width="{W}" height="{H}"><rect width="{W}" height="{H}" fill="black"/><path fill="white" d="{path}"/></svg>'
 assert np.array_equal(np.asarray(b.render(mask,W,H))[:,:,0]>127,wall)
 (S/'collision'/f'{ident}.png').write_bytes((A/f'{ident}-map.png').read_bytes())
 meta=dict(id=ident,name_zh=name,width=W,height=H,area=W*H,area_ratio=5,wall_fraction=float(wall.mean()),walkable_pixels=int((~wall).sum()),ground_components=1,sealed_decorative_pit_pixels=int(sealed.sum()),clearance3_main_fraction=max(major)/sum(major) if major else 1,source_sha256=sha(raw),svg_sha256=sha(target),image_sha256=sha(A/f'{ident}-image.png'),collision_sha256=sha(A/f'{ident}-map.png'),vector=stats,embedded_raster_images=0,builtin_generation=True,paid_model_api=False)
 (S/'collision'/f'{ident}.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf8')
 preview=small.copy();preview[wall]=(preview[wall]*.6+np.array([215,60,100])*.4).astype(np.uint8);Image.fromarray(preview).resize((1250,600)).save(O/f'map{ident}-collision.jpg',quality=84)
 print(json.dumps({k:meta[k] for k in ['id','area_ratio','wall_fraction','walkable_pixels','ground_components','clearance3_main_fraction']},ensure_ascii=False),flush=True)
