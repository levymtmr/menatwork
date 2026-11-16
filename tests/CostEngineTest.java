import com.neonshift.app.CostEngine;
import com.neonshift.app.TimeLedger;

public class CostEngineTest {
    static int assertions = 0;
    static void near(double actual, double expected) {
        assertions++;
        if (Math.abs(actual - expected) > 0.000001) throw new AssertionError(actual + " != " + expected);
    }
    static void check(boolean value) { assertions++; if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        // Approved example: 6.5 hours at 32, 15% tax, 12 km and 4.20 vehicle costs.
        double[] t = CostEngine.calculate(23400000, 32, 15, 12, 10, 2.3333333333333333, .025, .05, .041666666666666664, 0);
        near(t[0], 208); near(t[1], 31.2); near(t[2], 2.8);
        near(t[3], .3); near(t[4], .6); near(t[5], .5); near(t[8], 172.6);
        // Unpaid commute: negative net costs are visible, never clamped to zero.
        t = CostEngine.calculate(0,32,15,20,8,1.85,.025,.05,.1,5);
        near(t[0],0); near(t[1],0); near(t[2],2.96); near(t[7],11.46); near(t[8],-11.46);
        // Return distance, extras, zero tax and sub-hour precision.
        t = CostEngine.calculate(1800000,40,0,40,10,2,.02,.03,.1,7);
        near(t[0],20); near(t[7],21); near(t[8],-1);
        t = CostEngine.calculate(-1000,32,100,0,8,2,0,0,0,0);
        near(t[0],0); near(t[8],0);
        check(!CostEngine.valid(Double.NaN,100));check(!CostEngine.valid(Double.POSITIVE_INFINITY,100));
        check(!CostEngine.valid(-1,100));check(!CostEngine.valid(101,100));check(CostEngine.valid(100,100));
        // A wall-clock adjustment must not add paid time during the same boot.
        near(TimeLedger.segment(100000,1000,4,4000000,61000,4),60000);
        near(TimeLedger.segment(100000,1000,4,80000,61000,4),60000);
        // Reboot recovery uses the saved start time, and never returns negative.
        near(TimeLedger.segment(100000,1000,4,160000,500,5),60000);
        near(TimeLedger.segment(100000,1000,4,80000,500,5),0);
        System.out.println("PASS: " + assertions + " calculation assertions");
    }
}
