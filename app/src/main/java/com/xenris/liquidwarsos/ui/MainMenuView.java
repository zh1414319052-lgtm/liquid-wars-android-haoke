// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.ui;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import androidx.core.graphics.PathParser;
import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.BuildConfig;
import com.xenris.liquidwarsos.activities.MainMenuActivity;

/** Native responsive layout. Existing mode handlers continue owning all game/navigation state. */
public final class MainMenuView extends LinearLayout {
    private final MenuSkin s;
    private final MainMenuActivity host;
    public MainMenuView(MainMenuActivity activity){
        super(activity);host=activity;s=new MenuSkin(activity);setId(R.id.ui_root);setOrientation(VERTICAL);
        setBackgroundColor(MenuSkin.BG);setPadding(s.u(32),s.u(24),s.u(32),s.u(16));
        LinearLayout body=s.row();body.setGravity(Gravity.TOP);
        addView(body,new LayoutParams(-1,0,1));
        LinearLayout brand=s.column();
        TextView word=s.text("L I Q U I D   W A R S",14,MenuSkin.INK,true);brand.addView(word,new LayoutParams(-1,s.u(30)));
        TextView title=s.text(activity.getString(R.string.ui_game_title),52,MenuSkin.INK,true);
        title.setAccessibilityHeading(true);s.add(brand,title,16);
        s.add(brand,s.text(activity.getString(R.string.ui_edition),22,MenuSkin.INK,false),6);
        View rule=new View(activity);rule.setBackground(s.surface(MenuSkin.ACCENT,2,0));LayoutParams ruleLp=new LayoutParams(s.u(46),s.u(4));ruleLp.topMargin=s.u(18);brand.addView(rule,ruleLp);
        s.add(brand,s.text(activity.getString(R.string.ui_tagline),22,MenuSkin.INK,false),14);
        ParticleStreams art=new ParticleStreams(activity);brand.addView(art,new LayoutParams(-1,0,1));
        art.setMinimumHeight(s.u(120));
        ScrollView hero=scroll(brand);body.addView(hero,new LayoutParams(0,-1,42));
        Space gap=new Space(activity);body.addView(gap,new LayoutParams(s.u(28),1));
        LinearLayout modes=s.column();
        modes.setMinimumHeight(s.u(326));
        TextView section=s.text(activity.getString(R.string.menu_choose_mode),22,MenuSkin.INK,true);
        section.setAccessibilityHeading(true);modes.addView(section,new LayoutParams(-1,s.u(30)));
        LinearLayout first=s.row(),second=s.row();
        addCard(first,R.id.menu_single,R.string.single_player_game,R.string.menu_classic_desc,0,true,v->host.singlePlayerMenu(v));
        addCard(first,R.id.garden_trial,R.string.menu_garden_title,R.string.menu_garden_desc,1,false,v->host.garden3DMenu(v));
        addCard(second,R.id.menu_large,R.string.large_map_mode,R.string.menu_large_desc,2,false,v->host.largeMapMenu(v));
        addCard(second,R.id.menu_spectate,R.string.model_spectate,R.string.menu_spectate_desc,3,false,v->host.spectatorMenu(v));
        LayoutParams rows=new LayoutParams(-1,0,1);rows.topMargin=s.u(16);modes.addView(first,rows);
        LayoutParams rows2=new LayoutParams(-1,0,1);rows2.topMargin=s.u(12);modes.addView(second,rows2);
        LinearLayout local=s.row();
        Button wifi=s.button(activity.getString(R.string.ui_wifi),()->host.multiplayerMenu(null));wifi.setId(R.id.menu_wifi);
        Button bt=s.button(activity.getString(R.string.bt_multiplayer),()->host.bluetoothMenu(null));bt.setId(R.id.menu_bluetooth);
        addEqual(local,wifi,0);addEqual(local,bt,12);s.add(modes,local,18);
        modes.setPadding(0,0,0,s.u(14));
        body.addView(scroll(modes),new LayoutParams(0,-1,58));
        View line=new View(activity);line.setBackgroundColor(MenuSkin.LINE);LayoutParams lineLp=new LayoutParams(-1,s.dp(1));lineLp.topMargin=s.u(12);addView(line,lineLp);
        LinearLayout footer=s.row();LayoutParams footLp=new LayoutParams(-1,-2);footLp.topMargin=s.u(12);addView(footer,footLp);
        TextView version=s.text(activity.getString(R.string.menu_local_version,BuildConfig.VERSION_NAME),14,MenuSkin.MUTED,false);
        footer.addView(version,new LayoutParams(0,-2,1));
        Button notes=s.button(activity.getString(R.string.notes_entry),()->host.showReleaseNotes(null));notes.setId(R.id.menu_notes);
        Button help=s.button(activity.getString(R.string.how_to_play),()->host.instructions(null));help.setId(R.id.menu_help);
        Button support=s.button(activity.getString(R.string.support_game),()->host.supportGame(null));support.setId(R.id.support_game);
        for(Button b:new Button[]{notes,help,support}){LayoutParams lp=new LayoutParams(-2,-2);lp.leftMargin=s.u(12);footer.addView(b,lp);}
    }
    private ScrollView scroll(LinearLayout content){ScrollView v=new ScrollView(host);v.setFillViewport(true);v.setClipToPadding(false);v.addView(content,new ScrollView.LayoutParams(-1,-2));return v;}
    private void addEqual(LinearLayout row,View child,int gap){LayoutParams lp=new LayoutParams(0,-1,1);lp.leftMargin=s.u(gap);row.addView(child,lp);}
    private void addCard(LinearLayout row,int id,int title,int desc,int icon,boolean primary,OnClickListener action){
        LinearLayout card=s.row();card.setId(id);card.setPadding(s.u(16),s.u(18),s.u(16),s.u(18));
        card.setMinimumHeight(s.u(94));card.setBackground(s.pressable(primary));card.setFocusable(true);card.setClickable(true);
        card.setOnClickListener(action);card.setContentDescription(host.getString(title)+"，"+host.getString(desc));
        card.setAccessibilityDelegate(new AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View h,AccessibilityNodeInfo n){super.onInitializeAccessibilityNodeInfo(h,n);n.setClassName(Button.class.getName());}});
        int color=primary?MenuSkin.BG:MenuSkin.INK;
        Icon image=new Icon(host,icon,color);card.addView(image,new LayoutParams(s.u(38),s.u(38)));
        View divider=new View(host);divider.setBackgroundColor(primary?0x55315158:MenuSkin.LINE);
        LayoutParams dl=new LayoutParams(s.dp(1),s.u(54));dl.leftMargin=s.u(14);dl.rightMargin=s.u(14);card.addView(divider,dl);
        LinearLayout labels=s.column();labels.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        labels.addView(s.text(host.getString(title),21,color,true));s.add(labels,s.text(host.getString(desc),15,primary?MenuSkin.BG:MenuSkin.MUTED,false),6);
        card.addView(labels,new LayoutParams(0,-2,1));addEqual(row,card,row.getChildCount()==0?0:12);
    }
    private static final class Icon extends View {
        private final Path path;private final Paint paint=new Paint(3);
        Icon(Context c,int kind,int color){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);String[] paths={
            "M4,3 L19,18 M20,3 L5,18 M2,15 L9,22 M22,15 L15,22",
            "M3,6 L12,1 L21,6 L21,18 L12,23 L3,18 Z M3,6 L12,11 L21,6 M12,11 L12,23",
            "M2,5 L8,2 L16,5 L22,2 L22,20 L16,23 L8,20 L2,23 Z M8,2 L8,20 M16,5 L16,23",
            "M2,3 L22,3 L22,18 L2,18 Z M12,18 L12,22 M7,22 L17,22 M9,7 L16,11 L9,15 Z"};
            path=PathParser.createPathFromPathData(paths[kind]);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.4f);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);
        }
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/24f,getHeight()/24f);c.drawPath(path,paint);c.restore();}
    }
    private static final class ParticleStreams extends View {
        final Paint paint=new Paint(3);final int[] colors={0xffa2ddbf,0xffe38278,0xffe5cb82};
        ParticleStreams(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);
            for(int team=0;team<3;team++){paint.setColor(colors[team]);for(int j=0;j<190;j++){
                float t=(j%95)/94f;float x=getWidth()*(.02f+.95f*t);
                float y=getHeight()*(.40f+team*.12f+.26f*(float)Math.sin(t*4.4+team*2.1)+.045f*(float)Math.sin(j*7.1)+(j/95-.5f)*.05f);
                c.drawCircle(x,y,Math.max(1,getWidth()*.0024f),paint);
            }}
        }
    }
}
