package com.xenris.liquidwarsos.net;
import com.xenris.liquidwarsos.PlayerHistory;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClassicHistoryTest {
    @Test public void resendIndexIsRelativeToLastSentStepIncludingRingWrap() {
        PlayerHistory history=new PlayerHistory();
        short[][] x=new short[6][5],y=new short[6][5];
        for(int sent=0;sent<75;sent++){x[0][0]=(short)sent;y[0][0]=(short)(sent+100);history.savePlayerPositions(x,y);}
        int nextStep=75,lastSent=nextStep-1;
        for(int resend=25;resend<=lastSent;resend++){
            int[] packet=new int[63];history.serialiseHistoricalPlayerState(lastSent-resend,packet,3);
            assertEquals(resend,packet[3]);assertEquals(resend+100,packet[4]);
        }
        assertEquals(-1,history.getHistoryIndex(50));assertEquals(-1,history.getHistoryIndex(-1));
    }
}
