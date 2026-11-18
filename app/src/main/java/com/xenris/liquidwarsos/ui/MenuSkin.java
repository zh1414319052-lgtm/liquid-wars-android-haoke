// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Local menu tokens, mapped to docs/game/menu-2.12/design/menu.haoke-ui.json.
 * Fonts: Noto Sans SC, SIL OFL 1.1. Does not reskin the battlefield/other screens. */
public final class MenuSkin {
    public static final int BG=0xff10262c, PANEL=0xff1a363e, LINE=0xff315158,
        INK=0xfff5f4e9, MUTED=0xffb6cac5, ACCENT=0xffa2ddbf;
    public final Context context;
    public final float scale;
    private final Typeface regular, semibold;
    public MenuSkin(Context c){
        context=c;
        android.content.res.Configuration cfg=c.getResources().getConfiguration();
        scale=Math.max(.8f, Math.min(1.4f, Math.min(cfg.screenWidthDp/992f,cfg.screenHeightDp/455f)));
        regular=Typeface.createFromAsset(c.getAssets(),"fonts/NotoSansSC-Lobby400.ttf");
        semibold=Typeface.createFromAsset(c.getAssets(),"fonts/NotoSansSC-Lobby600.ttf");
    }
    public int dp(float n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    public int u(float n){return dp(n*scale);}
    public LinearLayout column(){LinearLayout l=new LinearLayout(context);l.setOrientation(1);return l;}
    public LinearLayout row(){LinearLayout l=new LinearLayout(context);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    public TextView text(String value,int size,int color,boolean bold){
        TextView v=new TextView(context);v.setText(value);v.setTextSize(Math.max(12,size*scale));
        v.setTypeface(bold?semibold:regular);v.setTextColor(color);v.setIncludeFontPadding(false);
        v.setGravity(Gravity.CENTER_VERTICAL);return v;
    }
    public GradientDrawable surface(int color,int radius,int stroke){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(u(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke);return d;
    }
    public RippleDrawable pressable(boolean primary){
        return new RippleDrawable(ColorStateList.valueOf(0x258de7c2),
            surface(primary?ACCENT:PANEL,12,primary?0:LINE),null);
    }
    public Button button(String text,Runnable action){
        Button v=new Button(context);v.setText(text);v.setTextSize(Math.max(13,16*scale));
        v.setTypeface(semibold);v.setAllCaps(false);v.setTextColor(INK);v.setIncludeFontPadding(false);
        v.setMinWidth(0);v.setMinimumWidth(dp(48));v.setMinHeight(dp(48));v.setMinimumHeight(dp(48));
        v.setPadding(u(16),u(8),u(16),u(8));v.setStateListAnimator(null);v.setBackgroundTintList(null);
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x258de7c2),surface(BG,10,LINE),null));
        v.setOnClickListener(view->action.run());return v;
    }
    public void add(LinearLayout p,android.view.View v,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=u(top);p.addView(v,lp);}
}
