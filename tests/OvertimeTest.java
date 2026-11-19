import com.neonshift.app.OvertimeEngine;
import java.time.*;import java.util.*;
public class OvertimeTest {
 static ZoneId z=ZoneId.of("Australia/Adelaide");static long t(String s){return LocalDateTime.parse(s).atZone(z).toInstant().toEpochMilli();}
 static void test(String a,String b,String lunch,int minutes,double regular,double one,double two){long l=lunch.isEmpty()?0:t(lunch);long[] bands=OvertimeEngine.split(Arrays.asList(new long[]{t(a),t(b)}),z,l,l+minutes*60000L);double[] expected={regular,one,two};for(int n=0;n<3;n++)if(Math.abs(bands[n]/3600000.0-expected[n])>0.000001)throw new AssertionError(Arrays.toString(bands));}
 public static void main(String[] args){
 test("2026-11-18T08:00","2026-11-18T18:00","2026-11-18T12:00",45,6.25,2,1);
 test("2026-11-18T15:00","2026-11-18T17:00","",0,0,2,0);
 test("2026-11-18T17:00","2026-11-18T18:30","",0,0,0,1.5);
 test("2026-11-18T14:00","2026-11-18T18:00","2026-11-18T14:45",45,.75,1.5,1);
 test("2026-11-18T23:00","2026-11-19T02:00","",0,2,0,1);
 long[] split=OvertimeEngine.split(Arrays.asList(new long[]{t("2026-11-18T14:00"),t("2026-11-18T15:30")},new long[]{t("2026-11-18T16:00"),t("2026-11-18T18:00")}),z,0,0);
 if(split[1]!=5400000||split[2]!=3600000)throw new AssertionError("pause paid");
 if(OvertimeEngine.gross(new long[]{22500000,7200000,3600000},32)!=360)throw new AssertionError("gross");
 System.out.println("PASS: overtime boundaries, lunch crossing bands, paused intervals, midnight and weighted gross.");
 }
}
