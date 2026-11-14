package com.xenris.liquidwarsos;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.util.*;

/** Runs against the final signed, non-debuggable app; no release test hooks. */
@RunWith(AndroidJUnit4.class)
public class Formal185DeviceTest {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    Context c=inst.getTargetContext();
    void main(Runnable r){inst.runOnMainSync(r);}
    Activity launch(String name){return inst.startActivitySync(new Intent().setClassName(c.getPackageName(),"com.xenris.liquidwarsos.activities."+name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    View find(View v,String text){if(v instanceof TextView&&text.equals(((TextView)v).getText().toString()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),text);if(found!=null)return found;}return null;}
    View findScroll(View v){if(v instanceof ScrollView)return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View s=findScroll(((ViewGroup)v).getChildAt(i));if(s!=null)return s;}return null;}
    void shot(String name)throws Exception{SystemClock.sleep(500);Bitmap b=inst.getUiAutomation().takeScreenshot();try(FileOutputStream f=new FileOutputStream(new File(c.getExternalFilesDir(null),"formal185-"+name+".jpg"))){Assert.assertNotNull(b);b.compress(Bitmap.CompressFormat.JPEG,88,f);}b.recycle();}
    @Test public void finalSignedRuntime()throws Exception{
        Assert.assertEquals("com.xenris.liquidwarsos.haoke",c.getPackageName());
        Assert.assertEquals(0,c.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE);
        Activity menu=launch("MainMenuActivity");
        SharedPreferences prefs=c.getSharedPreferences("haoke_signed_updates_v1",0);
        String state=null;
        for(int i=0;i<25;i++){SystemClock.sleep(1000);for(Object value:prefs.getAll().values())if(value instanceof String&&((String)value).contains("\"revision\""))state=(String)value;if(state!=null)break;}
        Assert.assertNotNull("Foreground check must verify and persist real signed feed",state);
        JSONObject verified=new JSONObject(state);Assert.assertTrue(verified.getLong("revision")>=0);Assert.assertEquals(64,verified.getString("digest").length());
        shot("main");
        Activity about=launch("InstructionsActivity");SystemClock.sleep(500);
        main(()->{ScrollView scroll=(ScrollView)findScroll(about.getWindow().getDecorView());Assert.assertNotNull(scroll);scroll.fullScroll(View.FOCUS_DOWN);});shot("about");
        int checkId=c.getResources().getIdentifier("updates_check","string",c.getPackageName());
        main(()->{View button=find(about.getWindow().getDecorView(),about.getString(checkId));Assert.assertNotNull(button);button.performClick();});
        String current=c.getString(c.getResources().getIdentifier("updates_current","string",c.getPackageName()));
        boolean currentShown=false;
        for(int i=0;i<18;i++){SystemClock.sleep(1000);android.view.accessibility.AccessibilityNodeInfo root=inst.getUiAutomation().getRootInActiveWindow();if(root!=null&&!root.findAccessibilityNodeInfosByText(current).isEmpty()){currentShown=true;break;}}
        Assert.assertTrue("Manual check must show a verified current-version result",currentShown);shot("manual-check");
        main(about::finish);SystemClock.sleep(500);
        Activity setup=launch("SinglePlayerGameSetupActivity");SystemClock.sleep(500);
        int start=c.getResources().getIdentifier("start_button","id",c.getPackageName());
        Instrumentation.ActivityMonitor gameMonitor=inst.addMonitor("com.xenris.liquidwarsos.activities.GameActivity",null,false);
        main(()->setup.findViewById(start).performClick());
        Activity game=gameMonitor.waitForActivityWithTimeout(6000);Assert.assertNotNull(game);SystemClock.sleep(12000);shot("battle");
        String during=prefs.getAll().toString();SystemClock.sleep(2500);Assert.assertEquals("Battle must not schedule updates",during,prefs.getAll().toString());
        main(()->{inst.callActivityOnPause(game);inst.callActivityOnResume(game);});SystemClock.sleep(1200);shot("battle-resumed");
        main(()->{game.finish();setup.finish();});inst.removeMonitor(gameMonitor);SystemClock.sleep(700);shot("return-menu");
        Assert.assertTrue(c.getSharedPreferences("formal185_probe",0).edit().putString("upgrade_marker","keep-settings-185").commit());
        try(FileWriter file=new FileWriter(new File(c.getExternalFilesDir(null),"formal185-result.json"))){file.write(new JSONObject().put("result","PASS").put("version","1.85").put("code",55).put("debuggable",false).put("automatic_verified_feed",verified).put("checks","signed install; foreground real feed; manual check; About controls; battle start; no battle updates; pause/resume; menu return").toString(2));}
        main(menu::finish);
    }
    @Test public void upgradeKeepsSettings(){Assert.assertEquals("keep-settings-185",c.getSharedPreferences("formal185_probe",0).getString("upgrade_marker",""));Assert.assertTrue(c.getSharedPreferences("formal185_probe",0).edit().remove("upgrade_marker").commit());}
}
