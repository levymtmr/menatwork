import com.neonshift.app.MarkenEngine;
import java.time.*;import java.util.*;
public class MarkenTest {
 static int checks;static void check(DayOfWeek day,double hours,double ordinary,double ot15,double ot2){LocalDate date=LocalDate.of(2026,11,16).plusDays(day.getValue()-1);long raw=Math.round(hours*3600000);long[] got=MarkenEngine.bands(date,raw,0);double[] expected={ordinary,ot15,ot2};for(int n=0;n<3;n++){checks++;if(Math.abs(got[n]/3600000.0-expected[n])>0.0000001)throw new AssertionError(day+" "+hours+" "+Arrays.toString(got));}checks++;if(Arrays.stream(got).sum()!=MarkenEngine.paid(raw))throw new AssertionError("paid total");}
 public static void main(String[] args){
  check(DayOfWeek.MONDAY,4,4,0,0);check(DayOfWeek.MONDAY,4.25,3.75,0,0);check(DayOfWeek.MONDAY,8,7.5,0,0);check(DayOfWeek.TUESDAY,9,7.5,1,0);check(DayOfWeek.WEDNESDAY,10,7.5,2,0);check(DayOfWeek.FRIDAY,11,7.5,2,1);
  check(DayOfWeek.SATURDAY,2,0,2,0);check(DayOfWeek.SATURDAY,4,0,2,2);check(DayOfWeek.SATURDAY,4.25,0,2,1.75);check(DayOfWeek.SATURDAY,8,0,2,5.5);check(DayOfWeek.SUNDAY,4,0,0,4);check(DayOfWeek.SUNDAY,8,0,0,7.5);
  long[] continued=MarkenEngine.bands(LocalDate.of(2026,11,16),2*MarkenEngine.HOUR,8*MarkenEngine.HOUR);if(continued[0]!=0||continued[1]!=2*MarkenEngine.HOUR)throw new AssertionError("daily tiers must not restart");checks++;
  System.out.println("PASS: "+checks+" Marken rules: weekday durations, strict >4h meal, Saturday OT2 meal, Sunday and daily offsets.");
 }
}
