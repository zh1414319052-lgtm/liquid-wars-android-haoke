"""Noto Sans SC (SIL OFL 1.1), commercial use allowed; preserve old glyph coverage."""
from pathlib import Path
from fontTools import subset
from fontTools.ttLib import TTFont
R=Path(__file__).resolve().parents[1]
source=R/'art/source/free-maps-1.60/fonts/NotoSansSC-VF.ttf'
target=R/'app/src/main/assets/fonts/NotoSansSC-Intro.ttf'
chars=set(TTFont(target).getBestCmap())
for name in ['TacticalOverlay','OperationMode']:
 chars.update(map(ord,(R/f'app/src/main/java/com/xenris/liquidwarsos/{name}.java').read_text('utf8')))
font=TTFont(source);options=subset.Options();options.layout_features=['*'];options.name_IDs=['*'];options.name_legacy=True;options.name_languages=['*']
worker=subset.Subsetter(options);worker.populate(unicodes=chars);worker.subset(font);font.save(target)
available=set(TTFont(target).getBestCmap());missing={c for c in chars if 0x4e00<=c<=0x9fff and c not in available};assert not missing
print('Noto Sans SC/OFL: verified',len(available),'glyphs;',target.stat().st_size,'bytes')
