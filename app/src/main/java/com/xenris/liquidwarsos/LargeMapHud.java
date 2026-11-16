// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import android.graphics.*;
import android.view.*;
import android.widget.Toast;
import com.xenris.liquidwarsos.cloud.SnapshotItems;

/** UI adapter only: classic snapshots/visibility/commands stay authoritative. */
final class LargeMapHud {
 private final ExpeditionOverlay view;
 private final Paint paint=new Paint(3);
 private final ActiveSlots slots;
 private Bitmap terrain;private int terrainId=-1;
 private float u,w,h,zoom=1,dx,dy,lastX,lastY,downX,downY,pinch;
 private boolean expanded,moved;private int tab,hit=-1,teleportNonce;
 LargeMapHud(ExpeditionOverlay view){this.view=view;slots=new ActiveSlots(view.getContext());paint.setTypeface(Typeface.createFromAsset(view.getContext().getAssets(),"fonts/NotoSansSC-Intro.ttf"));paint.setFontVariationSettings("'wght' 600");}
 private View root(){return (View)view.getParent();}
 private MyGLSurfaceView surface(){return root().findViewById(R.id.mySurfaceView);}
 private ItemOverlay items(){return root().findViewById(R.id.item_overlay);}
 private TacticalOverlay orders(){return root().findViewById(R.id.tactical_overlay);}
 private WorldCamera cam(){return surface().camera;}
 boolean active(){return view.getParent()!=null&&GardenSession.expeditionSeats==0&&cam().large();}
 boolean expanded(){return active()&&expanded;}
 void reset(){expanded=false;hit=-1;tab=0;zoom=1;dx=dy=0;}
 void dispose(){if(terrain!=null)terrain.recycle();terrain=null;terrainId=-1;}
 private void layout(){u=BattleHudLayout.unit(view.getWidth(),view.getHeight());w=view.getWidth()/u;h=view.getHeight()/u;}
 private RectF mini(){return BattleHudLayout.mini(w,cam().width,cam().height);}
 private RectF frame(){return BattleHudLayout.expanded(w,h,cam().width,cam().height);}
 private RectF image(RectF r,boolean large){float z=large?zoom:1,cx=r.centerX()+(large?dx:0),cy=r.centerY()+(large?dy:0);return new RectF(cx-r.width()*z/2,cy-r.height()*z/2,cx+r.width()*z/2,cy+r.height()*z/2);}
 private float x(int value,RectF r){return r.left+(value+.5f)*r.width()/cam().width;}
 private float y(int value,RectF r){return r.top+(cam().height-.5f-value)*r.height()/cam().height;}
 private void box(Canvas c,float x,float y,float width,float height,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawRoundRect(x,y,x+width,y+height,8,8,paint);}
 private void label(Canvas c,String s,float x,float y,float size,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTextSize(size);paint.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,paint);}
 private void dot(Canvas c,float x,float y,float radius,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(0xee193238);c.drawCircle(x,y,radius+1,paint);paint.setColor(color);c.drawCircle(x,y,radius,paint);}
 void draw(Canvas c){if(!active())return;int[] data=items().facts();if(data==null||data.length<209)return;View intro=root().findViewById(R.id.match_intro);if(intro!=null&&intro.getVisibility()==View.VISIBLE)return;layout();slots.classic(data,StaticBits.team);c.save();c.scale(u,u);
  if(!expanded){
   box(c,10,8,130,27,0x70193238);label(c,com.xenris.liquidwarsos.BattleText.t("大地图 · ","Large map · ")+data[13+StaticBits.team]+com.xenris.liquidwarsos.BattleText.t("颗"," units"),75,26,11,0xffe6f0e5);
   box(c,146,8,48,27,0x70193238);label(c,cam().overview||!cam().following?com.xenris.liquidwarsos.BattleText.t("跟随","Follow"):com.xenris.liquidwarsos.BattleText.t("全图","Map"),170,26,11,0xffb6e7d5);
   box(c,200,8,48,27,0x70193238);label(c,cam().detail?com.xenris.liquidwarsos.BattleText.t("远景","Far"):com.xenris.liquidwarsos.BattleText.t("近景","Near"),224,26,11,0xffb6e7d5);
   String[] labels={surface().audio().enabled()?com.xenris.liquidwarsos.BattleText.t("声音开","Sound"):com.xenris.liquidwarsos.BattleText.t("静音","Mute"),com.xenris.liquidwarsos.BattleText.t("帧率","FPS"),com.xenris.liquidwarsos.BattleText.t("玩法","Guide")};for(int i=0;i<3;i++){float xx=BattleHudLayout.utilityX(w,i),width=i==0?50:48;box(c,xx,8,width,27,0x70193238);label(c,labels[i],xx+width/2,26,i==0?10:11,i==2?0xffe8dfbd:0xffb6e7d5);}
   OperationsSnapshot ops=orders().facts();box(c,w/2-90,8,180,34,0x66223b3d);
   String state=SnapshotItems.count(data)>0?com.xenris.liquidwarsos.BattleText.t("本轮道具剩余 ","Items remaining: ")+SnapshotItems.count(data)+com.xenris.liquidwarsos.BattleText.t(" 件",""):data[2]==0?com.xenris.liquidwarsos.BattleText.t("首波道具 ","First items in ")+Math.max(0,(data[1]-data[0]+999)/1000)+com.xenris.liquidwarsos.BattleText.t("秒","s"):com.xenris.liquidwarsos.BattleText.t("下一轮即将出现","Next wave incoming");
   label(c,state,w/2,22,10,0xffe9edcf);label(c,ops!=null&&ops.mode()==1?com.xenris.liquidwarsos.BattleText.t("控点 ","Control ")+ops.raw[7+StaticBits.team]+" / "+ops.raw[4]:com.xenris.liquidwarsos.BattleText.t("全部吞噬后刷新下一轮","Finish all items for the next wave"),w/2,36,9,0xffd8e6d8);
   drawMap(c,mini(),false);label(c,com.xenris.liquidwarsos.BattleText.t("点击展开 · 战术地图","Tap to expand · Tactical map"),mini().centerX(),mini().bottom+16,10,0xffe5efdf);slots.draw(c,w,h);
  }else{
   box(c,0,0,w,h,0xea193036);drawMap(c,frame(),true);
   box(c,w-108,12,65,28,0xff416456);label(c,com.xenris.liquidwarsos.BattleText.t("关闭","Close"),w-75,32,12,0xffe4f0df);
   label(c,com.xenris.liquidwarsos.BattleText.t("战术地图","Tactical map"),w/2,32,14,0xffe4f0df);
   int count=items().teleportReady()?4:3;String[] labels={com.xenris.liquidwarsos.BattleText.t("查看","View"),com.xenris.liquidwarsos.BattleText.t("定位镜头","Camera"),com.xenris.liquidwarsos.BattleText.t("下达命令","Orders"),com.xenris.liquidwarsos.BattleText.t("传送落点","Teleport")};
   if(tab>=count)tab=0;for(int i=0;i<count;i++){float xx=w/2-count*54+i*108;box(c,xx,h-69,102,29,tab==i?0xff3c6658:0xff223f46);label(c,labels[i],xx+51,h-49,12,0xffe1eadb);}
   label(c,tab==3?com.xenris.liquidwarsos.BattleText.t("点合法地面传送 · 无效落点保留道具","Tap valid ground · Invalid destinations retain the item"):com.xenris.liquidwarsos.BattleText.t("冰晶=冻结  菱形=净化  方点=自动道具  圆点=据点 · 双指缩放 / 拖动查看","Ice=freeze  Diamond=cleanse  Square=auto item  Circle=outpost · Pinch / drag to view"),w/2,h-22,10,0xffd6e4d3);
  }c.restore();
 }
 private void drawMap(Canvas c,RectF frame,boolean large){WorldCamera cam=cam();RectF r=image(frame,large);box(c,frame.left-3,frame.top-3,frame.width()+6,frame.height()+6,0xf0193035);c.save();c.clipRect(frame);
  if(terrainId!=cam.id){dispose();try(java.io.InputStream in=view.getContext().getAssets().open(cam.id==1001?"garden3d/arena-image.png":"maps/"+cam.id+"-image.png")){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;terrain=BitmapFactory.decodeStream(in,null,o);}catch(java.io.IOException ignored){}terrainId=cam.id;}
  if(terrain!=null){c.save();c.scale((cam.flip&1)!=0?-1:1,(cam.flip&2)!=0?-1:1,r.centerX(),r.centerY());paint.setColor(Color.WHITE);c.drawBitmap(terrain,null,r,paint);c.restore();}
  int[] sight=items().minimapFacts();if(sight!=null&&sight.length>=5)for(int k=0;k<sight[4];k++){int b=5+k*4;if(b+3>=sight.length)break;dot(c,x(sight[b+1],r),y(sight[b+2],r),large?2.5f:1.2f,TeamPalette.color(sight[b]));}
  int[] data=items().facts();for(int k=0;k<SnapshotItems.slots(data);k++){int b=SnapshotItems.offset(data,k),kind=data[b];if(kind<0)continue;float xx=x(data[b+1],r),yy=y(data[b+2],r),radius=large?5:3;dot(c,xx,yy,radius+1,0xff193238);paint.setColor(kind==20?0xff77ddff:kind==21?0xffffe5b0:0xfff4f5e4);paint.setStrokeWidth(1.4f);
   if(kind==20){for(int a=0;a<3;a++){double t=a*Math.PI/3;c.drawLine(xx-(float)Math.cos(t)*radius,yy-(float)Math.sin(t)*radius,xx+(float)Math.cos(t)*radius,yy+(float)Math.sin(t)*radius,paint);}}
   else if(kind==21){Path q=new Path();q.moveTo(xx,yy-radius);q.lineTo(xx+radius,yy);q.lineTo(xx,yy+radius);q.lineTo(xx-radius,yy);q.close();c.drawPath(q,paint);}else c.drawRect(xx-radius*.7f,yy-radius*.7f,xx+radius*.7f,yy+radius*.7f,paint);
  }
  OperationsSnapshot ops=orders().facts();if(ops!=null){for(int k=0;k<ops.sites();k++){int b=ops.site(k);dot(c,x(ops.raw[b],r),y(ops.raw[b+1],r),large?6:4,ops.raw[b+2]<0?0xffecd6a1:TeamPalette.color(ops.raw[b+2]));if(large)label(c,""+(k+1),x(ops.raw[b],r),y(ops.raw[b+1],r)+3,8,0xff193238);}
   for(int g=0;g<3;g++){int b=ops.group(StaticBits.team,g);if(ops.raw[b]>0){dot(c,x(ops.raw[b+1],r),y(ops.raw[b+2],r),large?6:3.5f,TeamPalette.color(StaticBits.team));if(large)label(c,""+(char)('A'+g),x(ops.raw[b+1],r),y(ops.raw[b+2],r)+3,8,Color.WHITE);}}
  }
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(0xffd2e9df);c.drawRect(x((int)cam.left,r),y((int)(cam.bottom+cam.viewHeight),r),x((int)(cam.left+cam.viewWidth),r),y((int)cam.bottom,r),paint);paint.setStyle(Paint.Style.FILL);c.restore();
 }
 private int target(float x,float y){if(expanded){if(x>w-108&&x<w-43&&y>=12&&y<=40)return 41;int count=items().teleportReady()?4:3;for(int i=0;i<count;i++)if(y>=h-69&&y<=h-40&&x>=w/2-count*54+i*108&&x<=w/2-count*54+i*108+102)return 42+i;return 50;}
  if(y>=8&&y<=35){if(x>=10&&x<=140)return 99;if(x>=146&&x<=194)return 30;if(x>=200&&x<=248)return 31;for(int i=0;i<3;i++)if(x>=BattleHudLayout.utilityX(w,i)&&x<=BattleHudLayout.utilityX(w,i)+(i==0?50:48))return 32+i;}
  if(y>=8&&y<=42&&Math.abs(x-w/2)<=90)return 99;if(mini().contains(x,y))return 40;return slots.hit(x,y,w,h);
 }
 boolean blocks(float x,float y){if(!active())return false;layout();return target(x/u,y/u)>=0;}
 boolean back(){if(!expanded())return false;expanded=false;hit=-1;view.invalidate();return true;}
 boolean touch(MotionEvent e){if(!active())return false;layout();int action=e.getActionMasked();float x=e.getX()/u,y=e.getY()/u;
  if(action==MotionEvent.ACTION_DOWN){hit=target(x,y);if(hit<0)return false;downX=lastX=x;downY=lastY=y;moved=false;pinch=0;teleportNonce=items().pendingNonce();return true;}if(hit<0)return false;
  if(action==MotionEvent.ACTION_CANCEL){hit=-1;return true;}
  if(action==MotionEvent.ACTION_POINTER_DOWN){moved=true;if(expanded&&e.getPointerCount()>1)pinch=(float)Math.hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1));return true;}
  if(action==MotionEvent.ACTION_MOVE){if(Math.hypot(x-downX,y-downY)>6)moved=true;if(expanded&&hit==50){if(e.getPointerCount()>1){float n=(float)Math.hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1));if(pinch>0)zoom=Math.max(1,Math.min(4,zoom*n/pinch));pinch=n;}else{dx+=x-lastX;dy+=y-lastY;}RectF f=frame();dx=Math.max(-f.width()*(zoom-1)/2,Math.min(f.width()*(zoom-1)/2,dx));dy=Math.max(-f.height()*(zoom-1)/2,Math.min(f.height()*(zoom-1)/2,dy));}lastX=x;lastY=y;view.invalidate();return true;}
  if(action!=MotionEvent.ACTION_UP)return true;int clicked=hit;hit=-1;if(moved||clicked!=target(x,y))return true;view.performClick();
  if(clicked<2)orders().activeItem(clicked);
  else if(clicked==30)cam().toggle(cam().left+cam().viewWidth/2,cam().bottom+cam().viewHeight/2);
  else if(clicked==31)cam().detailToggle();else if(clicked==32)surface().audio().toggle();else if(clicked==33)items().showFrameOptions();else if(clicked==34)items().showGuide();
  else if(clicked==40){orders().cancelForModal();expanded=true;tab=items().teleportReady()?3:0;}
  else if(clicked==41)expanded=false;else if(clicked>=42&&clicked<=45)tab=clicked-42;
  else if(clicked==50&&frame().contains(x,y)){RectF r=image(frame(),true);int wx=Math.max(0,Math.min(cam().width-1,(int)((x-r.left)*cam().width/r.width()))),wy=Math.max(0,Math.min(cam().height-1,cam().height-1-(int)((y-r.top)*cam().height/r.height())));
   if(tab==1)cam().pan(wx,wy);else if(tab==2)orders().orderAt(wx,wy);else if(tab==3)items().chooseMapTeleport(wx,wy,teleportNonce);else{int[] data=items().facts();StringBuilder names=new StringBuilder();for(int k=0;k<SnapshotItems.slots(data);k++){int b=SnapshotItems.offset(data,k);if(data[b]>=0&&Math.hypot(x(data[b+1],r)-x,y(data[b+2],r)-y)<18){if(names.length()>0)names.append(" · ");names.append(items().itemName(data[b]));}}if(names.length()>0)Toast.makeText(view.getContext(),names,Toast.LENGTH_SHORT).show();}
  }surface().applyCamera();view.invalidate();return true;
 }
}
