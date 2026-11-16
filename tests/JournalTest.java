import com.neonshift.app.*;
import java.time.*;
public class JournalTest {
 static int checks;static final ZoneId Z=ZoneId.of("Australia/Adelaide");static final long NOW=ZonedDateTime.of(2026,11,16,18,0,0,0,Z).toInstant().toEpochMilli();
 static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 static JournalEngine.Entry plan(String d,String mode,String hours,String a,String b,boolean next,String pause){return JournalEngine.plan(d,mode,hours,a,b,next,pause,Z,NOW);}
 static void bad(Runnable r){try{r.run();throw new AssertionError("accepted invalid entry");}catch(IllegalArgumentException e){checks++;}}
 public static void main(String[]args){
  check(plan("2026-11-13","hours","8,5","","",false,"0").workedMs==30600000);
  check(plan("2026-11-13","hours","08:30","","",false,"0").workedMs==30600000);
  check(plan("2026-11-13","times","","08:00","17:00",false,"30").workedMs==30600000);
  check(plan("2026-11-13","times","","22:00","06:00",true,"0").workedMs==28800000);
  check(plan("2026-11-13","hours","0","","",false,"0").workedMs==0);
  bad(()->plan("2026-11-17","hours","8","","",false,"0"));
  bad(()->plan("2026-11-16","times","","08:00","19:00",false,"0"));
  bad(()->plan("2026-11-13","times","","17:00","08:00",false,"0"));
  bad(()->plan("2026-11-13","times","","08:00","09:00",false,"61"));
  bad(()->plan("2026-11-13","hours","24.1","","",false,"0"));
  bad(()->plan("2026-11-13","hours","-1","","",false,"0"));
  bad(()->plan("2026-11-13","times","","08:00","09:00",true,"0"));
  bad(()->plan("2026-10-04","times","","02:30","04:00",false,"0"));
  check(Progression.xp(3600000)==100);check(Progression.xp(35999)==0);check(Progression.xp(36000)==1);check(Progression.xp(-1)==0);
  check(Progression.level(999)==1);check(Progression.level(1000)==2);check(Progression.withinLevel(1500)==500);
  System.out.println("PASS: "+checks+" journal, overnight, DST, validation and progression checks.");
 }
}
