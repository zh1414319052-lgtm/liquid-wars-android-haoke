package com.xenris.liquidwarsos;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.*;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.xenris.liquidwarsos.activities.OnlineLobbyActivity;
import com.xenris.liquidwarsos.online.GameShare;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;

@RunWith(AndroidJUnit4.class)
public class Share185Test {
    View find(View v,String text){if(v instanceof TextView&&text.equals(((TextView)v).getText().toString()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),text);if(found!=null)return found;}return null;}
    @Test public void gameAndRoomShare()throws Exception{
        Intent game=GameShare.message("");
        Assert.assertEquals(Intent.ACTION_SEND,game.getAction());
        Assert.assertEquals("text/plain",game.getType());
        Assert.assertEquals("一起来玩液态战争！\nhttps://www.haoke.org/software/f2a585b84fbf5b85?platform=android",game.getStringExtra(Intent.EXTRA_TEXT));
        Assert.assertNull(game.getPackage()); // Users choose WeChat, QQ, or another installed handler.
        Intent invite=GameShare.message("HKQA85");
        Assert.assertTrue(invite.getStringExtra(Intent.EXTRA_TEXT).contains("https://cleanbizai.com/invite/HKQA85?game=liquid-wars"));
        Assert.assertTrue(invite.getStringExtra(Intent.EXTRA_TEXT).contains(GameShare.DOWNLOAD_URL));
        Assert.assertEquals(game.getStringExtra(Intent.EXTRA_TEXT),GameShare.message(null).getStringExtra(Intent.EXTRA_TEXT));
        Instrumentation inst=InstrumentationRegistry.getInstrumentation();
        Activity a=inst.startActivitySync(new Intent(inst.getTargetContext(),OnlineLobbyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{
            SystemClock.sleep(900);
            inst.runOnMainSync(()->{View share=find(a.getWindow().getDecorView(),"分享");Assert.assertNotNull(share);share.performClick();});
            SystemClock.sleep(1200);
            Bitmap image=inst.getUiAutomation().takeScreenshot();Assert.assertNotNull(image);
            try(FileOutputStream out=new FileOutputStream(new File(inst.getTargetContext().getFilesDir(),"ui185-share.jpg"))){image.compress(Bitmap.CompressFormat.JPEG,88,out);}image.recycle();
            // Leave the chooser open for visual verification; never choose or send to a contact.
        }finally{inst.runOnMainSync(a::finish);}
    }
}
