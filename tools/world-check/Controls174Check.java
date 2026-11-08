package com.xenris.liquidwarsos;
public final class Controls174Check {
 static int n;static void ok(boolean yes,String why){n++;if(!yes)throw new AssertionError(why);}static void near(float a,float b,float tolerance,String why){ok(Math.abs(a-b)<=tolerance,why+" "+a+" "+b);}
 static void frames(WorldCamera c,long start){for(int k=0;k<120;k++)c.frame(start+k*8333333L);}
 static TouchCursor cursor(){TouchCursor c=new TouchCursor();c.bounds(1600,960);c.target(600,400);c.grab(600,400);return c;}
 public static void main(String[] args){
  for(float density:new float[]{1,1.5f,2,3})for(float ratio:new float[]{16f/9,2.2f,2.55f}){
   StickLayout l=new StickLayout(400*density*ratio,400*density,density);near(l.radius/density,64,.001f,"128dp diameter");
   ok(l.leftX-l.hitRadius>=20*density&&l.y+l.hitRadius<=400*density-16*density,"safe fixed bases");
   ok(l.side(l.leftX,l.y)==0&&l.side(l.rightX,l.y)==1&&l.side((l.leftX+l.rightX)/2,l.y)==-1,"consistent hit regions");
  }
  TouchCursor a=cursor();a.axis(.09f,0);ok(!a.advance(.02f)&&a.x()==600,"resting thumb no drift");
  a.axis(.25f,0);for(int i=0;i<50;i++)a.advance(.02f);float fine=a.x()-600;ok(fine>5&&fine<30,"fine movement available");
  a=cursor();a.axis(1,0);for(int i=0;i<60;i++)a.advance(1f/60);near(a.x(),860,.02f,"full speed travel beyond old 88 tether");near(a.y(),400,0,"no unintended perpendicular movement");
  a.release();a.grab(200,200);near(a.x(),860,.02f,"handoff keeps far target");a.followMass(20,900);near(a.x(),860,.02f,"army movement cannot recenter target");
  a.axis(0,1);for(int i=0;i<60;i++)a.advance(1f/60);near(a.x(),860,.02f,"pure vertical does not pull x back");near(a.y(),660,.02f,"vertical travel");
  a.release();float held=a.y();a.advance(1);near(a.y(),held,0,"release stops point");
  TouchCursor b=cursor();b.axis(1,1);for(int i=0;i<120;i++)b.advance(1f/120);near((float)Math.hypot(b.x()-600,b.y()-400),260,.06f,"diagonal normalized");
  TouchCursor c=cursor();c.axis(1,0);for(int i=0;i<120;i++)c.advance(1f/120);near(c.x(),860,.05f,"60/120 hz same speed");
  c.target(1595,955);c.grab(5,5);c.axis(1,1);for(int i=0;i<60;i++)c.advance(.02f);ok(c.mapX()==1599&&c.mapY()==959,"map edge clamp");
  c.axis(Float.NaN,1);ok(!c.advance(.02f),"bad axis ignored");c.target(Float.NaN,5);ok(c.mapX()==1599,"bad target ignored");
  WorldCamera cam=new WorldCamera();cam.viewport(2.2f);cam.world(1600,960,54,0,false,800,480);cam.beginTouch(3);float left=cam.left,bottom=cam.bottom;
  int x=cam.touchX(700,1000,3),y=cam.touchY(250,500,3);cam.touchTarget(x,y);frames(cam,1000000000L);
  near(cam.left,left,0,"drag camera stable");near(cam.bottom,bottom,0,"drag y camera stable");
  for(int id:new int[]{0,7,11,19}){cam.beginTouch(id);ok(cam.touchX(650,1000,id)==cam.mapX(650,1000),"new finger matches visible image");}
  int dragged=cam.touchX(800,1000,3);cam.touchTarget(dragged,y);near(cam.screenX(dragged,1000),800,1.3f,"screen target under finger after drag");
  cam.endTouch(3);frames(cam,3000000000L);near(cam.left,left,0,"remaining fingers keep camera still");cam.endTouch();frames(cam,5000000000L);ok(cam.left>left+100,"release resumes target following");
  cam.pan(300,300);left=cam.left;cam.followTarget(1200,800);frames(cam,7000000000L);near(cam.left,left,0,"minimap pan remains manual");
  cam.toggle(800,480);cam.toggle(800,480);ok(cam.overview,"overview");cam.beginTouch(19);float vw=cam.viewWidth;int over=cam.touchX(600,1000,19);cam.touchTarget(over,400);near(cam.viewWidth,vw,0,"overview drag stays exact");cam.endTouch();ok(!cam.overview&&cam.viewWidth==800,"release overview zoom");
  for(int yy=20;yy<940;yy+=45)for(int xx=20;xx<1580;xx+=65){cam.focus(xx,yy);ok(cam.mapX(cam.screenX(xx,1240),1240)==xx&&cam.mapY(cam.screenY(yy,560),560)==yy,"integer cell screen round trip");}
  cam.world(800,480,1,0,false,400,240);cam.beginTouch();cam.touchTarget(600,300);frames(cam,9000000000L);cam.endTouch();near(cam.left,0,0,"small map unchanged");
  System.out.println("PASS "+n+" size/safe-area/input direction/speed/release/camera/5pointer/projection checks");
 }
}