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

import android.os.Bundle;
import android.view.Window;


import com.xenris.liquidwarsos.R;

public class InstructionsActivity extends LocalizedActivity {
    private com.xenris.liquidwarsos.updates.MenuUpdateHost updates;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.instructions);
        ((android.widget.TextView)findViewById(R.id.release_attribution_text)).setText(
                getString(R.string.release_attribution,getString(R.string.ui_version)));
        updates=new com.xenris.liquidwarsos.updates.MenuUpdateHost(this,false);
        updates.addAboutControls((android.widget.LinearLayout)findViewById(R.id.release_attribution_text).getParent());
    }
    @Override public void onResume(){super.onResume();if(updates!=null)updates.resume();}
    @Override public void onPause(){if(updates!=null)updates.pause();super.onPause();}
    @Override public void onDestroy(){if(updates!=null)updates.close();super.onDestroy();}
}
