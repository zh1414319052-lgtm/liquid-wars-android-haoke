// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.xenris.liquidwarsos.TeamPalette;

/** Static menu illustration. No simulation, RNG, timers, network or touch handling. */
public final class ArenaArtView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[][] streams = new float[6][160];

    public ArenaArtView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        for (int team=0;team<6;team++) for (int i=0;i<80;i++) {
            float t=(i+.35f*team)/82f;
            streams[team][i*2]=w*(.025f+.95f*t);
            streams[team][i*2+1]=h*(.50f+.25f*(float)Math.sin(t*6.2+team*.42)+.025f*(float)Math.sin(i*13+team));
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff52636d);
        float w=getWidth(),h=getHeight();
        canvas.drawOval(w*.28f,h*.48f,w*.40f,h*.70f,paint);
        canvas.drawOval(w*.66f,h*.27f,w*.78f,h*.48f,paint);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(2f,2.4f*getResources().getDisplayMetrics().density));
        for(int team=0;team<6;team++) {
            paint.setColor(TeamPalette.color(team));
            canvas.drawPoints(streams[team],paint);
        }
    }
}
