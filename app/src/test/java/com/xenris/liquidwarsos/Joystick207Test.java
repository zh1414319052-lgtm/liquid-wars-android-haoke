package com.xenris.liquidwarsos;
import org.junit.Test;import static org.junit.Assert.*;
public class Joystick207Test {
 @Test public void onlyCommandPointMovesAndReleaseKeepsIt(){JoystickCursor c=new JoystickCursor();c.bounds(3264,2048);c.target(100,100);c.axis(1,0);for(int i=0;i<20;i++)c.advance(.05f);assertEquals(250,c.x(),.01);assertEquals(100,c.y(),0);c.release();assertFalse(c.advance(.05f));assertEquals(250,c.x(),.01);}
 @Test public void deadzoneBoundsAndInterruptedFrames(){JoystickCursor c=new JoystickCursor();c.bounds(800,480);c.target(790,470);c.axis(.1f,0);assertFalse(c.advance(.05f));c.axis(1,1);c.advance(30);assertTrue(c.x()<796);for(int i=0;i<10;i++)c.advance(.05f);assertEquals(799,c.x(),0);assertEquals(479,c.y(),0);c.clear();assertFalse(c.ready());assertFalse(c.advance(.05f));}
 @Test public void particleConversionStaysSilentWithoutMutingItems(){AudioEvents a=new AudioEvents();assertFalse(a.allow(AudioEvents.SWALLOW,0));assertFalse(a.allow(AudioEvents.SWALLOW,3000));assertTrue(a.allow(AudioEvents.ITEM,3000));a.reset();assertFalse(a.allow(AudioEvents.SWALLOW,0));assertTrue(a.allow(AudioEvents.DEVOUR,0));}
}
