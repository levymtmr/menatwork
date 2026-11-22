package com.neonshift.app;
import java.math.*;import java.time.*;import java.util.*;
/** Sum recorded durations by base rate and band before rounding currency, as in the supplied timesheet. */
public final class MarkenBilling {
 public static final class Entry {
  public final String date,start,end,site,position,company;public final long breakMs,regularMs,ot15Ms,ot2Ms;public final BigDecimal rate;
  public Entry(String date,String start,String end,String site,String position,String company,long breakMs,long regular,long ot15,long ot2,String rate){
   LocalDate.parse(date);if(start.isEmpty()||end.isEmpty()||breakMs<0||regular<0||ot15<0||ot2<0)throw new IllegalArgumentException("Confira os horários do timesheet.");
   this.date=date;this.start=start;this.end=end;this.site=site;this.position=position;this.company=company;this.breakMs=breakMs;regularMs=regular;ot15Ms=ot15;ot2Ms=ot2;this.rate=InvoiceEngine.rate(rate);
  }
  public String signature(){return date+"|"+start+"|"+end+"|"+site+"|"+position+"|"+company+"|"+breakMs+"|"+regularMs+"|"+ot15Ms+"|"+ot2Ms+"|"+rate.stripTrailingZeros().toPlainString();}
 }
 public static final class Group {
  public final BigDecimal rate;public final long[] ms=new long[3];Group(BigDecimal rate){this.rate=rate;}
  public BigDecimal amount(int band){return new BigDecimal(ms[band]).multiply(rate).multiply(band==0?BigDecimal.ONE:band==1?new BigDecimal("1.5"):new BigDecimal("2")).divide(new BigDecimal("3600000"),2,RoundingMode.HALF_UP);}
 }
 public static List<Group> groups(List<Entry> entries){Map<String,Group> groups=new LinkedHashMap<>();for(Entry e:entries){String key=e.rate.stripTrailingZeros().toPlainString();Group g=groups.get(key);if(g==null){g=new Group(e.rate);groups.put(key,g);}g.ms[0]+=e.regularMs;g.ms[1]+=e.ot15Ms;g.ms[2]+=e.ot2Ms;}return new ArrayList<>(groups.values());}
 public static BigDecimal total(List<Entry> entries){BigDecimal out=BigDecimal.ZERO;for(Group g:groups(entries))for(int n=0;n<3;n++)out=out.add(g.amount(n));return out.setScale(2,RoundingMode.HALF_UP);}
 public static String duration(long ms){long seconds=Math.round(ms/1000.0);return String.format(Locale.US,seconds%60==0?"%d:%02d":"%d:%02d:%02d",seconds/3600,(seconds/60)%60,seconds%60);}
 public static String period(List<Entry> entries){String from=null,to=null;for(Entry e:entries){if(from==null||e.date.compareTo(from)<0)from=e.date;if(to==null||e.date.compareTo(to)>0)to=e.date;}return from==null?"":from.equals(to)?from:from+" to "+to;}
}
