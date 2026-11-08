"""Document-only rendering of original vector icons; never a game screenshot."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import fitz,json,re
R=Path(__file__).resolve().parents[1];data=json.loads((R/'art/source/items-1.66/catalog.json').read_text('utf8'))
fontfile=str(R/'art/source/free-maps-1.60/fonts/NotoSansSC-VF.ttf')
font=ImageFont.truetype(fontfile,22);font.set_variation_by_axes([500]);small=ImageFont.truetype(fontfile,15);small.set_variation_by_axes([450])
im=Image.new('RGB',(1500,1400),'#121c25');d=ImageDraw.Draw(im);d.text((24,14),'18种道具 · 图标与规则展示（不是游戏截图）',font=font,fill='#e9e5d7')
for i,item in enumerate(data):
 x=(i%3)*500+12;y=(i//3)*220+58;d.rounded_rectangle((x,y,x+476,y+206),12,fill='#22313e')
 svg=(R/f'art/source/items-1.66/{i:02}.svg').read_bytes();doc=fitz.open('svg',svg);pdf=fitz.open('pdf',doc.convert_to_pdf());pix=pdf[0].get_pixmap(matrix=fitz.Matrix(1.5,1.5),alpha=True);icon=Image.frombytes('RGBA',(pix.width,pix.height),pix.samples);im.paste(icon,(x+10,y+6),icon)
 d.text((x+81,y+16),f'{i+1:02} {item["name_zh"]}',font=font,fill=item['color']);d.text((x+82,y+46),item['name_en'],font=small,fill='#cbd4d6')
 tokens=re.findall(r'[0-9]+(?:\.[0-9]+)?%?|.',item['description_zh']);lines=[];line=''
 for token in tokens:
  if d.textlength(line+token,font=small)>448 and token not in '。，；、）':lines.append(line);line=token
  else:line+=token
 if line:lines.append(line)
 for k,line in enumerate(lines):d.text((x+14,y+85+k*22),line,font=small,fill='#e2e6df')
im.save(R/'docs/game/items-1.66/icon-catalog.png')
print('18icon catalog; Noto Sans SC(SIL OFL1.1); no game capture claims')
