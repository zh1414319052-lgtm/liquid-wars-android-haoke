package com.xenris.liquidwarsos.updates;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import com.xenris.liquidwarsos.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,34})
public class ReleaseNotesTest {
    public static class Screen extends Activity {
        boolean focus=true;
        @Override public boolean hasWindowFocus(){return focus;}
        @Override protected void onCreate(Bundle state){setTheme(R.style.Theme_LiqurdWars);super.onCreate(state);}
    }
    @Test public void firstDisplayIsRememberedBeforeCloseAndNewVersionGetsItsOwnNotice(){
        ActivityController<Screen> c=Robolectric.buildActivity(Screen.class).setup();Screen a=c.get();
        SharedPreferences p=a.getSharedPreferences("test-notes",0);p.edit().clear().commit();
        boolean[] blocked={false};ReleaseNotesHost h=new ReleaseNotesHost(a,v->blocked[0]=v,p,85);
        assertTrue(blocked[0]);h.resume();AlertDialog d=(AlertDialog)ShadowDialog.getLatestDialog();
        assertTrue(d.isShowing());assertTrue(p.getBoolean("shown_85",false));assertTrue(blocked[0]);
        d.findViewById(R.id.notes_close).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertFalse(blocked[0]);assertFalse(d.isShowing());
        h.focus(true);h.pause();h.resume();assertFalse(d.isShowing());h.close();
        ReleaseNotesHost recreated=new ReleaseNotesHost(a,v->{},p,85);recreated.resume();assertFalse(d.isShowing());recreated.close();
        ReleaseNotesHost next=new ReleaseNotesHost(a,v->{},p,86);next.resume();
        assertTrue(ShadowDialog.getLatestDialog().isShowing());assertTrue(p.getBoolean("shown_86",false));next.close();c.pause().stop().destroy();
    }
    @Test public void noFocusDoesNotConsumeNoticeAndPausePreventsBackgroundPopups(){
        ActivityController<Screen> c=Robolectric.buildActivity(Screen.class).setup();Screen a=c.get();a.focus=false;
        SharedPreferences p=a.getSharedPreferences("test-notes",0);p.edit().clear().commit();
        ReleaseNotesHost h=new ReleaseNotesHost(a,v->{},p,85);h.resume();assertFalse(p.getBoolean("shown_85",false));
        h.pause();a.focus=true;h.focus(true);assertFalse(p.getBoolean("shown_85",false));
        h.resume();AlertDialog d=(AlertDialog)ShadowDialog.getLatestDialog();assertTrue(d.isShowing());
        h.pause();assertFalse(d.isShowing());h.resume();assertFalse(d.isShowing());h.close();c.pause().stop().destroy();
    }
    @Test public void manualReopenCancelAndDuplicateTapPreserveOtherPreferences(){
        ActivityController<Screen> c=Robolectric.buildActivity(Screen.class).setup();Screen a=c.get();
        SharedPreferences p=a.getSharedPreferences("test-notes",0);p.edit().clear().putBoolean("shown_85",true).putString("controls","follow").commit();
        ReleaseNotesHost h=new ReleaseNotesHost(a,v->{},p,85);h.resume();h.manual();
        AlertDialog d=(AlertDialog)ShadowDialog.getLatestDialog();h.manual();assertSame(d,ShadowDialog.getLatestDialog());
        d.cancel();assertFalse(d.isShowing());h.focus(true);assertFalse(d.isShowing());
        assertEquals("follow",p.getString("controls",""));h.close();c.pause().stop().destroy();
    }
}
