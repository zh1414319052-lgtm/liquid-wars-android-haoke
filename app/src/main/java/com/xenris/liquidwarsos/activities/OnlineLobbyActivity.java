// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.xenris.liquidwarsos.online.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Native landscape lobby: one account, explicit command states, no permanent touch needed. */
public class OnlineLobbyActivity extends LocalizedActivity {
    private PlatformApi api;
    private LobbyUi skin;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private LinearLayout body;
    private TextView status,title;
    private ScrollView scroll;
    private EditText emailField,codeField,passwordField,repeatField;
    private Button sendButton;
    private final ArrayList<Button> actions=new ArrayList<>();
    private String challenge="",challengeEmail="",uid="",roomCode="",nickname="",accountEmail="";
    private String page="",authMode="code",emailDraft="",codeDraft="",roomSignature="";
    private boolean foreground,busy,queued,launched,polling,hasPassword;
    private JSONObject room;
    private long generation,resendAt,queueAt;
    private interface Job {JSONObject run() throws Exception;}
    private interface Done {void run(JSONObject value) throws Exception;}

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);requestWindowFeature(Window.FEATURE_NO_TITLE);api=new PlatformApi(this);skin=new LobbyUi(this);
        getWindow().setStatusBarColor(LobbyUi.BG);getWindow().setNavigationBarColor(LobbyUi.BG);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if(saved!=null){emailDraft=saved.getString("email","");codeDraft=saved.getString("code","");challenge=saved.getString("challenge","");challengeEmail=saved.getString("challengeEmail","");authMode=saved.getString("authMode","code");resendAt=saved.getLong("resendAt");queued=saved.getBoolean("queued");queueAt=saved.getLong("queueAt");}
        renderFrame();if(!api.signedIn()||authMode.equals("reset"))login();
    }
    @Override protected void onSaveInstanceState(Bundle out){
        captureDrafts();out.putString("email",emailDraft);out.putString("code",codeDraft);out.putString("challenge",challenge);out.putString("challengeEmail",challengeEmail);out.putString("authMode",authMode);out.putLong("resendAt",resendAt);out.putBoolean("queued",queued);out.putLong("queueAt",queueAt);super.onSaveInstanceState(out);
    }
    @Override public void onResume(){super.onResume();foreground=true;
        if(launched){launched=false;String previous=roomCode;OnlineSession s=OnlineSession.active;if(s!=null)s.close();room=null;roomCode="";
            if(!previous.isEmpty()){work("正在返回大厅…",()->{api.call("POST","/rooms/"+previous+"/leave",null);return new JSONObject();},d->refreshAccount());return;}}
        if(page.equals("auth")&&(!api.signedIn()||authMode.equals("reset"))){updateCooldown();return;}
        refreshAccount();
    }
    @Override public void onPause(){captureDrafts();foreground=false;ui.removeCallbacks(poll);ui.removeCallbacks(cooldown);if(body!=null){body.animate().cancel();body.setAlpha(1);}super.onPause();}
    @Override public void onDestroy(){generation++;ui.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
    private int dp(int v){return skin.dp(v);}
    private boolean compact(){return wide()&&getResources().getConfiguration().screenHeightDp<430;}
    private boolean wide(){return getResources().getConfiguration().screenWidthDp>=700;}
    private void renderFrame(){
        android.widget.FrameLayout frame=new android.widget.FrameLayout(this);frame.setBackground(skin.backdrop());frame.setFitsSystemWindows(true);
        LinearLayout root=skin.column();root.setPadding(dp(20),dp(8),dp(20),dp(8));
        int available=getResources().getConfiguration().screenWidthDp;android.widget.FrameLayout.LayoutParams rootParams=new android.widget.FrameLayout.LayoutParams(available>1120?dp(1120):-1,-1,Gravity.CENTER_HORIZONTAL);frame.addView(root,rootParams);
        LinearLayout header=skin.row();Button back=skin.button("返回",false,this::onBackPressed);header.addView(back,new LinearLayout.LayoutParams(dp(76),dp(48)));
        title=skin.label("液态战争 / 联网大厅",18,LobbyUi.INK,true);skin.ellipsis(title);title.setPadding(dp(16),0,dp(8),0);header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        Button share=skin.button("分享",false,()->share(false));header.addView(share,new LinearLayout.LayoutParams(dp(78),dp(48)));
        Button qq=skin.button("QQ群",false,()->copy("1136036532"));LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(dp(78),dp(48));q.leftMargin=dp(10);header.addView(qq,q);root.addView(header);
        status=skin.label("",13,LobbyUi.MUTED,false);status.setPadding(dp(2),dp(6),dp(2),dp(6));status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(status,new LinearLayout.LayoutParams(-1,-2));
        scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setPadding(0,0,0,dp(8));body=skin.column();scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(frame);
    }
    private void captureDrafts(){if(page.equals("auth")){if(emailField!=null)emailDraft=emailField.getText().toString();if(codeField!=null)codeDraft=codeField.getText().toString();}}
    private void beginPage(String next){captureDrafts();boolean different=!next.equals(page);body.animate().cancel();body.removeAllViews();actions.clear();emailField=codeField=passwordField=repeatField=null;sendButton=null;ui.removeCallbacks(cooldown);if(!next.equals("room"))roomSignature="";page=next;scroll.scrollTo(0,0);if(different)skin.enter(body);else body.setAlpha(1);}
    private void note(String value,boolean error){status.setText(value==null?"连接失败，请重试":value);status.setTextColor(error?LobbyUi.DANGER:LobbyUi.MUTED);}
    private Button action(LinearLayout parent,String label,boolean primary,Runnable job){Button b=skin.button(label,primary,()->{if(!busy)job.run();});actions.add(b);skin.add(parent,b,10);return b;}
    private void lock(boolean locked){busy=locked;for(Button b:actions){if(locked)b.setEnabled(false);else b.setEnabled(!Boolean.TRUE.equals(b.getTag()));}updateCooldown();}
    private LinearLayout columns(){LinearLayout p=wide()?skin.row():skin.column();p.setGravity(Gravity.TOP);body.addView(p,new LinearLayout.LayoutParams(-1,-2));return p;}
    private void column(LinearLayout parent,View child,float weight,boolean gap){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(wide()?0:-1,-2,wide()?weight:0);if(gap){if(wide())lp.leftMargin=dp(20);else lp.topMargin=dp(20);}parent.addView(child,lp);}
    private void label(LinearLayout parent,String text,int size,int color,boolean strong,int margin){skin.add(parent,skin.label(text,size,color,strong),margin);}
    private void hideKeyboard(){View v=getCurrentFocus();if(v!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);}
    private void work(String label,Job job,Done done){
        if(busy||isFinishing()||isDestroyed())return;lock(true);note(label,false);long stamp=generation;
        try{io.execute(()->{try{JSONObject value=job.run();ui.post(()->{if(stamp!=generation||isFinishing()||isDestroyed())return;lock(false);try{done.run(value);}catch(Exception e){note(e.getMessage(),true);}});}catch(Exception e){ui.post(()->{if(stamp!=generation||isFinishing()||isDestroyed())return;lock(false);if(e instanceof PlatformApi.ApiError&&((PlatformApi.ApiError)e).status==410){roomCode="";room=null;home();}if(!api.signedIn()&&!page.equals("auth")){authMode="code";login();}note(e.getMessage(),true);schedule();});}});}catch(RejectedExecutionException ignored){lock(false);}
    }
    private void refreshAccount(){
        if(!api.signedIn()){login();return;}
        work("正在恢复账号…",()->{JSONObject d=api.call("GET","/me",null);if(d.isNull("user")){api.clear();return d;}api.enter();return d;},d->{
            if(d.isNull("user")){login();return;}JSONObject u=d.getJSONObject("user");uid=u.getString("id");nickname=u.getString("nickname");accountEmail=u.getString("email");hasPassword=u.optBoolean("has_password");
            roomCode="";JSONArray rooms=d.optJSONArray("rooms");if(rooms!=null)for(int i=0;i<rooms.length();i++){JSONObject r=rooms.getJSONObject(i);if(r.getString("game_id").equals(PlatformApi.GAME))roomCode=r.getString("code");}home();schedule();
        });
    }
    private void chooseAuth(String mode){captureDrafts();if(!authMode.equals(mode)){challenge="";challengeEmail="";codeDraft="";}authMode=mode;login();}
    private void login(){
        beginPage("auth");title.setText("液态战争 / 联网大厅");boolean reset=authMode.equals("reset"),password=authMode.equals("password"),small=compact();
        note(reset?"密码保存后，其他设备需要重新登录":"一个邮箱账号，通用于浩科旗下游戏",false);
        LinearLayout layout=columns();LinearLayout brand=skin.column();brand.setPadding(dp(10),dp(10),dp(10),dp(8));
        label(brand,"LIQUID WARS OS",13,LobbyUi.MUTED,true,0);label(brand,"让每一滴",compact()?28:36,LobbyUi.INK,true,12);label(brand,"并肩作战",compact()?28:36,LobbyUi.ACCENT,true,0);
        ImageView mascot=skin.mascot("hello",compact()?100:140);brand.addView(mascot,new LinearLayout.LayoutParams(-1,dp(compact()?100:140)));label(brand,"6人对战 · 空位补电脑",15,LobbyUi.ACCENT,true,0);label(brand,"支持公网联机，无需同一 Wi-Fi",12,LobbyUi.MUTED,false,8);
        if(wide())column(layout,brand,.92f,false);
        LinearLayout form=skin.panel();if(small)form.setPadding(dp(16),dp(12),dp(16),dp(12));column(layout,form,1.08f,wide());if(!small)label(form,reset?"设置 / 重置密码":"邮箱登录",25,LobbyUi.INK,true,0);
        if(!reset){LinearLayout tabs=skin.row();Button otp=skin.button("验证码登录",!password,()->chooseAuth("code"));Button pw=skin.button("密码登录",password,()->chooseAuth("password"));actions.add(otp);actions.add(pw);tabs.addView(otp,new LinearLayout.LayoutParams(0,dp(48),1));LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,dp(48),1);b.leftMargin=dp(8);tabs.addView(pw,b);skin.add(form,tabs,small?0:12);}else if(!small)label(form,"验证邮箱后，设置一个新密码",13,LobbyUi.MUTED,false,8);
        TextView emailLabel=skin.label("邮箱地址",13,LobbyUi.INK,false);if(!small)skin.add(form,emailLabel,14);emailField=skin.field("输入你的邮箱",android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);emailField.setContentDescription("邮箱地址");emailField.setId(View.generateViewId());emailLabel.setLabelFor(emailField.getId());emailField.setText(emailDraft);skin.add(form,emailField,small?(reset?0:8):6);
        emailField.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){if(!challengeEmail.isEmpty()&&!s.toString().trim().equalsIgnoreCase(challengeEmail)){challenge="";challengeEmail="";codeDraft="";if(codeField!=null)codeField.setText("");}}public void afterTextChanged(Editable e){}});
        if(!password){if(!small)label(form,"邮箱验证码",13,LobbyUi.INK,false,12);LinearLayout row=skin.row();codeField=skin.field("6位验证码",android.text.InputType.TYPE_CLASS_NUMBER);codeField.setContentDescription("邮箱验证码");codeField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});codeField.setText(codeDraft);row.addView(codeField,new LinearLayout.LayoutParams(0,dp(48),1));sendButton=skin.button("获取验证码",false,this::sendCode);actions.add(sendButton);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(144),dp(48));lp.leftMargin=dp(10);row.addView(sendButton,lp);skin.add(form,row,small?8:6);}
        if(password||reset){if(!small)label(form,reset?"新密码 · 10–128个字符":"密码",13,LobbyUi.INK,false,12);passwordField=skin.field(reset?"新密码（10–128字符）":"输入你的密码",android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);passwordField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128)});passwordField.setSaveEnabled(false);passwordField.setContentDescription(reset?"新密码，10至128字符":"密码");skin.add(form,passwordField,small?8:6);if(reset){repeatField=skin.field("再次输入新密码",android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);repeatField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128)});repeatField.setSaveEnabled(false);repeatField.setContentDescription("再次输入新密码");skin.add(form,repeatField,small?8:10);}}
        LinearLayout submitRow=small?skin.row():form;if(small)skin.add(form,submitRow,8);
        Button enter=action(submitRow,reset?"保存密码并登录":password?"密码登录":"进入大厅",true,this::submitAuth);
        if(small)enter.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),1));
        EditText last=reset?repeatField:password?passwordField:codeField;last.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_ACTION_DONE);last.setOnEditorActionListener((v,id,event)->{if(id==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){enter.performClick();return true;}return false;});
        if(!small)label(form,reset?"首次使用请先验证码注册；密码通用于所有游戏":"首次验证码登录后自动注册",12,LobbyUi.MUTED,false,10);
        Button alternate=action(submitRow,reset?"返回登录":"设置 / 找回密码",false,()->{if(reset&&api.signedIn()){authMode="code";home();}else chooseAuth(reset?"code":"reset");});if(small){LinearLayout.LayoutParams alt=new LinearLayout.LayoutParams(0,dp(48),1);alt.leftMargin=dp(8);alternate.setLayoutParams(alt);}if(reset)title.setText("设置 / 重置密码");updateCooldown();
    }
    private String email(){return emailField.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);}
    private boolean validEmail(String value){if(!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()){emailField.setError("请输入有效邮箱地址");emailField.requestFocus();note("请检查邮箱地址",true);return false;}return true;}
    private void sendCode(){if(busy||SystemClock.elapsedRealtime()<resendAt)return;final String email=email();if(!validEmail(email))return;final String purpose=authMode.equals("reset")?"password":"signin";work("正在发送验证码…",()->api.call("POST","/auth/code",PlatformApi.object("email",email,"purpose",purpose)),d->{challenge=d.getString("challenge_id");challengeEmail=email;resendAt=SystemClock.elapsedRealtime()+d.optInt("retry_after",60)*1000L;note("验证码已发送，请查看邮箱和垃圾邮件",false);updateCooldown();if(codeField!=null)codeField.requestFocus();});}
    private final Runnable cooldown=this::updateCooldown;
    private void updateCooldown(){ui.removeCallbacks(cooldown);if(sendButton==null)return;long seconds=Math.max(0,(resendAt-SystemClock.elapsedRealtime()+999)/1000);sendButton.setText(seconds>0?seconds+"秒后重发":"获取验证码");sendButton.setEnabled(!busy&&seconds==0);if(seconds>0&&foreground)ui.postDelayed(cooldown,1000);}
    private void submitAuth(){
        final String email=email();if(!validEmail(email))return;final boolean password=authMode.equals("password"),reset=authMode.equals("reset");
        final String code=codeField==null?"":codeField.getText().toString().trim();final String secret=passwordField==null?"":passwordField.getText().toString();final String challengeId=challenge;
        if(!password&&(challenge.isEmpty()||!email.equals(challengeEmail))){note("请先为这个邮箱获取验证码",true);return;}
        if(!password&&!code.matches("[0-9]{6}")){codeField.setError("请输入6位验证码");codeField.requestFocus();return;}
        if((password&&secret.isEmpty())||(reset&&(secret.length()<10||secret.length()>128||secret.trim().isEmpty()))){passwordField.setError(reset?"请输入10–128个字符":"请输入密码");passwordField.requestFocus();return;}
        if(reset&&!secret.equals(repeatField.getText().toString())){repeatField.setError("两次密码不一致");repeatField.requestFocus();return;}
        hideKeyboard();work(reset?"正在保存新密码…":"正在登录…",()->{JSONObject data=password?PlatformApi.object("email",email,"password",secret,"client","native"):reset?PlatformApi.object("challenge_id",challengeId,"code",code,"password",secret,"client","native"):PlatformApi.object("challenge_id",challengeId,"code",code,"client","native");JSONObject d=api.call("POST",password?"/auth/password/login":reset?"/auth/password/reset":"/auth/verify",data);api.save(d.getString("access_token"));return d;},d->{challenge="";challengeEmail="";codeDraft="";authMode="code";if(codeField!=null)codeField.setText("");if(passwordField!=null)passwordField.setText("");if(repeatField!=null)repeatField.setText("");refreshAccount();});
    }
    private void home(){
        beginPage(queued?"queue":"home");title.setText(queued?"正在匹配":"联网大厅");note(nickname.isEmpty()?"正在连接账号…":nickname+" · 已登录",false);
        if(!roomCode.isEmpty()){label(body,"正在读取房间 "+roomCode,24,LobbyUi.INK,true,20);return;}
        if(queued){LinearLayout row=columns();ImageView mascot=skin.mascot("wait",220);if(wide())column(row,mascot,.65f,false);LinearLayout q=skin.panel();column(row,q,1.35f,wide());label(q,"寻找并肩作战的玩家",26,LobbyUi.INK,true,0);label(q,"等待8秒后，空位由电脑补齐6队",16,LobbyUi.ACCENT,true,18);label(q,"一个人也能开局。匹配完成后进入准备房间。",14,LobbyUi.MUTED,false,12);ProgressBar loading=new ProgressBar(this);loading.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(LobbyUi.ACCENT));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(30),dp(30));lp.topMargin=dp(20);q.addView(loading,lp);action(q,"取消匹配",false,()->work("正在取消匹配…",()->api.call("POST","/matchmaking/cancel",null),d->{queued=false;acceptQueue(d);if(roomCode.isEmpty())home();}));return;}
        LinearLayout top=skin.row();TextView welcome=skin.heading("准备好，让液滴出发。");top.addView(welcome,new LinearLayout.LayoutParams(0,-2,1));Button account=skin.button("账号",false,this::accountMenu);top.addView(account,new LinearLayout.LayoutParams(dp(82),dp(48)));actions.add(account);skin.add(body,top,0);
        LinearLayout layout=columns();LinearLayout games=skin.column();LinearLayout modes=skin.row();modeCard(modes,"01 / CLASSIC","经典战斗","包围 · 突围 · 吞噬",0,false);modeCard(modes,"02 / CONTROL","据点争夺","占领 · 产兵 · 分兵",1,true);games.addView(modes);if(!compact())label(games,"等待8秒后空位补电脑，一个人也能开局",14,LobbyUi.MUTED,false,16);column(layout,games,1.8f,false);
        LinearLayout friends=skin.panel();if(compact())friends.setPadding(dp(16),dp(12),dp(16),dp(12));label(friends,"与朋友一起玩",20,LobbyUi.INK,true,0);action(friends,"创建好友房间",false,()->work("正在建房…",()->api.call("POST","/rooms",PlatformApi.object("game_id",PlatformApi.GAME)),d->{room=d.getJSONObject("room");roomCode=room.getString("code");renderRoom();schedule();}));
        EditText input=skin.field("输入6位房间码",android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);input.setContentDescription("好友房间码");input.setFilters(new InputFilter[]{new InputFilter.AllCaps(),new InputFilter.LengthFilter(6)});skin.add(friends,input,12);
        action(friends,"加入房间",false,()->{final String code=input.getText().toString().trim().toUpperCase(java.util.Locale.ROOT);if(!code.matches("[A-Z2-9]{6}")){input.setError("请输入6位房间码");input.requestFocus();return;}hideKeyboard();work("正在加入房间…",()->api.call("POST","/rooms/"+code+"/join",PlatformApi.object("game_id",PlatformApi.GAME)),d->{room=d.getJSONObject("room");roomCode=room.getString("code");renderRoom();schedule();});});column(layout,friends,1,true);if(wide()){LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)friends.getLayoutParams();lp.leftMargin=dp(18);friends.setLayoutParams(lp);}else skin.gap(body,12);
    }
    private void modeCard(LinearLayout parent,String eyebrow,String title,String subtitle,int mode,boolean gap){LinearLayout card=skin.panel();label(card,eyebrow,12,LobbyUi.ACCENT,true,0);label(card,title,24,LobbyUi.INK,true,18);label(card,subtitle,14,LobbyUi.MUTED,false,10);action(card,mode==0?"匹配经典战斗":"匹配据点争夺",true,()->match(mode));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.topMargin=dp(16);if(gap)lp.leftMargin=dp(14);parent.addView(card,lp);}
    private void accountMenu(){LinearLayout panel=skin.panel();label(panel,nickname,22,LobbyUi.INK,true,0);label(panel,accountEmail,14,LobbyUi.MUTED,false,8);AlertDialog dialog=new AlertDialog.Builder(this).setView(panel).create();Button password=skin.button(hasPassword?"修改 / 重置密码":"设置登录密码",true,()->{dialog.dismiss();emailDraft=accountEmail;chooseAuth("reset");});skin.add(panel,password,20);skin.add(panel,skin.button("退出账号",false,()->{dialog.dismiss();work("正在退出…",()->api.call("POST","/auth/logout",null),d->{api.clear();nickname="";accountEmail="";authMode="code";login();});}),10);skin.add(panel,skin.button("返回大厅",false,dialog::dismiss),10);showDialog(dialog);}
    private void match(int mode){hideKeyboard();work("加入匹配队列…",()->{JSONObject j=api.hello();j.put("mode",mode);return api.call("POST","/matchmaking/enqueue",j);},d->{queued=true;queueAt=SystemClock.elapsedRealtime();acceptQueue(d);if(roomCode.isEmpty())home();schedule();});}
    private void acceptQueue(JSONObject d)throws Exception{if(d.optString("state").equals("room")){queued=false;room=d.getJSONObject("room");roomCode=room.getString("code");renderRoom();}else if(d.optString("state").equals("idle")){queued=false;home();}}
    private void renderRoom()throws Exception{
        if(room==null)return;if(room.optString("status").equals("playing")){launch();return;}if(room.optString("status").equals("closed")){room=null;roomCode="";home();note("房间已关闭，请重新创建或匹配",true);return;}
        JSONArray members=room.getJSONArray("members");String hostId=room.getString("host_id");StringBuilder sig=new StringBuilder(roomCode).append(hostId);boolean ownReady=false,allReady=members.length()>0;
        for(int i=0;i<members.length();i++){JSONObject m=members.getJSONObject(i);sig.append(m.optString("user_id")).append(m.optString("nickname")).append(m.optBoolean("ready")).append(m.optBoolean("online"));if(m.getString("user_id").equals(uid))ownReady=m.optBoolean("ready");allReady&=m.optBoolean("ready")&&m.optBoolean("online");}
        if(page.equals("room")&&sig.toString().equals(roomSignature))return;int oldScroll=page.equals("room")?scroll.getScrollY():0;beginPage("room");roomSignature=sig.toString();title.setText("好友房间 / 准备出发");note("真人 "+members.length()+" 人 · 电脑补位 "+Math.max(0,6-members.length())+" 队",false);
        LinearLayout heading=skin.row();TextView code=skin.label("房间码  "+roomCode,20,LobbyUi.INK,true);heading.addView(code,new LinearLayout.LayoutParams(0,dp(48),1));Button copy=skin.button("复制",false,()->copy(roomCode));heading.addView(copy,new LinearLayout.LayoutParams(dp(64),dp(48)));Button invite=skin.button("邀请",false,()->share(true));LinearLayout.LayoutParams inv=new LinearLayout.LayoutParams(dp(64),dp(48));inv.leftMargin=dp(8);heading.addView(invite,inv);Button leave=skin.button("离开",false,this::confirmLeave);actions.add(leave);LinearLayout.LayoutParams lv=new LinearLayout.LayoutParams(dp(64),dp(48));lv.leftMargin=dp(8);heading.addView(leave,lv);skin.add(body,heading,0);
        LinearLayout layout=columns();LinearLayout seats=skin.column();int cols=wide()?3:2;
        for(int row=0;row<(6+cols-1)/cols;row++){LinearLayout line=skin.row();line.setGravity(Gravity.TOP);for(int c=0;c<cols;c++){int i=row*cols+c;if(i>=6)break;JSONObject m=i<members.length()?members.getJSONObject(i):null;boolean bot=m==null;LinearLayout seat=skin.panel();seat.setPadding(dp(13),dp(12),dp(13),dp(12));String name=bot?"电脑补位":m.getString("nickname");String role=bot?"电脑 "+(i-members.length()+1):m.getString("user_id").equals(hostId)?"房主" :"玩家";if(!bot&&m.getString("user_id").equals(uid))role+=" · 你";label(seat,role,12,LobbyUi.MUTED,false,0);TextView player=skin.label(name,16,LobbyUi.INK,true);skin.ellipsis(player);skin.add(seat,player,9);String ready=bot?"开局自动加入":!m.optBoolean("online")?"暂时离线":m.optBoolean("ready")?"已准备":"未准备";label(seat,ready,12,!bot&&m.optBoolean("ready")?LobbyUi.ACCENT:LobbyUi.MUTED,false,8);seat.setContentDescription(role+"，"+name+"，"+ready);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(c>0)lp.leftMargin=dp(10);line.addView(seat,lp);}skin.add(seats,line,12);}column(layout,seats,1.9f,false);
        LinearLayout controls=skin.panel();if(compact())controls.setPadding(dp(16),dp(12),dp(16),dp(12));label(controls,allReady?"全员已就绪":"等待朋友准备",21,LobbyUi.INK,true,0);label(controls,"由房主选择地图、兵力并开始",14,LobbyUi.MUTED,false,10);final boolean ready=!ownReady;
        action(controls,ownReady?"取消准备":"我准备好了",!ownReady,()->work("更新准备状态…",()->api.call("POST","/rooms/"+roomCode+"/ready",PlatformApi.object("ready",ready)),d->{roomSignature="";refreshRoomSoon();}));
        if(hostId.equals(uid)){Button start=action(controls,allReady?"进入游戏设置":"等待全员就绪",allReady,()->work("正在检查玩家连接…",()->api.call("POST","/rooms/"+roomCode+"/start",null),d->refreshRoomSoon()));start.setTag(!allReady);start.setEnabled(allReady);}else label(controls,"房主开始后会自动进入游戏",13,LobbyUi.MUTED,false,12);
        column(layout,controls,1,wide());LinearLayout.LayoutParams cp=(LinearLayout.LayoutParams)controls.getLayoutParams();cp.topMargin=dp(12);controls.setLayoutParams(cp);
        if(oldScroll>0)scroll.post(()->scroll.scrollTo(0,oldScroll));
    }
    private void refreshRoomSoon(){ui.removeCallbacks(poll);if(foreground)ui.post(poll);}
    private void launch()throws Exception{if(launched||!foreground)return;launched=true;queued=false;ui.removeCallbacks(poll);boolean host=room.getString("host_id").equals(uid);OnlineSession existing=OnlineSession.active;if(existing!=null)existing.close();OnlineSession.active=new OnlineSession(api,roomCode,host);Intent intent=new Intent(this,host?MultiplayerGameSetupActivity.class:ClientGameSetupActivity.class);intent.putExtra("online",true);intent.putExtra("onlineMode",room.optInt("mode",0));intent.putExtra("name","公网房间 "+roomCode);intent.putExtra("ip","cleanbizai.com");startActivity(intent);}
    private void leave(){work("正在离开房间…",()->api.call("POST","/rooms/"+roomCode+"/leave",null),d->{roomCode="";room=null;home();});}
    private void confirmLeave(){if(busy)return;LinearLayout panel=skin.panel();label(panel,"离开房间？",24,LobbyUi.INK,true,0);boolean host=room!=null&&uid.equals(room.optString("host_id"));label(panel,host?"你是房主，离开会结束这个公网房间。":"离开后可以通过房间码重新加入等待房间。",16,LobbyUi.MUTED,false,14);AlertDialog dialog=new AlertDialog.Builder(this).setView(panel).create();skin.add(panel,skin.button("留下",true,dialog::dismiss),20);skin.add(panel,skin.button("确认离开",false,()->{dialog.dismiss();leave();}),10);showDialog(dialog);}
    private void showDialog(AlertDialog d){hideKeyboard();d.show();if(d.getWindow()!=null){d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);d.getWindow().setLayout(Math.min(dp(430),getResources().getDisplayMetrics().widthPixels-dp(40)),-2);}}
    private final Runnable poll=()->{
        if(!foreground||launched||!api.signedIn())return;if(busy||polling){schedule();return;}final String code=roomCode;final long stamp=generation;if(code.isEmpty()&&!queued)return;polling=true;
        try{io.execute(()->{try{JSONObject d;if(code.isEmpty())d=api.call("GET","/matchmaking/status",null);else{api.call("POST","/rooms/"+code+"/heartbeat",null);d=api.call("GET","/rooms/"+code,null);}ui.post(()->{polling=false;if(stamp!=generation||isFinishing()||!foreground)return;if(!busy&&code.equals(roomCode))try{if(code.isEmpty())acceptQueue(d);else if(!d.optBoolean("is_member")){roomCode="";room=null;home();}else{room=d.getJSONObject("room");renderRoom();}}catch(Exception e){note(e.getMessage(),true);}schedule();});}catch(Exception e){ui.post(()->{polling=false;if(stamp!=generation||isFinishing())return;if(!busy){if(!api.signedIn()){authMode="code";login();}else if(e instanceof PlatformApi.ApiError&&((PlatformApi.ApiError)e).status==410){roomCode="";room=null;home();}note(e.getMessage(),true);}schedule();});}});}catch(RejectedExecutionException ignored){polling=false;}
    };
    private void schedule(){ui.removeCallbacks(poll);if(foreground&&!launched)ui.postDelayed(poll,2000);}
    private void share(boolean invite){
        Intent send=GameShare.message(invite?roomCode:"");
        try{startActivity(Intent.createChooser(send,invite?"邀请好友":"分享游戏"));}
        catch(android.content.ActivityNotFoundException e){copy(send.getStringExtra(Intent.EXTRA_TEXT));}
    }
    private void copy(String value){android.content.ClipboardManager c=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);c.setPrimaryClip(android.content.ClipData.newPlainText("浩科游戏",value));Toast.makeText(this,"已复制",Toast.LENGTH_SHORT).show();}
    @Override public void onBackPressed(){if(busy)return;if(page.equals("auth")&&authMode.equals("reset")){authMode="code";if(api.signedIn())home();else login();return;}if(queued){work("取消匹配…",()->api.call("POST","/matchmaking/cancel",null),d->{queued=false;acceptQueue(d);if(roomCode.isEmpty())home();});}else if(!roomCode.isEmpty())confirmLeave();else super.onBackPressed();}
}
