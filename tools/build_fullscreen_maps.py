"""Build original vector-derived2D arenas. No game renderer/device is used.
SPDX-License-Identifier: GPL-3.0-or-later
Dependencies for authoring only: Pillow, numpy, scipy, OpenCV. Runtime uses PNG/GLES1.
"""
from pathlib import Path
import argparse,json,math,hashlib,csv,xml.etree.ElementTree as ET
import numpy as np
from PIL import Image,ImageDraw,ImageFont,ImageFilter
import cv2
from scipy import ndimage as ndi
from fullscreen_map_designs import designs,rect,oval,line,poly,W,H
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'art/source/fullscreen-1.47'
RUNTIME=ROOT/'app/src/main/assets/maps'
CHECK=ROOT/'docs/game/fullscreen-1.47'
FONT=Path('C:/Windows/Fonts/NotoSansSC-VF.ttf')
SIZE=(1980,900)
PALETTES={}
FRAME={}
TEAM_RGB=[]
PROFILE_NAME='全面屏地图'
def load_palette(path):
    global PALETTES,FRAME,TEAM_RGB,PROFILE_NAME
    profile=json.loads(Path(path).read_text(encoding='utf8'))
    PALETTES={k:(v['floor'],v['obstacle'],v['rim']) for k,v in profile['map_palettes'].items()}
    FRAME=profile['frame'];TEAM_RGB=profile['team_rgb'];PROFILE_NAME=profile['name']
SPAWNS=[(20,50),(20,430),(400,50),(400,430),(780,50),(780,430)]

def raster(ops,size,ss=1):
    sx=size[0]*ss/W;sy=size[1]*ss/H;im=Image.new('L',(size[0]*ss,size[1]*ss));d=ImageDraw.Draw(im)
    for op in ops:
        fill=0 if op.get('cut') else 255;k=op['kind']
        if k in ('rect','oval'):
            x0,y0,x1,y1=op['box'];box=(round(x0*sx),round(y0*sy),round(x1*sx),round(y1*sy))
            if k=='oval':d.ellipse(box,fill=fill)
            else:d.rounded_rectangle(box,radius=round(op.get('radius',0)*min(sx,sy)),fill=fill)
        elif k=='poly':d.polygon([(round(x*sx),round(y*sy)) for x,y in op['points']],fill=fill)
        else:
            # Expand strokes in design coordinates before the non-square rule-grid transform.
            points=op['points'];radius=op['width']/2
            for (ax,ay),(bx,by) in zip(points,points[1:]):
                length=math.hypot(bx-ax,by-ay)
                if length==0:continue
                nx=-(by-ay)/length*radius;ny=(bx-ax)/length*radius
                d.polygon([(round(x*sx),round(y*sy)) for x,y in [(ax+nx,ay+ny),(bx+nx,by+ny),(bx-nx,by-ny),(ax-nx,ay-ny)]],fill=fill)
            for x,y in points:d.ellipse(((x-radius)*sx,(y-radius)*sy,(x+radius)*sx,(y+radius)*sy),fill=fill)
    return im

def collision(ops):
    # One exported coverage image drives both artwork and collision. A second low-res
    # rasterizer can shift thin curves by a fraction of a cell, so sample the actual
    # vector coverage at rule-cell centers, then quantize only the authority PNG.
    coverage=np.asarray(raster(ops,SIZE,2).resize(SIZE,Image.Resampling.LANCZOS))
    yy,xx=np.mgrid[0:480,0:800].astype('float32')
    return cv2.remap(coverage,(xx+.5)*SIZE[0]/800-.5,(yy+.5)*SIZE[1]/480-.5,cv2.INTER_LINEAR)>127

def fractal_ops():
    yy,xx=np.mgrid[0:400,0:880];c=(xx/880*3.2-2.25)+1j*(yy/400*2.4-1.2);z=np.zeros_like(c);inside=np.ones(c.shape,bool)
    for _ in range(55):
        z[inside]=z[inside]*z[inside]+c[inside];inside &= np.abs(z)<2
    contours,_=cv2.findContours(inside.astype('uint8'),cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE)
    return [poly([(float(p[0])*W/880,float(p[1])*H/400) for p in cv2.approxPolyDP(c,1.3,True)[:,0]]) for c in contours if cv2.contourArea(c)>30]

