"""Original editable geometry for46 wide-screen Liquid Wars arenas. GPL-3.0-or-later."""
import math,random
W,H=2200,1000
def rect(x,y,w,h,r=0,cut=False):return dict(kind='rect',box=[x,y,x+w,y+h],radius=r,cut=cut)
def oval(x,y,rx,ry=None,cut=False,angle=0):
    ry=rx if ry is None else ry
    if angle:
        a=math.radians(angle)
        return poly([(x+rx*math.cos(t)*math.cos(a)-ry*math.sin(t)*math.sin(a),y+rx*math.cos(t)*math.sin(a)+ry*math.sin(t)*math.cos(a)) for t in [i*math.tau/80 for i in range(80)]],cut)
    return dict(kind='oval',box=[x-rx,y-ry,x+rx,y+ry],cut=cut)
def poly(points,cut=False):return dict(kind='poly',points=[list(p) for p in points],cut=cut)
def line(points,width=36,cut=False):return dict(kind='line',points=[list(p) for p in points],width=width,cut=cut)
def arc(x,y,rx,ry,start=25,end=335,width=36,cut=False):
    return line([(x+rx*math.cos(math.radians(a)),y+ry*math.sin(math.radians(a))) for a in [start+(end-start)*i/80 for i in range(81)]],width,cut)
def star(x,y,r,inside=None,n=5,angle=-90):
    inside=r*.43 if inside is None else inside
    return poly([(x+(r if i%2==0 else inside)*math.cos(math.radians(angle)+i*math.pi/n),y+(r if i%2==0 else inside)*math.sin(math.radians(angle)+i*math.pi/n)) for i in range(n*2)])
def pine(x,y,w,h):
    return [poly([(x,y-h*.5),(x+w*.36,y-h*.12),(x+w*.20,y-h*.12),(x+w*.48,y+h*.19),(x+w*.29,y+h*.19),(x+w*.56,y+h*.40),(x-w*.56,y+h*.40),(x-w*.29,y+h*.19),(x-w*.48,y-h*.0),(x-w*.20,y-h*.0),(x-w*.36,y-h*.12)]),rect(x-w*.08,y+h*.30,w*.16,h*.22,8)]
def leaf(x,y,w,h,angle=0):
    points=[];a=math.radians(angle)
    for i in range(80):
        t=i/80*math.tau;xx=w*.5*math.sin(t)*abs(math.sin(t))**.3;yy=h*.5*math.cos(t)
        points.append((x+xx*math.cos(a)-yy*math.sin(a),y+xx*math.sin(a)+yy*math.cos(a)))
    return poly(points)
