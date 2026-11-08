// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.activities;

import android.app.AlertDialog;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.os.*;
import android.text.*;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.xenris.liquidwarsos.*;
import com.xenris.liquidwarsos.cloud.*;
import java.util.*;
import java.util.concurrent.*;

/** Landscape, per-team editor. Credentials remain in encrypted storage or this RAM-only session. */
public class ModelSettingsActivity extends LocalizedActivity {
    private static final String[] FORMATS={"Chat Completions","Responses","Claude Messages","Gemini streaming", "SimpleTool function_call"};
    private static final class Session {
        final ModelSetupDraft[] drafts=new ModelSetupDraft[6];
        final boolean[] loadFailed=new boolean[6];
        int selected,tab,pairing,savedPairing; boolean paired,savedPaired;
    }
    private Session session;
    private boolean zh,binding,busy;
    private int bg,surface,field,ink,muted,accent,onAccent,line;
    private LinearLayout root,body,rail,details,editor;
    private TextView notice;
    private EditText key;
    private Switch enabled;
    private Button save,fetch,providerButton,modelButton;
    private final Button[] teamButtons=new Button[6];
    private AlertDialog popup;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor io=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1),r->{Thread t=new Thread(r,"Model list");t.setDaemon(true);return t;},new ThreadPoolExecutor.DiscardOldestPolicy());
    private ModelDirectory request;
    private Future<?> future;
    private Runnable deadline;
    private long generation;

    private String t(String cn,String en){return zh?cn:en;}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private ModelSetupDraft draft(){return session.drafts[session.selected];}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        zh="zh".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
        bg=getColor(R.color.ui_bg);surface=getColor(R.color.ui_surface);field=getColor(R.color.ui_field);
        ink=getColor(R.color.ui_ink);muted=getColor(R.color.ui_muted);accent=getColor(R.color.ui_accent);
        onAccent=getColor(R.color.ui_onAccent);line=getColor(R.color.ui_line);
        Object retained=getLastCustomNonConfigurationInstance();
        if(retained instanceof Session)session=(Session)retained;
        else{
            session=new Session();
            for(int i=0;i<6;i++)try{session.drafts[i]=new ModelSetupDraft(ModelStore.load(this,i));}
            catch(Exception e){session.drafts[i]=new ModelSetupDraft(ModelConfig.empty());session.loadFailed[i]=true;}
            SharedPreferences p=getSharedPreferences("alliance_rules",0);
            session.paired=session.savedPaired=p.getBoolean("paired",false);
            session.pairing=session.savedPairing=Math.max(0,Math.min(14,p.getInt("pairing",0)));
            if(state!=null){session.selected=Math.max(0,Math.min(5,state.getInt("team")));session.tab=state.getInt("tab")==1?1:0;}
        }
        render();
    }
    @Override public Object onRetainCustomNonConfigurationInstance(){capture();return session;}
    @Override protected void onSaveInstanceState(Bundle out){capture();out.putInt("team",session.selected);out.putInt("tab",session.tab);super.onSaveInstanceState(out);}
    @Override protected void onStop(){cancelQuery();super.onStop();}
    @Override protected void onDestroy(){cancelQuery();io.shutdownNow();ui.removeCallbacksAndMessages(null);if(popup!=null)popup.dismiss();super.onDestroy();}
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView text(String value,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);
        v.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));v.setIncludeFontPadding(false);return v;
    }
    private void gap(LinearLayout p,int n){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
    private void hgap(LinearLayout p,int n){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(dp(n),1));}
    private Button button(String title,boolean primary,Runnable action){
        Button b=new Button(this);b.setText(title);b.setTextSize(14);b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));b.setTextColor(primary?onAccent:ink);
        b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setMinWidth(0);b.setMinimumWidth(0);
        b.setPadding(dp(14),dp(8),dp(14),dp(8));b.setBackgroundTintList(null);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x30ffffff),shape(primary?accent:field),null));
        b.setStateListAnimator(null);b.setOnClickListener(v->action.run());return b;
    }
    private void weighted(LinearLayout row,View v){row.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
    private ScrollView scroll(LinearLayout content){ScrollView s=new ScrollView(this);s.setFillViewport(false);s.setClipToPadding(false);s.addView(content,new ScrollView.LayoutParams(-1,-2));return s;}
    private void label(LinearLayout p,String s){TextView v=text(s,13,muted,false);p.addView(v);gap(p,6);}
    private EditText input(String value,String hint,boolean secret){
        EditText e=new EditText(this);e.setSingleLine();e.setTextSize(15);e.setTextColor(ink);e.setHintTextColor(muted);
        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|(secret?android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD:android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS));
        e.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        e.setSaveEnabled(false);e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        e.setBackground(shape(field));e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setMinimumHeight(dp(48));
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(secret?4096:512)});e.setText(value);e.setHint(hint);return e;
    }
    private void render(){
        binding=true;key=null;enabled=null;notice=null;save=null;fetch=null;
        root=column();root.setBackgroundColor(bg);root.setPadding(dp(16),dp(8),dp(16),dp(12));root.setFitsSystemWindows(true);
        LinearLayout head=row();LinearLayout title=column();title.addView(text("LIQUID WARS",10,muted,true));gap(title,4);title.addView(text(t("AI 指挥室","AI command room"),22,ink,true));
        head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        head.addView(button(t("队伍 AI","Team AI"),session.tab==0,()->tab(0)));hgap(head,8);
        head.addView(button(t("组队规则","Alliances"),session.tab==1,()->tab(1)));hgap(head,8);
        head.addView(button(t("返回","Back"),false,this::leave));root.addView(head);gap(root,12);
        body=row();body.setGravity(Gravity.TOP);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        if(session.tab==0)renderTeams();else renderRules();
        setContentView(root);binding=false;
    }
    private void tab(int tab){capture();cancelQuery();hideKeyboard();session.tab=tab;render();}
    private void renderTeams(){
        rail=column();int width=Math.max(144,Math.min(208,getResources().getConfiguration().screenWidthDp/4));
        body.addView(scroll(rail),new LinearLayout.LayoutParams(dp(width),-1));hgap(body,16);
        label(rail,t("选择队伍","Choose a team"));
        for(int i=0;i<6;i++){
            final int team=i;ModelConfig cfg=session.drafts[i].value();
            String state=session.drafts[i].dirty()?t("未保存","Unsaved"):cfg.enabled?t("模型 AI","Model AI"):t("本地 AI","Local AI");
            Button b=button((i+1)+"  "+Util.teamToNameString(this,i)+"\n"+state,false,()->{capture();cancelQuery();hideKeyboard();session.selected=team;render();});
            b.setTextColor(TeamPalette.color(i));b.setTextSize(14);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
            GradientDrawable tile=shape(i==session.selected?field:surface);if(i==session.selected)tile.setStroke(dp(1),TeamPalette.color(i));
            b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x20ffffff),tile,null));teamButtons[i]=b;rail.addView(b,new LinearLayout.LayoutParams(-1,-2));gap(rail,6);
        }
        gap(rail,6);rail.addView(text(t("真人队伍由玩家操控","Human teams stay player-controlled"),12,muted,false));
        LinearLayout right=column();body.addView(right,new LinearLayout.LayoutParams(0,-1,1));
        details=column();details.setPadding(dp(16),dp(12),dp(16),dp(12));details.setBackground(shape(surface));
        right.addView(scroll(details),new LinearLayout.LayoutParams(-1,0,1));
        renderForm();
        gap(right,10);LinearLayout actions=row();
        fetch=button(t("获取模型","Fetch models"),false,()->query(false,true));weighted(actions,fetch);hgap(actions,10);
        save=button(t("保存此队","Save team"),true,this::saveTeam);weighted(actions,save);right.addView(actions);
    }
    private void renderForm(){
        ModelConfig cfg=draft().value();
        LinearLayout top=row();top.addView(text((session.selected+1)+"  "+Util.teamToNameString(this,session.selected),21,ink,true),new LinearLayout.LayoutParams(0,-2,1));
        enabled=new Switch(this);enabled.setText(t("使用模型","Model AI"));enabled.setTextColor(ink);enabled.setTextSize(14);enabled.setMinimumHeight(dp(48));enabled.setChecked(cfg.enabled);
        enabled.setOnCheckedChangeListener((b,v)->{if(!binding){capture();cancelQuery();notice(t("更改后请保存此队","Save this team to apply changes"),false);}});top.addView(enabled);details.addView(top);gap(details,12);
        LinearLayout choices=row(),service=column(),model=column();
        weighted(choices,service);hgap(choices,12);weighted(choices,model);
        label(service,t("服务商","Provider"));providerButton=button(ModelProviders.ALL[ModelProviders.find(cfg)].name,false,this::chooseProvider);providerButton.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);service.addView(providerButton,new LinearLayout.LayoutParams(-1,-2));
        label(model,t("模型 · 可切换","Model · selectable"));modelButton=button(cfg.model.isEmpty()?t("自动选择","Choose automatically"):ModelProviders.title(cfg.model),false,this::chooseModel);modelButton.setMaxLines(2);model.addView(modelButton,new LinearLayout.LayoutParams(-1,-2));details.addView(choices);gap(details,14);
        if(cfg.protocol==ModelConfig.SIMPLETOOL)details.addView(text(t("SimpleTool 需自行部署 HTTPS 服务。模型由服务端加载；获取模型可核对当前型号。切换模型需在服务端操作。","SimpleTool needs your HTTPS server. Fetch models checks its loaded model; changing models requires server-side setup."),13,muted,false));
        label(details,cfg.protocol==ModelConfig.SIMPLETOOL?t("API Key（服务端无认证时可留空）","API Key (optional if server has no auth)"):"API Key");LinearLayout secret=row();key=input(cfg.key,t("粘贴你的 API Key","Paste your API key"),true);secret.addView(key,new LinearLayout.LayoutParams(0,-2,1));hgap(secret,10);secret.addView(button(t("粘贴","Paste"),false,this::pasteKey));details.addView(secret);
        key.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int before,int count){if(!binding){if(s.length()>0&&!enabled.isChecked()){binding=true;enabled.setChecked(true);binding=false;}capture();cancelQuery();notice(t("密钥仅保存到本机；点击保存后生效","Stored only on this device; save to apply"),false);}}public void afterTextChanged(Editable e){}});
        gap(details,10);LinearLayout extras=row();
        TextView automatic=text(cfg.model.isEmpty()?t("地址已配置，保存时自动获取模型","Endpoint set; save to discover a model"):t("地址与接口已自动配置","Endpoint and API format are configured"),13,accent,false);
        extras.addView(automatic,new LinearLayout.LayoutParams(0,-2,1));hgap(extras,8);extras.addView(button(t("高级设置","Advanced"),false,this::advanced));details.addView(extras);gap(details,8);
        notice=text(draft().dirty()?t("有未保存的更改","You have unsaved changes"):t("各队独立配置 · 未启用时使用本地 AI","Independent per team · local AI when disabled"),13,muted,false);notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);details.addView(notice);gap(details,8);
        if(session.loadFailed[session.selected])notice(t("旧配置无法解密，原文件未改动。请重新填写后保存。","Saved settings could not be decrypted. Original file kept; enter settings again to save."),true);
        details.addView(text(t("密钥加密保存在本机。获取列表不发送对战指令；对战调用由服务商计费。每队持续请求、最多一个在途；模型直接控制五个触点。等待、过期、错误或停止 API 时，模型队伍原地停住，绝不由本地 AI 接管。","Keys are encrypted locally. Discovery sends no game prompt. Providers charge for match requests: continuous requests, one in flight per team. Models directly control five fingers; model teams stop while waiting or after expired/failed replies; local AI never takes over."),12,muted,false));
    }
    private void capture(){if(binding||session==null||session.tab!=0||key==null||enabled==null)return;ModelConfig c=draft().value();draft().edit(new ModelConfig(enabled.isChecked(),c.protocol,c.base,c.model,key.getText().toString()));if(teamButtons[session.selected]!=null){String state=draft().dirty()?t("未保存","Unsaved"):draft().value().enabled?t("模型 AI","Model AI"):t("本地 AI","Local AI");teamButtons[session.selected].setText((session.selected+1)+"  "+Util.teamToNameString(this,session.selected)+"\n"+state);}}
    private void notice(String message,boolean error){if(notice!=null){notice.setText(message);notice.setTextColor(error?0xffe3a49b:accent);}}
    private void pasteKey(){
        android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(cm==null||!cm.hasPrimaryClip()||cm.getPrimaryClip()==null||cm.getPrimaryClip().getItemCount()==0){notice(t("剪贴板没有文字","No text in clipboard"),true);return;}
        CharSequence value=cm.getPrimaryClip().getItemAt(0).getText();
        if(value==null||value.length()>4096){notice(t("剪贴板内容不是有效密钥","Clipboard text is not a valid key"),true);return;}
        key.setText(value.toString().trim());key.setSelection(key.length());
    }
    private void chooseProvider(){
        capture();String[] names=new String[ModelProviders.ALL.length];for(int i=0;i<names.length;i++)names[i]=ModelProviders.ALL[i].name;
        popup=new AlertDialog.Builder(this).setTitle(t("选择服务商","Choose provider")).setSingleChoiceItems(names,ModelProviders.find(draft().value()),(d,i)->{
            int old=ModelProviders.find(draft().value());d.dismiss();if(old==i)return;cancelQuery();draft().provider(i);render();if(i>=ModelProviders.ALL.length-2)advanced();
        }).setNegativeButton(android.R.string.cancel,null).show();
    }
    private void chooseModel(){
        capture();List<String> models=draft().models();if(models.isEmpty()){query(false,true);return;}modelPicker(models);
    }
    private void modelPicker(List<String> models){
        LinearLayout panel=column();panel.setPadding(dp(16),dp(8),dp(16),dp(4));
        EditText search=input("",t("搜索模型名称","Search models"),false);panel.addView(search);gap(panel,8);
        ListView list=new ListView(this);panel.addView(list,new LinearLayout.LayoutParams(-1,dp(Math.min(240,Math.max(120,getResources().getConfiguration().screenHeightDp/2)))));
        ArrayList<String> visible=new ArrayList<>(models);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,visible){
            @Override public View getView(int p,View recycled,android.view.ViewGroup parent){TextView v=(TextView)super.getView(p,recycled,parent);v.setTextColor(ink);v.setTextSize(15);v.setMinHeight(dp(48));v.setPadding(dp(8),dp(10),dp(8),dp(10));v.setSingleLine(false);return v;}
        };list.setAdapter(adapter);
        popup=new AlertDialog.Builder(this).setTitle(t("选择此队的模型","Choose this team's model")).setView(panel)
                .setNeutralButton(t("手动填写","Enter manually"),(d,w)->advanced()).setNegativeButton(android.R.string.cancel,null).create();
        list.setOnItemClickListener((p,v,pos,id)->{draft().model(visible.get(pos));popup.dismiss();render();notice(t("模型已选择，保存后生效","Model selected; save to apply"),false);});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){visible.clear();for(String m:models)if(m.toLowerCase(Locale.ROOT).contains(s.toString().toLowerCase(Locale.ROOT)))visible.add(m);adapter.notifyDataSetChanged();}public void afterTextChanged(Editable e){}});
        popup.show();popup.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
    private void advanced(){
        capture();ModelConfig cfg=draft().value();LinearLayout p=column();p.setPadding(dp(16),dp(8),dp(16),dp(8));
        p.addView(text(t("官方服务通常无需修改。更改地址或格式后需要重新填写密钥。","Official providers need no changes here. Changing endpoint or format clears the key."),13,muted,false));gap(p,12);
        label(p,t("服务地址（HTTPS）","Service URL (HTTPS)"));EditText base=input(cfg.base,"https://",false);p.addView(base);gap(p,12);
        label(p,t("接口格式","API format"));Spinner format=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,R.layout.ui_spinner_item,FORMATS);adapter.setDropDownViewResource(R.layout.ui_spinner_dropdown_item);format.setAdapter(adapter);format.setSelection(cfg.protocol);format.setMinimumHeight(dp(48));p.addView(format);gap(p,12);
        label(p,t("自定义模型 ID（留空自动获取）","Custom model ID (blank: discover)"));EditText model=input(cfg.model,"",false);p.addView(model);gap(p,12);
        p.addView(button(t("清除此队配置与密钥","Clear this team's settings and key"),false,()->{popup.dismiss();clearTeam();}));
        popup=new AlertDialog.Builder(this).setTitle(t("高级设置","Advanced")).setView(scroll(p)).setPositiveButton(t("应用更改","Apply"),null).setNegativeButton(android.R.string.cancel,null).create();
        popup.setOnShowListener(d->popup.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{
                ModelConfig c=new ModelConfig(cfg.enabled,format.getSelectedItemPosition(),base.getText().toString(),model.getText().toString(),cfg.key);
                new ModelConfig(true,c.protocol,c.base,c.model.isEmpty()?"discovery":c.model,"validation-only").validate();
                cancelQuery();draft().edit(c);popup.dismiss();render();notice(t("高级设置已应用，请保存此队","Advanced settings applied; save this team"),false);
            }catch(Exception e){base.setError(t("请检查 HTTPS 地址和模型 ID","Check HTTPS URL and model ID"));}
        }));
        popup.show();popup.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);popup.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private void clearTeam(){
        final int team=session.selected;
        popup=new AlertDialog.Builder(this).setTitle(t("清除此队配置？","Clear this team?")).setMessage((team+1)+" · "+Util.teamToNameString(this,team)+"\n"+t("删除此队保存的密钥，并改回本地 AI。其它队伍不变。","Delete this team's stored key and use local AI. Other teams stay unchanged."))
            .setPositiveButton(t("清除","Clear"),(d,w)->{cancelQuery();ModelStore.clear(this,team);session.drafts[team]=new ModelSetupDraft(ModelConfig.empty());session.loadFailed[team]=false;render();notice(t("此队配置已清除","Team settings cleared"),false);}).setNegativeButton(android.R.string.cancel,null).show();
    }
    private void saveTeam(){
        capture();ModelConfig c=draft().value();
        if(c.enabled&&c.model.isEmpty()){query(true,false);return;}persist();
    }
    private void persist(){
        try{ModelStore.save(this,session.selected,draft().value());draft().saved();session.loadFailed[session.selected]=false;render();notice(t("已保存 · 开局后使用；尚未验证实际调用","Saved · used next match; inference has not been tested"),false);}
        catch(Exception e){notice(t("保存失败：请检查密钥、模型或自定义地址。原配置仍保留。","Could not save. Check key, model or custom URL. Previous settings remain."),true);}
    }
    private void setBusy(boolean value){busy=value;if(fetch!=null){fetch.setEnabled(!value);fetch.setText(value?t("正在获取…","Fetching…"):t("获取模型","Fetch models"));}if(save!=null)save.setEnabled(!value);}
    private void cancelQuery(){
        if(busy)notice(t("获取已取消，草稿已保留；可再次获取。","Fetch cancelled. Draft kept; you can retry."),false);
        generation++;if(deadline!=null)ui.removeCallbacks(deadline);deadline=null;
        if(request!=null)request.cancel();request=null;if(future!=null)future.cancel(true);future=null;
        io.getQueue().clear();setBusy(false);
    }
    private void query(boolean saveAfter,boolean pickAfter){
        capture();cancelQuery();final ModelSetupDraft owner=draft();final ModelConfig config=owner.value();
        try{new ModelConfig(true,config.protocol,config.base,"discovery",config.key).validate();}
        catch(Exception e){notice(t("请检查 API Key 和 HTTPS 地址；SimpleTool 无认证时可留空密钥","Check API key and HTTPS URL; an unauthenticated SimpleTool server needs no key"),true);return;}
        hideKeyboard();final long token=generation,revision=owner.revision();final int team=session.selected;
        final ModelDirectory job=new ModelDirectory();request=job;setBusy(true);notice(t("正在读取服务商的模型列表…","Reading the provider's model list…"),false);
        deadline=()->{if(generation==token){cancelQuery();notice(t("读取超时，配置已保留。可重试或在高级设置选用自定义模型。","Timed out. Your draft is safe. Retry or enter a model in Advanced."),true);}};ui.postDelayed(deadline,15000);
        future=io.submit(()->{
            ModelDirectory.Result result=null;Exception error=null;try{result=job.fetch(config);}catch(Exception e){error=e;}
            final ModelDirectory.Result answer=result;final Exception failure=error;
            ui.post(()->{
                if(generation!=token||isFinishing()||isDestroyed()||session.selected!=team||owner.revision()!=revision)return;
                if(deadline!=null)ui.removeCallbacks(deadline);deadline=null;request=null;future=null;setBusy(false);
                if(failure!=null){String code=failure instanceof ModelHttp.Failure?"HTTP "+((ModelHttp.Failure)failure).status+": ":"";
                    notice(code+t("无法获取列表。请检查密钥、网络或服务地区；草稿未丢失。可重试或使用高级设置。","Could not read models. Check key, network or region. Draft kept; retry or use Advanced."),true);return;}
                if(!owner.directory(revision,answer.models))return;
                render();notice(t("已获取 ","Loaded ")+answer.models.size()+t(" 个模型；未发送对战指令"," models; no game prompt sent")+(answer.truncated?t("（列表已截取）"," (list limited)"):""),false);
                if(saveAfter)persist();else if(pickAfter)modelPicker(answer.models);
            });
        });
    }
    private void renderRules(){
        LinearLayout wrapper=column();body.addView(wrapper,new LinearLayout.LayoutParams(-1,-1));
        LinearLayout p=column();p.setPadding(dp(16),dp(8),dp(16),dp(16));wrapper.addView(scroll(p),new LinearLayout.LayoutParams(-1,0,1));
        p.addView(text(t("一起作战，共同获胜","Fight together. Win together."),24,ink,true));gap(p,12);
        p.addView(text(t("盟友接触不会扣血或吞噬；两队合计判胜。","Allies cannot damage or absorb each other; the combined pair score decides victory."),15,muted,false));gap(p,18);
        LinearLayout modes=row();weighted(modes,button(t("自由混战","Free-for-all"),!session.paired,()->{session.paired=false;render();}));hgap(modes,12);weighted(modes,button(t("两两组队","Paired teams"),session.paired,()->{session.paired=true;render();}));p.addView(modes);gap(p,18);
        label(p,t("联盟配对 · 15 种方案可选","Pairing · 15 combinations"));
        java.util.List<int[]> all=AllianceRules.pairings();String[] labels=new String[all.size()];for(int i=0;i<all.size();i++)labels[i]=pairLabel(all.get(i));
        Button pairing=button(labels[session.pairing],false,()->{popup=new AlertDialog.Builder(this).setTitle(t("选择盟友组合","Choose allies")).setSingleChoiceItems(labels,session.pairing,(dialog,i)->{session.pairing=i;dialog.dismiss();render();}).setNegativeButton(android.R.string.cancel,null).show();});pairing.setEnabled(session.paired);p.addView(pairing,new LinearLayout.LayoutParams(-1,-2));gap(p,16);
        p.addView(text(t("数字对应队伍编号。联机由房主决定规则，双方需使用兼容版本。","Numbers match team slots. The host chooses rules; all peers need compatible versions."),13,muted,false));
        Button rulesSave=button(t("保存组队规则","Save alliance rules"),true,()->{AllianceSettings.save(this,session.paired,session.pairing);session.savedPaired=session.paired;session.savedPairing=session.pairing;Toast.makeText(this,t("组队规则已保存","Alliance rules saved"),Toast.LENGTH_SHORT).show();});
        gap(wrapper,8);wrapper.addView(rulesSave,new LinearLayout.LayoutParams(-1,-2));
    }
    private String pairLabel(int[] groups){
        StringBuilder s=new StringBuilder();for(int g=0;g<3;g++){if(g>0)s.append("  /  ");boolean next=false;for(int i=0;i<6;i++)if(groups[i]==g){if(next)s.append(" + ");s.append(i+1).append(' ').append(Util.teamToNameString(this,i));next=true;}}return s.toString();
    }
    private void hideKeyboard(){View v=getCurrentFocus();if(v!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);v.clearFocus();}}
    private boolean unsaved(){for(ModelSetupDraft d:session.drafts)if(d.dirty())return true;return session.paired!=session.savedPaired||session.pairing!=session.savedPairing;}
    private void leave(){
        capture();cancelQuery();if(!unsaved()){finish();return;}
        popup=new AlertDialog.Builder(this).setTitle(t("还有未保存的更改","Unsaved changes")).setMessage(t("已保存的队伍不会丢失。要留在这里继续编辑吗？","Saved teams are safe. Stay here to finish editing?"))
            .setPositiveButton(t("继续编辑","Keep editing"),null).setNegativeButton(t("放弃未保存并返回","Discard unsaved and leave"),(d,w)->finish()).show();
    }
    @Override public void onBackPressed(){leave();}
}
