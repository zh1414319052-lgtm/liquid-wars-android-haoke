package com.xenris.liquidwarsos;
public final class Comfort178Check {
 static int n;static void ok(boolean b,String why){n++;if(!b)throw new AssertionError(why);}
 static WorldCamera camera(){WorldCamera c=new WorldCamera();c.viewport(2.2f);c.world(1600,960,54,0,false,800,480);return c;}
 static void held(WorldCamera c,float x,float y){c.beginTouch(19);c.trackTouch(0,19,x,y);c.finishTouchTargets(1);c.touchTarget(c.mapX(x,1),c.mapY(y,1));}
 static float run(int hz){WorldCamera c=camera();held(c,.85f,.5f);float start=c.left;short[] x=new short[5],y=new short[5];c.frame(1_000_000_000L);
  for(int k=1;k<=hz*8;k++){c.frame(1_000_000_000L+k*1_000_000_000L/hz);c.fillTouchTargets(x,y);c.touchTarget(x[0],y[0]);ok(Math.abs(c.screenX(x[0],1000)-850)<1.3f,"held target remains at actual screen location");}
  ok(c.left>start+20&&c.left<start+130,"stationary army permits modest lookahead, never travels to map edge");float stopped=c.left;
  for(int k=hz*8+1;k<=hz*12;k++)c.frame(1_000_000_000L+k*1_000_000_000L/hz);ok(Math.abs(c.left-stopped)<.5f,"no accumulating feedback while blocked");
  c.endTouch();short target=x[0];float released=c.left;for(int k=hz*12+1;k<hz*14;k++){c.frame(1_000_000_000L+k*1_000_000_000L/hz);ok(!c.fillTouchTargets(x,y),"no command after release");}ok(x[0]==target,"retain final world point");ok(Math.abs(c.left-released)<45,"release does not rush to the distant point");return stopped-start;
 }
 public static void main(String[] args){float lo=999,hi=0;for(int hz:new int[]{30,60,90,120}){float d=run(hz);lo=Math.min(lo,d);hi=Math.max(hi,d);}ok(hi-lo<2,"same pan extent at all refresh rates");
  WorldCamera c=camera();held(c,.52f,.49f);c.frame(1_000_000_000L);for(int k=1;k<600;k++){c.trackTouch(0,19,.5f+(k%2==0?.03f:-.03f),.5f);c.frame(1_000_000_000L+k*16_666_667L);}ok(c.left==400&&Math.abs(c.bottom-(480-c.viewHeight/2))<.01f,"centre tremor does not move screen");
  // Real army moves on both axes; held input alone cannot create the same unbounded travel.
  for(boolean vertical:new boolean[]{false,true}){c=camera();held(c,vertical?.5f:.9f,vertical?.1f:.5f);c.frame(1_000_000_000L);float oldx=c.left,oldy=c.bottom;double maxSpeed=0;for(int k=1;k<=240;k++){float ax=800+(vertical?0:k*1.2f),ay=480+(vertical?k*.7f:0);c.follow(ax,ay);c.frame(1_000_000_000L+k*16_666_667L);maxSpeed=Math.max(maxSpeed,Math.hypot(c.left-oldx,c.bottom-oldy)*60);oldx=c.left;oldy=c.bottom;ok(ax>=c.left&&ax<c.left+c.viewWidth&&ay>=c.bottom&&ay<c.bottom+c.viewHeight,"moving army remains in view");}ok(maxSpeed<=177,"speed remains bounded");}
  c=camera();held(c,.9f,.5f);c.frame(1_000_000_000L);for(int k=1;k<20;k++)c.frame(1_000_000_000L+k*16_666_667L);float before=c.left;c.trackTouch(0,19,.1f,.5f);c.frame(1_333_333_340L);ok(Math.abs(c.left-before)<3,"reversal never jumps");for(int k=21;k<=160;k++)c.frame(1_000_000_000L+k*16_666_667L);ok(c.left<400,"same gesture smoothly reverses");
  float x=c.left;c.frame(30_000_000_000L);ok(c.left==x,"stalled frame cannot jump");c.resetSmoothing();c.frame(31_000_000_000L);ok(c.left==x,"resume starts without catch-up");
  c.clearTarget();c.pan(1300,700);float manual=c.left;for(int k=1;k<100;k++){c.follow(200,200);c.frame(32_000_000_000L+k*16_666_667L);}ok(c.left==manual&&!c.following,"manual minimap inspection not pulled back");short[] xs=new short[5],ys=new short[5];ok(!c.fillTouchTargets(xs,ys),"inspection produces no controls");
  c.relocate(210,220);ok(!c.touching()&&c.following&&c.left==0,"teleport cancels old gesture and places destination without cross-map sweep");
  int[] ids={0,3,7,11,19};for(int k=0;k<5;k++){c.beginTouch(ids[k]);c.trackTouch(k,ids[k],.2f+k*.14f,.45f);}c.finishTouchTargets(5);c.fillTouchTargets(xs,ys);for(int k=0;k<5;k++)ok(xs[k]==c.mapX(.2f+k*.14f,1),"all pointer IDs convert with displayed viewport");c.endTouch(0);for(int k=0;k<4;k++)c.trackTouch(k,ids[k+1],.34f+k*.14f,.45f);c.finishTouchTargets(4);c.fillTouchTargets(xs,ys);ok(xs[4]==-1&&xs[0]==c.mapX(.34f,1),"lifting primary preserves remaining finger mapping");
  c.world(1600,960,54,0,false,800,20);held(c,.9f,.5f);c.frame(1_000_000_000L);float old=c.left;for(int k=1;k<90;k++){c.frame(1_000_000_000L+k*16_666_667L);ok(Math.abs(c.left-old)*60<97,"bottom edge does not trigger horizontal catch-up boost");old=c.left;}
  c.world(800,480,1,0,false,400,240);held(c,.9f,.1f);c.frame(1_000_000_000L);c.frame(1_016_666_667L);ok(c.left==0&&c.bottom==0&&c.overview,"ordinary maps unchanged");
  System.out.println("PASS "+n+" comfort/camera/30-60-90-120Hz/5pointer/lifecycle checks; stationary lead "+lo+".."+hi);
 }
}
