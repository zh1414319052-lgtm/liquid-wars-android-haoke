// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.online;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.TextUtils;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.io.InputStream;

/** Native design tokens mapped from docs/game/online-ui-1.85/lobby.haoke-ui.json. */
public final class LobbyUi {
    public static final int BG=0xff0c191b,PANEL=0xff14292c,FIELD=0xff1b3438,LINE=0xff315255,
        INK=0xfff0f7f2,MUTED=0xffb0c5be,ACCENT=0xffc5ee86,DANGER=0xffffada4;
    private final Context context;
    private final Typeface regular,semibold;
    public LobbyUi(Context c){context=c;regular=Typeface.createFromAsset(c.getAssets(),"fonts/NotoSansSC-Lobby400.ttf");semibold=Typeface.createFromAsset(c.getAssets(),"fonts/NotoSansSC-Lobby600.ttf");}
    public int dp(float n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    public LinearLayout column(){LinearLayout l=new LinearLayout(context);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public LinearLayout row(){LinearLayout l=new LinearLayout(context);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    public TextView label(String value,int size,int color,boolean strong){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(strong?semibold:regular);t.setIncludeFontPadding(false);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    public TextView heading(String value){return label(value,24,INK,true);}
    public GradientDrawable surface(int color,int radius,int border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border!=0)d.setStroke(dp(1),border);return d;}
    public LinearLayout panel(){LinearLayout p=column();p.setPadding(dp(20),dp(18),dp(20),dp(18));p.setBackground(surface(PANEL,18,LINE));return p;}
    public void gap(LinearLayout p,int h){Space s=new Space(context);p.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    public void add(LinearLayout p,View v,int top){LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(-1,-2);a.topMargin=dp(top);p.addView(v,a);}
    public Button button(String value,boolean primary,Runnable click){
        Button b=new Button(context);b.setText(value);b.setTextSize(15);b.setTypeface(semibold);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(14),dp(10),dp(14),dp(10));b.setStateListAnimator(null);b.setBackgroundTintList(null);b.setIncludeFontPadding(false);
        b.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{MUTED,primary?BG:INK}));
        StateListDrawable state=new StateListDrawable();state.addState(new int[]{-android.R.attr.state_enabled},surface(FIELD,12,LINE));state.addState(new int[]{},surface(primary?ACCENT:FIELD,12,primary?0:LINE));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x334fe4ba),state,null));b.setOnClickListener(v->click.run());return b;
    }
    public EditText field(String hint,int inputType){
        EditText e=new EditText(context);e.setTextSize(16);e.setTypeface(regular);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setHint(hint);e.setSingleLine(true);e.setInputType(inputType);e.setTypeface(regular);e.setPadding(dp(14),dp(11),dp(14),dp(11));e.setMinHeight(dp(48));e.setHeight(dp(48));e.setIncludeFontPadding(false);e.setSelectAllOnFocus(false);e.setBackgroundTintList(null);
        StateListDrawable state=new StateListDrawable();state.addState(new int[]{android.R.attr.state_focused},surface(FIELD,12,ACCENT));state.addState(new int[]{},surface(FIELD,12,LINE));e.setBackground(state);e.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);return e;
    }
    public ImageView mascot(String name,int size){ImageView v=new ImageView(context);try(InputStream in=context.getAssets().open("emotes/"+name+".png")){v.setImageBitmap(BitmapFactory.decodeStream(in));}catch(Exception ignored){}v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setAdjustViewBounds(true);v.setMaxHeight(dp(size));v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return v;}
    public void ellipsis(TextView t){t.setSingleLine(true);t.setEllipsize(TextUtils.TruncateAt.END);}
    public void enter(View v){v.animate().cancel();if(ValueAnimator.areAnimatorsEnabled()){v.setAlpha(.35f);v.animate().alpha(1).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();}else v.setAlpha(1);}
    public Drawable backdrop(){return new Drawable(){private final Paint p=new Paint(3);public void draw(Canvas c){Rect b=getBounds();c.drawColor(BG);p.setShader(new RadialGradient(b.width()*.16f,b.height()*.62f,Math.max(b.width()*.5f,1),new int[]{0xff194237,BG},null,Shader.TileMode.CLAMP));c.drawRect(b,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(0x223fae88);for(int i=0;i<4;i++)c.drawOval(new RectF(-b.width()*.3f-i*dp(50),b.height()*.18f+i*dp(28),b.width()*.43f+i*dp(24),b.height()*1.7f+i*dp(10)),p);p.setStyle(Paint.Style.FILL);}public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.OPAQUE;}};}
}