def finalize(plan):
    if plan['id']==39:plan['ops']+=fractal_ops()
    # Equal, generous pockets support all original team sizes without changing spawn code.
    for x,y in SPAWNS:plan['ops'].append(oval(x/800*W,(479-y)/480*H,195,160,True))
    # Existing screen-edge HUD occupies these explicit, non-playable frame strips.
    plan['ops'] += [dict(rect(0,0,3/800*W,H),frame=True),dict(rect(787/800*W,0,13/800*W,H),frame=True),dict(rect(0,0,W,3),frame=True),dict(rect(0,H-3,W,3),frame=True)]
    fixes=[]
    for attempt in range(100):
        wall=collision(plan['ops']);labels,count=ndi.label(~wall)
        if count<=1:break
        sizes=np.bincount(labels.ravel());sizes[0]=0;main=int(sizes.argmax());other=[i for i in range(1,count+1) if i!=main]
        label=min(other,key=lambda i:sizes[i]);ys,xs=np.where(labels==label)
        if sizes[label]<12:
            for x,y in zip(xs,ys):plan['ops'].append(rect(x/800*W,y/480*H,W/800,H/480))
            fixes.append(dict(kind='fill_tiny_island',cells=int(sizes[label])))
        else:
            dist,nearest=ndi.distance_transform_edt(labels!=main,return_indices=True);k=int(np.argmin(dist[ys,xs]));y,x=int(ys[k]),int(xs[k]);ny,nx=int(nearest[0,y,x]),int(nearest[1,y,x])
            a=((x+.5)/800*W,(y+.5)/480*H);b=((nx+.5)/800*W,(ny+.5)/480*H)
            plan['ops'].append(line([a,b],42,True));fixes.append(dict(kind='open_portal',from_cell=[x,y],to_cell=[nx,ny],width=42))
    else:raise RuntimeError(f"Connectivity did not converge map{plan['id']}")
    plan['connectivity_adjustments']=fixes
    wall=collision(plan['ops']);assert ndi.label(~wall)[1]==1
    assert (~wall).sum()>70000
    for x,y in SPAWNS:assert not wall[479-y,x],(plan['id'],x,y)
    return plan,wall

def svg_shape(op):
    if op['kind']=='rect':
        x,y,r,b=op['box'];return f'<rect x="{x:.3f}" y="{y:.3f}" width="{r-x:.3f}" height="{b-y:.3f}" rx="{op.get("radius",0):.3f}"/>'
    if op['kind']=='oval':
        x,y,r,b=op['box'];return f'<ellipse cx="{(x+r)/2:.3f}" cy="{(y+b)/2:.3f}" rx="{(r-x)/2:.3f}" ry="{(b-y)/2:.3f}"/>'
    pts=' '.join(f'{x:.3f},{y:.3f}' for x,y in op['points'])
    if op['kind']=='poly':return f'<polygon points="{pts}"/>'
    return f'<polyline points="{pts}" fill="none" stroke="currentColor" stroke-width="{op["width"]:.3f}" stroke-linecap="round" stroke-linejoin="round"/>'

def save_svg(plan):
    floor,base,rim=PALETTES[plan['palette']]
    color=lambda c:'#'+''.join(f'{v:02x}' for v in c)
    ops=''.join('<g fill="{0}" color="{0}">{1}</g>'.format('black' if p.get('cut') else 'white',svg_shape(p)) for p in plan['ops'])
    svg=f'''<svg xmlns="http://www.w3.org/2000/svg" width="2200" height="1000" viewBox="0 0 2200 1000">
<metadata>GPL-3.0-or-later; original editable geometry for Liquid Wars; canonical JSON and material exporter accompany this SVG. No text/font asset.</metadata>
<defs><linearGradient id="stone" x2=".4" y2="1"><stop stop-color="{color(tuple(min(255,v+18) for v in base))}"/><stop offset="1" stop-color="{color(base)}"/></linearGradient><mask id="wall"><rect width="2200" height="1000" fill="black"/>{ops}</mask></defs>
<rect width="2200" height="1000" fill="{color(floor)}"/><rect width="2200" height="1000" fill="url(#stone)" mask="url(#wall)"/>
<rect x="16" y="16" width="2168" height="968" rx="28" fill="none" stroke="{color(rim)}" stroke-opacity=".4" stroke-width="3"/></svg>'''
    (SOURCE/'svg'/f'{plan["id"]:02d}.svg').write_text(svg,encoding='utf8')

