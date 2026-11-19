package com.neonshift.app;
import java.time.*;
import java.util.*;
public final class OvertimeEngine {
 private static long overlap(long a,long b,long c,long d){return Math.max(0,Math.min(b,d)-Math.max(a,c));}
 /** Local clock bands reset at midnight. Lunch and paused time never earn overtime. */
 public static long[] split(List<long[]> intervals,ZoneId zone,long lunchStart,long lunchEnd){
  long[] out=new long[3];
  for(long[] span:intervals){long from=span[0],end=span[1];if(end<from||end-from>172800000L)throw new IllegalArgumentException("Confira os horários do overtime.");
   while(from<end){ZonedDateTime local=Instant.ofEpochMilli(from).atZone(zone);LocalDate day=local.toLocalDate();int hour=local.getHour(),band=hour<15?0:hour<17?1:2;
    long boundary=(band==0?day.atTime(15,0).atZone(zone):band==1?day.atTime(17,0).atZone(zone):day.plusDays(1).atStartOfDay(zone)).toInstant().toEpochMilli();long to=Math.min(end,boundary);
    out[band]+=to-from-overlap(from,to,lunchStart,lunchEnd);from=to;
   }
  }return out;
 }
 public static double gross(long[] bands,double rate){return (bands[0]+bands[1]*1.5+bands[2]*2.0)/3600000.0*rate;}
}
