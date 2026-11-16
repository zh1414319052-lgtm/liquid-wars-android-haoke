package com.xenris.liquidwarsos;
import org.junit.Test;
import static org.junit.Assert.*;

/** Pure coordinate checks; deliberately does not drive the user's emulator. */
public class Garden187CameraTest {
 @Test public void sixfoldWorldAndFarCornerOrders(){
  for(float aspect:new float[]{16f/9,2340f/1080,4f/3}){
   WorldCamera c=new WorldCamera();c.viewport(aspect);c.world(2160,1000,1001,0,false,640,340);
   assertFalse(c.overview);assertEquals(800,c.viewWidth,.01);assertTrue(c.left>=0&&c.bottom>=0);
   c.toggle(640,340);assertTrue(c.overview);
   assertTrue(c.left<=0&&c.bottom<=0&&c.left+c.viewWidth>=2160&&c.bottom+c.viewHeight>=1000);
   for(int[] p:new int[][]{{1,1},{2158,998},{1080,500}}){
    assertEquals(p[0],c.mapX(c.screenX(p[0],2340),2340),1);
    assertEquals(p[1],c.mapY(c.screenY(p[1],1080),1080),1);
   }
   c.toggle(2159,999);assertFalse(c.overview);assertTrue(c.left+c.viewWidth<=2160.01&&c.bottom+c.viewHeight<=1000.01);
   c.pan(0,0);assertEquals(0,c.left,.01);assertEquals(0,c.bottom,.01);
  }
 }
 @Test public void minimapAndClassicDimensionsRemainConsistent(){
  MiniMapLayout m=new MiniMapLayout(2340,1080,2,2160,1000,false);
  assertEquals(2.16,m.width/m.height,.0001);
  assertEquals(0,m.x(m.left));assertEquals(999,m.y(m.top));
  assertEquals(2159,m.x(m.left+m.width));assertEquals(0,m.y(m.top+m.height));
  WorldCamera c=new WorldCamera();c.world(800,480,1,0,false,100,100);
  assertTrue(c.overview);assertEquals(800,c.viewWidth,.01);assertEquals(480,c.viewHeight,.01);
  c.world(1600,960,50,0,false,800,480);assertFalse(c.overview);assertEquals(800,c.viewWidth,.01);
 }
}
