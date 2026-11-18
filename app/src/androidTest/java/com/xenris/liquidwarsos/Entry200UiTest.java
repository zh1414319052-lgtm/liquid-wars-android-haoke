package com.xenris.liquidwarsos;

import android.app.*;
import android.content.*;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.xenris.liquidwarsos.activities.MainMenuActivity;
import org.junit.*;
import org.junit.runner.RunWith;

/** Exercise the actual phone dialog. Intercept destinations BEFORE launch:
 * the two supported local seat limits can be verified without starting a battle. */
@RunWith(AndroidJUnit4.class)
public class Entry200UiTest {
    final Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    Intent destination;
    AccessibilityNodeInfo node(String text) {
        for(int attempt=0;attempt<30;attempt++){
            AccessibilityNodeInfo root=inst.getUiAutomation().getRootInActiveWindow();
            if(root!=null)for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(text))
                if(text.contentEquals(n.getText()) && n.isVisibleToUser())return n;
            SystemClock.sleep(100);
        }
        throw new AssertionError("Visible control missing: "+text);
    }
    void click(String text){Assert.assertTrue(node(text).performAction(AccessibilityNodeInfo.ACTION_CLICK));inst.waitForIdleSync();}
    @Test public void localEntryRoutesWithSavedSeatCount(){
        Context c=inst.getTargetContext();
        SharedPreferences prefs=c.getSharedPreferences("battle-controls",0);
        boolean existed=prefs.contains("expedition-seats");int saved=prefs.getInt("expedition-seats",6);
        MainMenuActivity menu=(MainMenuActivity)inst.startActivitySync(new Intent(c,MainMenuActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Instrumentation.ActivityMonitor monitor=new Instrumentation.ActivityMonitor(){
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent){
                if(intent.getComponent()!=null && intent.getComponent().getClassName().endsWith("GodotGardenActivity")){
                    destination=new Intent(intent);return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);
                }
                return null;
            }
        };
        inst.addMonitor(monitor);
        try {
            for(int seats:new int[]{2,10}){
                prefs.edit().putInt("expedition-seats",seats).commit();destination=null;
                inst.runOnMainSync(()->menu.garden3DMenu(null));
                node(Integer.toString(seats));click("开始本地对战");
                Assert.assertNotNull(destination);Assert.assertTrue(destination.getComponent().getClassName().endsWith("GodotGardenActivity"));
                Assert.assertTrue(destination.getBooleanExtra(GardenSession.EXTRA,false));
                Assert.assertEquals(seats,destination.getIntExtra("expedition_seats",-1));
            }
        } finally {
            inst.removeMonitor(monitor);
            SharedPreferences.Editor edit=prefs.edit();if(existed)edit.putInt("expedition-seats",saved);else edit.remove("expedition-seats");edit.commit();
            inst.runOnMainSync(menu::finish);
        }
    }
}
