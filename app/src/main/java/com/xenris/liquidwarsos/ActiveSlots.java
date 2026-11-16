// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.content.Context;import android.graphics.*;
/** Shared two-slot presentation. Only authority snapshots change inventory. */
final class ActiveSlots {
 final Paint p=new Paint(3);final Bitmap[] icons=new Bitmap[2];final int[] count=new int[2],cooldown=new int[2];int frozen,immune;
 ActiveSlots(Context c){p.setTypeface(Typeface.createFromAsset(c.getAssets(),"fonts/NotoSansSC-Intro.ttf"));for(int k=0;k<2;k++)try(java.io.InputStream in=c.getAssets().open("expedition/"+(k==0?"freeze":"cleanse")+".png")){icons[k]=BitmapFactory.decodeStream(in);}catch(java.io.IOException ignored){}}
 void classic(int[] a,int team){if(a==null)return;for(int b=a.length-1;b>=209;b--)if(a[b]==0x4c574131&&b+3<a.length){int t=a[b+2],at=b+3+team*8;if(team>=t||at+7>=a.length)return;set(a[at],a[at+1],a[at+2]-a[b+1],a[at+3]-a[b+1],a[at+4]-a[b+1],a[at+5]-a[b+1]);return;}}
 void expedition(ExpeditionSnapshot s,int t){int a=s.seat(t);set(s.bag(t,0),s.bag(t,1),s.cooldown(t,0),s.cooldown(t,1),s.raw[a+27]-s.time,s.raw[a+30]-s.time);}
 void set(int a,int b,int c,int d,int f,int i){count[0]=a;count[1]=b;cooldown[0]=Math.max(0,c);cooldown[1]=Math.max(0,d);frozen=Math.max(0,f);immune=Math.max(0,i);}
 int hit(float x,float y,float w,float h){if(y<h-68||y>h-8)return -1;float start=w-142;for(int k=0;k<2;k++)if(x>=start+k*66&&x<start+k*66+60)return k;return -1;}
 void text(Canvas c,String s,float x,float y,float size,int color){p.setColor(color);p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);}
 void draw(Canvas c,float w,float h){float x=w-142,y=h-68;
  for(int k=0;k<2;k++){boolean ready=count[k]>0&&cooldown[k]==0&&(k==1||frozen==0);float xx=x+k*66;p.setColor(ready?0xa83b6c60:0x88172b32);c.drawRoundRect(xx,y,xx+60,y+60,9,9,p);p.setAlpha(ready?255:125);if(icons[k]!=null)c.drawBitmap(icons[k],null,new RectF(xx+5,y+3,xx+43,y+41),p);p.setAlpha(255);text(c,"×"+count[k],xx+46,y+16,12,0xfff1ecd5);text(c,k==0?com.xenris.liquidwarsos.BattleText.t("冻结","Freeze"):com.xenris.liquidwarsos.BattleText.t("净化","Cleanse"),xx+30,y+53,12,0xffedf3e6);if(cooldown[k]>0)text(c,(cooldown[k]+999)/1000+"s",xx+30,y+34,13,0xffffd599);}
  if(frozen>0)text(c,com.xenris.liquidwarsos.BattleText.t("被冻结 ","Frozen ")+(frozen+999)/1000+com.xenris.liquidwarsos.BattleText.t("s · 可净化","s · Cleanse available"),w-76,h-71,9,0xffe4f7ff);else if(immune>0)text(c,com.xenris.liquidwarsos.BattleText.t("免冻 ","Immune ")+(immune+999)/1000+"s",w-76,h-71,9,0xffc9eee1);
 }
}
