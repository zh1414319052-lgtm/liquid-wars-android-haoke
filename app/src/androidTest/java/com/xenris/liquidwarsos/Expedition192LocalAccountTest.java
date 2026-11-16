package com.xenris.liquidwarsos;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.content.Context;
import com.xenris.liquidwarsos.online.PlatformApi;
import org.junit.*;import org.junit.runner.RunWith;import org.json.*;import java.io.*;

/** Opt-in loopback fixture; no production account or SMTP request is possible. */
@RunWith(AndroidJUnit4.class)
public class Expedition192LocalAccountTest {
 @Test public void isolatedHost()throws Exception{
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  Assert.assertTrue(BuildConfig.DEBUG&&c.getPackageName().endsWith("garden186"));
  PlatformApi api=new PlatformApi(c);Assert.assertEquals("http://127.0.0.1:19280",api.origin);
  boolean valid=false;try{valid=api.call("GET","/me",null).getJSONObject("user").getString("email").equals("qa192-host@example.com");}catch(Exception ignored){}
  if(!valid){
  JSONObject challenge=api.call("POST","/auth/code",PlatformApi.object("email","qa192-host@example.com"));
  String id=challenge.getString("challenge_id");
  String code=api.call("GET","/dev/mailbox?challenge_id="+id,null).getString("code");
  JSONObject login=api.call("POST","/auth/verify",PlatformApi.object("challenge_id",id,"code",code,"client","native"));
  api.save(login.getString("access_token"));
  }
  JSONObject room=api.call("POST","/expedition/rooms",PlatformApi.object("protocol",192,"build",BuildConfig.VERSION_CODE,"seats",10)).getJSONObject("room");
  try(FileWriter out=new FileWriter(new File(c.getExternalFilesDir(null),"expedition192-local-room.json"))){out.write(new JSONObject().put("code",room.getString("code")).put("seed",room.getInt("seed")).put("seats",10).put("test_identity","qa192-host@example.com").toString());}
 }
}
