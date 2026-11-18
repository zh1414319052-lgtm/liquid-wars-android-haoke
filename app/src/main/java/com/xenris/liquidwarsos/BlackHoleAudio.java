// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.SystemClock;

/** Main-thread streamed cue: 10 s of attraction + 0.5 s decay, aligned to game time.
 * SoundPool has a decoded-size limit, so it must not hold this long stereo asset. */
final class BlackHoleAudio {
 private final Context context;
 private final AudioAttributes attributes;
 private MediaPlayer player;
 private boolean prepared,wanted,seeking,needSeek=true;
 private int epoch=-1,clock=-1,start=-1,deadline,position;
 private long changedAt,lastSeek,retryAt;
 private float gain=.32f;
 int starts,errors,seeks;
 BlackHoleAudio(Context context,AudioAttributes attributes){this.context=context;this.attributes=attributes;}
 void sync(int[] facts){
  long wall=SystemClock.uptimeMillis();
  if(facts==null||facts.length<6||facts[5]!=0){clear();return;}
  int time=facts[1];
  if(epoch!=facts[0]||time<clock){clear();epoch=facts[0];}
  if(time!=clock){clock=time;changedAt=wall;}
  if(facts[3]>time){
   if(start!=facts[2]||deadline!=facts[3]){pause();start=facts[2];deadline=facts[3];}
  }else if(start<0||time<deadline||time>=deadline+500){pause();start=-1;deadline=0;return;}
  position=Math.max(0,Math.min(10499,time-start));
  // Covers a paused simulation even if its Activity still owns audio focus.
  if(wall-changedAt>250){pause();return;}
  wanted=true;
  if(player==null&&wall>=retryAt)prepare();
  if(!prepared||seeking)return;
  try{
   if(needSeek||(!player.isPlaying())||(wall-lastSeek>1000&&Math.abs(player.getCurrentPosition()-position)>350))seek();
  }catch(IllegalStateException e){failed();}
 }
 private void prepare(){
  MediaPlayer current=new MediaPlayer();player=current;
  try(AssetFileDescriptor file=context.getAssets().openFd("audio/black_hole_dark.wav")){
   current.setAudioAttributes(attributes);current.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());
   current.setVolume(gain,gain);
   current.setOnPreparedListener(p->{if(player!=current)return;prepared=true;if(wanted)seek();});
   current.setOnSeekCompleteListener(p->{if(player!=current)return;seeking=false;needSeek=false;if(wanted)try{p.start();starts++;}catch(IllegalStateException e){failed();}});
   current.setOnErrorListener((p,what,extra)->{if(player==current)failed();return true;});
   current.prepareAsync();
  }catch(Exception e){failed();}
 }
 private void seek(){
  if(!prepared||seeking||player==null)return;
  try{seeking=true;lastSeek=SystemClock.uptimeMillis();seeks++;player.seekTo(position,MediaPlayer.SEEK_CLOSEST);}
  catch(IllegalStateException e){failed();}
 }
 void volume(float duck){gain=.32f*duck;if(player!=null)try{player.setVolume(gain,gain);}catch(IllegalStateException e){failed();}}
 void pause(){wanted=false;needSeek=true;if(prepared&&player!=null)try{if(player.isPlaying())player.pause();}catch(IllegalStateException e){failed();}}
 private void clear(){pause();start=-1;deadline=0;clock=-1;}
 private void failed(){errors++;dispose();wanted=false;retryAt=SystemClock.uptimeMillis()+10000;}
 private void dispose(){MediaPlayer old=player;player=null;prepared=seeking=false;needSeek=true;if(old!=null)old.release();}
 void release(){clear();dispose();}
 boolean playing(){return prepared&&player!=null&&player.isPlaying();}
 int playbackPosition(){return prepared&&player!=null?player.getCurrentPosition():-1;}
}
