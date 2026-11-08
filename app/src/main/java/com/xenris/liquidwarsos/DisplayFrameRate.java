// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.app.Activity;
import android.content.*;
import android.os.Build;
import android.view.*;
import java.util.*;

/** Requests only same-resolution modes; device power/thermal policy retains final control. */
final class DisplayFrameRate {
    static final String PREFS="display_quality",KEY="frame_limit";
    static int preference(Context c){return c.getSharedPreferences(PREFS,0).getInt(KEY,0);}
    static void preference(Context c,int fps){c.getSharedPreferences(PREFS,0).edit().putInt(KEY,fps).apply();}
    static Activity activity(Context context){
        while(context instanceof ContextWrapper&&!(context instanceof Activity)){
            Context base=((ContextWrapper)context).getBaseContext();if(base==context)break;context=base;
        }return context instanceof Activity?(Activity)context:null;
    }
    static List<Display.Mode> modes(Display display){
        List<Display.Mode> modes=new ArrayList<>();if(display==null)return modes;Display.Mode current=display.getMode();
        for(Display.Mode m:display.getSupportedModes())if(m.getPhysicalWidth()==current.getPhysicalWidth()&&m.getPhysicalHeight()==current.getPhysicalHeight()&&m.getRefreshRate()>=59&&m.getRefreshRate()<=240)modes.add(m);
        return modes;
    }
    static int[] choices(Display display){
        TreeSet<Integer> values=new TreeSet<>();values.add(60);
        for(Display.Mode m:modes(display)){int hz=Math.round(m.getRefreshRate());if(hz>60)values.add(hz);}
        int[] choices=new int[values.size()+1];int i=1;for(int fps:values)choices[i++]=fps;return choices;
    }
    static float apply(MyGLSurfaceView view){
        Display display=view.getDisplay();float target=60;Display.Mode chosen=null;int preference=preference(view.getContext());
        for(Display.Mode mode:modes(display)){
            float hz=mode.getRefreshRate();boolean eligible=preference==0||hz<=preference+.5f;
            if(eligible&&(chosen==null||hz>chosen.getRefreshRate()))chosen=mode;
        }
        // A fixed 120 Hz panel can present 60 uniformly even without a separate 60 Hz mode.
        if(chosen==null)for(Display.Mode mode:modes(display))if(chosen==null||mode.getRefreshRate()<chosen.getRefreshRate())chosen=mode;
        if(chosen!=null){target=chosen.getRefreshRate();if(preference>0&&target>preference+.5f)target/=Math.ceil(target/preference-.01);}
        Activity a=activity(view.getContext());if(a!=null){WindowManager.LayoutParams p=a.getWindow().getAttributes();int id=chosen==null?0:chosen.getModeId();if(p.preferredDisplayModeId!=id||p.preferredRefreshRate!=target){p.preferredDisplayModeId=id;p.preferredRefreshRate=target;a.getWindow().setAttributes(p);}}
        Surface surface=view.getHolder().getSurface();
        if(Build.VERSION.SDK_INT>=30&&surface.isValid())try{
            if(Build.VERSION.SDK_INT>=31)surface.setFrameRate(target,Surface.FRAME_RATE_COMPATIBILITY_DEFAULT,Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS);
            else surface.setFrameRate(target,Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);
        }catch(IllegalArgumentException|IllegalStateException ignored){/* Driver/transition: vsync remains usable. */}
        return target;
    }
    static void release(MyGLSurfaceView view){
        if(Build.VERSION.SDK_INT>=30&&view.getHolder().getSurface().isValid())try{view.getHolder().getSurface().setFrameRate(0,Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);}catch(IllegalArgumentException|IllegalStateException ignored){}
        Activity a=activity(view.getContext());if(a!=null){WindowManager.LayoutParams p=a.getWindow().getAttributes();p.preferredDisplayModeId=0;p.preferredRefreshRate=0;a.getWindow().setAttributes(p);}
    }
}
