package com.xenris.liquidwarsos;
public final class Controls175Check{
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 static void close(float a,float b,float eps,String s){ok(Math.abs(a-b)<=eps,s+" "+a+" != "+b);}
 static TouchCursor make(){TouchCursor c=new TouchCursor();c.bounds(1600,960);c.grab(500,400);c.observe(500,400,0,false);c.axis(1,0);return c;}
 public static void main(String[]args){
  for(int hz:new int[]{30,60,120}){
   TouchCursor c=make();float maxGap=0;
   for(int k=1;k<=hz*12;k++){float mass=500+k*25f/hz;c.observe(mass,400,Math.round(k*1000f/hz),false);c.advance(1f/hz);float gap=c.x()-mass;maxGap=Math.max(gap,maxGap);ok(gap<=40.001f,"moving army lead bounded");close(c.y(),400,0,"horizontal never drifts");}
   close(c.x(),840,.1f,"steady target matches 25cell/sec army progress");ok(maxGap<=40.001,"no growing lead with hold duration");
  }
  TouchCursor c=make();for(int k=1;k<=1000;k++){c.observe(500,400,k*10,false);c.advance(.01f);}close(c.x(),540,.001f,"blocked army waits");
  float stop=c.x();for(int k=0;k<1000;k++)c.advance(.016f);close(c.x(),stop,0,"no simulation progress no travel");
  c.axis(0,1);for(int k=1001;k<=2000;k++){c.observe(500,400,k*10,false);c.advance(.01f);}close(c.x(),540,.001f,"turn at lead cap no radial pull");close(c.y(),440,.001f,"turn can proceed while forward blocked");
  c.axis(-1,0);for(int k=2001;k<=2100;k++){c.observe(500,400,k*10,false);c.advance(.01f);}close(c.x(),460,.001f,"reverse responsive and limited");close(c.y(),440,.001f,"reverse no sideways correction");
  c.target(1200,700);c.grab(500,400);c.observe(500,400,0,false);c.axis(1,1);for(int k=1;k<=100;k++){c.observe(500,400,k*10,false);c.advance(.01f);}close(c.x(),1200,0,"far direct click not extended");close(c.y(),700,0,"far click not snapped");
  c.axis(-1,0);c.observe(500,400,1010,false);ok(c.advance(.01f)&&c.x()<1200,"far click can move back");float x=c.x();c.release();c.observe(600,400,5000,false);ok(!c.advance(.05f),"release");close(c.x(),x,0,"release holds");
  c=make();for(int k=1;k<=200;k++){c.observe(500+k*.2f,400,k*10,true);ok(!c.advance(.01f),"freeze cannot move point");}close(c.x(),500,0,"freeze retained");c.observe(540,400,2010,false);ok(c.advance(.01f),"unfreeze moves again");
  c=make();c.observe(600,400,200000,false);c.advance(10);ok(c.x()<=504.501f,"no reconnect time burst");c.observe(500,400,0,false);x=c.x();ok(!c.advance(.05f),"rewound clock no credit");close(c.x(),x,0,"rewind observation cannot pull point");
  c=make();c.mapScale(.75f);for(int k=1;k<=100;k++){c.observe(500,400,k*10,false);c.advance(.01f);}close(c.x(),530,.001f,"small-map horizontal screen scale");
  c=make();c.axis(.25f,0);for(int k=1;k<=100;k++){c.observe(500,400,k*10,false);c.advance(.01f);}ok(c.x()>501&&c.x()<510,"light push remains precise");
  c=make();c.axis(Float.NaN,1);c.observe(500,400,10,false);ok(!c.advance(.01f),"invalid axis");c.target(Float.NaN,4);ok(Float.isFinite(c.x()),"invalid target");c.clear();ok(!c.ready()&&!c.active(),"new match reset");
  System.out.println("PASS "+n+" army-paced target/no runaway/blocked/turn/release/freeze/sim clock/frame-rate checks");
 }
}