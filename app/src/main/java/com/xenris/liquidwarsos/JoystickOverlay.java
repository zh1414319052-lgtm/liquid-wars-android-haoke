// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.content.Context;import android.graphics.*;import android.util.AttributeSet;import android.view.*;
/** Optional, shared 2D/Godot HUD. Feeds the existing FOLLOW command protocol. */
public final class JoystickOverlay extends View {
 private final Paint paint=new Paint(3);private final float dp;final JoystickCursor cursor=new JoystickCursor();
 private int pointer=-1,secondary=-1,secondarySlot=-1,epoch=-1,generation=-1,group=-1,lastX=-1,lastY=-1;private long lastFrame,sentAt;private float knobX,knobY;private boolean drawnEnabled;
 public JoystickOverlay(Context c,AttributeSet a){super(c,a);dp=getResources().getDisplayMetrics().density;setContentDescription(BattleText.t("摇杆移动指挥点","Joystick moves command point"));}
 private View root(){return (View)getParent();}
 private TacticalOverlay orders(){return root().findViewById(R.id.tactical_overlay);}
 private MyGLSurfaceView surface(){return root().findViewById(R.id.mySurfaceView);}
 private boolean enabled(){return getParent()!=null&&orders().joystickEnabled()&&!((android.app.Activity)getContext()).getIntent().getBooleanExtra("spectator",false);}
 float padX(){return 61*dp;}float padY(){return orders().controlsTop()-56*dp;}
 private boolean inPad(float x,float y){return Math.hypot(x-padX(),y-padY())<=48*dp;}
 private void release(){pointer=secondary=-1;secondarySlot=-1;knobX=knobY=0;cursor.release();lastFrame=0;invalidate();}
 void clear(){release();cursor.clear();lastX=lastY=-1;}
 void cancelForModal(){release();}
 void frame(long nanos){
  if(!enabled()){if(drawnEnabled){drawnEnabled=false;invalidate();}if(cursor.ready()||pointer>=0)clear();return;}
  View intro=root().findViewById(R.id.match_intro);boolean show=intro==null||intro.getVisibility()!=VISIBLE;
  // Mode changes and countdown completion must reveal the pad before its first touch.
  if(show!=drawnEnabled){drawnEnabled=show;invalidate();}
  OperationsSnapshot s=orders().facts();if(s==null)return;int team=StaticBits.team,g=orders().selectedGroup();
  if(epoch!=s.epoch()||generation!=s.generation(team)||group!=g){clear();epoch=s.epoch();generation=s.generation(team);group=g;}
  int count=0;for(int k=0;k<3;k++)if(g==3||k==g)count+=s.raw[s.group(team,k)];if(count==0){clear();return;}
  float dt=lastFrame==0?0:Math.min(.05f,(nanos-lastFrame)/1e9f);lastFrame=nanos;
  if(pointer>=0&&hasWindowFocus()&&cursor.advance(dt))send(false);
  if(pointer>=0||cursor.ready())invalidate();
 }
 private void begin(){
  OperationsSnapshot s=orders().facts();if(s==null)return;int t=StaticBits.team,g=orders().selectedGroup();WorldCamera cam=surface().camera;cursor.bounds(cam.width,cam.height);
  if(!cursor.ready()){long x=0,y=0,n=0;for(int k=0;k<3;k++)if(g==3||k==g){int b=s.group(t,k),count=s.raw[b];n+=count;x+=(long)s.raw[b+1]*count;y+=(long)s.raw[b+2]*count;}
   if(n==0)return;cursor.target(x/(float)n,y/(float)n);
   int b=s.group(t,g==3?0:g);if((g<3||!s.split(t))&&s.raw[b+3]!=0&&s.raw[b+4]==0&&s.raw[b+7]==1)cursor.target(s.raw[b+8],s.raw[b+9]);
  }
  surface().camera.endTouch();lastFrame=0;lastX=lastY=-1;
 }
 private void send(boolean force){long now=android.os.SystemClock.uptimeMillis();int x=Math.round(cursor.x()),y=Math.round(cursor.y());if(!cursor.ready()||(!force&&now-sentAt<50)||(x==lastX&&y==lastY))return;orders().joystickAt(x,y);lastX=x;lastY=y;sentAt=now;}
 private void axis(float x,float y){float dx=x-padX(),dy=y-padY(),n=(float)Math.hypot(dx,dy),r=34*dp;float scale=n>r?r/n:1;knobX=dx*scale;knobY=dy*scale;cursor.axis(knobX/r,-knobY/r);invalidate();}
 private int itemAt(float x,float y){float u=(GardenSession.expeditionSeats>0||surface().camera.large())?BattleHudLayout.unit(getWidth(),getHeight()):dp;return ActiveSlots.hit(x/u,y/u,getWidth()/u,getHeight()/u);}
 private void useItem(int slot){if(GardenSession.expeditionSeats>0)((ExpeditionOverlay)root().findViewById(R.id.expedition_overlay)).joystickItem(slot);else orders().activeItem(slot);}
 @Override protected void onDraw(Canvas c){if(!enabled()||orders().facts()==null)return;View intro=root().findViewById(R.id.match_intro);if(intro!=null&&intro.getVisibility()==VISIBLE)return;
  paint.setStyle(Paint.Style.FILL);paint.setColor(0x32172730);c.drawCircle(padX(),padY(),40*dp,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp);paint.setColor(0x7896b9b0);c.drawCircle(padX(),padY(),40*dp,paint);
  paint.setStyle(Paint.Style.FILL);paint.setColor(pointer>=0?0xb3b6d9ce:0x7392b1a5);c.drawCircle(padX()+knobX,padY()+knobY,17*dp,paint);
  if(cursor.ready()){MyGLSurfaceView v=surface();float x=v.getLeft()+v.camera.screenX(cursor.x(),v.getWidth()),y=v.getTop()+v.camera.screenY(cursor.y(),v.getHeight());c.save();c.clipRect(v.getLeft(),v.getTop(),v.getRight(),v.getBottom());paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.2f*dp);paint.setColor(0xc5e9d797);c.drawCircle(x,y,6*dp,paint);c.drawLine(x-10*dp,y,x-3*dp,y,paint);c.drawLine(x+3*dp,y,x+10*dp,y,paint);c.drawLine(x,y-10*dp,x,y-3*dp,paint);c.drawLine(x,y+3*dp,x,y+10*dp,paint);c.restore();}
 }
 @Override public boolean onTouchEvent(MotionEvent e){if(!enabled())return false;int action=e.getActionMasked(),index=e.getActionIndex();
  if(action==MotionEvent.ACTION_DOWN){if(!inPad(e.getX(),e.getY()))return false;begin();if(!cursor.ready())return false;pointer=e.getPointerId(0);axis(e.getX(),e.getY());return true;}
  if(pointer<0)return false;
  if(action==MotionEvent.ACTION_CANCEL){release();return true;}
  if(action==MotionEvent.ACTION_POINTER_DOWN){int slot=itemAt(e.getX(index),e.getY(index));if(slot>=0){secondary=e.getPointerId(index);secondarySlot=slot;}return true;}
  if(action==MotionEvent.ACTION_POINTER_UP){int id=e.getPointerId(index);if(id==secondary){if(secondarySlot==itemAt(e.getX(index),e.getY(index)))useItem(secondarySlot);secondary=-1;secondarySlot=-1;}else if(id==pointer){send(true);release();}return true;}
  int at=e.findPointerIndex(pointer);if(at<0){release();return true;}if(action==MotionEvent.ACTION_MOVE)axis(e.getX(at),e.getY(at));
  if(action==MotionEvent.ACTION_UP){send(true);release();performClick();}return true;
 }
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(!focus)release();}
 @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
 @Override public boolean performClick(){super.performClick();return true;}
}
