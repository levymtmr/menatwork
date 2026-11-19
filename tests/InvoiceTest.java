import com.neonshift.app.InvoiceEngine;
import com.neonshift.app.InvoicePdf;
import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public class InvoiceTest {
    static int assertions;
    static void eq(Object a, Object b) { assertions++; if (!a.equals(b)) throw new AssertionError(a + " != " + b); }
    static void bad(Runnable r) { assertions++; try { r.run(); } catch (IllegalArgumentException e) { return; } throw new AssertionError("Expected invalid input"); }
    static InvoiceEngine.Item item(String h, String rate) { return new InvoiceEngine.Item("2026-11-16", "Work site", h, rate, ""); }
    public static void main(String[] args) throws Exception {
        eq(new InvoiceEngine.Item("2026-11-16","OT site","9.25","32","source","64").amount.toPlainString(),"360.00");
        bad(()->new InvoiceEngine.Item("2026-11-16","OT site","9.25","32","source","-1"));
        eq(item("08:01", "32").billed.toPlainString(), "8.25");
        eq(item("08:01", "32").amount.toPlainString(), "264.00");
        eq(item("08:15", "32").amount.toPlainString(), "264.00");
        eq(item("08:16", "32").amount.toPlainString(), "272.00");
        eq(item("8,5", "32").amount.toPlainString(), "272.00");
        eq(item("8.2500000001", "32").amount.toPlainString(), "272.00");
        eq(item("0.1", "0.02").amount.toPlainString(), "0.01");
        eq(item("0.25", "0.06").amount.toPlainString(), "0.02");
        eq(item("24:00", "32").amount.toPlainString(), "768.00");
        eq(new InvoiceEngine.Item("2026-11-17", "Day off", "INVALID", "32", "").amount.toPlainString(), "0.00");
        eq(InvoiceEngine.hours("").signum(), 0);
        bad(()->item("24:01", "32"));bad(()->item("-1", "32"));bad(()->item("08:60", "32"));
        bad(()->item("NaN", "32"));bad(()->item("Infinity", "32"));bad(()->item("8", "0"));
        bad(()->new InvoiceEngine.Item("2026-11-16", "", "8", "32", ""));
        bad(()->new InvoiceEngine.Item("2026-02-30", "Work", "8", "32", ""));
        eq(InvoiceEngine.number(18),"018");bad(()->InvoiceEngine.number(0));
        List<InvoiceEngine.Item> sum=Arrays.asList(item("0.01","32"),item("0.01","32"));
        eq(InvoiceEngine.total(sum).toPlainString(),"16.00"); // rounding per entry, not after summing
        bad(()->InvoiceEngine.total(Arrays.asList(item("0","32"))));
        Properties props=new Properties();
        try(java.io.Reader in=Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8)){props.load(in);}
        Map<String,String> profile=new HashMap<>();for(String key:props.stringPropertyNames())profile.put(key,props.getProperty(key));
        List<InvoiceEngine.Item> items=new ArrayList<>();
        String[] addresses={"16 East Pkwy, Fulham SA 5024","Lot 210 Birdsong Kidman Park","Day off","3 Gault ave, Fulham","13 Sturdee St, Linden Park","17 Fairway Boulevard, Woodcroft"};
        String[] hours={"08:01","7","0","8.5","7.75","7"};
        for(int i=0;i<hours.length;i++)items.add(new InvoiceEngine.Item(LocalDate.of(2026,11,9).plusDays(i).toString(),addresses[i],hours[i],"32",""));
        eq(InvoiceEngine.total(items).toPlainString(),"1232.00");
        Files.createDirectories(Paths.get("build/qa"));
        byte[] pdf=InvoicePdf.generate(18,LocalDate.of(2026,11,16),profile,items);
        Files.write(Paths.get("build/qa/invoice-preview.pdf"),pdf);
        String contents=new String(pdf,StandardCharsets.ISO_8859_1);
        eq(contents.contains("/Count 1"),true);eq(contents.contains("1,232.00"),true);
        eq(contents.contains("L\u00cdQUIDO"),false);eq(contents.contains("15%"),false);
        Map<String,String> excessive=new HashMap<>(profile);excessive.put("description",String.join("\n",Collections.nCopies(90,"Line")));
        boolean headerRejected=false;try{InvoicePdf.generate(18,LocalDate.of(2026,11,16),excessive,items);}catch(IllegalArgumentException e){headerRejected=true;}
        eq(headerRejected,true); // An oversized header must never produce a clipped PDF.
        Files.write(Paths.get("build/qa/invoice-overtime.pdf"),InvoicePdf.generate(20,LocalDate.of(2026,11,16),profile,Arrays.asList(new InvoiceEngine.Item("2026-11-16","Example site","9.25","32","source","64"))));
        List<InvoiceEngine.Item> many=new ArrayList<>();
        for(int i=0;i<100;i++)many.add(new InvoiceEngine.Item(LocalDate.of(2026,1,1).plusDays(i).toString(), i==0?"Long location (São Paulo) - "+String.join(" ",Collections.nCopies(80,"Building")):addresses[i%addresses.length],"8:01","32",""));
        Files.write(Paths.get("build/qa/invoice-multipage.pdf"),InvoicePdf.generate(19,LocalDate.of(2026,11,16),profile,many));
        System.out.println("PASS: "+assertions+" invoice assertions; one-page and multipage PDFs generated.");
    }
}
