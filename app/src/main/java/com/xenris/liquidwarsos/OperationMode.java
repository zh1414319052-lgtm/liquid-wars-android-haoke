// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.app.Activity;import android.graphics.Typeface;import android.view.View;import android.view.ViewGroup;import android.widget.*;
/** Shared setup and tie-breaker; classic mode retains its existing winner rules. */
public final class OperationMode {
 public static int compare(int a,int b){if(StaticBits.operationsMode==1){int alive=Integer.compare(AllianceRules.score(a)>0?1:0,AllianceRules.score(b)>0?1:0);if(alive!=0)return alive;int d=Integer.compare(NativeInterface.operationScore(a),NativeInterface.operationScore(b));if(d!=0)return d;}return Integer.compare(AllianceRules.score(a),AllianceRules.score(b));}
 public static boolean wins(int t){return NativeInterface.operationWinner()==t||(NativeInterface.totalParticles()>0&&AllianceRules.score(t)==NativeInterface.totalParticles());}
 public static void bind(Activity a,boolean editable,Runnable changed){
  View sibling=a.findViewById(R.id.teamsize_spinner);if(sibling==null)return;ViewGroup parent=(ViewGroup)sibling.getParent();
  // Existing setup uses a vertically stacked settings card. Place the mode beside its controls.
  while(parent!=null&&!(parent instanceof LinearLayout)&&!(parent instanceof android.widget.TableLayout)){if(!(parent.getParent() instanceof ViewGroup))break;parent=(ViewGroup)parent.getParent();}
  if(parent==null)return;
  boolean zh="zh".equals(a.getResources().getConfiguration().getLocales().get(0).getLanguage());
  LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(0,8,0,8);
  TextView label=new TextView(a);label.setTypeface(Typeface.createFromAsset(a.getAssets(),"fonts/NotoSansSC-Intro.ttf"));label.setTextColor(0xffcce0d8);label.setText(zh?"玩法 · 据点可产兵，控点得分180获胜":"MODE · Capture, grow, reach 180 control points");label.setTextSize(12);row.addView(label);
  Spinner pick=new Spinner(a);pick.setBackgroundResource(R.drawable.ui_panel);pick.setContentDescription(zh?"玩法模式":"Game mode");ArrayAdapter<String> adapter=new ArrayAdapter<>(a,R.layout.ui_spinner_item,zh?new String[]{"经典吞噬","据点争夺 · 产兵与控点"}:new String[]{"Classic","Conquest · production & control"});adapter.setDropDownViewResource(R.layout.ui_spinner_dropdown_item);pick.setAdapter(adapter);pick.setSelection(StaticBits.operationsMode);pick.setEnabled(editable);row.addView(pick,new LinearLayout.LayoutParams(-1,48*(int)Math.max(1,a.getResources().getDisplayMetrics().density)));
  pick.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int position,long id){StaticBits.operationsMode=position;if(changed!=null)changed.run();}});parent.addView(row);
 }
}
