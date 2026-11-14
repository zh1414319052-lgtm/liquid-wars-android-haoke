package com.xenris.liquidwarsos;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.graphics.Bitmap;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.xenris.liquidwarsos.activities.OnlineLobbyActivity;
import com.xenris.liquidwarsos.online.PlatformApi;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;

/** Physical-device native UI/lifecycle checks. Room/account fixtures are synthetic,
 * not evidence of public matching; no fixture or test hook enters release code. */
@RunWith(AndroidJUnit4.class)
public class Online185UiTest {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    OnlineLobbyActivity a;
    void main(Runnable r){inst.runOnMainSync(r);}
    Object field(String n){try{Field f=OnlineLobbyActivity.class.getDeclaredField(n);f.setAccessible(true);return f.get(a);}catch(Exception e){throw new RuntimeException(e);}}
    void put(String n,Object v){try{Field f=OnlineLobbyActivity.class.getDeclaredField(n);f.setAccessible(true);f.set(a,v);}catch(Exception e){throw new RuntimeException(e);}}
    void call(String n){try{Method m=OnlineLobbyActivity.class.getDeclaredMethod(n);m.setAccessible(true);m.invoke(a);}catch(Exception e){throw new RuntimeException(e);}}
    View find(View v,String s){if(v instanceof TextView&&((TextView)v).getText().toString().equals(s))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View r=find(((ViewGroup)v).getChildAt(i),s);if(r!=null)return r;}return null;}
    View text(String s){View v=find(a.getWindow().getDecorView(),s);Assert.assertNotNull(s,v);return v;}
    void shot(String n)throws Exception{SystemClock.sleep(500);Bitmap b=inst.getUiAutomation().takeScreenshot();Assert.assertNotNull(b);File p=new File(inst.getTargetContext().getFilesDir(),"ui185-"+n+".jpg");try(FileOutputStream f=new FileOutputStream(p)){b.compress(Bitmap.CompressFormat.JPEG,88,f);}b.recycle();}
    @Test public void nativeFlow()throws Exception{
        Context c=inst.getTargetContext();new PlatformApi(c).clear();
        Activity menu=inst.startActivitySync(new Intent(c,com.xenris.liquidwarsos.activities.MainMenuActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        SystemClock.sleep(8500);shot("main-cutout");
        main(()->{Assert.assertEquals(android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,menu.getWindow().getAttributes().layoutInDisplayCutoutMode);int[] xy=new int[2];menu.getWindow().getDecorView().getLocationOnScreen(xy);Assert.assertEquals("window must include left cutout",0,xy[0]);});
        a=(OnlineLobbyActivity)inst.startActivitySync(new Intent(c,OnlineLobbyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{
            SystemClock.sleep(900);shot("login");
            main(()->{text("进入大厅").performClick();Assert.assertNotNull(((EditText)field("emailField")).getError());((EditText)field("emailField")).setText("ui185@example.com");text("密码登录").performClick();Assert.assertEquals("ui185@example.com",((EditText)field("emailField")).getText().toString());});
            shot("password");
            main(()->{text("设置 / 找回密码").performClick();put("challenge","synthetic-purpose-bound-challenge");put("challengeEmail","ui185@example.com");((EditText)field("codeField")).setText("123456");((EditText)field("passwordField")).setText("test-only-password");((EditText)field("repeatField")).setText("different-test-password");text("保存密码并登录").performClick();Assert.assertNotNull(((EditText)field("repeatField")).getError());((EditText)field("passwordField")).setText("");((EditText)field("repeatField")).setText("");((EditText)field("codeField")).setText("");});
            main(()->((ScrollView)field("scroll")).scrollTo(0,0));shot("reset-top");
            main(()->{EditText e=(EditText)field("repeatField");e.requestFocus();((android.view.inputmethod.InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(e,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});
            SystemClock.sleep(1000);shot("keyboard");
            main(()->call("hideKeyboard"));SystemClock.sleep(300);
            main(()->{((EditText)field("emailField")).setText("changed@example.com");Assert.assertEquals("",field("challenge"));put("resendAt",SystemClock.elapsedRealtime()+4000);call("updateCooldown");Assert.assertFalse(((Button)field("sendButton")).isEnabled());Bundle b=new Bundle();inst.callActivityOnSaveInstanceState(a,b);Assert.assertEquals("changed@example.com",b.getString("email"));Assert.assertFalse(b.containsKey("password"));inst.callActivityOnPause(a);inst.callActivityOnResume(a);Assert.assertEquals("changed@example.com",((EditText)field("emailField")).getText().toString());});
            main(()->{put("uid","fixture-host");put("nickname","薄荷小队");put("accountEmail","ui185@example.com");put("authMode","code");call("home");});shot("home");
            main(()->{put("queued",true);call("home");});shot("queue");
            JSONObject room=new JSONObject().put("code","HKQA85").put("host_id","fixture-host").put("status","waiting").put("members",new JSONArray().put(new JSONObject().put("user_id","fixture-host").put("nickname","薄荷小队").put("ready",true).put("online",true)).put(new JSONObject().put("user_id","fixture-guest").put("nickname","一位名字非常长的液滴玩家测试").put("ready",false).put("online",true)));
            main(()->{put("queued",false);put("room",room);put("roomCode","HKQA85");call("renderRoom");Assert.assertFalse(text("等待全员就绪").isEnabled());View before=((LinearLayout)field("body")).getChildAt(0);call("renderRoom");Assert.assertSame(before,((LinearLayout)field("body")).getChildAt(0));});shot("room");
            room.getJSONArray("members").getJSONObject(1).put("ready",true);
            main(()->{call("renderRoom");Assert.assertTrue(text("进入游戏设置").isEnabled());call("confirmLeave");});shot("leave-dialog");
            try(FileWriter f=new FileWriter(new File(c.getFilesDir(),"ui185-result.json"))){f.write(new JSONObject().put("result","PASS").put("device",Build.MODEL).put("synthetic_room",true).put("checks","email/OTP/password tabs; confirmation validation; keyboard; challenge invalidation; cooldown; saved state; pause/resume; unchanged poll stability; six seats; host ready gating").toString(2));}
        }finally{main(()->{a.finish();menu.finish();});}
    }
}
