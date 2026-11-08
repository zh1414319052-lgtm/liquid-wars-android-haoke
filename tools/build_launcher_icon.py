"""Convert the archived AI icon to Android density/mask assets; no creative image edits.
Source artwork remains intact. Only alpha bounds, uniform scale, padding and density export.
"""
from pathlib import Path
from PIL import Image
import hashlib,json

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'art/source/icon-1.53/launcher-foreground-original.png'
RES=ROOT/'app/src/main/res'
BG=(32,38,42,255)

def write(path,s):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(s+'\n',encoding='utf8')
def icon_on_canvas(emblem,size,extent):
    image=emblem.copy();image.thumbnail((round(size*extent),round(size*extent)),Image.Resampling.LANCZOS)
    canvas=Image.new('RGBA',(size,size),(0,0,0,0));canvas.alpha_composite(image,((size-image.width)//2,(size-image.height)//2));return canvas
def main():
    source=Image.open(SRC).convert('RGBA');alpha=source.getchannel('A');bounds=alpha.point(lambda a:255 if a>8 else 0).getbbox()
    assert bounds and alpha.getextrema()==(0,255),'Need real transparent source'
    emblem=source.crop(bounds)
    # Adaptive foreground is 108dp; the entire mark fits a central diameter of 66dp.
    # Uniform fit into 58dp square also protects diagonal droplets in circular masks.
    for density,scale in [('mdpi',1),('hdpi',1.5),('xhdpi',2),('xxhdpi',3),('xxxhdpi',4)]:
        folder=RES/('mipmap-'+density);folder.mkdir(parents=True,exist_ok=True)
        size=round(48*scale);image=Image.new('RGBA',(size,size),BG)
        image.alpha_composite(icon_on_canvas(emblem,size,.76))
        image.save(folder/'ic_launcher.png');image.save(folder/'ic_launcher_round.png')
    drawable=RES/'drawable-nodpi';drawable.mkdir(parents=True,exist_ok=True)
    foreground=icon_on_canvas(emblem,432,58/108);foreground.save(drawable/'launcher_foreground.png')
    monochrome=Image.new('RGBA',foreground.size,(255,255,255,0));monochrome.putalpha(foreground.getchannel('A'));monochrome.save(drawable/'launcher_monochrome.png')
    for qualified in ['mipmap-anydpi-v26','mipmap-anydpi-v33']:
        extra='<monochrome android:drawable="@drawable/launcher_monochrome"/>' if qualified.endswith('v33') else ''
        xml='<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/ui_bg"/><foreground android:drawable="@drawable/launcher_foreground"/>'+extra+'</adaptive-icon>'
        for name in ['ic_launcher','ic_launcher_round']:write(RES/qualified/(name+'.xml'),xml)
    # Store/preview image, outside APK; no title or other font is rendered.
    preview=Image.new('RGBA',(512,512),BG);preview.alpha_composite(icon_on_canvas(emblem,512,.76));preview.save(SRC.parent/'launcher-preview.png')
    manifest={'source':SRC.name,'source_sha256':hashlib.sha256(SRC.read_bytes()).hexdigest(),'origin':'OpenAI built-in image generation, 2026-11-06; no third-party stock artwork','font':'No text/font in icon','processing':'Uniform density resizing and Android adaptive mask padding only; original retained','source_size':source.size,'source_alpha_bounds':bounds,'background':'#20262A','adaptive_foreground_dp':108,'emblem_max_extent_dp':58,'densities':['mdpi','hdpi','xhdpi','xxhdpi','xxxhdpi'],'license':'AI-generated output; original game copyright and GPL-3.0-or-later retained. No additional third-party asset imported.'}
    write(SRC.parent/'SOURCE.json',json.dumps(manifest,ensure_ascii=False,indent=2))
    print('Generated legacy, adaptive, round and monochrome launcher resources.')
if __name__=='__main__':main()
