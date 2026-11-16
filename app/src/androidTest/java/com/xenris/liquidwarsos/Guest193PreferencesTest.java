package com.xenris.liquidwarsos;
import android.content.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
/** Only temporarily changes the command preference; restores the exact prior value. */
public class Guest193PreferencesTest {
 @Test public void followDefaultAndSavedModesSurviveViews() throws Exception {
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  SharedPreferences prefs=c.getSharedPreferences("battle-controls",0);
  boolean existed=prefs.contains("command-mode");int old=prefs.getInt("command-mode",2);
  java.lang.reflect.Field field=TacticalOverlay.class.getDeclaredField("input");field.setAccessible(true);
  try {
   prefs.edit().remove("command-mode").commit();
   InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{try{Assert.assertEquals(2,field.getInt(new TacticalOverlay(c,null)));}catch(IllegalAccessException e){throw new AssertionError(e);}});
   for(int mode:new int[]{0,1,2,-3,99}){prefs.edit().putInt("command-mode",mode).commit();final int expected=mode>=0&&mode<=2?mode:2;
    InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{try{for(int n=0;n<2;n++)Assert.assertEquals(expected,field.getInt(new TacticalOverlay(c,null)));}catch(IllegalAccessException e){throw new AssertionError(e);}});
   }
  }finally{SharedPreferences.Editor e=prefs.edit();if(existed)e.putInt("command-mode",old);else e.remove("command-mode");e.commit();}
 }
}
