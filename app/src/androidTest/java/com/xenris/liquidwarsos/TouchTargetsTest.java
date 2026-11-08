package com.xenris.liquidwarsos;

import static org.junit.Assert.assertArrayEquals;

import android.view.MotionEvent;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TouchTargetsTest {
    private final short[] xs = new short[5];
    private final short[] ys = new short[5];

    @Test
    public void liftingFirstFingerPromotesRemainingTargetsRegardlessOfIds() {
        update(MotionEvent.ACTION_POINTER_UP, new int[]{17, 2, 9}, new float[]{100, 200, 300}, 200);
        assertArrayEquals(new short[]{200, 300, -1, -1, -1}, xs);
        assertArrayEquals(new short[]{279, 279, -1, -1, -1}, ys);
    }

    @Test
    public void sixthFingerCanReplaceALiftedTarget() {
        int action = MotionEvent.ACTION_POINTER_UP | (2 << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
        update(action, new int[]{0, 1, 12, 13, 4, 29}, new float[]{20, 40, 60, 80, 100, 120}, 80);
        assertArrayEquals(new short[]{20, 40, 80, 100, 120}, xs);
        update(MotionEvent.ACTION_CANCEL, new int[]{0, 1, 13}, new float[]{21, 41, 81}, 88);
        assertArrayEquals(new short[]{20, -1, -1, -1, -1}, xs);
        assertArrayEquals(new short[]{399, -1, -1, -1, -1}, ys);
    }

    @Test
    public void finalLiftKeepsDestinationAndClampsEdges() {
        update(MotionEvent.ACTION_UP, new int[]{29}, new float[]{810}, -3);
        assertArrayEquals(new short[]{799, -1, -1, -1, -1}, xs);
        assertArrayEquals(new short[]{479, -1, -1, -1, -1}, ys);
    }

    private void update(int action, int[] ids, float[] positions, float y) {
        MotionEvent.PointerProperties[] pointers = new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[ids.length];
        for (int index = 0; index < ids.length; index++) {
            pointers[index] = new MotionEvent.PointerProperties();
            pointers[index].id = ids[index];
            pointers[index].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coordinates[index] = new MotionEvent.PointerCoords();
            coordinates[index].x = positions[index];
            coordinates[index].y = y;
        }
        MotionEvent event = MotionEvent.obtain(0, 1, action, ids.length, pointers, coordinates,
                0, 0, 1, 1, 0, 0, 0, 0);
        try {
            TouchTargets.update(event, 800, 480, xs, ys);
        } finally {
            event.recycle();
        }
    }
}
