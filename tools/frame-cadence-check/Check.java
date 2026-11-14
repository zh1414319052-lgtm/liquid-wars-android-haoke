package com.xenris.liquidwarsos;
/** Deterministic timing cases: rate limits, refresh changes, stale callbacks and resume. */
public final class Check {
 static int assertions;static void check(boolean value){assertions++;if(!value)throw new AssertionError("case "+assertions);}
 public static void main(String[] args){
  FrameCadence cadence=new FrameCadence();
  for(int hz:new int[]{60,90,120,144,165,240})for(int target:new int[]{60,hz}){
   cadence.reset(hz,target);long period=Math.round(1e9/hz),base=1000000000L;int drawn=0;
   for(int i=0;i<hz*2;i++)if(cadence.due(base+i*period,base+i*period+1000000))drawn++;
   int hold=Math.max(1,(int)Math.ceil(hz/(double)target-.01));int expected=(hz*2+hold-1)/hold;check(drawn==expected);
  }
  cadence.reset(120,120);check(cadence.due(1000000000L,1000000000L));check(!cadence.due(1001000000L,1001000000L));check(!cadence.due(1020000000L,1100000000L));check(cadence.due(1110000000L,1110000000L));check(!cadence.due(1110000000L,1110000000L));
  cadence.reset(60,120);check(cadence.due(2000000000L,2000000000L));check(!cadence.due(2008300000L,2008300000L));check(cadence.due(2016666667L,2016666667L));
  cadence.reset(120,60);check(cadence.due(1000000000L,1000000000L));check(!cadence.due(1008333333L,1008333333L));check(cadence.due(1016666666L,1016666666L));
  cadence.reset(120.00001f,60);check(cadence.due(1000000000L,1000000000L));check(cadence.due(1016666666L,1016666666L));
  System.out.println("PASS "+assertions+" cadence assertions; integer refresh holds, stale skip, reset, 60–240 Hz");
 }
}
