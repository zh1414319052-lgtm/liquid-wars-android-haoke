// SPDX-License-Identifier: GPL-3.0-or-later
import com.xenris.liquidwarsos.GameViewport;

/** Desktop geometry check only. Does not launch Android or inject device input. */
public final class GameViewportCheck {
    public static void main(String[] args) {
        int[][] screens={{2376,1080},{2400,1080},{2560,1080},{1920,1080},{1280,800},{1024,768},{800,480},{1080,2376},{400,300}};
        double worstAnisotropy=0;
        int roundTrips=0;
        for(int[] screen:screens) {
            int[] fit=GameViewport.fit(screen[0],screen[1]);
            if(fit[0]>screen[0]||fit[1]>screen[1]||fit[0]<=0||fit[1]<=0)throw new AssertionError("Bounds");
            // New native map quad has no sidebar squeeze; artwork is1980x900.
            double sx=fit[0]/1980.0, sy=fit[1]/900.0;
            double error=Math.abs(sx/sy-1);worstAnisotropy=Math.max(worstAnisotropy,error);
            if(error>.004)throw new AssertionError("Map stretched");
            for(int x=0;x<800;x++) {
                float pixel=(float)((x+.5)*fit[0]/800.0);
                if(GameViewport.mapX(pixel,fit[0])!=x)throw new AssertionError("Touch inverse at "+x);
                roundTrips++;
            }
            for(int y=0;y<480;y++) {
                float pixel=(float)((1-(y+.5)/480.0)*fit[1]);
                int result=479-(int)(pixel*480/fit[1]);
                if(result!=y)throw new AssertionError("Touch y inverse at "+y);
                roundTrips++;
            }
            if(GameViewport.mapX(-100,fit[0])!=0||GameViewport.mapX(fit[0]+100,fit[0])!=799)throw new AssertionError("Edge clamp");
            System.out.println(screen[0]+"x"+screen[1]+" -> view "+fit[0]+"x"+fit[1]+", shape_error="+error);
        }
        int[] empty=GameViewport.fit(0,0);if(empty[0]!=0||empty[1]!=0)throw new AssertionError("Zero-sized surface");
        System.out.println("PASS: "+roundTrips+" touch roundtrips; worst shape error="+worstAnisotropy);
    }
}