def artwork(plan):
    coverage=raster(plan['ops'],SIZE,2).resize(SIZE,Image.Resampling.LANCZOS)
    alpha=np.asarray(coverage,dtype=np.float32)/255;wall=alpha>.5
    distance=ndi.distance_transform_edt(wall).astype('float32');gy,gx=np.gradient(distance)
    y,x=np.mgrid[0:SIZE[1],0:SIZE[0]].astype('float32');floor,base,rim=PALETTES[plan['palette']]
    vignette=np.clip(1-.17*((x/SIZE[0]-.5)**2+(y/SIZE[1]-.5)**2),.8,1)
    light=1.0+.12*np.exp(-((x-SIZE[0]*.3)**2+(y-SIZE[1]*.1)**2)/(SIZE[0]*.55)**2)
    rgb=np.empty((*alpha.shape,3),np.float32)
    grain=(np.sin(x*.034+y*.021)+np.sin(x*.011-y*.017))*.35
    for c in range(3):rgb[:,:,c]=floor[c]*vignette*light+grain
    # Quiet floor inlays are decorative only, consistently below obstruction contrast.
    lanes=(np.abs(np.sin((x+y*.44)/140))<.006).astype('float32')*.9
    rgb+=lanes[:,:,None]
    shadow=np.asarray(coverage.filter(ImageFilter.GaussianBlur(9)),np.float32)/255
    shadow=np.roll(shadow,(7,8),(0,1));rgb*=1-shadow[:,:,None]*.43
    material=np.empty_like(rgb)
    veins=np.exp(-((np.sin(x*.019+y*.013+1.4*np.sin(y*.014)))/.07)**2)*3
    bevel=np.clip(distance/7,0,1);facing=np.clip(.5+.5*(gx*.55+gy*.83),0,1)
    for c in range(3):material[:,:,c]=base[c]*(.86+.14*bevel)+np.exp(-distance/3.5)*facing*rim[c]*.18-veins
    if plan['palette']=='glass':
        labels,n=ndi.label(wall);tints=np.array([[0,0,0],[12,-6,-5],[-10,8,0],[-5,-5,12],[8,5,-10],[-8,3,9]],np.float32)
        material+=tints[labels%6]
    rgb=rgb*(1-alpha[:,:,None])+material*alpha[:,:,None]
    # Dark metal edge panels match the explicit HUD frame collision strips.
    for xx in [0,1,2,3]:rgb[:,xx,:]=np.array(FRAME['left'])
    panel=int(SIZE[0]*787/800);rgb[:,panel:,:]=np.array(FRAME['hud_panel'])
    image=Image.fromarray(np.clip(rgb,0,255).astype('uint8'),'RGB');draw=ImageDraw.Draw(image)
    sx=SIZE[0]/W;sy=SIZE[1]/H
    draw.rounded_rectangle((14*sx,14*sy,SIZE[0]-14*sx,SIZE[1]-14*sy),radius=25*sx,outline=tuple(FRAME['edge']),width=2)
    for yy in [23*sy,SIZE[1]-23*sy]:
        for xx in [36*sx,SIZE[0]-180*sx]:draw.line((xx,yy,xx+144*sx,yy),fill=tuple(int(v*.58) for v in rim),width=2)
    if plan['id']==34:
        colors=[tuple(int(floor[c]+(team[c]-floor[c])*.15) for c in range(3)) for team in TEAM_RGB]
        overlay=Image.new('RGB',SIZE);overlay.paste(image);od=ImageDraw.Draw(overlay)
        for (px,py),col in zip(SPAWNS,colors):
            cx=px/800*SIZE[0];cy=(479-py)/480*SIZE[1];od.ellipse((cx-95,cy-55,cx+95,cy+55),outline=col,width=4)
        image=overlay
    return image,coverage

def names(locale):
    xml=ET.parse(ROOT/f'app/src/main/res/{locale}/strings.xml');return [e.text for e in xml.find("string-array[@name='maps_array']")][1:]

def gallery(plans):
    assert FONT.exists(),'Commercial Noto Sans SC font required'
    font=ImageFont.truetype(str(FONT),21);small=ImageFont.truetype(str(FONT),17)
    cw,ch=440,245;page=Image.new('RGB',(cw*6,ch*8+70),(8,14,22));d=ImageDraw.Draw(page)
    d.text((18,10),PROFILE_NAME+' · 46图素材总览（非游戏截图）',font=font,fill=(190,202,211))
    for i,p in enumerate(plans):
        im=Image.open(RUNTIME/f'{i}-image.png').convert('RGB');im.thumbnail((cw-12,196));x=i%6*cw+6;y=i//6*ch+55;page.paste(im,(x,y));d.text((x+5,y+198),f'{i+1:02d}  {p["name_zh"]}',font=font,fill=(200,217,227))
    page.save(CHECK/'all-46-artwork.png')
    thumb=page.copy();thumb.thumbnail((1536,1536));thumb.save(CHECK/'all-46-artwork-preview.jpg',quality=85)