def maze(cols,rows,seed,thick=24):
    rng=random.Random(seed);visited={(0,0)};stack=[(0,0)];links=set()
    while stack:
        x,y=stack[-1];options=[(x+dx,y+dy) for dx,dy in [(1,0),(-1,0),(0,1),(0,-1)] if 0<=x+dx<cols and 0<=y+dy<rows and (x+dx,y+dy) not in visited]
        if options:
            q=rng.choice(options);links.add(tuple(sorted(((x,y),q))));visited.add(q);stack.append(q)
        else:stack.pop()
    ox,oy=260,255;cw,ch=1680/cols,490/rows;result=[]
    for y in range(rows):
        for x in range(cols):
            if x+1<cols and tuple(sorted(((x,y),(x+1,y)))) not in links:result.append(line([(ox+(x+1)*cw,oy+y*ch),(ox+(x+1)*cw,oy+(y+1)*ch)],thick))
            if y+1<rows and tuple(sorted(((x,y),(x,y+1)))) not in links:result.append(line([(ox+x*cw,oy+(y+1)*ch),(ox+(x+1)*cw,oy+(y+1)*ch)],thick))
    # Reach the frame between spawn columns so the entire maze cannot be bypassed
    # along a wide, empty top/bottom band. The six spawn pockets remain clear.
    for col in [cols//3,cols-cols//3]:
        x=ox+col*cw
        result += [line([(x,0),(x,oy)],thick),line([(x,oy+490),(x,H)],thick)]
    return result

def designs():
    out=[]
    def add(i,palette,ops,note):out.append(dict(id=i,palette=palette,ops=ops,design_note=note))
    ops=[]
    for x,y,w,h in [(380,485,240,420),(730,570,245,420),(1100,455,290,480),(1470,570,245,420),(1820,485,240,420)]:ops+=pine(x,y,w,h)
    add(0,'jade',ops,'五片松林形成两条横向通路和中央斜切线。')
    add(1,'porcelain',[oval(*p) for p in [(350,340,145,90,-0, -20),(640,590,190,105,False,14),(1070,390,210,125,False,-9),(1430,640,180,115,False,18),(1830,345,160,115,False,-17),(370,690,85,55),(740,290,90,55),(1000,710,90,50),(1450,300,110,55),(1840,680,100,65),(520,460,42,29),(870,520,48,32),(1270,550,54,34),(1620,450,50,30),(1550,760,43,27)]],'错落椭圆岛横向展开，主岛之间保留宽通道。')
    ops=[]
    for x,y,w,h in [(420,410,290,410),(780,620,200,310),(1110,450,380,570),(1500,640,220,330),(1800,420,300,420)]:ops+=pine(x,y,w,h)
    add(2,'slate',ops,'主松与侧林高低错落，中央两侧可包抄。')
    ops=[]
    for x,y in [(420,320),(750,655),(1100,350),(1450,655),(1780,320)]:ops+=pine(x,y,260,350)
    add(3,'jade',ops,'交错松林留出贯穿全宽的S形空地。')
    add(4,'slate',[],'空白竞技场；地面纹样只作装饰，不设置隐形障碍。')
    add(5,'porcelain',[poly([(1100,200),(1500,770),(1270,770),(1210,590),(990,590),(930,770),(700,770)]),oval(1100,735,170,205,True),poly([(390,650),(540,390),(680,650)]),poly([(1520,650),(1660,390),(1810,650)])],'抽象拱门加两侧三角哨塔，拱底可穿行。')
    add(6,'copper',[line([(300,300),(700,300)],48),line([(510,480),(930,480)],48),line([(300,690),(690,690)],48),line([(1100,310),(1500,310)],48),line([(1300,510),(1880,510)],48),line([(1010,720),(1410,720)],48),line([(1770,270),(1880,340)],36)],'短线段交错形成多个宽缺口。')
    add(7,'porcelain',[oval(1100,500,540,230),oval(340,520,90,145),oval(1860,520,90,145)],'巨大椭圆主岛与两侧小岛，重点为上下两条长线。')
    add(8,'violet',[rect(330,240,1540,520,220),oval(1100,500,650,215,True),rect(1025,180,150,660,cut=True),rect(250,445,1700,110,cut=True)],'反色椭圆拆成四块弧墙，四向入口通向内场。')
    add(9,'slate',[line([(450,300),(450,650),(790,650)],78),line([(990,310),(1100,660),(1250,450),(1400,660),(1520,310)],78),oval(1770,440,90),oval(1830,660,45),oval(800,330,42)],'以自绘L/W几何字形为核心，周围留出绕行空间。')
    ops=[]
    for sx in [-1,1]:
        for sy in [-1,1]:ops.append(poly([(1100+sx*140,500+sy*80),(1100+sx*560,500+sy*80),(1100+sx*720,500+sy*230),(1100+sx*380,500+sy*230)]))
    add(10,'jade',ops+[oval(1100,500,70)],'四象限对称翼墙，横纵和对角线均有通路。')
    add(11,'porcelain',maze(7,3,11,32),'低密度宽通道迷宫，多侧入口适合触屏。')
    add(12,'jade',[leaf(460,440,200,350,-35),leaf(850,580,220,380,30),leaf(1250,420,230,370,-30),leaf(1740,560,270,420,28),line([(370,680),(560,590)],26),line([(1530,320),(1720,270)],26)],'叶片剪影与短枝组成错位路线。')
    add(13,'copper',[poly([(x,y-125),(x+145,y+105),(x-145,y+105)]) for x,y in [(420,390),(800,620),(1130,385),(1500,620),(1830,390)]],'交替朝向的三角群，中路留宽敞折返通道。')
    ops=[]
    for i,(x,y) in enumerate([(370,340),(700,650),(1070,400),(1430,650),(1820,350)]):
        ops += [oval(x,y,100,70),star(x+110,y+100,65,n=6),rect(x-130,y+110,90,65,18)]
    ops += [oval(1000,720,43),oval(1500,300,48)]
    add(14,'violet',ops,'混合形状聚落，保留可辨认的多条进攻通路。')
    add(15,'jade',[rect(x,y,w,h,18) for x,y,w,h in [(300,275,260,130),(620,500,250,230),(950,280,300,180),(1350,510,240,210),(1690,280,250,150),(300,610,200,100)]],'宽窄矩形错位布置，避免细碎格子。')
    add(16,'jade',[leaf(650,500,460,400,-35),leaf(1510,500,460,400,35),oval(1100,500,95)],'绿色主题双叶主岛，中央有开放争夺点。')
    add(17,'porcelain',[poly([(380,500),(690,300),(1100,235),(1510,300),(1820,500),(1510,700),(1100,765),(690,700)]),oval(1100,500,565,185,True),rect(1045,170,110,660,cut=True),oval(1100,500,120)],'眼形外墙保留上下入口，瞳孔为独立岛。')
    add(18,'slate',[oval(350+c*300,310+r*190,38+(c+r)%3*7) for r in range(3) for c in range(6)],'小圆点使用宽间距，整体覆盖横向画幅。')
    add(19,'porcelain',[oval(x,y,r) for x,y,r in [(380,390,120),(720,630,135),(1090,425,155),(1460,630,135),(1820,390,120)]],'五个大圆岛形成起伏的主路线。')
    add(20,'violet',[poly([(x,y-145),(x+160,y),(x,y+145),(x-160,y)]) for x,y in [(410,490),(840,340),(1120,640),(1470,340),(1830,570)]],'菱形错位，尖角之间留足绕行距离。')
    ops=[]
    for y,phase in [(310,0),(510,1.6),(700,3.0)]:
        for lo,hi in [(290,930),(1250,1910)]:ops.append(line([(x,y+65*math.sin(x/200+phase)) for x in range(lo,hi+1,20)],46))
    add(21,'violet',ops,'紫色曲线分段成岛，中央直通带连通上下。')
    add(22,'copper',[line([(280,500),(440,290),(610,670),(790,330),(930,560)],46),line([(1230,560),(1390,300),(1560,690),(1740,340),(1910,520)],46)],'两组锐角折线，中间与两端均留大通道。')
    add(23,'slate',[rect(370+i*355,260+(i%2)*150,48,360,24) for i in range(5)],'交错隔板，避免形成一条过窄强制通道。')
    add(24,'copper',[line([(330,280),(1810,280),(1810,480),(520,480),(520,710),(1720,710)],48),rect(1040,200,120,620,cut=True)],'往返长墙中切出中央快速通路。')
    ops=[]
    for x in [330,920,1510]:
        ops += [poly([(x,330),(x+350,260),(x+430,430),(x+365,690),(x+90,730),(x-30,540)]),poly([(x+90,390),(x+290,350),(x+335,455),(x+280,610),(x+125,630),(x+65,525)],True),rect(x+245,240,100,210,cut=True)]
    add(25,'porcelain',ops,'三座有开口的锯齿堡垒，内部空间可进入。')
    ops=[]
    for y,p in [(340,0),(640,1.2)]:
        for lo,hi in [(270,960),(1240,1930)]:ops.append(line([(x,y+100*math.sin((x-lo)/220+p)) for x in range(lo,hi+1,15)],52))
    add(26,'jade',ops,'双层波浪与中央缺口形成横向流动路线。')
    add(27,'violet',[arc(1100,500,rx,ry,20+i*80,330+i*80,34) for i,(rx,ry) in enumerate([(740,300),(530,220),(320,135)])]+[oval(1100,500,55)],'三层错向开口的巢形弧墙。')
    add(28,'copper',[poly([(320,630),(590,260),(760,300),(540,720)]),poly([(950,330),(1180,270),(1300,670),(1080,720)]),poly([(1500,310),(1670,260),(1890,630),(1680,720)])],'红色主题的三片斜向刃岛，饱和度压低以区分红队。')
    ops=[]
    for y,gap in [(300,720),(440,1350),(580,880),(720,1550)]:ops += [line([(280,y),(gap-100,y)],38),line([(gap+100,y),(1920,y)],38)]
    add(29,'slate',ops,'每层缺口宽敞且交错，两侧均可绕行。')
    add(30,'jade',maze(10,4,30,26),'中迷宫一，横向网格加宽，保留多侧出入口。')
    add(31,'slate',maze(15,5,31,22),'大迷宫使用可辨认的通道宽度，不堆细线。')
    add(32,'violet',maze(9,5,32,25),'中迷宫二，纵向岔路多、横向间距宽。')
    add(33,'copper',maze(12,4,33,24),'中迷宫三，横向多岔路与开放外环。')
    add(34,'porcelain',[arc(x,500,210,155,30,330,34) for x in [470,1100,1730]],'六队主题以低饱和地面出生区提示，中央三拱可穿。')
    add(35,'violet',[star(x,y,135,n=5,angle=a) for x,y,a in [(400,400,-90),(760,650,-75),(1100,415,-100),(1470,650,-65),(1820,400,-90)]],'宽间隔星形岛，不让尖角产生单像素障碍。')
    ops=[oval(1100,450,340,180),rect(900,480,400,230,55),oval(980,450,85,60,True),oval(1220,450,85,60,True),line([(720,450),(1480,450)],55,True),poly([(1100,470),(1150,550),(1050,550)],True),line([(1100,450),(1100,650)],65,True),rect(920,590,360,65,cut=True)]
    for x in [380,1600]:ops += [line([(x,350),(x+230,680)],52),line([(x,680),(x+230,350)],52)]
    add(36,'porcelain',ops,'骷髅主岛的眼眶与口部有真实出口，左右交叉骨岛。')
    ops=[poly([(360,710),(530,280),(610,280),(780,710),(675,710),(620,565),(520,565),(465,710)]),poly([(550,475),(590,365),(625,475)],True),rect(970,290,90,420,10),oval(1110,400,165,110),oval(1110,590,180,120),oval(1110,400,70,45,True),oval(1110,590,80,48,True),line([(1110,400),(1360,400)],34,True),line([(1110,590),(1390,590)],34,True),arc(1710,500,220,210,45,315,80)]
    add(37,'slate',ops,'自绘ABC轮廓，字腔通过开口连接战场，不依赖字体。')
    ops=[]
    for x in [680,1520]:ops += [oval(x,500,340,205),oval(x,500,270,140,True),rect(x+200,445,220,110,cut=True),oval(x,500,65)]
    add(38,'jade',ops,'双眼主岛均有侧向通路，瞳孔为独立障碍。')
    add(39,'violet',[],'Mandelbrot分形轮廓由独立数学几何生成，保留可通行外环。')
    ops=[]
    for k,ox in enumerate([330,930,1530]):
        for row in range(3):
            for col in range(3):
                if (row*2+col+k)%4!=0:ops.append(rect(ox+col*120,325+row*130,90,95,8))
    add(40,'porcelain',ops,'三组填字块以宽街道连接，局部缝隙可辨认。')
    ops=[]
    for x,variant in [(540,0),(1100,1),(1660,2)]:
        ops += [oval(x,480,110,140),poly([(x-100,410),(x-180,300),(x-35,355)]),poly([(x+100,410),(x+180,300),(x+35,355)]),rect(x-60,550,120,170,35)]
        if variant!=1:ops += [line([(x-65,355),(x-105,200),(x-160,160)],35),line([(x+65,355),(x+105,200),(x+160,160)],35)]
        else:ops += [oval(x,395,185,130)]
    add(41,'copper',ops,'羚羊与野生动物头部几何剪影，两侧和中间留通路。')
    ops=[]
    for x in [450,1100,1750]:ops += [arc(x,500,230,230,35,325,40),oval(x-72,430,27),oval(x+72,430,27),arc(x,490,110,120,25,155,30)]
    add(42,'porcelain',ops,'笑脸外圈有开口，内部是可到达的交战区域。')
    ops=[]
    for row in range(3):
        for col in range(6):
            x=370+col*292+(row%2)*50;y=320+row*175
            ops.append(poly([(x+125*math.cos(i*math.tau/6),y+75*math.sin(i*math.tau/6)) for i in range(6)]))
    add(43,'glass',ops,'低饱和六边玻璃岛，缝隙是可玩通道。')
    add(44,'jade',[arc(x,y,r,r,35+a,320+a,32) for x,y,r,a in [(420,430,165,0),(810,640,130,90),(1100,370,165,30),(1470,640,130,160),(1810,430,165,20)]],'不同开口方向的圆环，所有环内空间都能到达。')
    add(45,'slate',[star(420,390,135,n=6),rect(650,545,300,170,50),poly([(1120,250),(1280,420),(1180,590),(990,560),(945,380)]),oval(1480,650,140,90,angle=-18),poly([(1680,280),(1920,540),(1600,590)]),oval(670,315,75)],'六类几何形状组合成横向多路线战场。')
    assert [m['id'] for m in out]==list(range(46))
    return out
