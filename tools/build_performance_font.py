"""Extend existing Noto Sans SC subset for the high-refresh controls. SIL OFL 1.1."""
from pathlib import Path
from fontTools import subset
from fontTools.ttLib import TTFont
R=Path(__file__).resolve().parents[1];target=R/'app/src/main/assets/fonts/NotoSansSC-Intro.ttf'
old=TTFont(target);chars=set(old.getBestCmap());old.close()
chars.update(map(ord,(R/'app/src/main/java/com/xenris/liquidwarsos/ItemOverlay.java').read_text('utf8')))
font=TTFont(R/'art/source/free-maps-1.60/fonts/NotoSansSC-VF.ttf');s=subset.Subsetter(options=subset.Options());s.populate(unicodes=chars);s.subset(font);font.save(target)
print('Extended Noto Sans SC subset; SIL OFL 1.1; original source preserved')
