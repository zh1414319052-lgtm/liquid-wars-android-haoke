//    This file is part of Liquid Wars.
//
//    Copyright (C) 2013 Henry Shepperd (hshepperd@gmail.com)
//
//    Liquid Wars is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Liquid Wars is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Liquid Wars.  If not, see <http://www.gnu.org/licenses/>.

package com.xenris.liquidwarsos.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public abstract class LocalizedActivity extends AppCompatActivity {
    @Override protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        getWindow().setBackgroundDrawableResource(com.xenris.liquidwarsos.R.color.ui_bg);
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            android.view.WindowManager.LayoutParams p=getWindow().getAttributes();
            p.layoutInDisplayCutoutMode=android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(p);
        }
    }

    /** Menus paint behind the camera cutout; interactive content stays inside it.
     * The battlefield keeps its existing GameWindow/GL coordinate ownership. */
    @Override public void onContentChanged() {
        super.onContentChanged();
        android.view.ViewGroup content=findViewById(android.R.id.content);
        if(content==null||content.getChildCount()==0||findViewById(com.xenris.liquidwarsos.R.id.mySurfaceView)!=null)return;
        android.view.View root=content.getChildAt(0);
        final int l=root.getPaddingLeft(),t=root.getPaddingTop(),r=root.getPaddingRight(),b=root.getPaddingBottom();
        root.setFitsSystemWindows(false);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            androidx.core.graphics.Insets safe=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout()|androidx.core.view.WindowInsetsCompat.Type.ime());
            view.setPadding(l+safe.left,t+safe.top,r+safe.right,b+safe.bottom);
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(root);
    }
    /** Menu-only back affordance; existing activity lifecycle owns connection cleanup. */
    public void uiBack(android.view.View view) {
        finish();
    }
    public void modelSettings(android.view.View view){startActivity(new android.content.Intent(this,ModelSettingsActivity.class));}

    static Locale uiLocale(LocaleList systemLocales) {
        return !systemLocales.isEmpty() && "zh".equals(systemLocales.get(0).getLanguage())
                ? Locale.SIMPLIFIED_CHINESE : Locale.ENGLISH;
    }

    @Override
    protected void attachBaseContext(Context base) {
        Configuration language = new Configuration();
        language.setLocale(uiLocale(Resources.getSystem().getConfiguration().getLocales()));
        super.attachBaseContext(base.createConfigurationContext(language));
    }
}
