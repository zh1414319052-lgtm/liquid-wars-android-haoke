package com.xenris.liquidwarsos;

import android.content.*;
import android.content.res.Configuration;
import android.os.LocaleList;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Same-signature release instrumentation; no rule/inventory injection. */
public class Formal207ReleaseTest {
 private final Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();
 private File baseline(){return new File(app.getExternalFilesDir(null),"formal207-upgrade-baseline.json");}
 private JSONObject prefs()throws Exception{
  SharedPreferences p=app.getSharedPreferences("battle-controls",0),audio=app.getSharedPreferences("liquid_audio",0);
  return new JSONObject().put("hasMode",p.contains("command-mode")).put("mode",p.getInt("command-mode",2))
   .put("hasSound",audio.contains("enabled")).put("sound",audio.getBoolean("enabled",true));
 }
 @Test public void readBaselineOnly()throws Exception{
  JSONObject data=prefs().put("version",app.getPackageManager().getPackageInfo(app.getPackageName(),0).versionName);
  try(FileOutputStream f=new FileOutputStream(baseline())){f.write(data.toString().getBytes(StandardCharsets.UTF_8));}
 }
 @Test public void sameIdentityUpgradeKeepsSettings()throws Exception{
  Assert.assertEquals("com.xenris.liquidwarsos.haoke",app.getPackageName());
  Assert.assertEquals("2.07",app.getPackageManager().getPackageInfo(app.getPackageName(),0).versionName);
  Assert.assertEquals(0,app.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE);
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(FileInputStream f=new FileInputStream(baseline())){byte[] b=new byte[1024];int n;while((n=f.read(b))>=0)bytes.write(b,0,n);}
  JSONObject old=new JSONObject(bytes.toString("UTF-8")),now=prefs();
  for(String k:new String[]{"hasMode","mode","hasSound","sound"})Assert.assertEquals("Upgrade setting "+k,old.get(k),now.get(k));
 }
 @Test public void localizedReleaseIdentity(){
  String[] codes={"zh-CN","en-US","fr-FR"};String[] names=new String[3];
  for(int i=0;i<codes.length;i++){
   Configuration c=new Configuration(app.getResources().getConfiguration());c.setLocales(new LocaleList(Locale.forLanguageTag(codes[i])));
   Context local=app.createConfigurationContext(c);int id=local.getResources().getIdentifier("app_name","string",app.getPackageName());
   names[i]=local.getString(id);Assert.assertFalse(names[i].contains("试玩"));
  }
  Assert.assertTrue(names[0].contains("液态战争"));Assert.assertTrue(names[1].contains("Liquid"));Assert.assertEquals(names[1],names[2]);
 }
}
