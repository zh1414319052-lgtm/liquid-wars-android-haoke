from pathlib import Path
from PIL import Image
R=Path(__file__).resolve().parents[1];S=R/'art/source/world-1.71';D=R/'work/compressed-images/large171';D.mkdir(parents=True,exist_ok=True)
(S/'vertical-inputs').mkdir(exist_ok=True);(S/'vertical-paints').mkdir(exist_ok=True)
im=Image.open(S/'composites/52.png');tw=im.width//4;th=im.height//2;sheet=Image.new('RGB',(1024*3,614*2))
for row in range(2):
 for seam in range(1,4):
  k=row*3+seam-1;x=seam*tw-tw//2;crop=im.crop((x,row*th,x+tw,(row+1)*th));crop.save(S/f'vertical-inputs/{k}.png');crop.thumbnail((1024,614));sheet.paste(crop,((seam-1)*1024,row*614))
sheet.thumbnail((1536,1536));sheet.save(D/'vertical-input-contact.jpg',quality=84)
