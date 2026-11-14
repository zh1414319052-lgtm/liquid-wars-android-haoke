// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.*;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
/** Main-thread owner of eight short local cues. No permission, microphone or rule mutations. */
public final class GameAudio {
 private final Context context;private final SharedPreferences prefs;private final AudioManager manager;private final AudioEvents events=new AudioEvents();
 private final Handler ui=new Handler(Looper.getMainLooper());private final AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
 private SoundPool pool;private AudioFocusRequest request;private boolean foreground,focused,enabled;private float duck=1;
 // Retain more handles than live voices: SoundPool may evict a newer low-priority cue.
 // 32 accepted handles cover >2s at our 65ms gate; every asset is shorter than0.8s.
 private final int[] ids=new int[AudioEvents.COUNT],streams=new int[32];private final boolean[] ready=new boolean[AudioEvents.COUNT];private int slot,submittedPoints;
 final int[] played=new int[AudioEvents.COUNT];int loaded,loadErrors,playErrors; // bounded device diagnostics, no private data
 public GameAudio(Context c){context=c.getApplicationContext();prefs=context.getSharedPreferences("liquid_audio",Context.MODE_PRIVATE);enabled=prefs.getBoolean("enabled",true);manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);load();}
 private void load(){if(pool!=null)return;loaded=0;java.util.Arrays.fill(ready,false);pool=new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build();SoundPool current=pool;
  current.setOnLoadCompleteListener((p,id,status)->ui.post(()->{if(pool!=current)return;for(int i=0;i<ids.length;i++)if(ids[i]==id){ready[i]=status==0;if(status==0)loaded++;else loadErrors++;}}));
  for(int i=0;i<ids.length;i++)try(android.content.res.AssetFileDescriptor file=context.getAssets().openFd("audio/"+AudioEvents.FILES[i]+".wav")){ids[i]=current.load(file,1);}catch(java.io.IOException e){loadErrors++;}
 }
 public boolean enabled(){return enabled;}
 public void toggle(){enabled=!enabled;prefs.edit().putBoolean("enabled",enabled).apply();events.reset();if(enabled&&foreground){acquire();play(AudioEvents.ORDER);}else silence(true);}
 public void foreground(boolean active){if(foreground==active)return;foreground=active;events.reset();if(active&&enabled){load();acquire();}else silence(true);}
 private void acquire(){if(focused||!foreground||!enabled)return;
  if(request==null)request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attributes).setOnAudioFocusChangeListener(change->{
   if(!foreground||!enabled||pool==null)return;
   if(change==AudioManager.AUDIOFOCUS_GAIN){focused=true;duck=1;for(int stream:streams)if(stream!=0)pool.setVolume(stream,.40f,.40f);events.reset();}
   else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){duck=.25f;if(pool!=null)for(int stream:streams)if(stream!=0)pool.setVolume(stream,.10f,.10f);}
   else {focused=false;stop();events.reset();}
  },ui).build();
  focused=manager.requestAudioFocus(request)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;duck=1;
 }
 private void stop(){if(pool!=null)for(int stream:streams)if(stream!=0)pool.stop(stream);java.util.Arrays.fill(streams,0);}
 private void silence(boolean abandon){stop();if(abandon&&request!=null)manager.abandonAudioFocusRequest(request);focused=false;}
 public void release(){foreground=false;silence(true);events.reset();if(pool!=null){pool.release();pool=null;}}
 public void frame(int[] items,OperationsSnapshot operations,int viewer,boolean spectator){if(!foreground||!enabled||!focused)return;int event=events.observe(items,operations,viewer,spectator);if(event>=0)play(event);}
 public void command(short[] x,short[] y){if(x.length==5&&y.length==5&&x[0]==-5){int command=y[0]/4;if(command==OrderProtocol.BEGIN)submittedPoints=0;if(command==OrderProtocol.BEGIN||command==OrderProtocol.APPEND)for(int i=2;i<5;i++)if(x[i]>=0&&y[i]>=0)submittedPoints++;if(command>=OrderProtocol.COMMIT&&command<=OrderProtocol.MERGE){play(command==OrderProtocol.COMMIT&&submittedPoints>1?AudioEvents.ROUTE:AudioEvents.ORDER);submittedPoints=0;}}}
 boolean play(int cue){if(!foreground||!enabled||!focused||pool==null||cue<0||cue>=ready.length||!ready[cue]||!events.allow(cue,SystemClock.uptimeMillis()))return false;
  // Peak of each asset <=0.4; even four simultaneous voices at gain0.4 retain headroom.
  int stream=pool.play(ids[cue],.40f*duck,.40f*duck,AudioEvents.priority(cue),0,1f);if(stream==0){playErrors++;return false;}
  streams[slot++%streams.length]=stream;played[cue]++;return true;
 }
}
