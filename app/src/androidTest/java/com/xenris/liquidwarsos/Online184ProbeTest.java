// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.xenris.liquidwarsos.activities.*;
import com.xenris.liquidwarsos.online.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.util.concurrent.Callable;
import static com.xenris.liquidwarsos.World170ProbeTest.write;

/** Real Android UI + production JNI through the public WSS service; no simulated game screen. */
@RunWith(AndroidJUnit4.class)
public class Online184ProbeTest {
    private Context context;
    private String role;
    private void picture(String name)throws Exception{
        if("solo".equals(role)){World170ProbeTest.picture(name);return;}
        // Android allows only one UiAutomation connection. Both real app processes
        // use the ADB capture driver during the concurrent test instead.
        record("online184-capture.txt",name);
        long end=SystemClock.elapsedRealtime()+5000;
        File ack=new File(context.getFilesDir(),"online184-capture-ack.txt");
        while(SystemClock.elapsedRealtime()<end){
            if(ack.isFile()&&new String(java.nio.file.Files.readAllBytes(ack.toPath()),"UTF-8").equals(name))return;
            SystemClock.sleep(100);
        }
    }
    private static void main(Runnable task){InstrumentationRegistry.getInstrumentation().runOnMainSync(task);}
    private static <T extends Activity> T activity(Class<T> type){
        Activity[] result={null};main(()->{for(Stage stage:Stage.values())for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))if(type.isInstance(a))result[0]=a;});return type.cast(result[0]);
    }
    private static View text(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View found=text(g.getChildAt(i),label);if(found!=null)return found;}}return null;}
    private static void click(Activity a,String label)throws Exception{
        until("interactive control: "+label,15000,()->{boolean[] ready={false};main(()->{ready[0]=text(a.getWindow().getDecorView(),label)!=null;if(a instanceof OnlineLobbyActivity)try{java.lang.reflect.Field busy=OnlineLobbyActivity.class.getDeclaredField("busy");busy.setAccessible(true);ready[0]&=!busy.getBoolean(a);}catch(Exception e){throw new RuntimeException(e);}});return ready[0];});
        main(()->{View found=text(a.getWindow().getDecorView(),label);Assert.assertNotNull(label,found);Assert.assertTrue(found.performClick());});
    }
    private static void until(String message,long ms,Callable<Boolean> predicate)throws Exception{long end=SystemClock.elapsedRealtime()+ms;while(SystemClock.elapsedRealtime()<end){if(predicate.call())return;SystemClock.sleep(100);}Assert.fail(message);}
    private static void front(Activity a){main(()->a.startActivity(new Intent(a,a.getClass()).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));}
    private void record(String name,String value)throws Exception{try(FileOutputStream out=context.openFileOutput(name,Context.MODE_PRIVATE)){out.write(value.getBytes("UTF-8"));}}
    @Test public void onlineJourney() throws Exception {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        JSONObject fixture=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"online184-fixture.json").toPath()),"UTF-8"));
        role=fixture.getString("role");PlatformApi api=new PlatformApi(context);api.save(fixture.getString("token"));
        Intent launch=new Intent(context,OnlineLobbyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
        try{
            until("native lobby login",25000,()->{OnlineLobbyActivity a=activity(OnlineLobbyActivity.class);if(a==null)return false;boolean[] found={false};main(()->found[0]=text(a.getWindow().getDecorView(),"创建好友房间")!=null);return found[0];});
            OnlineLobbyActivity lobby=activity(OnlineLobbyActivity.class);picture("online184-"+role+"-lobby.jpg");
            if(role.equals("solo"))click(lobby,"匹配据点争夺");
            else if(role.equals("host")){
                click(lobby,"创建好友房间");
                until("create room",20000,()->api.call("GET","/me",null).getJSONArray("rooms").length()>0);
                String code=api.call("GET","/me",null).getJSONArray("rooms").getJSONObject(0).getString("code");record("online184-room.txt",code);
                until("ready button",15000,()->{boolean[] f={false};main(()->f[0]=text(lobby.getWindow().getDecorView(),"我准备好了")!=null);return f[0];});click(lobby,"我准备好了");
                until("guest joined and ready",70000,()->{JSONArray ms=api.call("GET","/rooms/"+code,null).getJSONObject("room").getJSONArray("members");if(ms.length()!=2)return false;for(int i=0;i<ms.length();i++)if(!ms.getJSONObject(i).getBoolean("ready"))return false;return true;});
                front(lobby);SystemClock.sleep(1500);click(lobby,"进入游戏设置");
            }else{
                main(()->fillHint(lobby.getWindow().getDecorView(),"输入6位房间码",fixture.optString("room")));
                click(lobby,"加入房间");until("joined",20000,()->{boolean[] f={false};main(()->f[0]=text(lobby.getWindow().getDecorView(),"我准备好了")!=null);return f[0];});click(lobby,"我准备好了");
                until("host transport ready",60000,()->{JSONArray messages=api.call("GET","/rooms/"+fixture.getString("room"),null).getJSONArray("messages");for(int i=0;i<messages.length();i++)if(messages.getJSONObject(i).optString("emote_id").equals("ready"))return true;return false;});
                front(lobby);
            }
            if(!role.equals("guest")){
                until("host setup through real match/room",30000,()->activity(MultiplayerGameSetupActivity.class)!=null&&StaticBits.server!=null&&StaticBits.server.isAccepting());
                if(role.equals("host")){api.call("POST","/rooms/"+OnlineSession.active.code+"/emotes",PlatformApi.object("emote_id","ready"));until("peer handshake",35000,()->StaticBits.server.connectedClientCount()==1);}
                MultiplayerGameSetupActivity setup=activity(MultiplayerGameSetupActivity.class);front(setup);SystemClock.sleep(1000);
                picture("online184-"+role+"-setup.jpg");
                main(()->{StaticBits.dotsPerTeam=500;setup.start(setup.findViewById(R.id.start_button));});
                until("native host game",25000,()->activity(GameServerActivity.class)!=null);GameServerActivity game=activity(GameServerActivity.class);SystemClock.sleep(1200);front(game);
                until("real production simulation advances",35000,()->NativeInterface.playTimeMillis()>8000);
                java.lang.reflect.Field frozen=GameServerActivity.class.getDeclaredField("frozen");frozen.setAccessible(true);frozen.setBoolean(game,true);SystemClock.sleep(1200);
                front(game);SystemClock.sleep(350);picture("online184-"+role+"-battle.jpg");
                int computers=0;for(int t:StaticBits.teams)if(t==StaticBits.AI_PLAYER)computers++;
                Assert.assertEquals(role.equals("solo")?5:4,computers);
                java.lang.reflect.Field step=GameServerActivity.class.getDeclaredField("gameStep");step.setAccessible(true);
                write("online184-"+role+"-result.json",new JSONObject().put("status","PASS").put("role",role).put("computers",computers).put("human_players",6-computers).put("play_ms",NativeInterface.playTimeMillis()).put("hash",Long.toUnsignedString(NativeInterface.stateHash())).put("replicated_hash",Long.toUnsignedString(Online184StateProbe.hashes()[0])).put("host_ai_seed",Long.toUnsignedString(Online184StateProbe.hashes()[2])).put("step",step.getInt(game)).put("public_origin",PlatformApi.ORIGIN));
                if(role.equals("host"))SystemClock.sleep(23000);
            }else{
                until("native guest battle",70000,()->activity(GameClientActivity.class)!=null&&NativeInterface.playTimeMillis()>3500);
                GameClientActivity game=activity(GameClientActivity.class);front(game);SystemClock.sleep(700);picture("online184-guest-playing.jpg");
                until("host settled",25000,()->{int before=NativeInterface.playTimeMillis();if(before<8000)return false;SystemClock.sleep(500);return NativeInterface.playTimeMillis()==before;});
                front(game);SystemClock.sleep(400);picture("online184-guest-battle.jpg");
                java.lang.reflect.Field step=GameClientActivity.class.getDeclaredField("gameStep");step.setAccessible(true);
                write("online184-guest-result.json",new JSONObject().put("status","PASS").put("role",role).put("play_ms",NativeInterface.playTimeMillis()).put("hash",Long.toUnsignedString(NativeInterface.stateHash())).put("replicated_hash",Long.toUnsignedString(Online184StateProbe.hashes()[0])).put("host_ai_seed",Long.toUnsignedString(Online184StateProbe.hashes()[2])).put("step",step.getInt(game)).put("public_origin",PlatformApi.ORIGIN));
                main(()->OnlineSession.active.pick(game));SystemClock.sleep(350);picture("online184-guest-emotes.jpg");
            }
        }finally{
            OnlineSession s=OnlineSession.active;if(s!=null)s.close();
            main(()->{for(Class<? extends Activity> t:new Class[]{GameClientActivity.class,GameServerActivity.class,ClientGameSetupActivity.class,MultiplayerGameSetupActivity.class,OnlineLobbyActivity.class})for(Stage stage:Stage.values())for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))if(t.isInstance(a))a.finish();});
            SystemClock.sleep(1000);
        }
    }
    private static void fillHint(View v,String hint,String value){if(v instanceof EditText&&hint.contentEquals(((EditText)v).getHint()))((EditText)v).setText(value);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)fillHint(g.getChildAt(i),hint,value);}}
}

