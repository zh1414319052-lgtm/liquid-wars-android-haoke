class_name BattleVfx extends Node3D
## Read-only, bounded instancing. Effects use game time and filtered native facts.
const LIMIT := 1536
const COLORS := [Color("ffd780"),Color("d8b184"),Color("65c9f0"),Color("c2abff"),Color("c9c1ee"),Color("c6a8ed"),Color("ffe2a2"),Color("ff8795"),Color("91dcc0"),Color("efb486"),Color("91b9d1"),Color("ffcf9d"),Color("91d9dc"),Color("c7c1e6"),Color("9ea9e1"),Color("f6b19b"),Color("edd6a5"),Color("b9dfce"),Color("b2c0e1"),Color("f3d4a0"),Color("65c9f0"),Color("3fbf94")]
var mesh := MultiMesh.new()
var buffer := PackedFloat32Array()
var used := 0
var last_epoch := -1
var previous: Dictionary = {}
var births: Dictionary = {}
var events: Dictionary = {}
var bursts: Array[Dictionary] = []
var armies: Dictionary = {}
var clock := 0.0
var bounds := Rect2()
var initialized := false

func _ready() -> void:
 var plane := PlaneMesh.new()
 plane.size = Vector2.ONE
 var material := ShaderMaterial.new()
 material.shader = preload("res://vfx.gdshader")
 plane.material = material
 mesh.transform_format = MultiMesh.TRANSFORM_3D
 mesh.use_colors = true
 mesh.mesh = plane
 mesh.instance_count = LIMIT
 mesh.visible_instance_count = 0
 mesh.custom_aabb = AABB(Vector3(-20,-5,-2070),Vector3(3300,150,2100))
 var node := MultiMeshInstance3D.new()
 node.multimesh = mesh
 node.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
 add_child(node)
 buffer.resize(LIMIT*16)

func _piece(p: Vector3, sx: float, sz: float, angle: float, color: Color) -> void:
 if used >= LIMIT or not bounds.has_point(Vector2(p.x,-p.z)):
  return
 var a := cos(angle)
 var b := sin(angle)
 var at := used*16
 buffer[at]=a*sx;buffer[at+1]=0;buffer[at+2]=-b*sz;buffer[at+3]=p.x
 buffer[at+4]=0;buffer[at+5]=1;buffer[at+6]=0;buffer[at+7]=p.y
 buffer[at+8]=b*sx;buffer[at+9]=0;buffer[at+10]=a*sz;buffer[at+11]=p.z
 buffer[at+12]=color.r;buffer[at+13]=color.g;buffer[at+14]=color.b;buffer[at+15]=color.a
 used += 1

func _line(a: Vector3,b: Vector3,width: float,color: Color) -> void:
 var d := b-a
 _piece((a+b)*0.5,maxf(.1,d.length()),width,atan2(d.z,d.x),color)

func _ring(p: Vector3,radius: float,color: Color,phase: float,segments: int=28) -> void:
 for j in segments:
  var angle := float(j)*TAU/segments+phase
  var start := p+Vector3(cos(angle),0,sin(angle))*radius
  var end := p+Vector3(cos(angle+TAU/segments*.68),0,sin(angle+TAU/segments*.68))*radius
  _line(start,end,.36,color)

func _burst(kind: int,p: Vector3,owner: int,amount: int=1) -> void:
 if bursts.size()>=64:
  bursts.pop_front()
 bursts.append({"kind":kind,"p":p,"owner":owner,"at":clock,"amount":amount})

func _persistent(kind: int,p: Vector3,radius: float) -> void:
 var color: Color = COLORS[mini(kind,21)]
 color.a=.72
 if kind==2:
  # Stable ice facets identify a frozen formation without shaking its units.
  for j in 6:
   var a := j*TAU/6+.25
   var tip := p+Vector3(cos(a),0,sin(a))*radius
   _line(tip-Vector3(cos(a),0,sin(a))*4,tip+Vector3(cos(a),0,sin(a))*3,.55,color)
   _line(tip,tip+Vector3(cos(a+.8),0,sin(a+.8))*3,.4,color)
   _line(tip,tip+Vector3(cos(a-.8),0,sin(a-.8))*3,.4,color)
 elif kind==6 or kind==16:
  for j in 6:
   var a := j*TAU/6+clock*.09
   var tip := p+Vector3(cos(a),0,sin(a))*radius
   _line(tip,tip+Vector3(cos(a+2.1),0,sin(a+2.1))*5,.65,color)
 elif kind==5 or kind==18:
  for j in 9:
   var a := j*2.399+clock*.16
   var tip := p+Vector3(cos(a),0,sin(a))*radius*.8
   color.a=.18 if kind==18 else .4
   _piece(tip,3,.45,a,color)
 elif kind==21 or kind==11:
  for j in 10:
   var a := j*TAU/10-clock*.17
   var r := radius*(.85+fmod(clock*.4+j*.1,1)*.15)
   _piece(p+Vector3(cos(a),.3,sin(a))*r,2.1,.75,a,color)
 elif kind==1:
  for j in 8:
   var a := j*TAU/8+clock*.4
   _piece(p+Vector3(cos(a),.2,sin(a))*radius*.8,1.4,.8,a,color)
 elif kind==7:
  _ring(p,radius,color,-clock*.3,12)
 elif kind>=9:
  # Counter families have small segmented marks, not opaque enclosing shields.
  var count := 4+(kind%4)
  for j in count:
   var a := j*TAU/count+kind*.3
   var tip := p+Vector3(cos(a),.2,sin(a))*radius
   _line(tip,tip+Vector3(cos(a+PI*.5),0,sin(a+PI*.5))*2.8,.5,color)

