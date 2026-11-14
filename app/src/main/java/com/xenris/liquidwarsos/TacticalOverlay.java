// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.app.*;import android.content.*;import android.graphics.*;import android.util.AttributeSet;import android.view.*;import android.widget.*;import java.util.*;
/** Touch planning and presentation only. Authoritative routes/economy come from native snapshots. */
public final class TacticalOverlay extends View {
 private final Paint p=new Paint(3);private final float dp;private final boolean zh;private final OrderProtocol codec=new OrderProtocol();
 private OperationsSnapshot snap;private long readAt;private int epoch=-1,orderGeneration=-1,selected=3,input=0,button=-1; // 0 tap, 1 draw, 2 legacy follow
 private final int[] lastBorn=new int[5],production=new int[5];
 private final ArrayList<int[]> draft=new ArrayList<>();private boolean drawing,wasFollowing,splitRequested;private int pointer=-1;private float mapLeft,mapBottom,mapW,mapH;
 private boolean tracking;private float downX,downY,fingerX,fingerY;private long sentAt;private int sentX=-1,sentY=-1;
 private final Runnable hold=()->{if(drawing&&input==0&&hasWindowFocus())startTracking();};
 private final int[] accents={0xff91e3d1,0xffffd582,0xffc3b0ff};private String hint="";private long hintUntil;
 public TacticalOverlay(Context c,AttributeSet a){super(c,a);dp=getResources().getDisplayMetrics().density;zh="zh".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());p.setTypeface(Typeface.createFromAsset(c.getAssets(),"fonts/NotoSansSC-Intro.ttf"));p.setFontVariationSettings("'wght' 600");setContentDescription(zh?"单指路线与分队指挥":"Route and squad commands");}
 private MyGLSurfaceView surface(){return ((View)getParent()).findViewById(R.id.mySurfaceView);}
 private ItemOverlay items(){return ((View)getParent()).findViewById(R.id.item_overlay);}
 OperationsSnapshot facts(){return snap;}
 private boolean spectator(){return ((Activity)getContext()).getIntent().getBooleanExtra("spectator",false);}
 private void births(int[] data){int count=Math.min(5,data[5]);for(int i=0;i<count;i++){int born=data[13+i*8+6];if(data[3]!=epoch){production[i]=0;}else if(born>lastBorn[i])production[i]=born-lastBorn[i];lastBorn[i]=born;}}
 void frame(long nanos){long now=nanos/1000000;if(now-readAt>=70){readAt=now;int[] data=NativeInterface.operationsSnapshot(StaticBits.team,spectator());if(data!=null){births(data);snap=new OperationsSnapshot(data);if(epoch>=0&&epoch!=snap.epoch()){cancelDraft();splitRequested=false;selected=3;}epoch=snap.epoch();if(orderGeneration>=0&&orderGeneration!=snap.generation(StaticBits.team)){cancelDraft();splitRequested=false;selected=3;}orderGeneration=snap.generation(StaticBits.team);}}if(snap!=null){if(tracking){int total=0;for(int g=0;g<3;g++)total+=snap.raw[snap.group(StaticBits.team,g)];if(total==0)cancelDraft();else updateTracking(false);}invalidate();}}
 private void note(String s){hint=s;hintUntil=android.os.SystemClock.uptimeMillis()+3500;invalidate();}
 private float baseline(){int[] d=items().facts();boolean card=d!=null&&d.length>208&&d[147+StaticBits.team*10]>0;return getHeight()-(card?142:62)*dp;}
 private float buttonWidth(){return Math.min(66*dp,(getWidth()-20*dp)/9f);}
 private void label(Canvas c,String s,float x,float y,float size,int color){p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(color);p.setTextSize(size*dp);p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);}
 private float xx(int x){MyGLSurfaceView v=surface();return v.getLeft()+v.camera.screenX(x,v.getWidth());}
 private float yy(int y){MyGLSurfaceView v=surface();return v.getTop()+v.camera.screenY(y,v.getHeight());}
 private void panel(Canvas c,float x,float y,float w,float h,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(x,y,x+w,y+h,9*dp,9*dp,p);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(snap==null)return;int t=StaticBits.team;int[] d=snap.raw;WorldCamera cam=surface().camera;
  c.save();c.clipRect(surface().getLeft(),surface().getTop(),surface().getRight(),surface().getBottom());
  for(int i=0;i<snap.sites();i++){int a=snap.site(i),owner=d[a+2],claim=d[a+3];float x=xx(d[a]),y=yy(d[a+1]),r=OperationsRadius()*surface().getWidth()/cam.viewWidth;int col=owner<0?0xffd2c49e:TeamPalette.color(owner);
   p.setStyle(Paint.Style.FILL);p.setColor((col&0xffffff)|0x16000000);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f*dp);p.setColor((col&0xffffff)|0xc0000000);c.drawCircle(x,y,r,p);
   if(d[a+4]>0){p.setColor(TeamPalette.color(Math.max(0,claim)));p.setStrokeWidth(4*dp);c.drawArc(x-r,y-r,x+r,y+r,-90,360*d[a+4]/4000f,false,p);}
   float pulse=(d[2]-d[a+7])/850f;if(pulse>=0&&pulse<1){p.setColor((col&0xffffff)|((int)((1-pulse)*170)<<24));p.setStrokeWidth(2*dp);c.drawCircle(x,y,10*dp+r*pulse,p);for(int k=0;k<6;k++){double angle=k*Math.PI/3;float radius=(1-pulse)*r*.65f;p.setStyle(Paint.Style.FILL);c.drawCircle(x+(float)Math.cos(angle)*radius,y+(float)Math.sin(angle)*radius,2.3f*dp,p);}if(production[i]>0){panel(c,x-18*dp,y+r+29*dp,36*dp,21*dp,0xe916252e);label(c,"+"+production[i],x,y+r+44*dp,12,col);}}
   panel(c,x-16*dp,y-13*dp,32*dp,26*dp,0xeb12242c);label(c,""+(i+1),x,y+5*dp,14,col);
   String text=d[a+5]!=0?(zh?"争夺 · 暂停收益":"CONTESTED"):claim>=0?(owner>=0?(zh?"正在中立化":"NEUTRALIZING"):(zh?"正在占领":"CAPTURING")):owner<0?(zh?"驻留4秒占领":"HOLD 4s TO CAPTURE"):(zh?"产液 · 控点":"PRODUCTION · CONTROL");p.setTextSize(12*dp);float tw=p.measureText(text)+18*dp;panel(c,x-tw/2,y+r+3*dp,tw,23*dp,0xe916252e);label(c,text,x,y+r+19*dp,12,0xfff0eee2);
  }
  if(!spectator())for(int g=0;g<3;g++){int a=snap.group(t,g);if(d[a]<=0)continue;int color=accents[g];if(snap.split(t)){panel(c,xx(d[a+1])-13*dp,yy(d[a+2])-30*dp,26*dp,22*dp,0xdc10232b);label(c,""+(char)('A'+g),xx(d[a+1]),yy(d[a+2])-14*dp,12,color);}
   if(d[a+3]==0||d[a+4]!=0)continue;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth((selected==g||selected==3?1.6f:.8f)*dp);p.setColor((color&0xffffff)|(d[a+5]==2?0x45000000:0xb0000000));p.setPathEffect(new DashPathEffect(new float[]{7*dp,5*dp},0));Path path=new Path();float endX=xx(d[a+1]),endY=yy(d[a+2]);path.moveTo(endX,endY);for(int k=d[a+6];k<d[a+7];k++){endX=xx(d[a+8+k*2]);endY=yy(d[a+9+k*2]);path.lineTo(endX,endY);c.drawCircle(endX,endY,3*dp,p);}c.drawPath(path,p);p.setPathEffect(null);c.drawCircle(endX,endY,8*dp,p);
  }
  if(drawing&&!tracking&&!draft.isEmpty()){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f*dp);p.setColor(0xfff9e8af);Path path=new Path();for(int i=0;i<draft.size();i++){int[] v=draft.get(i);if(i==0)path.moveTo(xx(v[0]),yy(v[1]));else path.lineTo(xx(v[0]),yy(v[1]));}c.drawPath(path,p);}
  c.restore();
  if(snap.mode()==1){StringBuilder score=new StringBuilder(zh?"控点 ":"CONTROL ");for(int i=0;i<6;i++)if(d[7+i]>0||i==t)score.append(i==t?(zh?"我方":"YOU"):String.valueOf(i+1)).append(' ').append(d[7+i]).append("  ");score.append("/ ").append(d[4]);float w=Math.min(360*dp,getWidth()-30*dp);panel(c,getWidth()/2f-w/2,51*dp,w,25*dp,0xd8172730);label(c,score.toString(),getWidth()/2f,68*dp,11,0xffe7ddba);}
  if(cam.large()){int[] item=items().facts();boolean teleport=item!=null&&item.length>208&&item[147+t*10]==3;MiniMapLayout mini=new MiniMapLayout(getWidth(),getHeight(),dp,cam.width,cam.height,teleport);for(int i=0;i<snap.sites();i++){int a=snap.site(i);float x=mini.left+d[a]*mini.width/cam.width,y=mini.top+(cam.height-1-d[a+1])*mini.height/cam.height;panel(c,x-5*dp,y-5*dp,10*dp,10*dp,0xff13232a);p.setColor(d[a+2]<0?0xffecd6a1:TeamPalette.color(d[a+2]));c.drawCircle(x,y,3.2f*dp,p);}}
  if(spectator())return;
  float y=baseline(),w=buttonWidth();String[] names=zh?new String[]{input==0?"点选":input==1?"画线":"跟随","全队","A","B","C",snap.split(t)?"重分":"分兵","合流","停止","帮助"}:new String[]{input==0?"TAP":input==1?"DRAW":"FOLLOW","ALL","A","B","C","SPLIT","MERGE","STOP","HELP"};
  for(int i=0;i<names.length;i++){boolean chosen=i==0||(selected<3&&i==selected+2)||(i==1&&selected==3);panel(c,10*dp+i*w,y,w-3*dp,48*dp,chosen?0xee285047:0xea14242e);label(c,names[i],10*dp+(i+.5f)*w,y+29*dp,12,i==7?0xffffae9e:0xffe1ece7);}
  String status=input==2?(zh?"按住跟随 · 支持五指，松手追点":"Hold to guide · five fingers supported"):(input==1?(zh?"划线后松手执行 · 再次下令替换路线":"Draw and release · new order replaces route"):(zh?"点选目的地 · 长按拖动跟随 · 松手继续":"Tap destination · hold/drag to follow · release to keep"));
  if(selected<3){int a=snap.group(t,selected);String[] states=zh?new String[]{"待命","执行中","已到达","道路受阻","目标无效"}:new String[]{"IDLE","MOVING","ARRIVED","BLOCKED","INVALID"};status=(char)('A'+selected)+" · "+d[a]+(zh?"颗 · ":" units · ")+states[Math.max(0,Math.min(4,d[a+5]))]+(d[a+7]>1?" · "+(d[a+6]+1)+"/"+d[a+7]:"")+(d[a+4]!=0?(zh?" · 已停止":" · STOPPED"):"");}
  if(selected==3){boolean blocked=false,holding=false;for(int g=0;g<3;g++){int a=snap.group(t,g);if(d[a]>0&&d[a+3]!=0){blocked|=d[a+5]==3;holding|=d[a+4]!=0;}}if(blocked)status=zh?"道路受阻 · 点选新目标，或切换跟随":"BLOCKED · Set a new target or switch to FOLLOW";else if(holding)status=zh?"全队 · 已停止":"ALL · STOPPED";}
  if(snap.feedback(t)>=3)status=zh?"路线不可达或目标在墙内 · 已保留上一条命令":"Unreachable route · previous order retained";
  if(android.os.SystemClock.uptimeMillis()<hintUntil)status=hint;if(tracking)status=zh?"正在跟随手指 · 松手保留目标":"Following finger · release keeps target";
  drawStatus(c,status,10*dp+4.5f*w,y,9*w-3*dp);
 }
 private void drawStatus(Canvas c,String status,float center,float bottom,float maxWidth){
  float padding=9*dp;
  p.setTextSize(12*dp);
  if(maxWidth<=2*padding)return;
  if(p.measureText(status)>maxWidth-2*padding){
   // Fit the longest feedback to narrow screens without extending over the map.
   int end=p.breakText(status,true,Math.max(0,maxWidth-2*padding-p.measureText("…")),null);
   status=status.substring(0,end)+"…";
  }
  float width=Math.min(maxWidth,p.measureText(status)+2*padding);
  panel(c,center-width/2,bottom-28*dp,width,23*dp,0x6014242e);
  p.setShadowLayer(dp,0,dp,0xc0000000);
  label(c,status,center,bottom-12*dp,12,0xffeef2ed);
  p.clearShadowLayer();
 }
 private float OperationsRadius(){return 38;}
 private void startTracking(){if(tracking||!drawing)return;tracking=true;removeCallbacks(hold);items().resetControl();surface().camera.endTouch();surface().camera.following=true;sentAt=0;sentX=sentY=-1;surface().audio().play(AudioEvents.ORDER);updateTracking(true);}
 private void updateTracking(boolean force){
  if(!tracking||snap==null)return;long now=android.os.SystemClock.uptimeMillis();if(!force&&now-sentAt<50)return;
  MyGLSurfaceView v=surface();int x=v.camera.mapX(fingerX-v.getLeft(),v.getWidth()),y=v.camera.mapY(fingerY-v.getTop(),v.getHeight());
  if(x==sentX&&y==sentY)return;sentAt=now;sentX=x;sentY=y;
  short[][] packet=codec.packet(OrderProtocol.FOLLOW,selected,snap.generation(StaticBits.team),Collections.singletonList(new int[]{x,y}),0);
  v.controls(packet[0],packet[1]);v.camera.touchTarget(x,y);invalidate();
 }
 private void send(short[][] packet){items().resetControl();surface().camera.endTouch();surface().controls(packet[0],packet[1]);}
 private void command(int op){if(snap==null)return;send(codec.packet(op,selected,snap.generation(StaticBits.team),null,0));}
 private void chooseButton(int n){
  int[] current=NativeInterface.operationsSnapshot(StaticBits.team,spectator());if(current!=null)snap=new OperationsSnapshot(current);
  if(n==0){input=(input+1)%3;cancelDraft();surface().camera.endTouch();note(input==2?(zh?"跟随模式：按住或五指指挥":"Follow: hold or use multiple fingers"):(zh?"单指下令，松手后继续":"One-finger orders continue after release"));}
  else if(n==1)selected=3;else if(n>=2&&n<=4){if(!snap.split(StaticBits.team)&&!splitRequested){note(zh?"先点“分兵”，分为主力与两支小队":"Tap SPLIT to create three persistent squads");return;}selected=n-2;}
  else if(n==5){command(OrderProtocol.SPLIT);splitRequested=true;selected=0;note(zh?"A约一半，B/C各约四分之一；分别选队下令":"A half; B/C quarter each. Select a squad to order.");}
  else if(n==6){command(OrderProtocol.MERGE);splitRequested=false;selected=3;note(zh?"各队向A主力合流":"Regrouping on the main force");}
  else if(n==7){cancelDraft();command(OrderProtocol.STOP);note(zh?"已停止所选队伍，敌人仍可攻击":"Selected force stopped; enemies can still attack");}
  else if(n==8)help();invalidate();
 }
 int[] selectedPosition(){if(snap==null||selected>=3)return null;int a=snap.group(StaticBits.team,selected);return snap.raw[a]>0?new int[]{snap.raw[a+1],snap.raw[a+2]}:null;}
 private void help(){TextView text=new TextView(getContext());text.setPadding((int)(22*dp),(int)(18*dp),(int)(22*dp),(int)(18*dp));text.setTypeface(p.getTypeface());text.setTextSize(15);text.setText(zh?"点选：轻点目的地自动寻路；长按或拖动实时跟随手指，松手保留目标。跟随直接引导，墙体仍会阻挡；可用画线绕行。\n点左侧模式切换画线，划出路线后松手执行；道路不通会提示。\n分兵：A为主力，B/C是两支小队。先选队，再点选或画线；其他队保留命令。合流回到A，停止清除当前路线。\n跟随：保留原来的按住拖动与五指；会接管整队。\n据点：圈内驻留约4秒占领；夺敌点先中立化。敌我争夺时停止进度与收益。每2秒固定产液，产兵达到初始兵力2倍后停止；空位不足不会补发。\n占领方每秒得1控点分，盟友共享胜利，180分或歼灭获胜。分兵扩张更快，但小队容易被截击。\n可以画绕后路线、佯攻后撤、守住通道或趁敌军离开偷点。隐身时粒子隐藏，接触敌军就显形；占点争夺信息公开。\n暂停不计时；回溯清空据点、积分和路线。":"TAP sets a persistent routed destination. Hold or drag to guide the selected force immediately; release keeps the last target. Direct guiding respects walls; use DRAW to route around them. Cycle the mode to DRAW, trace a route and release.\nSPLIT creates A (half), B/C (quarters). Select each squad before ordering; others keep their orders. MERGE regroups on A; STOP clears the selected route.\nFOLLOW preserves hold/drag and five-finger controls for the whole army.\nCONQUEST: hold a site for about 4 seconds. Enemy sites must first be neutralized. Contests pause income and capture. Fixed production every 2 seconds stops at twice the initial team population; blocked spawns are not banked.\nEach owned site gives one control point/second. Alliances share the 180-point victory; elimination also wins. Split expansion is faster but exposes smaller squads.\nUse routes for flanking, feints, retreats and intercepts. Invisible units reveal on hostile contact; site contests remain public. Rewind clears sites, score and orders.");ScrollView scroll=new ScrollView(getContext());scroll.addView(text);AlertDialog dialog=new AlertDialog.Builder(getContext()).setView(scroll).setPositiveButton(zh?"返回战场":"Return",null).create();dialog.show();dialog.getButton(-1).setTypeface(p.getTypeface());}
 private int[] point(float x,float y){MyGLSurfaceView v=surface();int wx=Math.round(mapLeft+(x-v.getLeft())/v.getWidth()*mapW),wy=Math.round(mapBottom+(1-(y-v.getTop())/v.getHeight())*mapH);return new int[]{Math.max(0,Math.min(v.camera.width-1,wx)),Math.max(0,Math.min(v.camera.height-1,wy))};}
 private void cancelDraft(){removeCallbacks(hold);if(drawing&&!tracking){surface().camera.following=wasFollowing;surface().camera.resetSmoothing();}drawing=tracking=false;pointer=-1;draft.clear();}
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(!focus){cancelDraft();button=-1;}}
 @Override protected void onDetachedFromWindow(){cancelDraft();super.onDetachedFromWindow();}
 @Override public boolean onTouchEvent(MotionEvent e){if(spectator()||snap==null)return false;int action=e.getActionMasked();float x=e.getX(),y=e.getY();
  if(action==MotionEvent.ACTION_DOWN){button=-1;float top=baseline(),w=buttonWidth();if(y>=top&&y<=top+48*dp&&x>=10*dp&&x<10*dp+9*w){button=(int)((x-10*dp)/w);return true;}
   if(input==2||items().blocksTactical(x,y))return false;
   MyGLSurfaceView v=surface();if(x<v.getLeft()||x>=v.getRight()||y<v.getTop()||y>=v.getBottom())return false;int total=0;for(int g=0;g<3;g++)total+=snap.raw[snap.group(StaticBits.team,g)];if(total==0)return false;
   v.camera.endTouch();wasFollowing=v.camera.following;v.camera.following=false;mapLeft=v.camera.left;mapBottom=v.camera.bottom;mapW=v.camera.viewWidth;mapH=v.camera.viewHeight;drawing=true;pointer=e.getPointerId(0);draft.clear();draft.add(point(x,y));downX=fingerX=x;downY=fingerY=y;if(input==0)postDelayed(hold,180);return true;
  }
  if(button>=0){if(action==MotionEvent.ACTION_UP){int b=button;button=-1;performClick();chooseButton(b);}else if(action==MotionEvent.ACTION_CANCEL)button=-1;return true;}
  if(!drawing)return false;
  if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN){cancelDraft();return true;}
  int index=e.findPointerIndex(pointer);if(index<0){cancelDraft();return true;}fingerX=e.getX(index);fingerY=e.getY(index);int[] next=point(fingerX,fingerY);
  if(input==0&&action==MotionEvent.ACTION_MOVE&&Math.hypot(fingerX-downX,fingerY-downY)>8*dp)startTracking();
  if(tracking){updateTracking(action==MotionEvent.ACTION_UP);if(action==MotionEvent.ACTION_UP){cancelDraft();note(zh?"松手继续追点 · 点选或拖动可替换":"Released · keeps the last target");}invalidate();return true;}
  if(input==1&&(action==MotionEvent.ACTION_MOVE||action==MotionEvent.ACTION_UP)){int[] last=draft.get(draft.size()-1);if(Math.hypot(last[0]-next[0],last[1]-next[1])>=5||action==MotionEvent.ACTION_UP){draft.add(next);if(draft.size()>384){ArrayList<int[]> reduced=new ArrayList<>();for(int i=0;i<draft.size();i+=2)reduced.add(draft.get(i));draft.clear();draft.addAll(reduced);}}}
  if(action==MotionEvent.ACTION_UP){if(input==0){draft.clear();draft.add(next);}for(short[][] packet:codec.route(selected,snap.generation(StaticBits.team),draft))send(packet);cancelDraft();note(zh?"命令已提交 · 松手继续执行":"Order submitted · continues after release");}
  invalidate();return true;
 }
 @Override public boolean performClick(){super.performClick();return true;}
}
