// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.graphics.*;
/** Procedural, read-only effects over actual rule samples; no gameplay RNG or hidden input. */
final class BattleEffects {
 static final int[] COLOR={0xffffd780,0xffebc397,0xff9fe6ff,0xffc1a7ff,0xffd0beff,0xffd9a8ff,0xffffe39b,0xffff899f,0xff9fe2bb,0xffffb67e,0xff83c7eb,0xffffca8a,0xff8dd9e7,0xffc9c2ff,0xffa6aeff,0xffffae9a,0xfff0d49a,0xffb9e3d1,0xffafc4ef,0xffffd89a};
 int worldWidth=800,worldHeight=480;float viewportLeft,viewportTop,viewportWidth,viewportHeight;boolean zh,all;int viewer;
 boolean hidden(int[] s,int team){if(team<0||team>=6||all||s[25+team*20+18]<=0)return false;int group=com.xenris.liquidwarsos.cloud.SnapshotItems.group(s,viewer);if(group==com.xenris.liquidwarsos.cloud.SnapshotItems.group(s,team))return false;for(int t=0;t<6;t++)if(s[13+t]>0&&s[25+t*20+19]>0&&com.xenris.liquidwarsos.cloud.SnapshotItems.group(s,t)==group)return false;return true;}android.graphics.drawable.Drawable[] symbols;
 final Paint p=new Paint(3);final Path path=new Path();float left,top,w,h,dp;int time;
 void space(float l,float t,float width,float height,float density,int now){left=l;top=t;w=width;h=height;dp=density;time=now;}
 float x(int v){return left+(v+.5f)*w/worldWidth;}float y(int v){return top+(worldHeight-.5f-v)*h/worldHeight;}
 void line(Canvas c,int color,float alpha,float stroke,float x1,float y1,float x2,float y2){p.setShader(null);p.setColor(color);p.setAlpha((int)(255*Math.max(0,Math.min(1,alpha))));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke*dp);c.drawLine(x1,y1,x2,y2,p);}
 void ring(Canvas c,float x,float y,float r,int color,float alpha,float stroke){p.setShader(null);p.setColor(color);p.setAlpha((int)(255*Math.max(0,Math.min(1,alpha))));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke*dp);c.drawCircle(x,y,Math.max(1,r),p);}
 void glow(Canvas c,float x,float y,float r,int color,float alpha){p.setStyle(Paint.Style.FILL);p.setAlpha(255);p.setShader(new RadialGradient(x,y,Math.max(1,r),new int[]{(color&0xffffff)|((int)(Math.min(1,alpha)*190)<<24),color&0xffffff},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r,p);p.setShader(null);}
 void polygon(Canvas c,float x,float y,float r,int sides,float angle,int color,float alpha){path.reset();for(int i=0;i<=sides;i++){double a=angle+i*2*Math.PI/sides;float xx=x+(float)Math.cos(a)*r,yy=y+(float)Math.sin(a)*r;if(i==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);}p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f*dp);p.setColor(color);p.setAlpha((int)(255*alpha));c.drawPath(path,p);}
 void devour(Canvas c,int kind,float cx,float cy,float rx,float ry,int hp,int maxHp,int leader,boolean eating,int[] state){
  float remaining=Math.max(0,Math.min(1,hp/(float)Math.max(1,maxHp))),damage=1-remaining;int color=leader>=0&&leader<6?TeamPalette.color(leader):COLOR[kind];
  p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f*dp);p.setColor(0xbbebeee5);p.setAlpha(200);c.drawOval(cx-rx,cy-ry,cx+rx,cy+ry,p);
  if(damage>0){p.setColor(0xee17232b);p.setStyle(Paint.Style.FILL);for(int j=0;j<3;j++){double a=j*2.399+kind*.7;float r=(.8f+damage*2.8f)*dp;c.drawCircle(cx+(float)Math.cos(a)*rx*.7f,cy+(float)Math.sin(a)*ry*.7f,r,p);}}
  if(eating&&leader>=0&&leader<6){int at=147+leader*10;float direction=(float)Math.atan2(y(state[at+3])-cy,x(state[at+2])-cx);for(int j=0;j<12;j++){float u=((time%900)/900f+j/12f)%1,angle=direction+(j%5-2)*.23f,sx=cx+(float)Math.cos(angle)*rx,sy=cy+(float)Math.sin(angle)*ry;float reach=(10+12*(j%3))*dp;p.setColor(color);p.setAlpha((int)(220*(1-u)));p.setStyle(Paint.Style.FILL);c.drawCircle(sx+(float)Math.cos(angle)*reach*u,sy+(float)Math.sin(angle)*reach*u,(1.3f+1.8f*(1-u))*dp,p);line(c,color,(1-u)*.6f,1,sx,sy,sx+(float)Math.cos(angle)*reach*u*.65f,sy+(float)Math.sin(angle)*reach*u*.65f);}}
  p.setAlpha(255);p.setStyle(Paint.Style.FILL);
 }
 void pickup(Canvas c,int kind,float x,float y,float radius){int color=COLOR[kind];float phase=(time%2100)/2100f;glow(c,x,y,radius*2.5f,color,.32f);ring(c,x,y,radius*(1.15f+.35f*phase),color,.65f*(1-phase),1.6f);ring(c,x,y,radius*1.05f,color,.8f,1);
  p.setColor(0xed111d28);p.setAlpha(240);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,radius,p);
  for(int i=0;i<3;i++){double a=time*.0018+i*2.094;float xx=x+(float)Math.cos(a)*radius*1.25f,yy=y+(float)Math.sin(a)*radius*1.25f;glow(c,xx,yy,4*dp,color,.8f);}
 }
 void counter(Canvas c,int kind,float x,float y,float u,float alpha){int col=COLOR[kind];float r=(16+25*u)*dp;
  switch(kind){
   case 9: // Suppressed multiplication: two split rings pulled into a fixed bar.
    ring(c,x-r*.6f,y,r*.55f,col,alpha,2);ring(c,x+r*.6f,y,r*.55f,col,alpha,2);line(c,col,alpha,3,x-r,y+r,x+r,y-r);break;
   case 10: // Lithic seal: interlocking stone lattice, locking from the outside in.
    for(int k=-2;k<=2;k++){float q=k*r/2;line(c,col,alpha*.6f,1,x-r,y+q,x+r,y+q);line(c,col,alpha*.6f,1,x+q,y-r,x+q,y+r);}polygon(c,x,y,r*1.3f,4,.785f,col,alpha);break;
   case 11: // Thaw: rising warm ribbons and embers, unlike static ice branches.
    for(int j=0;j<6;j++){float yy=y+r-((u+j/6f)%1)*r*2,xx=x+(float)Math.sin(j+u*9)*r*.6f;glow(c,xx,yy,9*dp,col,alpha*.5f);line(c,col,alpha,2,xx,yy,xx-4*dp,yy+12*dp);}break;
   case 12: // Anchor: linked chain drops into a broad ground ring.
    for(int j=0;j<5;j++)ring(c,x,y-r+j*r*.4f,4*dp,col,alpha,1.5f);line(c,col,alpha,3,x-r,y+r*.4f,x,y+r);line(c,col,alpha,3,x+r,y+r*.4f,x,y+r);ring(c,x,y+r,r,col,alpha*.5f,1);break;
   case 13: // Causal lock: immobile minute ticks within a rotating outer cage.
    for(int j=0;j<12;j++){double a=j*Math.PI/6;line(c,col,alpha,2,x+(float)Math.cos(a)*r*.8f,y+(float)Math.sin(a)*r*.8f,x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r);}polygon(c,x,y,r*1.25f,6,-u*3,col,alpha);line(c,col,alpha,3,x,y,x,y-r*.6f);break;
   case 14: // Phase nail: long needles strike vertically and fracture a ghost ring.
    for(int j=-2;j<=2;j++){float xx=x+j*r*.4f,yy=y-r+(u*2+j*.12f)%1*r;line(c,col,alpha,2,xx,yy-r,xx,yy);line(c,col,alpha,1,xx-4*dp,yy-7*dp,xx,yy);line(c,col,alpha,1,xx+4*dp,yy-7*dp,xx,yy);}break;
   case 15: // Shatter: separated shield facets accelerate outward.
    for(int j=0;j<6;j++){double a=j*Math.PI/3;float xx=x+(float)Math.cos(a)*r,yy=y+(float)Math.sin(a)*r;polygon(c,xx,yy,7*dp,3,(float)a+u*4,col,alpha);}break;
   case 16: // One-use talisman: paper rune and concentric protective seal.
    polygon(c,x,y,r*.7f,4,.785f,col,alpha);line(c,col,alpha,2,x-r*.3f,y-r*.3f,x+r*.3f,y-r*.3f);line(c,col,alpha,2,x,y-r*.45f,x,y+r*.4f);line(c,col,alpha,2,x-r*.25f,y+r*.1f,x+r*.25f,y+r*.1f);ring(c,x,y,r,col,alpha*.4f,1);break;
   case 17: // Declaration: two broken links separate as a flag unfurls.
    ring(c,x-r*.7f,y,r*.35f,col,alpha,2);ring(c,x+r*.7f,y,r*.35f,col,alpha,2);line(c,col,alpha,2,x,y+r,x,y-r);line(c,col,alpha,3,x,y-r,x+r*.7f,y-r*.7f);line(c,col,alpha,3,x+r*.7f,y-r*.7f,x,y-r*.4f);break;
  }
 }
 void draw(Canvas c,int[] s){if(s==null||s.length<209)return;int count=s[208],samplesAt=209+count*9;if(samplesAt>=s.length)return;
  c.save();c.clipRect(left,top,left+w,top+h);
  // Persistent effects wrap real sampled particles, keeping team colors visible.
  int samples=s[samplesAt];for(int i=0;i<samples&&samplesAt+3+i*3<s.length;i++){int a=samplesAt+1+i*3,t=s[a],wx=s[a+1],wy=s[a+2];if(hidden(s,t))continue;float xx=x(wx),yy=y(wy);int base=25+t*20;
   if(s[base+2]>0&&i%4==0){float r=(4+(i%3))*dp;for(int arm=0;arm<3;arm++){double ang=arm*Math.PI/3;line(c,COLOR[2],.7f,1,xx-(float)Math.cos(ang)*r,yy-(float)Math.sin(ang)*r,xx+(float)Math.cos(ang)*r,yy+(float)Math.sin(ang)*r);}glow(c,xx,yy,9*dp,COLOR[2],.14f);}
   if(s[base+5]>0&&i%3==0){line(c,COLOR[5],.35f,2,xx-10*dp,yy+4*dp,xx,yy);ring(c,xx,yy,5*dp,COLOR[5],.45f,1);}
   if(s[base+6]>0&&i%4==0){polygon(c,xx,yy,7*dp,6,time*.00015f,COLOR[6],.5f);}
   if(s[base+1]>0&&i%6==0){double ang=time*.01+i;line(c,COLOR[1],.75f,1.4f,xx,yy,xx+(float)Math.cos(ang)*7*dp,yy+(float)Math.sin(ang)*7*dp);}
  }
  for(int t=0;t<6;t++){if(s[13+t]<=0||hidden(s,t))continue;int at=147+t*10;float cx=x(s[at+2]),cy=y(s[at+3]);int base=25+t*20;
   if(s[base+2]>0){float r=Math.max(23*dp,Math.min(85*dp,(s[at+6]-s[at+4])*w/(2*worldWidth)+18*dp));glow(c,cx,cy,r*1.5f,COLOR[2],.22f);polygon(c,cx,cy,r,6,.52f,COLOR[2],.9f);polygon(c,cx,cy,r+5*dp,6,.52f,COLOR[2],.35f);for(int j=0;j<6;j++){double a=j*Math.PI/3+.52;float px=cx+(float)Math.cos(a)*r,py=cy+(float)Math.sin(a)*r;line(c,COLOR[2],.8f,1.7f,px-5*dp,py,px+5*dp,py);line(c,COLOR[2],.8f,1.7f,px,py-5*dp,px,py+5*dp);}float ly=Math.max(top+20*dp,cy-r-11*dp);p.setStyle(Paint.Style.FILL);p.setColor(0xe61a344a);c.drawRoundRect(cx-31*dp,ly-12*dp,cx+31*dp,ly+5*dp,5*dp,5*dp,p);p.setColor(COLOR[2]);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(10*dp);c.drawText((zh?"冻结 ":"FREEZE ")+((s[base+2]+999)/1000)+"s",cx,ly,p);}
   if(s[base+6]>0){float radius=Math.max(24*dp,Math.min(90*dp,(s[at+6]-s[at+4])*w/(2*worldWidth)+18*dp));glow(c,cx,cy,radius*1.4f,COLOR[6],.1f);polygon(c,cx,cy,radius,6,time*.0002f,COLOR[6],.7f);ring(c,cx,cy,radius+5*dp,COLOR[6],.4f,1);}
   if(s[base+11]>0){float r=25*dp;glow(c,cx,cy,r*1.7f,COLOR[11],.28f);ring(c,cx,cy,r,COLOR[11],.65f,1.8f);for(int j=0;j<3;j++){float px=cx+(j-1)*13*dp,py=cy+(float)Math.sin(time*.004+j)*6*dp;path.reset();path.moveTo(px,py+15*dp);path.cubicTo(px-11*dp,py,px+11*dp,py-7*dp,px,py-24*dp);p.setColor(COLOR[11]);p.setAlpha(190);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*dp);c.drawPath(path,p);}if(symbols!=null){android.graphics.drawable.Drawable flame=symbols[11];flame.setBounds((int)(cx-9*dp),(int)(cy-r-21*dp),(int)(cx+9*dp),(int)(cy-r-3*dp));flame.draw(c);}}
   if(s[base+18]>0){ring(c,cx,cy,25*dp,COLOR[18],.25f,1);ring(c,cx,cy,31*dp,COLOR[18],.12f,1);}
   if(s[base+7]>0){polygon(c,cx,cy,29*dp,3,time*.002f,COLOR[7],.8f);}
   if(s[base+16]>0){polygon(c,cx,cy-20*dp,10*dp,4,0,COLOR[16],.9f);}
   for(int k=9;k<18;k++)if(s[base+k]>0&&k!=16){float r=(19+(k-9)*2)*dp;double a=time*.001+(k-9)*.7;float xx=cx+(float)Math.cos(a)*r,yy=cy+(float)Math.sin(a)*r;ring(c,cx,cy,r,COLOR[k],.14f,1);polygon(c,xx,yy,4*dp,k==14?3:4,(float)a,COLOR[k],.8f);}
  }
  // Transients come from a bounded authoritative event journal, not just its last item.
  for(int k=0;k<count;k++){int a=209+k*9;if(a+8>=s.length)break;int kind=s[a+1],team=s[a+2],at=s[a+6],stage=s[a+8],target=s[a+7];if(kind<0||kind>=20||hidden(s,team)||hidden(s,target))continue;int age=time-at;if(kind==4&&s[a+5]>=0)age=s[207]>0?3003-s[207]:time-at+3003;if(age<0||age>2200)continue;float u=age/2200f,fade=1-u,cx=x(s[a+3]),cy=y(s[a+4]);int color=COLOR[kind];
   if(kind==4&&s[a+5]>=0){cx=viewportLeft+viewportWidth/2;cy=viewportTop+viewportHeight/2;glow(c,cx,cy,Math.max(viewportWidth,viewportHeight)*.65f,color,.14f*fade);float radius=Math.min(viewportWidth,viewportHeight)*(.15f+.4f*u);ring(c,cx,cy,radius,color,.7f*fade,2);for(int j=0;j<12;j++){double ang=j*Math.PI/6-age*.004;line(c,color,fade,2,cx+(float)Math.cos(ang)*radius*.88f,cy+(float)Math.sin(ang)*radius*.88f,cx+(float)Math.cos(ang)*radius,cy+(float)Math.sin(ang)*radius);}line(c,color,fade,3,cx,cy,cx+(float)Math.cos(-age*.008)*radius*.7f,cy+(float)Math.sin(-age*.008)*radius*.7f);}
   else if(kind==3&&stage==2){float r=(18+25*(float)Math.sin(u*Math.PI))*dp;glow(c,cx,cy,r*3,color,.4f*fade);for(int j=0;j<3;j++)ring(c,cx,cy,r+j*8*dp,color,fade*(1-j*.2f),2);for(int j=0;j<32;j++){double ang=j*2*Math.PI/32+age*.004;float rr=r+u*40*dp;line(c,color,fade,1.6f,cx+(float)Math.cos(ang)*rr,cy+(float)Math.sin(ang)*rr,cx+(float)Math.cos(ang)*(rr+12*dp),cy+(float)Math.sin(ang)*(rr+12*dp));}}
   else if(kind==7&&stage==2){if(symbols!=null){android.graphics.drawable.Drawable skull=symbols[7];int r=(int)(28*dp);skull.setAlpha((int)(255*fade));skull.setBounds((int)cx-r,(int)cy-r,(int)cx+r,(int)cy+r);skull.draw(c);skull.setAlpha(255);}glow(c,cx,cy,(45+80*u)*dp,color,.4f*fade);for(int j=0;j<48;j++){double ang=j*2.399;float rr=(10+u*(35+j%10*8))*dp,px=cx+(float)Math.cos(ang)*rr,py=cy+(float)Math.sin(ang)*rr;polygon(c,px,py,(3+4*fade)*dp,3,(float)ang+u*5,color,fade);}ring(c,cx,cy,(20+120*u)*dp,color,fade,3);}
   else if(kind==8&&stage==2){int b=147+team*10;float x1=x(s[b+2]),y1=y(s[b+3]);line(c,color,fade,4,x1,y1,cx,cy);for(int j=0;j<8;j++){float q=(u+j/8f)%1;glow(c,x1+(cx-x1)*q,y1+(cy-y1)*q,8*dp,color,fade*.7f);}polygon(c,cx,cy,(20+30*u)*dp,6,u*3,color,fade);}
   else if(kind==2){float rr=(12+u*170)*dp;ring(c,cx,cy,rr,color,fade,2);for(int j=0;j<12;j++){double ang=j*Math.PI/6;polygon(c,cx+(float)Math.cos(ang)*rr,cy+(float)Math.sin(ang)*rr,5*dp,6,u*3,color,fade);}}
   else if(kind>=18){ring(c,cx,cy,(12+u*60)*dp,color,fade*.6f,2);glow(c,cx,cy,25*dp,color,fade*.15f);}
   else if(kind>=9){counter(c,kind,cx,cy,u,fade);}
   else {float radius=(12+u*65)*dp;glow(c,cx,cy,radius,color,.22f*fade);ring(c,cx,cy,radius,color,fade,kind>=9?3:2);int rays=kind==0?24:kind>=9?9:14;for(int j=0;j<rays;j++){double ang=j*2*Math.PI/rays+kind*.4;float rr=(14+u*(35+j%4*8))*dp;float px=cx+(float)Math.cos(ang)*rr,py=cy+(float)Math.sin(ang)*rr;if(kind>=9)polygon(c,px,py,4*dp,(kind%3)+3,u*3,color,fade);else line(c,color,fade,2,px,py,px+(float)Math.cos(ang)*8*dp,py+(float)Math.sin(ang)*8*dp);}}
   if(s[a+5]<0){line(c,0xffffa0a0,fade,4,cx-18*dp,cy-18*dp,cx+18*dp,cy+18*dp);line(c,0xffffa0a0,fade,4,cx+18*dp,cy-18*dp,cx-18*dp,cy+18*dp);}
  }
  c.restore();p.setShader(null);p.setAlpha(255);
 }
}
