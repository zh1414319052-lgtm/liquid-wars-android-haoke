"""Editable extension of the approved dusk UI. Noto Sans SC / SIL OFL 1.1."""
from pathlib import Path
import json,copy
R=Path(__file__).resolve().parents[1];O=R/'docs/game/model-ui-1.56'
tokens=json.loads((R/'docs/game/ui-1.52/design.haoke-ui.json').read_text('utf8'))['tokens']
colors=['#79AE8B','#7DA3C8','#C7867E','#72B6B6','#C7AE70','#B18FB9']
names=['绿队','蓝队','红队','青队','黄队','洋红队']
def rect(i,x,y,w,h,fill='$surface',radius=12):return dict(type='rect',id=i,x=x,y=y,width=w,height=h,fill=fill,radius=radius)
def text(i,x,y,w,h,s,size=16,fill='$ink',weight=400):return dict(type='text',id=i,x=x,y=y,width=w,height=h,text=s,fontFamily='Noto Sans SC',fontSize=size,fontWeight=weight,fill=fill,lineHeight=1.25)
def button(i,x,y,w,s,primary=False):
 inset,size=(8,13) if i=='back' else (16,16)
 return dict(type='group',id=i,x=x,y=y,width=w,height=48,children=[rect(i+'-bg',0,0,w,48,'$accent' if primary else '$field',10),text(i+'-text',inset,13,w-inset*2,24,s,size,'$onAccent' if primary else '$ink',600)])
def board(i,name):
 c=[text('brand',24,18,260,18,'LIQUID WARS / 对战设置',11,'$muted',600),text('title',24,40,224,40,'AI 指挥室',28,'$ink',700),text('subtitle',264,48,360,24,'选服务商，填密钥，其余自动准备。',14,'$muted'),button('tab-ai',622,26,110,'队伍 AI',True),button('tab-pairs',744,26,122,'组队规则'),button('back',878,26,58,'返回')]
 c.append(rect('divider',24,94,912,1,'$line',0))
 return dict(id=i,name=name,width=960,height=480,unit='dp',background='$bg',children=c)
b=board('setup','队伍 AI · 默认设置');c=b['children']
c.append(text('roster-label',24,108,180,20,'选择队伍',13,'$muted'))
for p in range(6):
 y=138+p*48
 c.extend([rect('team-bg-'+str(p),24,y,204,44,'$surface' if p else '$field',10),rect('team-dot-'+str(p),38,y+15,12,12,colors[p],6),text('team-name-'+str(p),60,y+10,85,24,str(p+1)+'  '+names[p],16,'$ink',600),text('team-local-'+str(p),150,y+12,70,20,'编辑中' if p==0 else '本地 AI',12,'$accent' if p==0 else '$muted')])
c.extend([text('roster-note',24,440,210,22,'真人队伍由玩家操控',12,'$muted'),rect('content',248,112,688,300,'$surface',16),text('team-title',272,130,350,28,'1  绿队',22,'$ink',600),button('enabled',756,122,156,'使用模型  ●',True),text('service-label',272,176,290,20,'服务商',13,'$muted'),text('model-label',600,176,300,20,'模型 · 可切换',13,'$muted'),button('provider',272,200,304,'DeepSeek'),button('model',600,200,312,'DeepSeek Flash'),text('key-label',272,264,500,20,'API Key',13,'$muted'),rect('key-field',272,288,500,48,'$field',10),text('key-placeholder',288,302,420,22,'粘贴你的 API Key',15,'$muted'),button('paste',788,288,124,'粘贴密钥'),text('automatic',272,356,350,22,'地址与接口已自动配置',13,'$accent'),button('advanced',784,348,128,'高级设置'),text('privacy',264,428,366,36,'密钥仅保存在本机 · 对战调用按服务商计费',12,'$muted'),button('fetch',640,424,140,'获取模型'),button('save',792,424,144,'保存此队',True)])
saved=copy.deepcopy(b);saved.update(id='saved',name='队伍 AI · 保存反馈')
for n in saved['children']:
 if n['id']=='key-placeholder':n['text']='••••••••••••••••••••••••'
 if n['id']=='automatic':n['text']='已保存 · 开局后使用此模型'
 if n['id']=='team-local-0':n['text']='模型 AI'
a=board('advanced','高级设置 · 自定义兼容接口');a['children']+= [rect('advanced-panel',176,116,608,288,'$surface',16),text('advanced-title',200,132,520,30,'高级设置',22,'$ink',600),text('advanced-note',200,168,550,22,'官方服务通常无需修改；自定义网关在这里配置。',14,'$muted'),text('url-label',200,210,550,20,'服务地址（HTTPS）',13,'$muted'),button('url',200,236,560,'https://api.deepseek.com'),text('format-label',200,298,260,20,'接口格式',13,'$muted'),text('manual-label',500,298,260,20,'自定义模型 ID',13,'$muted'),button('format',200,326,276,'Chat Completions'),button('manual-model',500,326,260,'deepseek-flash'),button('advanced-back',640,424,144,'返回设置',True)]
p=board('pairs','组队规则');p['children'] += [text('pairs-title',48,128,500,36,'一起作战，共同获胜',26,'$ink',600),text('pairs-note',48,176,850,26,'两支队伍成为盟友，接触不会扣血或吞噬；按联盟合计判胜。',16,'$muted'),button('mode-free',48,224,180,'自由混战'),button('mode-paired',244,224,180,'两两组队',True),text('pairing-label',48,296,800,22,'联盟配对 · 15 种方案可选',13,'$muted'),button('pairing',48,328,864,'1 绿队 + 2 蓝队         /         3 红队 + 4 青队         /         5 黄队 + 6 洋红队'),button('pairs-save',736,424,176,'保存组队规则',True)]
for n in p['children']:
 if n['id']=='tab-ai':n['children'][0]['fill']='$field';n['children'][1]['fill']='$ink'
 if n['id']=='tab-pairs':n['children'][0]['fill']='$accent';n['children'][1]['fill']='$onAccent'
boards=[b,saved,a,p]
states=[dict(id=q['id'],name=q['name'],board=q['id']) for q in boards]
edges=[]
def edge(fr,to,node):edges.append(dict(id=fr+'-'+to+'-'+node,**{'from':fr,'to':to},trigger='click',node=node,duration=100,easing='outCubic'))
edge('setup','advanced','advanced');edge('advanced','setup','advanced-back');edge('setup','saved','save');edge('saved','setup','provider');edge('setup','pairs','tab-pairs');edge('saved','pairs','tab-pairs');edge('pairs','setup','tab-ai');edge('pairs','setup','pairs-save')
doc=dict(format='haoke-ui-design',version=1,id='liquidwars-model-ui-156',name='液态战争 · AI 指挥室',revision=0,tokens=tokens,components={},boards=boards,motion=dict(version=1,initial='setup',states=states,transitions=edges))
target=O/'design.haoke-ui.json'
assert not target.exists(),'Existing registered design must be edited with the workbench API'
target.write_text(json.dumps(doc,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(target)