def main():
    global CHECK
    ap=argparse.ArgumentParser();ap.add_argument('--ids',nargs='*',type=int);ap.add_argument('--from-json',action='store_true',help='Rebuild edited resolved geometry from maps.json');ap.add_argument('--palette',default='art/source/comfort-1.48/palette.json');ap.add_argument('--check-dir',default='docs/game/comfort-1.48');args=ap.parse_args()
    CHECK=ROOT/args.check_dir;load_palette(ROOT/args.palette)
    for p in [SOURCE/'svg',SOURCE/'geometry',CHECK,RUNTIME]:p.mkdir(parents=True,exist_ok=True)
    en,zh=names('values'),names('values-zh');plans=[];inventory=[]
    input_plans=json.loads((SOURCE/'maps.json').read_text(encoding='utf8'))['maps'] if args.from_json else designs()
    for plan in input_plans:
        plan['name_en']=en[plan['id']];plan['name_zh']=zh[plan['id']]
        if args.ids is not None and plan['id'] not in args.ids:continue
        if args.from_json:
            wall=collision(plan['ops']);assert ndi.label(~wall)[1]==1 and (~wall).sum()>70000
            for x,y in SPAWNS:assert not wall[479-y,x]
        else:plan,wall=finalize(plan)
        plans.append(plan);i=plan['id'];save_svg(plan)
        image,coverage=artwork(plan);image.save(RUNTIME/f'{i}-image.png',optimize=True)
        mask=np.zeros((480,800,4),np.uint8);mask[wall]=255;Image.fromarray(mask,'RGBA').save(RUNTIME/f'{i}-map.png',optimize=True)
        coverage.save(SOURCE/'geometry'/f'{i:02d}.png',optimize=True)
        inventory.append(dict(id=i,name_zh=plan['name_zh'],palette=plan['palette'],free_cells=int((~wall).sum()),free_fraction=float((~wall).mean()),floor_components=int(ndi.label(~wall)[1]),connectivity_adjustments=len(plan['connectivity_adjustments']),image_sha256=hashlib.sha256((RUNTIME/f'{i}-image.png').read_bytes()).hexdigest(),mask_sha256=hashlib.sha256((RUNTIME/f'{i}-map.png').read_bytes()).hexdigest()))
        print(f'{i:02d} {plan["name_zh"]}: free={(~wall).mean():.1%}, portals={len(plan["connectivity_adjustments"])}',flush=True)
    suffix='' if args.ids is None else '-sample'
    (SOURCE/f'maps{suffix}.json').write_text(json.dumps(dict(license='GPL-3.0-or-later',canonical_size=[W,H],runtime_image_size=list(SIZE),rule_mask_size=[800,480],maps=plans),ensure_ascii=False,indent=2),encoding='utf8')
    (CHECK/f'inventory{suffix}.json').write_text(json.dumps(inventory,ensure_ascii=False,indent=2),encoding='utf8')
    if args.ids is not None and (SOURCE/'maps.json').exists():
        full=json.loads((SOURCE/'maps.json').read_text(encoding='utf8'));updates={p['id']:p for p in plans};full['maps']=[updates.get(p['id'],p) for p in full['maps']];(SOURCE/'maps.json').write_text(json.dumps(full,ensure_ascii=False,indent=2),encoding='utf8');plans=full['maps']
        full_inventory=json.loads((CHECK/'inventory.json').read_text(encoding='utf8'));updates={p['id']:p for p in inventory};full_inventory=[updates.get(p['id'],p) for p in full_inventory];(CHECK/'inventory.json').write_text(json.dumps(full_inventory,ensure_ascii=False,indent=2),encoding='utf8')
    if len(plans)==46:
        gallery(plans)
        auxiliary=dict(id=-1,palette='slate',ops=[line([(980,360),(980,315),(1040,270),(1140,270),(1210,320),(1210,395),(1180,445),(1110,485),(1110,545)],70),oval(1110,650,42)])
        save_svg(auxiliary);image,_=artwork(auxiliary);image.save(RUNTIME/'random-map.png',optimize=True)
        (SOURCE/'random-preview.json').write_text(json.dumps(auxiliary,indent=2),encoding='utf8')
if __name__=='__main__':main()
