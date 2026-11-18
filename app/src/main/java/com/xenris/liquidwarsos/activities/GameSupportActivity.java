// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.activities;

import android.app.AlertDialog;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.online.LobbyUi;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;

/** Local voluntary-support information. No order, payment callback or player entitlement. */
public final class GameSupportActivity extends LocalizedActivity {
    private static final int SAVE_CODE=211;
    public static final String CODE_ASSET="support/wechat-appreciation.jpg";
    private static final String EMAIL="1414319052@qq.com";
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private Button save;
    private Bitmap code;
    private AlertDialog preview;
    private LobbyUi ui;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        ui=new LobbyUi(this);
        LinearLayout root=ui.column();root.setPadding(ui.dp(20),ui.dp(12),ui.dp(20),ui.dp(12));root.setBackgroundColor(LobbyUi.BG);
        LinearLayout header=ui.row();header.addView(ui.button(getString(R.string.support_back),false,this::finish));
        TextView title=ui.heading(getString(R.string.support_game));LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(0,-2,1);titleParams.leftMargin=ui.dp(16);header.addView(title,titleParams);root.addView(header);
        LinearLayout body=ui.row();body.setGravity(Gravity.TOP);LinearLayout.LayoutParams bodyParams=new LinearLayout.LayoutParams(-1,0,1);bodyParams.topMargin=ui.dp(12);root.addView(body,bodyParams);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout info=ui.panel();scroll.addView(info);body.addView(scroll,new LinearLayout.LayoutParams(0,-1,1.35f));
        ui.add(info,ui.label(getString(R.string.support_tagline),20,LobbyUi.ACCENT,true),0);
        section(info,R.string.support_purpose_heading,R.string.support_purpose);
        section(info,R.string.support_voluntary_heading,R.string.support_voluntary);
        section(info,R.string.support_notice_heading,R.string.support_notice);
        ui.add(info,ui.label(getString(R.string.support_contact),13,LobbyUi.MUTED,false),16);
        ui.add(info,ui.button(getString(R.string.support_copy_email),false,this::copyEmail),10);
        LinearLayout qr=ui.panel();LinearLayout.LayoutParams qrParams=new LinearLayout.LayoutParams(0,-1,1);qrParams.leftMargin=ui.dp(16);body.addView(qr,qrParams);
        qr.addView(ui.label(getString(R.string.support_code_title),17,LobbyUi.INK,true));
        ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription(getString(R.string.support_code_description));
        LinearLayout.LayoutParams imageParams=new LinearLayout.LayoutParams(-1,0,1);imageParams.topMargin=ui.dp(8);imageParams.bottomMargin=ui.dp(8);qr.addView(image,imageParams);
        try(InputStream in=getAssets().open(CODE_ASSET)){code=BitmapFactory.decodeStream(in);if(code==null)throw new IOException("Invalid code image");image.setImageBitmap(code);image.setOnClickListener(v->showOriginal());}
        catch(IOException e){qr.addView(ui.label(getString(R.string.support_image_error),14,LobbyUi.DANGER,false));}
        TextView hint=ui.label(getString(R.string.support_code_hint),12,LobbyUi.MUTED,false);ui.add(qr,hint,0);
        save=ui.button(getString(R.string.support_save),true,this::chooseDestination);save.setEnabled(code!=null);ui.add(qr,save,10);
        setContentView(root);
    }
    private void section(LinearLayout parent,int heading,int text){
        ui.add(parent,ui.label(getString(heading),16,LobbyUi.INK,true),16);
        TextView paragraph=ui.label(getString(text),14,LobbyUi.MUTED,false);paragraph.setLineSpacing(ui.dp(3),1);ui.add(parent,paragraph,7);
    }
    private void showOriginal(){
        if(preview!=null&&preview.isShowing())return;
        ImageView image=new ImageView(this);image.setImageBitmap(code);image.setBackgroundColor(0xffFFFFFF);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription(getString(R.string.support_code_description));
        preview=new AlertDialog.Builder(this).setView(image).setPositiveButton(getString(R.string.support_close),null).create();
        preview.show();preview.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
        preview.getButton(-1).setTypeface(android.graphics.Typeface.createFromAsset(getAssets(),"fonts/NotoSansSC-Lobby600.ttf"));preview.getButton(-1).setTextColor(LobbyUi.ACCENT);
    }
    private void copyEmail(){
        ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(clipboard!=null){clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.support_contact_label),EMAIL));Toast.makeText(this,R.string.support_email_copied,Toast.LENGTH_SHORT).show();}
    }
    private void chooseDestination(){
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/jpeg").putExtra(Intent.EXTRA_TITLE,"liquid-wars-wechat-appreciation.jpg");
        try{startActivityForResult(intent,SAVE_CODE);}catch(ActivityNotFoundException e){Toast.makeText(this,R.string.support_picker_error,Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=SAVE_CODE||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri destination=data.getData();save.setEnabled(false);
        Context app=getApplicationContext();WeakReference<GameSupportActivity> owner=new WeakReference<>(this);Handler main=new Handler(Looper.getMainLooper());
        io.execute(()->{
            boolean success=false;
            try{
                try(InputStream in=app.getAssets().open(CODE_ASSET);OutputStream out=app.getContentResolver().openOutputStream(destination,"wt")){
                    if(out==null)throw new IOException("No output stream");byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);out.flush();
                }
                success=true;
            }catch(IOException|SecurityException ignored){}
            final boolean saved=success;
            main.post(()->{GameSupportActivity activity=owner.get();if(activity==null||activity.isFinishing()||activity.isDestroyed())return;activity.save.setEnabled(activity.code!=null);Toast.makeText(activity,saved?R.string.support_saved:R.string.support_save_error,Toast.LENGTH_LONG).show();});
        });
    }
    @Override protected void onDestroy(){if(preview!=null)preview.dismiss();io.shutdown();super.onDestroy();}
}
