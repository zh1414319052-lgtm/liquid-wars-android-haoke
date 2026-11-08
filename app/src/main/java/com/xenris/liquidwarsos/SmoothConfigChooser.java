// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import android.opengl.GLSurfaceView;
import android.util.Log;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/** GLES1-compatible selection; all three game activities share this viewport. */
public final class SmoothConfigChooser implements GLSurfaceView.EGLConfigChooser {
    @Override public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        for (int samples : new int[]{4, 2, 0}) {
            EGLConfig config = find(egl, display, 8, 8, 8, samples);
            if (config != null) {
                Log.i("LWGraphics", "RGB888 EGL samples=" + attribute(egl, display, config, EGL10.EGL_SAMPLES));
                return config;
            }
        }
        EGLConfig fallback = find(egl, display, 5, 6, 5, 0);
        if (fallback == null) throw new IllegalArgumentException("No compatible GLES1 window configuration");
        Log.w("LWGraphics", "RGB565 EGL fallback, no multisampling");
        return fallback;
    }

    private static EGLConfig find(EGL10 egl, EGLDisplay display, int red, int green, int blue, int samples) {
        int[] spec = {
            EGL10.EGL_RED_SIZE, red, EGL10.EGL_GREEN_SIZE, green, EGL10.EGL_BLUE_SIZE, blue,
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,
            0x3040, 1, // EGL_RENDERABLE_TYPE, EGL_OPENGL_ES_BIT (GLES1)
            EGL10.EGL_SAMPLE_BUFFERS, samples == 0 ? 0 : 1,
            EGL10.EGL_SAMPLES, samples, EGL10.EGL_NONE
        };
        int[] count = new int[1];
        if (!egl.eglChooseConfig(display, spec, null, 0, count) || count[0] == 0) return null;
        EGLConfig[] configs = new EGLConfig[count[0]];
        if (!egl.eglChooseConfig(display, spec, configs, configs.length, count)) return null;
        EGLConfig best = null; int bestScore = Integer.MAX_VALUE;
        for (EGLConfig config : configs) {
            int actual = attribute(egl, display, config, EGL10.EGL_SAMPLES);
            if (samples == 0 && attribute(egl, display, config, EGL10.EGL_SAMPLE_BUFFERS) != 0) continue;
            int score = (actual-samples)*1000
                + attribute(egl, display, config, EGL10.EGL_DEPTH_SIZE)*4
                + attribute(egl, display, config, EGL10.EGL_STENCIL_SIZE)*4
                + attribute(egl, display, config, EGL10.EGL_ALPHA_SIZE);
            if (actual >= samples && score < bestScore) { best = config; bestScore = score; }
        }
        return best;
    }

    private static int attribute(EGL10 egl, EGLDisplay display, EGLConfig config, int key) {
        int[] value = new int[1];
        return egl.eglGetConfigAttrib(display, config, key, value) ? value[0] : 0;
    }
}
