// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import java.io.IOException;
import java.io.InputStream;

/** Projects the approved Pixelbolt sprite at the authoritative map center. */
final class BlackHoleVisual {
    static final int FACTS = 0x4c574248;
    static final int CLASSIC_PICKUP = 22;
    private static final int FRAME_SIZE = 96, ATLAS_STRIDE = 98, ATLAS_COLUMNS = 8;
    private static final int FRAME_COUNT = 48, FRAME_MS = 60;
    private static Bitmap sharedAtlas;
    private final Context context;
    private final Paint paint = new Paint();
    private final Paint particles = new Paint();
    private final Rect source = new Rect();
    private final RectF destination = new RectF();

    BlackHoleVisual(Context context) {
        this.context = context.getApplicationContext();
        paint.setFilterBitmap(false);
    }

    static Bitmap load(Context context, String file) {
        try (InputStream input = context.getAssets().open("expedition/" + file + ".png")) {
            return BitmapFactory.decodeStream(input);
        } catch (IOException unavailable) {
            return null;
        }
    }

    private static synchronized Bitmap atlas(Context context) {
        if (sharedAtlas == null) sharedAtlas = load(context, "black-hole-atlas");
        return sharedAtlas;
    }

    void draw(Canvas canvas, WorldCamera camera, float left, float top, float width, float height,
              float density, int time, int start, int until, int absorbed, int lastAbsorb) {
        if (until <= time || time < start) return;
        float x = left + camera.screenX(camera.width / 2, width);
        float y = top + camera.screenY(camera.height / 2, height);
        int age = time - start;
        float half = Math.min(98 * density, Math.max(21 * density, 26 * width / camera.viewWidth));
        half *= .68f + .32f * Math.min(1f, age / 220f);
        float reach = half * 1.65f;
        if (x + reach < left || x - reach > left + width || y + reach < top || y - reach > top + height) return;
        Bitmap image = atlas(context);
        if (image == null) return;
        int frame = age / FRAME_MS % FRAME_COUNT;
        int sx = 1 + frame % ATLAS_COLUMNS * ATLAS_STRIDE;
        int sy = 1 + frame / ATLAS_COLUMNS * ATLAS_STRIDE;
        source.set(sx, sy, sx + FRAME_SIZE, sy + FRAME_SIZE);
        destination.set(x - half, y - half, x + half, y + half);
        int alpha = Math.min(255, (until - time) * 255 / 220);
        float pixel = Math.max(1, Math.round(half / 26));
        suction(canvas, x, y, half, pixel, age, alpha);
        paint.setAlpha(alpha);
        canvas.drawBitmap(image, source, destination, paint);
        paint.setAlpha(255);
        int pulse = time - lastAbsorb;
        if (absorbed > 0 && lastAbsorb >= start && pulse >= 0 && pulse < 320) {
            absorption(canvas, x, y, half, pixel, pulse / 320f, alpha);
        }
    }

    private void square(Canvas canvas, float x, float y, float pixel) {
        x = Math.round(x / pixel) * pixel;
        y = Math.round(y / pixel) * pixel;
        canvas.drawRect(x, y, x + pixel, y + pixel, particles);
    }

    private void suction(Canvas canvas, float x, float y, float half, float pixel, int age, int alpha) {
        for (int i = 0; i < 16; i++) {
            float phase = ((age + i * 211) % 1150) / 1150f;
            float angle = i * 2.39996f + 2.9f * phase * phase;
            float taper = Math.min(1, Math.min(phase * 6, (1 - phase) * 5));
            for (int tail = 2; tail >= 0; tail--) {
                float radius = half * (1.52f - 1.34f * phase * phase + tail * .061f);
                float bearing = angle - tail * .058f;
                particles.setColor(i % 5 == 0 ? 0xffffc77f : 0xffc8a0f4);
                particles.setAlpha((int)(alpha * taper * (.80f - tail * .23f)));
                square(canvas, x + radius * (float)Math.cos(bearing), y + radius * (float)Math.sin(bearing), pixel);
            }
        }
    }

    private void absorption(Canvas canvas, float x, float y, float half, float pixel, float progress, int alpha) {
        float radius = half * (.08f + .34f * (1 - progress) * (1 - progress));
        for (int i = 0; i < 9; i++) {
            float bearing = i * 2.39996f + 1.55f * progress;
            float edge = radius * (.75f + (i % 3) * .14f);
            particles.setColor(i % 3 == 0 ? 0xffffdb9b : 0xffe2c1fc);
            particles.setAlpha((int)(alpha * (1 - progress)));
            square(canvas, x + edge * (float)Math.cos(bearing), y + edge * (float)Math.sin(bearing), pixel * (i % 3 == 0 ? 2 : 1));
        }
    }
}