func update_frame(frame: PackedFloat32Array,offset: int,count: int,seconds: float) -> void:
 var epoch := int(frame[2])
 if epoch!=last_epoch or seconds<clock:
  previous.clear();births.clear();events.clear();bursts.clear();initialized=false
  last_epoch=epoch
 clock=seconds;used=0;armies.clear()
 bounds=Rect2(frame[4]-30,frame[5]-30,frame[6]+60,frame[7]+60)
 var current: Dictionary = {}
 for i in count:
  var a := offset+i*10
  if int(frame[a])==50:
   armies[int(frame[a+6])]=Vector3(frame[a+2],frame[a+3]+.5,frame[a+4])
 for i in count:
  var a := offset+i*10
  var kind := int(frame[a])
  var id := int(frame[a+1])
  var owner := int(frame[a+6])
  var p := Vector3(frame[a+2],frame[a+3]+.8,frame[a+4])
  if kind<2 or kind==8 or (kind>=20 and kind<40):
   var hp := clampf(frame[a+8],0,1)
   var eating := frame[a+7]>.5
   current[id]={"kind":kind,"hp":hp,"p":p,"eating":eating,"owner":owner}
   var color: Color = Color("f2c663") if kind==8 else COLORS[kind-20 if kind>=20 else (20 if kind==0 else 21)]
   if hp<.999:
    var bar := p+Vector3(0,2.4,-7)
    _line(bar+Vector3(-4,0,0),bar+Vector3(4,0,0),.8,Color(.07,.15,.17,.85))
    _line(bar+Vector3(-4,0,0),bar+Vector3(-4+8*hp,0,0),.52,color)
   if eating:
    var goal: Vector3 = armies.get(owner,p+Vector3(5,0,0))
    var direction := goal-p
    direction.y=0
    if direction.length()>30:direction=direction.normalized()*30
    for j in 10:
     var f := fmod(clock*1.4+j*.1,1)
     var angle := j*2.399+id
     var start := p+Vector3(cos(angle),.5,sin(angle))*4*sqrt(maxf(hp,.1))
     var end := p+direction
     var q := start.lerp(end,f)
     q.y+=sin(f*PI)*1.5
     color.a=(1-f)*.8
     _piece(q,.6+(1-f)*.7,.45,angle,color)
   elif initialized and not previous.has(id):
    _burst(-2,p,owner)
  elif kind>=60 and kind<82:
   _persistent(kind-60,p,frame[a+5])
  elif kind>=100 and kind<122:
   if not events.has(id) and clock-frame[a+7]<1.8:
    events[id]=clock
    _burst(kind-100,p,owner,int(frame[a+8]))
 for id in previous:
  if not current.has(id):
   var old: Dictionary=previous[id]
   if old.hp<.2 and old.eating:
    _burst(-1 if old.kind==8 else -2,old.p,old.owner)
 previous=current;initialized=true
 for id in events.keys():
  if clock-float(events[id])>4:events.erase(id)
 var site_offset := 16+int(frame[0])*16
 for i in int(frame[3]):
  var a := site_offset+i*8
  var born := int(frame[a+6])
  if births.has(i) and born>int(births[i]):
   _burst(0,Vector3(frame[a],frame[a+1]+1,frame[a+2]),int(frame[a+3]),born-int(births[i]))
  births[i]=born
 for index in range(bursts.size()-1,-1,-1):
  var burst: Dictionary=bursts[index]
  var age := clock-float(burst.at)
  if age>1.2:
   bursts.remove_at(index);continue
  var f := clampf(age/1.2,0,1)
  var kind: int=burst.kind
  var p: Vector3=burst.p
  var color: Color=Color("f5ce76") if kind==-1 else Color("9be3ca") if kind==-2 else COLORS[kind]
  color.a=(1-f)*.8
  var inward := kind==0 or kind==4 or kind==7
  var rays := 18 if kind==3 or kind==7 else 12
  for j in rays:
   var angle := j*2.399+(f*3 if kind==3 or kind==4 else .2)
   var radius := (1-f if inward else f)*(15+j%4*3)+2
   var q := p+Vector3(cos(angle)*radius,sin(f*PI)*(2+j%3),sin(angle)*radius)
   _piece(q,(1-f)*2.3+.4,.65 if kind>=0 and kind!=0 else 1.1,angle,color)
  if kind==2 or kind==20:
   _ring(p,4+f*24,color,0,18)
  elif kind==3:
   _ring(p,4+sin(f*PI)*9,color,f*3)
  elif kind==11 or kind==21:
   _ring(p,3+f*17,color,-f,20)
  elif kind>=9 and kind<18:
   _ring(p,5+(1-f)*10,color,.2,8)
 mesh.buffer=buffer
 mesh.visible_instance_count=used
