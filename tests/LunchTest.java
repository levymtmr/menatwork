import com.neonshift.app.*;
public class LunchTest {
 static int n;static void check(boolean yes){n++;if(!yes)throw new AssertionError("Lunch check "+n);}
 static void bad(String minutes,long ms){try{LunchEngine.paid(ms,minutes);throw new AssertionError("Invalid lunch accepted");}catch(IllegalArgumentException e){n++;}}
 public static void main(String[]args){
  long full=8*3600000L,paid=LunchEngine.paid(full,"45");check(paid==26100000L);check(LunchEngine.paid(full,"0")==full);check(LunchEngine.paid(full,"480")==0);check(LunchEngine.paid(full,"45,5")==26070000);check(LunchEngine.paid(full,"45.5")==26070000);
  check(Progression.xp(paid)==725);check(CostEngine.calculate(paid,32,15,0,8,1.85,.025,.05,.1,0)[0]==232);check(InvoiceEngine.hours("7.25").toPlainString().equals("7.25"));
  bad("481",full);bad("-1",full);bad("x",full);bad("1441",full);bad("45",0);check(LunchEngine.paid(0,"0")==0);System.out.println("PASS: "+n+" lunch deduction and paid-time checks.");
 }
}
