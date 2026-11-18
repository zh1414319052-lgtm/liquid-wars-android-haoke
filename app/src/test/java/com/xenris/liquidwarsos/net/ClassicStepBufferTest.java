package com.xenris.liquidwarsos.net;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClassicStepBufferTest {
    @Test public void burstPreservesEveryStepAndControlOrder() throws Exception {
        ClassicStepBuffer q=new ClassicStepBuffer(7);
        for(int i=0;i<50;i++)assertTrue(q.offer(new int[]{7,i}));
        assertTrue(q.offer(new int[]{9,123}));
        assertEquals(50,q.pendingSteps());
        for(int i=0;i<50;i++)assertArrayEquals(new int[]{7,i},q.poll(1));
        assertEquals(0,q.pendingSteps());
        assertArrayEquals(new int[]{9,123},q.poll(1));
        assertNull(q.poll(1));
    }
    @Test public void overflowFailsExplicitlyAndCloseDrains() throws Exception {
        ClassicStepBuffer q=new ClassicStepBuffer(7);
        for(int i=0;i<ClassicStepBuffer.CAPACITY;i++)assertTrue(q.offer(new int[]{7,i}));
        assertFalse(q.offer(new int[]{7,999}));q.close();
        assertEquals(0,q.pendingSteps());assertNull(q.poll(100));
        assertTrue(q.offer(new int[]{7,1000}));assertNull(q.poll(1));
    }
    @Test public void closeWakesSleepingConsumer() throws Exception {
        ClassicStepBuffer q=new ClassicStepBuffer(7);
        Thread consumer=new Thread(()->{try{assertNull(q.poll(10000));}catch(InterruptedException e){throw new AssertionError(e);}});
        consumer.start();q.close();consumer.join(1000);assertFalse(consumer.isAlive());
    }
    @Test public void catchupNeverChangesTickCountOrUnnecessarilySpeedsStableStream() {
        assertEquals(7000,ClassicStepBuffer.tickMicros(7000,0));
        assertEquals(7000,ClassicStepBuffer.tickMicros(7000,1));
        assertEquals(6300,ClassicStepBuffer.tickMicros(7000,2));
        assertEquals(3500,ClassicStepBuffer.tickMicros(7000,100));
    }
    @Test public void transportDelayDoesNotCauseCubicHostStall() {
        assertEquals(8,ClassicStepBuffer.hostWindow(0));
        int window=ClassicStepBuffer.hostWindow(600);
        assertTrue(window>=20);assertEquals(0,ClassicStepBuffer.hostDelayMs(15,window));
        assertEquals(20,ClassicStepBuffer.hostDelayMs(45,window));
        assertEquals(32,ClassicStepBuffer.hostWindow(10000));
    }
    @Test public void threeSecondNetworkSpikeCanRecoverWithoutBeingKicked(){
        assertFalse(ClassicStepBuffer.shouldDisconnect(47,3000,3000));
        assertFalse(ClassicStepBuffer.shouldDisconnect(80,500,500));
        assertEquals(70,ClassicStepBuffer.hostDelayMs(60,16));
        assertTrue(ClassicStepBuffer.shouldDisconnect(60,21000,21000));
        assertTrue(ClassicStepBuffer.shouldDisconnect(100,500,21000));
    }
}
