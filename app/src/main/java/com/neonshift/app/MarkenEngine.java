package com.neonshift.app;
import java.time.LocalDate;
/** Marken / SA Recruitment: elapsed daily bands, automatic unpaid meal per shift. */
public final class MarkenEngine {
 public static final long HOUR=3600000L,MEAL=1800000L;
 public static long meal(long duration){return duration>4*HOUR?MEAL:0;}
 public static long[] bands(LocalDate date,long duration,long previous){
  if(duration<0||previous<0||previous>Long.MAX_VALUE-duration)throw new IllegalArgumentException("Confira a duração da jornada Marken.");
  int day=date.getDayOfWeek().getValue();long[] b=new long[3];
  if(day<=5){b[0]=Math.min(duration,Math.max(0,8*HOUR-previous));b[1]=Math.max(0,Math.min(previous+duration,10*HOUR)-Math.max(previous,8*HOUR));b[2]=duration-b[0]-b[1];}
  else if(day==6){b[1]=Math.min(duration,Math.max(0,2*HOUR-previous));b[2]=duration-b[1];}
  else b[2]=duration;
  long unpaid=meal(duration);int[] order=day<=5?new int[]{0,1,2}:new int[]{2,1,0};
  for(int band:order){long take=Math.min(unpaid,b[band]);b[band]-=take;unpaid-=take;}
  return b;
 }
 public static long paid(long duration){return duration-meal(duration);}
}
