package com.xenris.liquidwarsos;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class DualStick213Test {
    @Test public void previewDiameterIsDoubled(){assertEquals(232,JoystickPlacement.RADIUS_DP*2,0);}
    @Test public void sensitivityChangesPointSpeedAndPersists(){
        JoystickCursor slow=new JoystickCursor(),fast=new JoystickCursor();
        for(JoystickCursor c:new JoystickCursor[]{slow,fast}){c.bounds(1000,1000);c.target(200,200);c.axis(1,0);}
        for(int i=0;i<10;i++){slow.advance(.02f,.5f);fast.advance(.02f,2f);}
        assertEquals(4,(fast.x()-200)/(slow.x()-200),.01);
        SharedPreferences p=RuntimeEnvironment.getApplication().getSharedPreferences("sensitivity213",0);
        JoystickPlacement s=new JoystickPlacement();s.sensitivity(1.5f);s.save(p);s.sensitivity(.5f);s.load(p);assertEquals(1.5f,s.sensitivity(),0);
        s.sensitivity(Float.NaN);assertEquals(1,s.sensitivity(),0);s.defaults();assertEquals(1,s.sensitivity(),0);
    }
    @Test public void continuousStickUpdatesCannotDiscardCleanse(){
        OrderQueue q=new OrderQueue();OrderProtocol codec=new OrderProtocol();short[][] x=new short[6][5],y=new short[6][5];
        short[][] route=codec.packet(OrderProtocol.FOLLOW,3,0,java.util.Collections.singletonList(new int[]{40,80}),0);
        short[][] cleanse=codec.packet(OrderProtocol.ACTIVE,3,0,java.util.Collections.singletonList(new int[]{1,0}),0);
        q.offer(0,route[0],route[1],x,y);q.offer(0,cleanse[0],cleanse[1],x,y);
        for(int i=0;i<4;i++)q.offer(0,new short[]{(short)(120+i),-1,-1,-1,-1},new short[]{100,-1,-1,-1,-1},x,y);
        assertEquals(1,q.size(0));q.apply(x,y);assertArrayEquals(cleanse[0],x[0]);assertEquals(0,q.size(0));
    }
    @Test public void sizePersistsWithPlacementAndCancelRestoresBoth(){
        SharedPreferences p=RuntimeEnvironment.getApplication().getSharedPreferences("size213",0);
        JoystickPlacement layout=new JoystickPlacement();layout.defaults();layout.size(.75f);layout.move(0,180,220,960,440);layout.save(p);
        layout.size(1.25f);layout.move(0,300,300,960,440);layout.load(p);
        assertEquals(.75f,layout.size(),0);assertEquals(180,layout.x(0,960,58),.01);
        layout.size(10);assertEquals(1.25f,layout.size(),0);layout.size(0);assertEquals(.5f,layout.size(),0);
        layout.size(Float.NaN);assertEquals(1,layout.size(),0);
        layout.defaults();layout.save(p);layout.load(p);assertEquals(1,layout.size(),0);
    }
    @Test public void firstPadOwnsOnePointAndOtherTakesOver(){
        DualStickInput s=new DualStickInput();assertTrue(s.press(1,17));s.axis(1,1,0);
        assertTrue(s.press(0,3));s.axis(0,-1,0);
        assertEquals(1,s.owner());assertEquals(1,s.x(),0); // opposing thumbs never add or average
        s.lift(17);assertEquals(0,s.owner());assertEquals(-1,s.x(),0);
        s.lift(3);assertFalse(s.held());assertEquals(0,s.x(),0);
    }
    @Test public void duplicatePointersAndCancellationDoNotLeaveMotion(){
        DualStickInput s=new DualStickInput();assertTrue(s.press(0,9));assertFalse(s.press(1,9));assertFalse(s.press(0,12));
        s.axis(0,10,10);assertEquals(1,Math.hypot(s.x(),s.y()),.0001);
        s.clear();assertFalse(s.held());assertEquals(-1,s.sideOf(9));assertEquals(0,s.y(),0);
        s.press(1,0);s.axis(1,Float.NaN,1);assertEquals(0,s.x(),0);assertEquals(0,s.y(),0);
    }
    @Test public void savedPositionsScaleAndCancelDoesNotPersist(){
        SharedPreferences prefs=RuntimeEnvironment.getApplication().getSharedPreferences("layout213",Context.MODE_PRIVATE);
        JoystickPlacement p=new JoystickPlacement();p.move(0,180,220,960,440);p.move(1,800,210,960,440);p.save(prefs);
        JoystickPlacement restored=new JoystickPlacement();restored.load(prefs);
        assertEquals(360,restored.x(0,1920,58),.01);assertEquals(440,restored.y(0,880,58,100,700),.01);
        restored.move(0,400,300,960,440);restored.load(prefs);assertEquals(180,restored.x(0,960,58),.01);
        restored.defaults();restored.save(prefs);p.load(prefs);assertEquals(76,p.x(0,960,58),.01);assertEquals(884,p.x(1,960,58),.01);
    }
    @Test public void centresCannotLeaveScreenOrOverlapSides(){
        JoystickPlacement p=new JoystickPlacement();p.move(0,-300,-200,960,440);p.move(1,10000,10000,960,440);
        assertTrue(p.x(0,960,58)>=66);assertTrue(p.x(1,960,58)<=894);
        assertEquals(140,p.y(0,440,58,140,300),0);assertEquals(300,p.y(1,440,58,140,300),0);
    }
}
