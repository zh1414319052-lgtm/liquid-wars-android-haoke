// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.online;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;
import org.json.*;

/** Process-owned room lifetime continues while setup/game activities exchange callbacks. */
public final class OnlineSession {
    public static volatile OnlineSession active;
    public final PlatformApi api;
    public final String code;
    public final boolean host;
    public final RelayTransport relay;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
    private volatile boolean closed;
    private long lastMessage=-1;
    private WeakReference<ImageView> bubble=new WeakReference<>(null);
    private boolean showEmotes=true;
    private volatile String battleId="";
    public OnlineSession(PlatformApi api,String code,boolean host){
        this.api=api;this.code=code;this.host=host;relay=new RelayTransport(api,code,host);
        worker.scheduleWithFixedDelay(this::poll,1,3,TimeUnit.SECONDS);
    }
    private void poll(){
        if(closed)return;
        try{
            api.call("POST","/rooms/"+code+"/heartbeat",null);
            JSONObject state=api.call("GET","/rooms/"+code,null);
            if(!state.optBoolean("is_member")||state.getJSONObject("room").optString("status").equals("closed")){close();return;}
            JSONArray messages=state.optJSONArray("messages");
            if(messages!=null){
                long previous=lastMessage;
                for(int i=0;i<messages.length();i++){
                    JSONObject message=messages.getJSONObject(i);long id=message.getLong("id");
                    if(id>lastMessage)lastMessage=id;
                    if(previous>=0&&id>previous&&showEmotes)show(message.optString("emote_id"),message.optString("nickname"));
                }
                if(lastMessage<0)lastMessage=0;
            }
        }catch(Exception error){if(!api.signedIn())close();}
    }
    private void show(String id,String nickname){
        ImageView image=bubble.get();if(image==null)return;
        image.post(()->{if(!closed&&showEmotes){setImage(image,id);image.setContentDescription(nickname+"的表情");image.setVisibility(View.VISIBLE);image.removeCallbacks(hide);image.postDelayed(hide,3000);}});
    }
    private final Runnable hide=()->{ImageView image=bubble.get();if(image!=null)image.setVisibility(View.GONE);};
    public void attach(Activity activity){
        if(closed)return;
        FrameLayout content=activity.findViewById(android.R.id.content);
        LinearLayout holder=new LinearLayout(activity);holder.setOrientation(LinearLayout.VERTICAL);holder.setGravity(Gravity.END);
        Button button=new Button(activity);button.setText("表情");button.setTextSize(13);button.setAlpha(.68f);
        button.setAllCaps(false);button.setPadding(0,0,0,0);button.setTextColor(0xffdceee7);
        android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();background.setColor(0xaa132b34);background.setCornerRadius(dp(activity,12));button.setBackground(background);
        button.setTypeface(font(activity));holder.addView(button,new LinearLayout.LayoutParams(dp(activity,64),dp(activity,48)));
        ImageView image=new ImageView(activity);image.setVisibility(View.GONE);holder.addView(image,new LinearLayout.LayoutParams(dp(activity,72),dp(activity,72)));bubble=new WeakReference<>(image);
        FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);position.topMargin=dp(activity,64);position.rightMargin=dp(activity,8);content.addView(holder,position);
        button.setOnClickListener(v->pick(activity));
        button.setOnLongClickListener(v->{showEmotes=!showEmotes;if(!showEmotes)image.setVisibility(View.GONE);Toast.makeText(activity,showEmotes?"已显示表情提示":"已隐藏表情提示",Toast.LENGTH_SHORT).show();return true;});
    }
    public void pick(Activity activity){
        if(closed)return;
        GridLayout grid=new GridLayout(activity);grid.setColumnCount(4);int size=dp(activity,64);
        String[] ids={"hello","ready","roger","rally","help","careful","retreat","wait","great","thanks","sorry","phew","haha","wow","gg","again"};
        String[] labels={"你好","准备好了","收到","集合","我来帮忙","小心","撤退","等一下","干得漂亮","谢谢","抱歉","好险","哈哈","震惊","打得好","再来一局"};
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("液滴表情 · 每3秒一次").setView(grid).setNegativeButton("关闭",null).create();
        for(int i=0;i<ids.length;i++){
            String id=ids[i];ImageView icon=new ImageView(activity);icon.setContentDescription(labels[i]);icon.setPadding(4,4,4,4);setImage(icon,id);grid.addView(icon,new android.view.ViewGroup.LayoutParams(size,size));
            icon.setOnClickListener(v->{dialog.dismiss();try{worker.execute(()->{try{api.call("POST","/rooms/"+code+"/emotes",PlatformApi.object("emote_id",id));}catch(Exception e){activity.runOnUiThread(()->Toast.makeText(activity,e.getMessage(),Toast.LENGTH_SHORT).show());}});}catch(RejectedExecutionException ignored){}});
        }
        dialog.show();applyFont(dialog.getWindow().getDecorView(),font(activity));
    }
    public void beginBattle() throws Exception {battleId=api.call("POST","/online/rooms/"+code+"/battle-start",null).getString("battle_id");}
    public void ended(){String id=battleId;battleId="";if(host&&!closed&&!id.isEmpty())worker.execute(()->{try{api.call("POST","/online/rooms/"+code+"/battle-end",PlatformApi.object("battle_id",id));}catch(Exception ignored){}});}
    public synchronized void close(){
        if(closed)return;closed=true;relay.close();bubble.clear();worker.shutdownNow();
        if(active==this)active=null;
        new Thread(()->{try{api.call("POST","/rooms/"+code+"/leave",null);}catch(Exception ignored){}} ,"Leave online room").start();
    }
    public static Typeface font(android.content.Context c){return Typeface.createFromAsset(c.getAssets(),"fonts/NotoSansSC-Intro.ttf");}
    public static void applyFont(View v,Typeface font){if(v instanceof TextView)((TextView)v).setTypeface(font);if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)applyFont(g.getChildAt(i),font);}}
    private static int dp(android.content.Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    private static void setImage(ImageView image,String id){if(!id.matches("[a-z]+"))return;try(java.io.InputStream in=image.getContext().getAssets().open("emotes/"+id+".png")){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;image.setImageBitmap(BitmapFactory.decodeStream(in,null,o));}catch(Exception ignored){}}
}
