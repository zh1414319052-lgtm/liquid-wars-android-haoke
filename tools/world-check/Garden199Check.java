package com.xenris.liquidwarsos;
/** The rendered zoom must keep touch/world round trips and map edges aligned. */
public final class Garden199Check {
 static int n;
 static void ok(boolean b,String why){n++;if(!b)throw new AssertionError(why);}
 public static void main(String[] args){
  for(float aspect:new float[]{16f/9,20f/9,4f/3})for(int x:new int[]{0,1632,3263})for(int y:new int[]{0,1024,2047}){
   WorldCamera c=new WorldCamera();c.viewport(aspect);c.world(3264,2048,1001,0,false,x,y);
   ok(!c.overview&&!c.detail&&c.viewWidth==560,"new match uses wider view");
   for(int z=0;z<3;z++){
    ok(c.left>=0&&c.bottom>=0&&c.left+c.viewWidth<=3264.01&&c.bottom+c.viewHeight<=2048.01,"viewport clamps to mainland");
    ok(Math.abs(c.viewWidth/c.viewHeight-aspect)<.001,"aspect preserved");
    for(int wx=(int)c.left+1;wx<c.left+c.viewWidth-1;wx+=31)for(int wy=(int)c.bottom+1;wy<c.bottom+c.viewHeight-1;wy+=29){
     ok(c.mapX(c.screenX(wx,2340),2340)==wx,"X touch/render agreement");
     ok(c.mapY(c.screenY(wy,1080),1080)==wy,"Y touch/render agreement");
    }
    c.detailToggle();ok(c.viewWidth==(z%2==0?200:560),"near then far");
   }
   c.world(3264,2048,1001,0,false,x,y);ok(c.viewWidth==560&&!c.detail,"restart resets manual near");
  }
  WorldCamera c=new WorldCamera();c.world(1600,960,54,0,false,800,480);ok(c.viewWidth==800,"2D large map preserved");c.detailToggle();ok(c.viewWidth==400,"2D close view preserved");
  c.world(800,480,1,0,false,400,240);ok(c.overview&&c.viewWidth==800&&c.viewHeight==480,"classic map preserved");
  System.out.println("PASS "+n+" Garden199 zoom, aspect, projection, bounds and restart checks");
 }
}
