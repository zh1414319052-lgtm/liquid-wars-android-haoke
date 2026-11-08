// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.content.Context;import android.util.AttributeSet;import android.view.View;
import android.graphics.Canvas;import android.graphics.Paint;import android.graphics.Typeface;

/** Static, non-intercepting spawn locator above GLES. Noto Sans SC, SIL OFL1.1.
 * UI only; never reads live simulation or consumes a random number. */
public final class MatchIntroView extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private short[] starts;
 private int team;private boolean spectator;private volatile long remaining=MatchIntro.DURATION_MS;
 private final float dp;private final boolean zh;
 public MatchIntroView(Context c,AttributeSet a){super(c,a);dp=c.getResources().getDisplayMetrics().density;zh="zh".equals(c.getResources().getConfiguration().getLocales().get(0).getLanguage());paint.setTypeface(Typeface.createFromAsset(c.getAssets(),"fonts/NotoSansSC-Intro.ttf"));paint.setFontVariationSettings("'wght' 600");setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
 public void bind(int team,boolean spectator,short[] positions){this.team=team;this.spectator=spectator;starts=positions.clone();setContentDescription(zh?"开局倒计时，寻找自己的队伍":"Match countdown; find your team");}
 public void showMillis(long ms){long old=remaining;remaining=Math.max(0,ms);if((old>0)==(remaining>0)&&MatchIntro.seconds(old)==MatchIntro.seconds(remaining))return;post(()->{setVisibility(remaining>0?VISIBLE:GONE);invalidate();});}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(remaining<=0||starts==null)return;
  MyGLSurfaceView surface=((View)getParent()).findViewById(R.id.mySurfaceView);float sw=surface.getWidth(),sh=surface.getHeight(),left=surface.getLeft(),top=surface.getTop();
  for(int p=0;p<6;p++)if(spectator||p==team){float x=left+surface.camera.screenX(starts[p*2],sw),y=top+surface.camera.screenY(starts[p*2+1],sh);
   paint.setColor(0xfff1f1de);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(5*dp);c.drawCircle(x,y,20*dp,paint);paint.setColor(TeamPalette.color(p));paint.setStrokeWidth(3*dp);c.drawCircle(x,y,24*dp,paint);paint.setStyle(Paint.Style.FILL);
   String label=spectator?Integer.toString(p+1):(zh?"你在这里":"YOU ARE HERE");paint.setTextSize(15*dp);paint.setTextAlign(Paint.Align.CENTER);float w=paint.measureText(label)+18*dp,lx=Math.max(w/2+4*dp,Math.min(getWidth()-w/2-4*dp,x)),ly=Math.max(29*dp,y-35*dp);
   paint.setColor(0xed172027);c.drawRoundRect(lx-w/2,ly-20*dp,lx+w/2,ly+6*dp,8*dp,8*dp,paint);paint.setColor(TeamPalette.color(p));c.drawText(label,lx,ly,paint);
  }
  String title=spectator?(zh?"观战 · 六队已就位":"SPECTATING · ALL TEAMS READY"):(zh?"你是 ":"YOU ARE ")+Util.teamToNameString(getContext(),team);
  float x=getWidth()/2f,y=45*dp;
  if(!spectator){
   float ownX=left+surface.camera.screenX(starts[team*2],sw),ownY=top+surface.camera.screenY(starts[team*2+1],sh);
   // Free births may sit at the upper edge. Keep the countdown card away
   // from the human's true marker instead of hiding it behind the title.
   if(ownY<155*dp&&Math.abs(ownX-x)<225*dp)x=ownX<getWidth()/2f?getWidth()-190*dp:190*dp;
  }
  paint.setStyle(Paint.Style.FILL);paint.setColor(0xef172027);c.drawRoundRect(x-180*dp,y-28*dp,x+180*dp,y+94*dp,14*dp,14*dp,paint);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(17*dp);paint.setColor(spectator?0xffd0ded9:TeamPalette.color(team));c.drawText(title,x,y,paint);
  paint.setTextSize(42*dp);paint.setColor(0xffedf3ed);c.drawText(Integer.toString(MatchIntro.seconds(remaining)),x,y+54*dp,paint);paint.setTextSize(11*dp);paint.setColor(0xffc6d4cf);c.drawText(zh?"找到自己 · 可以先选择移动方向":"FIND YOUR TEAM · YOU CAN AIM NOW",x,y+78*dp,paint);
 }
}
