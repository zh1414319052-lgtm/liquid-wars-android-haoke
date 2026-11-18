"""Create a NEW editable menu/notes baseline; never overwrite a registered design."""
from pathlib import Path
import json, math, shutil

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/game/menu-2.12/design'
OUT.mkdir(parents=True,exist_ok=True)
target=OUT/'menu.haoke-ui.json'
if target.exists():
    raise SystemExit('Existing design: edit through workbench session instead.')
ref=Path(r'C:\Users\Administrator\.codex\generated_images\01a1d6f4-6ae5-78e1-8957-863f06d1e5c9\exec-0d40d1e4-3d6f-48c5-94b5-4783f8e294c8.png')
shutil.copyfile(ref, OUT/'direction.png')
tokens={'bg':'#10262C','panel':'#1A363E','line':'#315158','ink':'#F5F4E9','muted':'#B6CAC5','accent':'#A2DDBF','motion.dialog':160}
nodes=[]
def rect(i,x,y,w,h,fill,r=0,stroke=None):
    n=dict(id=i,type='rect',x=x,y=y,width=w,height=h,fill=fill,radius=r)
    if stroke:n.update(stroke=stroke,strokeWidth=1)
    nodes.append(n)
def txt(i,text,x,y,w,h,size=18,fill='$ink',weight=400):
    nodes.append(dict(id=i,type='text',x=x,y=y,width=w,height=h,text=text,fontFamily='Noto Sans SC',fontSize=size,fontWeight=weight,fill=fill,verticalAlign='center'))
txt('brand','L I Q U I D   W A R S',32,22,360,25,14,weight=600)
txt('title','液态战争',32,57,380,72,54,weight=600)
txt('edition','重制版',34,132,360,30,22)
rect('accent-rule',34,178,46,4,'$accent',2)
txt('tagline','指尖引流，包围对手。',34,193,365,36,22)
for team,col in enumerate(['#A2DDBF','#E38278','#E5CB82']):
    for j in range(190):
        t=(j%95)/94; lane=j//95
        x=16+390*t
        y=250+team*24+53*math.sin(t*4.4+team*2.1)+(math.sin(j*7.1)*9)+(lane-.5)*10
        nodes.append(dict(id=f'particle-{team}-{j}',type='ellipse',x=x,y=y,width=1.9,height=1.9,fill=col))
txt('mode-heading','选择模式',444,22,520,26,22,weight=600)
cards=[('single','单人对战','经典粒子战场',444,68,True),('garden','3D模式','仍在优化中',710,68,False),('large','大地图模式','更大的战场',444,180,False),('spectate','观战 AI 对战','观察电脑交锋',710,180,False)]
for i,title,desc,x,y,primary in cards:
    rect(i,x,y,250,100,'$accent' if primary else '$panel',12,'$line' if not primary else None)
    ink='$bg' if primary else '$ink'
    # Same editable line icons as the native view; no flattened screenshot controls.
    paths={'single':'M 4 3 L 19 18 M 20 3 L 5 18 M 2 15 L 9 22 M 22 15 L 15 22',
      'garden':'M 3 6 L 12 1 L 21 6 L 21 18 L 12 23 L 3 18 Z M 3 6 L 12 11 L 21 6 M 12 11 L 12 23',
      'large':'M 2 5 L 8 2 L 16 5 L 22 2 L 22 20 L 16 23 L 8 20 L 2 23 Z M 8 2 L 8 20 M 16 5 L 16 23',
      'spectate':'M 2 3 L 22 3 L 22 18 L 2 18 Z M 12 18 L 12 22 M 7 22 L 17 22 M 9 7 L 16 11 L 9 15 Z'}
    nodes.append(dict(id=i+'-icon',type='path',x=x+16,y=y+29,width=40,height=40,d=paths[i],fill='transparent',stroke=ink,strokeWidth=1.5))
    rect(i+'-divider',x+70,y+22,1,56,'$line')
    txt(i+'-title',title,x+84,y+22,160,30,21,ink,600)
    txt(i+'-desc',desc,x+84,y+56,158,25,15,ink)
for i,title,x in [('wifi','Wi-Fi 联机',444),('bluetooth','蓝牙联机',710)]:
    rect(i,x,300,250,50,'$bg',10,'$accent');txt(i+'-text',title,x+66,300,170,50,20)
rect('footer-rule',32,382,928,1,'$line')
txt('version','本地游玩 · 2.12',32,397,340,40,15,'$muted')
for i,title,x in [('notes-open','更新说明',594),('help','玩法说明',720),('support','支持游戏',846)]:
    rect(i,x,397,114,40,'$bg',10,'$line');txt(i+'-text',title,x+17,397,95,40,17)
home=dict(id='home',name='主菜单',width=992,height=455,unit='dp',scale=1,background='$bg',code='MainMenuView',children=nodes)
nodes=[]
rect('notes-panel',146,24,700,407,'$panel',16,'$line')
txt('notes-title','本次更新 · 2.12',170,42,540,34,24,weight=600)
txt('notes-subtitle','新版本首次启动时显示，可随时关闭',170,81,560,24,14,'$muted')
rect('notes-close',756,40,64,48,'$bg',8);txt('close-label','关闭',766,40,54,48,16)
txt('changes-heading','本次调整',170,120,560,28,19,'$accent',600)
txt('changes-body','主菜单重新排版；新增支持游戏入口；取消粒子互相吞噬音效。',170,155,640,46,16)
txt('tips-heading','道具使用技巧',170,214,580,28,19,'$accent',600)
txt('tips-body','冻结：一键冻结全部敌方 10 秒。\n净化：被冻住也能使用，立即解冻并解除黑洞吸引。\n黑洞：地图中心持续 10 秒，也会吸引自己；障碍仍会阻挡。',170,250,630,98,16)
txt('inventory','主动道具每种最多 10 个，每次消耗 1 个。',170,370,630,24,15,'$muted')
notes=dict(id='notes',name='首次启动说明',width=992,height=455,unit='dp',scale=1,background='$bg',code='ReleaseNotesHost',children=nodes)
doc=dict(format='haoke-ui-design',version=1,id='liquidwars-menu212',name='液态战争主菜单与本地更新说明',revision=1,tokens=tokens,components={},boards=[home,notes],motion={'version':1,'initial':'home-state','states':[{'id':'home-state','name':'主菜单','board':'home','overrides':{}},{'id':'notes-state','name':'更新说明','board':'notes','overrides':{}}],'transitions':[{'id':'open-notes','from':'home-state','to':'notes-state','trigger':'click','node':'notes-open','duration':160,'easing':'outCubic'},{'id':'close-notes','from':'notes-state','to':'home-state','trigger':'click','node':'notes-close','duration':160,'easing':'outCubic'},{'id':'back-notes','from':'notes-state','to':'home-state','trigger':'back','duration':160,'easing':'outCubic'}]})
target.write_text(json.dumps(doc,ensure_ascii=False,indent=2),encoding='utf-8')
print(target)
