// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

/** Full-screen map artwork is authored at2200x1000; the rule grid remains800x480. */
public final class GameViewport {
    private static final int MAP_WIDTH = 800;
    private static final int MAP_HEIGHT = 480;
    private static final double ASPECT = 2200.0 / 1000.0;

    private GameViewport() {}

    public static int[] fit(int availableWidth, int availableHeight) {
        if (availableWidth <= 0 || availableHeight <= 0) return new int[]{0, 0};
        int width = availableWidth;
        int height = Math.max(1, (int) Math.round(width / ASPECT));
        if (height > availableHeight) {
            height = availableHeight;
            width = Math.max(1, (int) Math.round(height * ASPECT));
        }
        return new int[]{Math.min(width, availableWidth), Math.min(height, availableHeight)};
    }

    public static short mapX(float localX, int viewWidth) {
        int x = (int) (localX * MAP_WIDTH / (double) viewWidth);
        return (short) Math.max(0, Math.min(MAP_WIDTH - 1, x));
    }
}
