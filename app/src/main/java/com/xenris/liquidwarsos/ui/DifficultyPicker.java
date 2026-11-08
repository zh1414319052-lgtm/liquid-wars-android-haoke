// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.xenris.liquidwarsos.AiDifficulty;
import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.StaticBits;

public final class DifficultyPicker {
    private DifficultyPicker() {}
    public static void bind(Activity activity,boolean room,Runnable changed) {
        SharedPreferences prefs=activity.getSharedPreferences("ai_difficulty",Context.MODE_PRIVATE);
        String key=room?"room":"solo";
        int selected=AiDifficulty.valid(prefs.getInt(key,AiDifficulty.CLASSIC));
        StaticBits.aiDifficulty=selected;
        Spinner spinner=activity.findViewById(R.id.difficulty_spinner);
        TextView description=activity.findViewById(R.id.difficulty_description);
        ArrayAdapter<CharSequence> adapter=ArrayAdapter.createFromResource(activity,R.array.ai_difficulty_options,R.layout.ui_spinner_item);
        adapter.setDropDownViewResource(R.layout.ui_spinner_dropdown_item);
        spinner.setAdapter(adapter);spinner.setSelection(selected);
        description.setText(description(selected));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                int value=AiDifficulty.valid(position);description.setText(description(value));
                if(StaticBits.aiDifficulty==value)return;
                StaticBits.aiDifficulty=value;prefs.edit().putInt(key,value).apply();
                if(changed!=null)changed.run();
            }
        });
    }
    private static int description(int level) {
        return level==AiDifficulty.RELAXED?R.string.ai_relaxed_description:level==AiDifficulty.HARD?R.string.ai_hard_description:R.string.ai_classic_description;
    }
}
