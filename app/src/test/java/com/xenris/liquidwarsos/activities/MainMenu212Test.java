package com.xenris.liquidwarsos.activities;
import android.view.View;
import android.content.Intent;
import com.xenris.liquidwarsos.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="en-rUS-w960dp-h432dp-land")
public class MainMenu212Test {
    @Test public void gridAndFooterAlignAndNavigationStillUsesExistingModes(){
        ActivityController<MainMenuActivity> c=Robolectric.buildActivity(MainMenuActivity.class).create().start();
        MainMenuActivity a=c.get();View root=a.findViewById(R.id.ui_root);
        int width=1920,height=864;root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);
        View single=a.findViewById(R.id.menu_single),garden=a.findViewById(R.id.garden_trial);
        assertEquals(single.getHeight(),garden.getHeight());assertEquals(single.getTop(),garden.getTop());
        assertTrue(single.getWidth()>0);assertTrue(single.getHeight()>0);
        assertEquals(a.findViewById(R.id.menu_notes).getTop(),a.findViewById(R.id.support_game).getTop());
        a.findViewById(R.id.menu_large).performClick();Intent i=shadowOf(a).getNextStartedActivity();assertTrue(i.getBooleanExtra("largeMode",false));
        a.findViewById(R.id.menu_spectate).performClick();assertTrue(shadowOf(a).getNextStartedActivity().getBooleanExtra("spectatorSetup",false));
        a.findViewById(R.id.support_game).performClick();assertEquals(GameSupportActivity.class.getName(),shadowOf(a).getNextStartedActivity().getComponent().getClassName());
        c.stop().destroy();
    }
}
