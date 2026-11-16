package com.xenris.liquidwarsos;
import org.junit.Test;
import static org.junit.Assert.*;

public class Garden189CameraTest {
 @Test public void nearWideOverviewRetainWorldCoordinatesAndBounds(){
  for(float aspect:new float[]{16f/9,2340f/1080,4f/3}){
   WorldCamera c=new WorldCamera();c.viewport(aspect);c.world(3264,2048,1001,0,false,1600,1000);
   assertFalse(c.detail);assertEquals(400,c.viewWidth,.01);
   float close=c.viewWidth;c.touchTarget(1660,1030);
   for(int i=0;i<6;i++){
    c.detailToggle();assertTrue(c.following);assertFalse(c.overview);
    int x=(int)(c.left+c.viewWidth*.63),y=(int)(c.bottom+c.viewHeight*.42);
    assertEquals(x,c.mapX(c.screenX(x,2340),2340),1);
    assertEquals(y,c.mapY(c.screenY(y,1080),1080),1);
    c.pan(i%2==0?0:3263,i%2==0?0:2047);
    assertTrue(c.left>=0&&c.bottom>=0&&c.left+c.viewWidth<=3264&&c.bottom+c.viewHeight<=2048.01);
    c.toggle(1600,1000);
   }
   assertEquals(close,c.viewWidth,.01);
   c.toggle(1600,1000);assertTrue(c.overview);
   c.detailToggle();assertFalse(c.overview);assertTrue(c.viewWidth<3264);
   c.world(800,480,1,0,false,200,200);c.detailToggle();
   assertTrue(c.overview);assertEquals(800,c.viewWidth,.01);
  }
 }
}
