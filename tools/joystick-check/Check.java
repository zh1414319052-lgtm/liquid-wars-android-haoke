package com.xenris.liquidwarsos;
import com.xenris.liquidwarsos.net.GameWire;
import java.io.*;import java.util.*;
public final class Check{
 static int checks;static void ok(boolean x,String why){checks++;if(!x)throw new AssertionError(why);}
 static void close(float a,float b,String why){ok(Math.abs(a-b)<.002,why);}
 public static void main(String[] args)throws Exception{
 TouchCursor c=new TouchCursor();c.grab(300,200);ok(c.ready()&&c.active(),"grab begins near army");c.axis(1,0);c.advance(.1f);close(c.x(),318,"stick moves target coordinate");close(c.y(),200,"no y drift");c.release();float x=c.x();ok(!c.advance(.1f)&&c.x()==x,"release holds target while army may follow");
 c.target(700,450);c.grab(200,100);close(c.x(),200,"far old click recenters near army");c.target(220,110);c.grab(200,100);close(c.x(),220,"near click retained across hands");c.axis(.1f,0);ok(!c.advance(1),"dead zone does not creep");c.axis(1,0);for(int k=0;k<100;k++)c.advance(.05f);ok(Math.hypot(c.x()-200,c.y()-100)<=TouchCursor.MAX_LEAD+.01,"target always near army");float old=c.x();c.followMass(240,100);c.advance(.1f);ok(c.x()>old,"lead follows actual army movement");c.followMass(600,400);close(c.x(),600,"teleport recenters virtual touch");
 c.target(799,479);c.grab(799,479);c.axis(1,1);c.advance(.1f);ok(c.mapX()==799&&c.mapY()==479,"map upper edges");c.target(0,0);c.grab(0,0);c.axis(-1,-1);c.advance(.1f);ok(c.mapX()==0&&c.mapY()==0,"map lower edges");c.grab(300,200);c.axis(1,0);c.advance(10);close(c.x(),318,"long UI stall capped at100ms");c.axis(Float.NaN,0);ok(!c.advance(.1f)&&Float.isFinite(c.x()),"bad axis rejected");ok(!c.advance(Float.NaN),"bad dt rejected");
 float reference=0;for(int hz:new int[]{10,30,60}){TouchCursor p=new TouchCursor();p.grab(300,200);p.axis(.6f,.8f);for(int k=0;k<hz;k++)p.advance(.3f/hz);if(reference==0)reference=p.x();else close(p.x(),reference,"input frame cadence independent");}
 for(int direction=0;direction<16;direction++){double a=direction*Math.PI/8;TouchCursor p=new TouchCursor();p.grab(400,240);p.axis((float)Math.cos(a),(float)Math.sin(a));for(int k=0;k<60;k++){p.advance(1f/60);ok(p.mapX()>=0&&p.mapX()<800&&p.mapY()>=0&&p.mapY()<480,"ordinary valid touch coordinates");ok(Math.hypot(p.x()-400,p.y()-240)<=88.01,"lead cap in every direction");}}
 int[] packet={7,318,-1,-1,-1,-1,200,-1,-1,-1,-1};ByteArrayOutputStream bytes=new ByteArrayOutputStream();GameWire.write(bytes,11,packet);ok(Arrays.equals(packet,GameWire.read(new ByteArrayInputStream(bytes.toByteArray()))),"same legacy ten-coordinate wire format");
 TouchCursor widescreen=new TouchCursor();widescreen.grab(300,200);widescreen.mapScale(.75f);widescreen.axis(.7071068f,.7071068f);widescreen.advance(.1f);close((widescreen.x()-300)/.75f,widescreen.y()-200,"diagonal screen direction survives anisotropic rule grid");
 c.clear();ok(!c.ready()&&!c.active()&&!c.advance(.1f),"reset has no stale touch");System.out.println("PASS "+checks+" virtual-touch assertions; no Android/performance/human claim");
 }
}
