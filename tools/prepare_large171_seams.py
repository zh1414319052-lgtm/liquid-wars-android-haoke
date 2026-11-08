from pathlib import Path
from PIL import Image
R=Path(__file__).resolve().parents[1];S=R/'art/source/world-1.71';D=R/'work/compressed-images/large171';D.mkdir(parents=True,exist_ok=True)
(S/'seam-inputs').mkdir(exist_ok=True);(S/'seam-paints').mkdir(exist_ok=True)
im=Image.open(S/'composites/52-hard-join.png');tw=im.width//4;th=im.height//2
im.resize((1536,461),Image.Resampling.LANCZOS).save(R/'docs/game/world-1.71/map52-v1-rejected.jpg',quality=88)
for k in range(4):
 crop=im.crop((k*tw,th-th//2,(k+1)*tw,th-th//2+th));crop.save(S/f'seam-inputs/{k}.png')
 crop.thumbnail((1024,1024));crop.save(D/f'seam-{k}.jpg',quality=82)
