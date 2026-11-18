// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import android.content.Context;
import android.graphics.*;

/** Shared active-item presentation. Only authority snapshots change inventory. */
final class ActiveSlots {
    static final int COUNT = 3, BLACK_HOLE = 2, WIDTH = 208;
    private static final int STRIDE = 10, MARKER = 0x4c574131;
    private final Paint paint = new Paint(3);
    private final RectF bounds = new RectF();
    private final Bitmap[] icons;
    final int[] count = new int[COUNT], cooldown = new int[COUNT];
    int frozen, immune, holeStart, holeUntil, holeAbsorbed, lastHoleAbsorb;
    boolean holeExempt;

    ActiveSlots(Context context) {
        paint.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/NotoSansSC-Intro.ttf"));
        icons = new Bitmap[]{BlackHoleVisual.load(context, "freeze"), BlackHoleVisual.load(context, "cleanse"),
                BlackHoleVisual.load(context, "black-hole")};
    }

    Bitmap blackHoleIcon() { return icons[BLACK_HOLE]; }

    void holeMarker(Canvas canvas, float x, float y, float radius) {
        Bitmap icon = blackHoleIcon();
        if (icon == null) return;
        paint.setColor(Color.WHITE);
        paint.setFilterBitmap(false);
        bounds.set(x - radius, y - radius, x + radius, y + radius);
        canvas.drawBitmap(icon, null, bounds, paint);
    }

    void classic(int[] data, int team) {
        if (data == null) return;
        for (int b = data.length - 3; b >= 209; b--) {
            if (data[b] != MARKER) continue;
            int teams = data[b + 2], at = b + 3 + team * STRIDE, footer = b + 3 + teams * STRIDE;
            if (teams < 2 || teams > 10 || team < 0 || team >= teams || footer + 6 >= data.length
                    || data[footer] != BlackHoleVisual.FACTS) continue;
            int time = data[b + 1];
            set(data[at], data[at + 1], data[at + 2] - time, data[at + 3] - time,
                    data[at + 4] - time, data[at + 5] - time);
            setHole(data[at + 8], data[footer + 1], data[footer + 2], time, data[at + 9] != 0);
            holeAbsorbed = data[footer + 5]; lastHoleAbsorb = data[footer + 6];
            return;
        }
    }

    void expedition(ExpeditionSnapshot snap, int team) {
        int a = snap.seat(team);
        set(snap.bag(team, 0), snap.bag(team, 1), snap.cooldown(team, 0), snap.cooldown(team, 1),
                snap.raw[a + 27] - snap.time, snap.raw[a + 30] - snap.time);
        setHole(snap.holeStock, snap.holeStart, snap.holeUntil, snap.time, snap.holeExempt);
        holeAbsorbed = snap.holeAbsorbed; lastHoleAbsorb = snap.lastHoleAbsorb;
    }

    private void set(int first, int second, int firstWait, int secondWait, int freezeTime, int immunityTime) {
        count[0] = first; count[1] = second;
        cooldown[0] = Math.max(0, firstWait); cooldown[1] = Math.max(0, secondWait);
        frozen = Math.max(0, freezeTime); immune = Math.max(0, immunityTime);
    }

    private void setHole(int stock, int start, int until, int time, boolean exempt) {
        count[BLACK_HOLE] = stock;
        holeStart = start; holeUntil = until; holeExempt = exempt;
        cooldown[BLACK_HOLE] = Math.max(0, until - time);
    }

    static int hit(float x, float y, float width, float height) {
        if (y < height - 68 || y > height - 8) return -1;
        float start = width - WIDTH;
        for (int k = 0; k < COUNT; k++) if (x >= start + k * 66 && x < start + k * 66 + 60) return k;
        return -1;
    }

    private void label(Canvas c, String s, float x, float y, float size, int color) {
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(s, x, y, paint);
    }

    void draw(Canvas canvas, float width, float height) {
        float x = width - WIDTH, y = height - 68;
        for (int k = 0; k < COUNT; k++) {
            boolean hole = k == BLACK_HOLE;
            boolean ready = count[k] > 0 && cooldown[k] == 0 && (k == 1 || frozen == 0);
            float xx = x + k * 66;
            paint.setColor(ready ? (hole ? 0xbd503570 : 0xa83b6c60) : 0x88172b32);
            canvas.drawRoundRect(xx, y, xx + 60, y + 60, 9, 9, paint);
            paint.setAlpha(ready ? 255 : 140);
            paint.setFilterBitmap(!hole);
            bounds.set(xx + 5, y + 3, xx + 43, y + 41);
            if (icons[k] != null) canvas.drawBitmap(icons[k], null, bounds, paint);
            paint.setAlpha(255);
            label(canvas, "×" + count[k], xx + 46, y + 16, 12, 0xfff1ecd5);
            String name = hole ? BattleText.t("黑洞", "Black hole") : k == 0 ? BattleText.t("冻结", "Freeze") : BattleText.t("净化", "Cleanse");
            label(canvas, name, xx + 30, y + 53, hole ? 11 : 12, 0xffedf3e6);
            if (cooldown[k] > 0) label(canvas, (cooldown[k] + 999) / 1000 + "s", xx + 30, y + 34, 13, 0xffffd599);
        }
        if (cooldown[BLACK_HOLE] > 0) {
            String status = holeExempt ? BattleText.t("黑洞吸引已净化", "Black-hole pull cleansed")
                    : BattleText.t("黑洞吸引中 · 可净化", "Black-hole pull · Cleanse");
            label(canvas, status, width - 112, height - 71, 9, 0xffedd9ff);
        } else if (frozen > 0) {
            label(canvas, BattleText.t("被冻结 ", "Frozen ") + (frozen + 999) / 1000
                    + BattleText.t("s · 可净化", "s · Cleanse"), width - 112, height - 71, 9, 0xffe4f7ff);
        } else if (immune > 0) {
            label(canvas, BattleText.t("免冻 ", "Immune ") + (immune + 999) / 1000 + "s",
                    width - 112, height - 71, 9, 0xffc9eee1);
        }
    }
}
