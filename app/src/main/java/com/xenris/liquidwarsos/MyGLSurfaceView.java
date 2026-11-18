//    This file is part of Liquid Wars.
//
//    Copyright (C) 2013 Henry Shepperd (hshepperd@gmail.com)
//
//    Liquid Wars is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Liquid Wars is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Liquid Wars.  If not, see <http://www.gnu.org/licenses/>.

package com.xenris.liquidwarsos;

import android.opengl.GLSurfaceView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyGLSurfaceView extends android.widget.FrameLayout {
    private GLSurfaceView classic;
    private volatile float[] godotCamera={0,0,400,185,0};
    public float[] godotCamera(){return godotCamera;}
    public volatile boolean godotReady;
    public android.view.Surface renderSurface(){return classic==null?null:classic.getHolder().getSurface();}
    public void queueEvent(Runnable task){if(classic!=null)classic.queueEvent(task);}
    public boolean renderReady(){return !garden3D()||godotReady;}
    public void godotFrame(){framesDrawn++;}

    private GameAudio audio;
    public GameAudio audio(){return audio;}
    private SurfaceCallbacks surfaceCallbacks;
    public final WorldCamera camera=new WorldCamera();private boolean worldBound;
    public void syncWorld(){int[] s=NativeInterface.sceneInfo();if(s!=null&&s.length>=6){android.app.Activity a=DisplayFrameRate.activity(getContext());boolean spectator=a!=null&&a.getIntent().getBooleanExtra("spectator",false);if(camera.id!=s[2]||camera.width!=s[0]||camera.height!=s[1])camera.world(s[0],s[1],s[2],s[3],spectator,s[4],s[5]);worldBound=true;applyCamera();if(getParent() instanceof android.view.View){android.view.View intro=((android.view.View)getParent()).findViewById(R.id.match_intro);if(intro!=null)intro.invalidate();}}}
    public void applyCamera(){godotCamera=new float[]{camera.left,camera.bottom,camera.viewWidth,camera.viewHeight,(DisplayFrameRate.activity(getContext())!=null&&DisplayFrameRate.activity(getContext()).getIntent().getBooleanExtra("spectator",false)?1:0)};NativeInterface.setCamera(camera.left,camera.bottom,camera.viewWidth,camera.viewHeight,DisplayFrameRate.activity(getContext())!=null&&DisplayFrameRate.activity(getContext()).getIntent().getBooleanExtra("spectator",false));}
    private final short[] heldX=new short[5],heldY=new short[5];
    public void refreshHeldTargets(ItemOverlay overlay){if(resumed&&hasWindowFocus()&&camera.fillTouchTargets(heldX,heldY)){controls(heldX,heldY);overlay.noteTargets(heldX,heldY);}}
    private final AtomicBoolean framePending=new AtomicBoolean();
    private final VsyncPacer pacer=new VsyncPacer(this::displayFrame);
    private boolean resumed,surfaceReady,attached,gardenFailureShown;
    private volatile float targetHz=60;
    private volatile long framesDrawn;
    private final DisplayManager.DisplayListener displayListener=new DisplayManager.DisplayListener(){
        public void onDisplayAdded(int id){}public void onDisplayRemoved(int id){}
        public void onDisplayChanged(int id){if(getDisplay()!=null&&getDisplay().getDisplayId()==id&&resumed)refreshRate();}
    };

    public MyGLSurfaceView(Context context) {
        super(context);
        
        initialize();
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        initialize();
    }

    public boolean garden3D(){android.app.Activity a=DisplayFrameRate.activity(getContext());return a!=null&&a.getIntent().getBooleanExtra(GardenSession.EXTRA,false);}
    private void initialize(){
        audio=new GameAudio(getContext());
        if(garden3D()){surfaceReady=true;return;}
        classic=new GLSurfaceView(getContext());
        classic.setEGLConfigChooser(new SmoothConfigChooser(false));
        classic.setRenderer(new MyRenderer(()->{framesDrawn++;framePending.set(false);}));
        classic.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        classic.getHolder().addCallback(new SurfaceHolder.Callback(){
            public void surfaceCreated(SurfaceHolder h){surfaceReady=true;framePending.set(false);if(resumed)refreshRate();updatePacer();}
            public void surfaceChanged(SurfaceHolder h,int f,int w,int hgt){if(resumed)refreshRate();}
            public void surfaceDestroyed(SurfaceHolder h){surfaceReady=false;pacer.stop();audio.foreground(false);framePending.set(false);}
        });
        addView(classic,new LayoutParams(-1,-1));
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event){return true;}
    private void displayFrame(long time){

        if(!worldBound)syncWorld();
        if(getWidth()>0&&getHeight()>0)camera.viewport(getWidth()/(float)getHeight());
        if(getParent() instanceof android.view.View){ItemOverlay overlay=((android.view.View)getParent()).findViewById(R.id.item_overlay);if(overlay!=null)overlay.onDisplayFrame(time);TacticalOverlay tactical=((android.view.View)getParent()).findViewById(R.id.tactical_overlay);if(tactical!=null)tactical.frame(time);JoystickOverlay joystick=((android.view.View)getParent()).findViewById(R.id.joystick_overlay);if(joystick!=null)joystick.frame(time);ExpeditionOverlay expedition=((android.view.View)getParent()).findViewById(R.id.expedition_overlay);if(expedition!=null)expedition.frame(time);android.app.Activity a=DisplayFrameRate.activity(getContext());audio.frame(overlay==null?null:overlay.facts(),tactical==null?null:tactical.facts(),StaticBits.team,a!=null&&a.getIntent().getBooleanExtra("spectator",false));}
        if(classic!=null&&framePending.compareAndSet(false,true))classic.requestRender();
    }
    public float targetHz(){return targetHz;}
    public boolean pacingActive(){return pacer.active();}
    /** Diagnostic render completions, explicitly not SurfaceFlinger presentation FPS. */
    public long framesDrawn(){return framesDrawn;}
    public void refreshRate(){
        targetHz=DisplayFrameRate.apply(this);float actual=getDisplay()==null?60:getDisplay().getRefreshRate();pacer.rate(actual,targetHz);
    }
    private void updatePacer(){boolean active=attached&&resumed&&surfaceReady&&hasWindowFocus()&&getWindowVisibility()==VISIBLE;if(active)pacer.start();else pacer.stop();if(audio!=null)audio.foreground(active);}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(pacer!=null)updatePacer();}
    public void onResume(){if(garden3D())NativeInterface.gardenPauseVisual(false);if(classic!=null)classic.onResume();resumed=true;worldBound=false;framePending.set(false);refreshRate();updatePacer();}
    public void onPause(){if(getParent() instanceof android.view.View){JoystickOverlay stick=((android.view.View)getParent()).findViewById(R.id.joystick_overlay);if(stick!=null)stick.cancelForModal();}resumed=false;audio.foreground(false);camera.endTouch();camera.resetSmoothing();pacer.stop();if(garden3D())NativeInterface.gardenPauseVisual(true);DisplayFrameRate.release(this);if(classic!=null)classic.onPause();framePending.set(false);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;((DisplayManager)getContext().getSystemService(Context.DISPLAY_SERVICE)).registerDisplayListener(displayListener,new Handler(Looper.getMainLooper()));updatePacer();}
    @Override protected void onDetachedFromWindow(){attached=false;pacer.stop();audio.release();((DisplayManager)getContext().getSystemService(Context.DISPLAY_SERVICE)).unregisterDisplayListener(displayListener);super.onDetachedFromWindow();}
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) width = MyRenderer.WIDTH;
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) height = MyRenderer.HEIGHT;
        int[] size = garden3D()?new int[]{width,height}:GameViewport.fit(width, height);
        super.onMeasure(MeasureSpec.makeMeasureSpec(size[0],MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(size[1],MeasureSpec.EXACTLY));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        TacticalOverlay tactical=getParent() instanceof android.view.View?((android.view.View)getParent()).findViewById(R.id.tactical_overlay):null;
        if(tactical!=null&&tactical.joystickEnabled()){camera.endTouch();return true;}
        if(surfaceCallbacks != null)
            surfaceCallbacks.onTouch(event);

        return true;
    }

    public void setSurfaceCallbacks(SurfaceCallbacks sc) {
        surfaceCallbacks = sc;
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        updatePacer();
        if (!focused) { camera.endTouch(); camera.resetSmoothing(); return; }
        if(resumed)refreshRate();
        Context context = getContext();
        while (!(context instanceof android.app.Activity) && context instanceof android.content.ContextWrapper) {
            Context base = ((android.content.ContextWrapper) context).getBaseContext();
            if (base == context) break;
            context = base;
        }
        if (context instanceof android.app.Activity){((android.app.Activity)context).setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);GameWindow.apply((android.app.Activity) context);}
    }

    public void controls(short[] x,short[] y){if(surfaceCallbacks!=null){surfaceCallbacks.onControls(x,y);audio.command(x,y);}}

    public interface SurfaceCallbacks {
        void onControls(short[] x,short[] y);
        public void onTouch(MotionEvent event);
    }
}
