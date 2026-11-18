// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.*;

/** Two interchangeable pads, one existing FOLLOW touch point, and a local layout editor. */
public final class JoystickOverlay extends View {
    private final Paint paint=new Paint(3);
    private final float dp;
    private final SharedPreferences prefs;
    private final JoystickPlacement placement=new JoystickPlacement();
    final DualStickInput sticks=new DualStickInput();
    private int epoch=-1,generation=-1,group=-1,lastX=-1,lastY=-1;
    private int button=-1,drag=-1,dragPointer=-1,sliderPointer=-1,sliderKind=-1,secondary=-1,secondarySlot=-1;
    private long lastFrame,sentAt;
    private boolean editing;
    private float offsetX,offsetY;

    public JoystickOverlay(Context c,AttributeSet a){
        super(c,a);dp=getResources().getDisplayMetrics().density;
        prefs=c.getSharedPreferences("battle-controls",0);placement.load(prefs);
        paint.setTypeface(Typeface.createFromAsset(c.getAssets(),"fonts/NotoSansSC-Lobby600.ttf"));
        setContentDescription(BattleText.t("左右摇杆与位置设置","Left and right sticks and placement settings"));
    }
    private View root(){return (View)getParent();}
    private TacticalOverlay orders(){return root().findViewById(R.id.tactical_overlay);}
    private ItemOverlay items(){return root().findViewById(R.id.item_overlay);}
    private MyGLSurfaceView surface(){return root().findViewById(R.id.mySurfaceView);}
    JoystickCursor cursor(){return items().followCursor;}
    private boolean visible(){
        if(getParent()==null||orders().facts()==null)return false;
        if(((android.app.Activity)getContext()).getIntent().getBooleanExtra("spectator",false))return false;
        View intro=root().findViewById(R.id.match_intro);
        return (intro==null||intro.getVisibility()!=VISIBLE)&&!((ExpeditionOverlay)root().findViewById(R.id.expedition_overlay)).modalOpen();
    }
    private boolean enabled(){return visible()&&orders().joystickEnabled();}
    float radius(){return Math.min(JoystickPlacement.RADIUS_DP*dp,getHeight()*.29f)*placement.size();}
    float padX(){return padX(0);}float padY(){return padY(0);}
    private float[] centre(int side){
        float r=radius(),x=placement.x(side,getWidth(),r),top=75*dp+r,bottom=orders().controlsTop()-r-12*dp;
        if(side==1){
            float u=BattleHudLayout.unit(getWidth(),getHeight());
            bottom=Math.min(bottom,getHeight()-120*u-r);
        }
        float y=placement.y(side,getHeight(),r,Math.min(top,bottom),bottom);
        if(side==1&&surface().camera.large()){
            float u=BattleHudLayout.unit(getWidth(),getHeight());
            RectF mini=BattleHudLayout.mini(getWidth()/u,surface().camera.width,surface().camera.height);
            if(x+r>mini.left*u-8*dp&&y-r<(mini.bottom+24)*u)
                x=Math.min(x,Math.max(getWidth()/2f+r+6,mini.left*u-r-8*dp));
        }
        return new float[]{x,y};
    }
    float padX(int side){return centre(side)[0];}float padY(int side){return centre(side)[1];}
    private int padAt(float x,float y){for(int i=0;i<2;i++)if(Math.hypot(x-padX(i),y-padY(i))<=radius()+7*dp)return i;return -1;}
    private RectF editButton(int i){float width=62*dp;return new RectF(getWidth()/2f+(i-1.5f)*(width+6*dp),12*dp,getWidth()/2f+(i-1.5f)*(width+6*dp)+width,46*dp);}
    private int editButtonAt(float x,float y){for(int i=0;i<3;i++)if(editButton(i).contains(x,y))return i;return -1;}
    private RectF settingTrack(int kind){return new RectF(getWidth()/2f-135*dp,(76+60*kind)*dp,getWidth()/2f+135*dp,(110+60*kind)*dp);}
    private int sliderAt(float x,float y){for(int i=0;i<2;i++){RectF t=settingTrack(i);t.inset(-12*dp,-4*dp);if(t.contains(x,y))return i;}return -1;}
    private void adjustSlider(float x){
        RectF t=settingTrack(sliderKind);float f=JoystickPlacement.clamp((x-t.left)/t.width(),0,1);
        if(sliderKind==0)placement.size(JoystickPlacement.MIN_SCALE+f*(JoystickPlacement.MAX_SCALE-JoystickPlacement.MIN_SCALE));
        else placement.sensitivity(JoystickPlacement.MIN_SENSITIVITY+f*(JoystickPlacement.MAX_SENSITIVITY-JoystickPlacement.MIN_SENSITIVITY));
    }
    private void release(){sticks.clear();secondary=secondarySlot=-1;cursor().release();lastFrame=0;invalidate();}
    void clear(){release();items().resetControl();lastX=lastY=-1;}
    void cancelForModal(){release();}
    void modeChanged(){release();items().releasePointerGesture();items().keepPrimaryTarget();lastX=lastY=-1;}
    boolean editing(){return editing;}
    public boolean back(){if(!editing)return false;finishEdit(false);return true;}
    void startEdit(){orders().cancelForModal();placement.load(prefs);editing=true;button=drag=dragPointer=sliderPointer=sliderKind=-1;invalidate();}
    private void finishEdit(boolean save){
        if(save)placement.save(prefs);else placement.load(prefs);
        editing=false;button=drag=dragPointer=sliderPointer=sliderKind=-1;release();
    }
    void frame(long nanos){
        OperationsSnapshot s=orders().facts();if(s==null)return;
        int team=StaticBits.team,g=orders().selectedGroup();
        if((epoch>=0&&epoch!=s.epoch())||(generation>=0&&generation!=s.generation(team))||(group>=0&&group!=g))clear();
        epoch=s.epoch();generation=s.generation(team);group=g;
        if(!enabled()||editing){if(sticks.held())release();invalidate();return;}
        int count=0;for(int k=0;k<3;k++)if(g==3||k==g)count+=s.raw[s.group(team,k)];
        if(count==0){clear();return;}
        if(sticks.held()&&!cursor().ready())release();
        float dt=lastFrame==0?0:Math.min(.05f,(nanos-lastFrame)/1e9f);lastFrame=nanos;
        if(sticks.held()&&hasWindowFocus()){
            cursor().axis(sticks.x(),-sticks.y());
            if(cursor().advance(dt,placement.sensitivity()))send(false);
        }
        invalidate();
    }
    private boolean begin(){
        OperationsSnapshot s=orders().facts();if(s==null)return false;
        int t=StaticBits.team,g=orders().selectedGroup();WorldCamera cam=surface().camera;
        cursor().bounds(cam.width,cam.height);
        if(!cursor().ready()){
            long x=0,y=0,n=0;for(int k=0;k<3;k++)if(g==3||k==g){int b=s.group(t,k),count=s.raw[b];n+=count;x+=(long)s.raw[b+1]*count;y+=(long)s.raw[b+2]*count;}
            if(n==0)return false;
            cursor().target(x/(float)n,y/(float)n);
            int b=s.group(t,g==3?0:g);
            if((g<3||!s.split(t))&&s.raw[b+3]!=0&&s.raw[b+4]==0&&s.raw[b+7]==1)cursor().target(s.raw[b+8],s.raw[b+9]);
        }
        items().keepPrimaryTarget();surface().camera.endTouch();lastFrame=0;lastX=lastY=-1;send(true);return true;
    }
    private void send(boolean force){
        long now=android.os.SystemClock.uptimeMillis();int x=Math.round(cursor().x()),y=Math.round(cursor().y());
        if(!cursor().ready()||(!force&&now-sentAt<50)||(x==lastX&&y==lastY))return;
        orders().joystickAt(x,y);lastX=x;lastY=y;sentAt=now;
    }
    private void axis(int side,float x,float y){float travel=radius()*.66f;sticks.axis(side,(x-padX(side))/travel,(y-padY(side))/travel);}
    private int itemAt(float x,float y){float u=(GardenSession.expeditionSeats>0||surface().camera.large())?BattleHudLayout.unit(getWidth(),getHeight()):dp;return ActiveSlots.hit(x/u,y/u,getWidth()/u,getHeight()/u);}
    private void useItem(int slot){if(GardenSession.expeditionSeats>0)((ExpeditionOverlay)root().findViewById(R.id.expedition_overlay)).joystickItem(slot);else orders().activeItem(slot);}
    private void text(Canvas c,String value,float x,float y,float size,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTextSize(size);paint.setTextAlign(Paint.Align.CENTER);c.drawText(value,x,y,paint);}
    private void button(Canvas c,RectF box,String label){paint.setStyle(Paint.Style.FILL);paint.setColor(0xb31b3439);c.drawRoundRect(box,8*dp,8*dp,paint);text(c,label,box.centerX(),box.centerY()+4*dp,11*dp,0xffe0eee6);}
    private void slider(Canvas c,int kind,float value){
        RectF track=settingTrack(kind);float thumb=track.left+value*track.width();
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4*dp);paint.setColor(0xff526b6d);c.drawLine(track.left,track.centerY(),track.right,track.centerY(),paint);
        paint.setColor(0xffa1cbbb);c.drawLine(track.left,track.centerY(),thumb,track.centerY(),paint);paint.setStyle(Paint.Style.FILL);c.drawCircle(thumb,track.centerY(),9*dp,paint);
    }
    @Override protected void onDraw(Canvas c){
        if(!visible())return;
        // Existing follow marker, including touch mode. No separate joystick crosshair.
        items().drawFollowTargets(c);
        if(editing){paint.setStyle(Paint.Style.FILL);paint.setColor(0x55102024);c.drawRect(0,0,getWidth(),getHeight(),paint);}
        if(enabled()||editing())for(int i=0;i<2;i++){
            float x=padX(i),y=padY(i),r=radius();
            paint.setStyle(Paint.Style.FILL);paint.setColor(editing?0x80213e42:0x38172730);c.drawCircle(x,y,r,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp);paint.setColor(0x9896b9b0);c.drawCircle(x,y,r,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(sticks.owner()==i?0xc7b6d9ce:0x8092b1a5);
            c.drawCircle(x+sticks.x(i)*r*.66f,y+sticks.y(i)*r*.66f,r*.39f,paint);
        }
        if(editing){
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xfa1b3439);
            float half=Math.min(180*dp,getWidth()/2f-12*dp);
            c.drawRoundRect(getWidth()/2f-half,6*dp,getWidth()/2f+half,200*dp,10*dp,10*dp,paint);
            button(c,editButton(0),BattleText.t("恢复默认","Reset"));button(c,editButton(1),BattleText.t("取消","Cancel"));button(c,editButton(2),BattleText.t("保存","Save"));
            text(c,BattleText.t("摇杆大小 ","Stick size ")+Math.round(placement.size()*200)+"%",getWidth()/2f,68*dp,12*dp,0xffe7eee1);
            slider(c,0,(placement.size()-JoystickPlacement.MIN_SCALE)/(JoystickPlacement.MAX_SCALE-JoystickPlacement.MIN_SCALE));
            text(c,BattleText.t("摇杆灵敏度 ","Stick sensitivity ")+Math.round(placement.sensitivity()*100)+"%",getWidth()/2f,128*dp,12*dp,0xffe7eee1);
            slider(c,1,(placement.sensitivity()-JoystickPlacement.MIN_SENSITIVITY)/(JoystickPlacement.MAX_SENSITIVITY-JoystickPlacement.MIN_SENSITIVITY));
            text(c,BattleText.t("拖动摇杆调整位置 · 保存后生效","Drag a stick to reposition · Save to keep"),getWidth()/2f,188*dp,12*dp,0xffe7eee1);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(!visible()&&!editing)return false;
        int action=e.getActionMasked(),idx=e.getActionIndex();float x=e.getX(idx),y=e.getY(idx);
        if(editing){
            if(action==MotionEvent.ACTION_DOWN){
                button=editButtonAt(x,y);drag=-1;
                sliderKind=sliderAt(x,y);
                if(sliderKind>=0){sliderPointer=e.getPointerId(idx);adjustSlider(x);}
                else if(button<0){drag=padAt(x,y);dragPointer=e.getPointerId(idx);if(drag>=0){offsetX=x-padX(drag);offsetY=y-padY(drag);}}
            }
            else if(action==MotionEvent.ACTION_MOVE&&sliderPointer>=0){int at=e.findPointerIndex(sliderPointer);if(at>=0)adjustSlider(e.getX(at));}
            else if(action==MotionEvent.ACTION_MOVE&&drag>=0){int at=e.findPointerIndex(dragPointer);if(at>=0){placement.move(drag,e.getX(at)-offsetX,e.getY(at)-offsetY,getWidth(),getHeight());placement.move(drag,padX(drag),padY(drag),getWidth(),getHeight());}}
            else if(action==MotionEvent.ACTION_UP){if(button>=0&&button==editButtonAt(x,y)){if(button==0)placement.defaults();else finishEdit(button==2);}button=drag=dragPointer=sliderPointer=sliderKind=-1;performClick();}
            else if(action==MotionEvent.ACTION_CANCEL){button=drag=dragPointer=sliderPointer=sliderKind=-1;}
            invalidate();return true;
        }
        if(action==MotionEvent.ACTION_DOWN){
            if(!enabled())return false;int side=padAt(x,y);if(side<0||!begin())return false;
            sticks.press(side,e.getPointerId(idx));axis(side,x,y);return true;
        }
        if(action==MotionEvent.ACTION_CANCEL){release();return true;}
        if(action==MotionEvent.ACTION_POINTER_DOWN){int side=padAt(x,y);if(side>=0&&sticks.press(side,e.getPointerId(idx)))axis(side,x,y);else {int slot=itemAt(x,y);if(slot>=0){secondary=e.getPointerId(idx);secondarySlot=slot;}}return true;}
        if(action==MotionEvent.ACTION_MOVE){for(int i=0;i<2;i++){int at=e.findPointerIndex(sticks.pointer(i));if(at>=0)axis(i,e.getX(at),e.getY(at));}return true;}
        if(action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_UP){
            int id=e.getPointerId(idx);if(id==secondary){int slot=secondarySlot;secondary=secondarySlot=-1;if(slot==itemAt(x,y))useItem(slot);}
            if(sticks.sideOf(id)>=0){send(true);sticks.lift(id);cursor().release();lastFrame=0;}
            if(action==MotionEvent.ACTION_UP){release();performClick();}invalidate();return true;
        }
        return sticks.held();
    }
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(!focus&&getParent()!=null)release();}
    @Override protected void onDetachedFromWindow(){if(getParent()!=null)release();super.onDetachedFromWindow();}
    @Override public boolean performClick(){super.performClick();return true;}
}
