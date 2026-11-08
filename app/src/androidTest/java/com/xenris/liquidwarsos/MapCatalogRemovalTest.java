// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import android.graphics.Bitmap;
import java.io.*;import java.util.*;
import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;import org.junit.runner.RunWith;
import com.xenris.liquidwarsos.activities.MainMenuActivity;
import com.xenris.liquidwarsos.activities.SinglePlayerGameSetupActivity;

/** Menu/list/touch checks only. Never starts configured model teams or opens API settings. */
@RunWith(AndroidJUnit4.class) public class MapCatalogRemovalTest {
 static Instrumentation i(){return InstrumentationRegistry.getInstrumentation();}static Context c(){return i().getTargetContext();}
 static void picture(String name)throws Exception {Bitmap b=i().getUiAutomation().takeScreenshot();Assert.assertNotNull(b);try(OutputStream o=c().openFileOutput(name,0)){b.compress(Bitmap.CompressFormat.PNG,100,o);}b.recycle();}
 static void tap(View v,boolean hold)throws Exception {int[] xy=new int[2],wh=new int[2];i().runOnMainSync(()->{v.getLocationOnScreen(xy);wh[0]=v.getWidth();wh[1]=v.getHeight();Assert.assertTrue(v.isShown());});long t=SystemClock.uptimeMillis();MotionEvent a=MotionEvent.obtain(t,t,0,xy[0]+wh[0]/2f,xy[1]+wh[1]/2f,0);a.setSource(4098);Assert.assertTrue(i().getUiAutomation().injectInputEvent(a,true));a.recycle();SystemClock.sleep(hold?650:50);MotionEvent b=MotionEvent.obtain(t,SystemClock.uptimeMillis(),1,xy[0]+wh[0]/2f,xy[1]+wh[1]/2f,0);b.setSource(4098);Assert.assertTrue(i().getUiAutomation().injectInputEvent(b,true));b.recycle();i().waitForIdleSync();}
 @Test public void mainMenuNoPreviewOrLegacyDeepLink()throws Exception {
  Intent old=new Intent(c(),MainMenuActivity.class).putExtra("qa_ruins_preview",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  try(ActivityScenario<MainMenuActivity> s=ActivityScenario.launch(old)) {
   SystemClock.sleep(650);s.onActivity(a->{Assert.assertTrue(a.hasWindowFocus());ArrayDeque<View> queue=new ArrayDeque<>();queue.add(a.getWindow().getDecorView());int actions=0;while(!queue.isEmpty()){View v=queue.remove();if(v instanceof TextView){String text=((TextView)v).getText().toString();Assert.assertFalse(text.contains("新地貌")||text.contains("New biome")||text.contains("playtest"));}if(v instanceof com.google.android.material.button.MaterialButton)actions++;if(v instanceof android.view.ViewGroup)for(int k=0;k<((android.view.ViewGroup)v).getChildCount();k++)queue.add(((android.view.ViewGroup)v).getChildAt(k));}Assert.assertEquals("Only single/spectator/WiFi/Bluetooth/help",5,actions);});picture("world169-catalog-main-menu.png");
  }
 }
 @Test public void fiftyOneMapsAndTouchBounds()throws Exception {
  Intent old=new Intent(c(),SinglePlayerGameSetupActivity.class).putExtra("qa_biome_trial",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  try(ActivityScenario<SinglePlayerGameSetupActivity> s=ActivityScenario.launch(old)) {
   final SinglePlayerGameSetupActivity[] a={null};s.onActivity(x->a[0]=x);i().waitForIdleSync();Spinner spinner=a[0].findViewById(R.id.map_spinner);
   Assert.assertEquals(51,StaticBits.NUMBER_OF_MAPS);Assert.assertEquals(23,StaticBits.VERSION_CODE);Assert.assertEquals(52,spinner.getCount());Assert.assertEquals("Ordinary new-map default, no legacy trial path",50,spinner.getSelectedItemPosition());Assert.assertEquals(50,StaticBits.map);
   s.onActivity(v->spinner.setSelection(0));i().waitForIdleSync();
   for(int p=1;p<=51;p++){String name=spinner.getItemAtPosition(p).toString();Assert.assertFalse(name.contains("苔石遗迹")||name.contains("Moss Ruins"));Assert.assertTrue(name.startsWith(String.format(java.util.Locale.ROOT,"%02d",p)));Assert.assertEquals(p,MapCatalog.mapIdForSelection(p));Assert.assertEquals(p,MapCatalog.selectionForMapId(p));}
   Assert.assertFalse(MapCatalog.validMapChoice(0));Assert.assertFalse(MapCatalog.validMapChoice(52));Assert.assertTrue(MapCatalog.validMapChoice(-1));Assert.assertFalse(MapCatalog.validSelection(52));
   View next=a[0].findViewById(R.id.next_button),previous=a[0].findViewById(R.id.previous_button);
   tap(next,false);Assert.assertEquals(1,StaticBits.map);Assert.assertNotNull(((ImageView)a[0].findViewById(R.id.map_imageview)).getDrawable());picture("world169-catalog-first-map.png");
   tap(next,true);Assert.assertEquals(21,StaticBits.map);tap(next,true);Assert.assertEquals(41,StaticBits.map);tap(next,true);Assert.assertEquals(51,StaticBits.map);tap(next,false);Assert.assertEquals(51,StaticBits.map);picture("world169-catalog-last-map.png");
   tap(previous,true);Assert.assertEquals(31,StaticBits.map);tap(previous,true);Assert.assertEquals(11,StaticBits.map);tap(previous,true);Assert.assertEquals(-1,StaticBits.map);tap(previous,false);Assert.assertEquals(-1,StaticBits.map);
   Assert.assertTrue(c().getString(R.string.ui_maps_hint).contains("51"));
  }
 }
}
