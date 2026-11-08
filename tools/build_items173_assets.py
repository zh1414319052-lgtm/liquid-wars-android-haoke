from pathlib import Path
import json,html
from fontTools import subset
from fontTools.ttLib import TTFont
R=Path(__file__).resolve().parents[1];O=R/'art/source/items-1.73';rows=json.loads((O/'catalog.json').read_text('utf8'))
for r in rows:
 p=r['path'];c=r['color'];i=r['id']
 (O/f'{i:02}.svg').write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48"><path d="{p}" stroke="{c}" stroke-width="2.6" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>',encoding='utf8')
 (R/f'app/src/main/res/drawable/item_{i:02}.xml').write_text(f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="48dp" android:height="48dp" android:viewportWidth="48" android:viewportHeight="48"><path android:pathData="{p}" android:fillColor="#00000000" android:strokeColor="{c}" android:strokeWidth="2.6" android:strokeLineCap="round" android:strokeLineJoin="round"/></vector>',encoding='utf8')
for folder,lang in [('values','en'),('values-zh','zh')]:
 text=['<?xml version="1.0" encoding="utf-8"?>','<resources>']
 for field,key in [('name','item_names'),('description','item_descriptions')]:
  text.append(f'<string-array name="{key}">')
  for r in rows:text.append('<item>'+html.escape(r[field+'_'+lang]).replace("'",r"\'")+'</item>')
  text.append('</string-array>')
 text.append('</resources>');(R/f'app/src/main/res/{folder}/items.xml').write_text('\n'.join(text),encoding='utf8')
old=TTFont(R/'app/src/main/assets/fonts/NotoSansSC-Intro.ttf');chars=set(old.getBestCmap());old.close();chars.update(map(ord,json.dumps(rows,ensure_ascii=False)))
for f in ['ItemOverlay','BattleEffects']:chars.update(map(ord,(R/f'app/src/main/java/com/xenris/liquidwarsos/{f}.java').read_text('utf8')))
font=TTFont(R/'art/source/free-maps-1.60/fonts/NotoSansSC-VF.ttf');ss=subset.Subsetter();ss.populate(unicodes=chars);ss.subset(font);font.save(R/'app/src/main/assets/fonts/NotoSansSC-Intro.ttf')
print('20paired item vectors/resources + Noto Sans SC/OFL1.1 exported')
