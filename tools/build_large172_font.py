from pathlib import Path
from fontTools import subset
from fontTools.ttLib import TTFont
R=Path(__file__).resolve().parents[1];p=R/'app/src/main/assets/fonts/NotoSansSC-Intro.ttf';old=TTFont(p);chars=set(old.getBestCmap());old.close()
for name in ['app/src/main/java/com/xenris/liquidwarsos/ItemOverlay.java','art/source/catalog-1.72.json','app/src/main/res/values-zh/ui_strings.xml']:chars.update(map(ord,(R/name).read_text('utf8')))
font=TTFont(R/'art/source/free-maps-1.60/fonts/NotoSansSC-VF.ttf');s=subset.Subsetter(options=subset.Options());s.populate(unicodes=chars);s.subset(font);font.save(p)
print('Noto Sans SC subset extended, SIL OFL 1.1; original retained')
