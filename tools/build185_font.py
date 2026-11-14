"""Static Noto Sans SC 400/600; SIL OFL 1.1, commercial use allowed."""
from pathlib import Path
from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
root=Path(__file__).resolve().parents[1]
chars=set(range(32,127))
for path in list((root/'app/src/main/java').rglob('*.java'))+list((root/'app/src/main/res').glob('values*/*.xml')):
    chars.update(map(ord,path.read_text('utf-8')))
for weight in (400,600):
    font=TTFont(root/'art/source/free-maps-1.60/fonts/NotoSansSC-VF.ttf')
    instantiateVariableFont(font,{'wght':weight},inplace=True)
    options=subset.Options();options.layout_features=['*'];options.name_IDs=['*']
    worker=subset.Subsetter(options);worker.populate(unicodes=chars);worker.subset(font)
    target=root/f'app/src/main/assets/fonts/NotoSansSC-Lobby{weight}.ttf'
    font.save(target)
    assert 'fvar' not in TTFont(target)
    print(target.name,target.stat().st_size)
