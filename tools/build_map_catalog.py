"""Generate stable Java/native map IDs and display lists; no art or rule generation."""
from pathlib import Path
import json,re,html
R=Path(__file__).resolve().parents[1]
def main():
    data=json.loads((R/'art/source/catalog-1.72.json').read_text('utf8'));maps=data['maps'];ids=[m['id'] for m in maps];n=len(ids)
    assert n==59 and ids==list(range(1,60)) and data['large_ids']==list(range(50,60)) and data['retired_ids']==[0]
    numbers=','.join(map(str,ids))
    java=f'''// SPDX-License-Identifier: GPL-3.0-or-later
// Generated from art/source/catalog-1.72.json. IDs are NOT display positions.
package com.xenris.liquidwarsos;
public final class MapCatalog {{
    private MapCatalog() {{}}
    public static final int COUNT={n};
    public static final int LARGE_COUNT=10;
    public static int largeIdAt(int position) {{if(position<0||position>=LARGE_COUNT)throw new IllegalArgumentException("Large map selection");return 50+position;}}
    private static final int[] IDS={{{numbers}}};
    public static int width(int id) {{return id>=50?1600:800;}}
    public static int height(int id) {{return id>=50?960:480;}}
    public static boolean validSelection(int position) {{return position>=0&&position<=COUNT;}}
    public static int mapIdForSelection(int position) {{
        if(!validSelection(position))throw new IllegalArgumentException("Map selection");
        return position==0?-1:IDS[position-1];
    }}
    public static int selectionForMapId(int id) {{for(int i=0;i<COUNT;i++)if(IDS[i]==id)return i+1;return 0;}}
    public static boolean validMapChoice(int id) {{return id==-1||selectionForMapId(id)>0;}}
}}
'''
    native=f'''// SPDX-License-Identifier: GPL-3.0-or-later
// Generated from art/source/catalog-1.72.json. Never renumber surviving seed identities.
#pragma once
namespace map_catalog {{
static constexpr int COUNT={n};
static constexpr int IDS[COUNT]={{{numbers}}};
inline int width(int id) {{return id>=50?1600:800;}}
inline int height(int id) {{return id>=50?960:480;}}
inline int idAt(unsigned index) {{return IDS[index%COUNT];}}
inline int resolveExplicit(int id) {{for(int active:IDS)if(id==active)return id;return IDS[0];}}
}}
'''
    (R/'app/src/main/java/com/xenris/liquidwarsos/MapCatalog.java').write_text(java,encoding='utf8')
    (R/'app/src/main/cpp/application/map_catalog.hpp').write_text(native,encoding='utf8')
    p=R/'app/src/main/cpp/application/info.hpp';p.write_text(re.sub(r'#define NUMBER_OF_MAPS \d+',f'#define NUMBER_OF_MAPS {n}',p.read_text('utf8')),encoding='utf8')
    for loc in ['values','values-zh','values-de','values-fr','values-it']:
        p=R/f'app/src/main/res/{loc}/strings.xml';text=p.read_text('utf8')
        old=re.search(r'<string-array name="maps_array">(.*?)</string-array>',text,re.S);assert old
        random_name=re.search(r'<item>(.*?)</item>',old.group(1),re.S).group(1)
        names=[random_name]+[html.escape(f'{i+1:02} · '+m['name_zh' if loc=='values-zh' else 'name_en']) for i,m in enumerate(maps)]
        array='<string-array name="maps_array">\n'+''.join(f'        <item>{name}</item>\n' for name in names)+'    </string-array>'
        text=text[:old.start()]+array+text[old.end():];p.write_text(text,encoding='utf8')
    for loc in ['values','values-zh']:
        p=R/f'app/src/main/res/{loc}/ui_strings.xml';text=p.read_text('utf8')
        text=re.sub(r'(<string name="ui_maps_hint">)\d+',lambda m:m.group(1)+str(n),text)
        text=re.sub(r'\s*<string name="ruins_menu">.*?</string>','',text)
        p.write_text(text,encoding='utf8')
    print('Generated59 stable IDs, five map lists and native/Java catalog')
if __name__=='__main__':main()
