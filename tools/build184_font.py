"""Noto Sans SC, SIL OFL 1.1: preserve existing glyphs and cover native online UI."""
from pathlib import Path
from fontTools import subset
from fontTools.ttLib import TTFont
root=Path(__file__).resolve().parents[1]
target=root/'app/src/main/assets/fonts/NotoSansSC-Intro.ttf'
chars=set(TTFont(target).getBestCmap())
for path in list((root/'app/src/main/java').rglob('*.java'))+list((root/'app/src/main/res').glob('values*/*.xml')):
    chars.update(map(ord,path.read_text('utf-8')))
font=TTFont(root/'art/source/free-maps-1.60/fonts/NotoSansSC-VF.ttf')
options=subset.Options();options.layout_features=['*'];options.name_IDs=['*'];options.name_legacy=True;options.name_languages=['*']
worker=subset.Subsetter(options);worker.populate(unicodes=chars);worker.subset(font);font.save(target)
available=set(TTFont(target).getBestCmap())
assert not {c for c in chars if 0x4e00<=c<=0x9fff and c not in available}
print('Noto Sans SC/OFL glyphs:',len(available))
