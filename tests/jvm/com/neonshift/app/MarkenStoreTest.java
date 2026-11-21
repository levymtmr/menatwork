package com.neonshift.app;
import android.content.Context;import java.time.*;import org.json.*;
public class MarkenStoreTest{
 static int checks;static void eq(double got,double expected,String label){checks++;if(Math.abs(got-expected)>0.00001)throw new AssertionError(label+": "+got+" != "+expected);}
 static JSONObject shift(Store s,String id,String date,String from,String to)throws Exception{return new JSONObject().put("requestId",id).put("workDate",date).put("entryMode","times").put("startTime",from).put("endTime",to).put("breakMinutes","0").put("lunchMinutes","45").put("client","Marken").put("config",new JSONObject(s.config().toString()).put("clientId","marken-id").put("payRule","marken").put("overtime",true)).put("km",0).put("extras",0);}
 public static void main(String[] a)throws Exception{
  Context context=new Context();Store s=new Store(context);String monday="2026-11-09",sat="2026-11-14",sun="2026-11-15";
  JSONObject one=s.saveJournal(shift(s,"one",monday,"06:00","14:00"));eq(one.getLong("workedMs"),27000000,"8h includes exactly one meal");eq(one.getJSONObject("totals").getDouble("gross"),240,"weekday ordinary 7.5h at 32");
  JSONObject two=s.saveJournal(shift(s,"two",monday,"14:00","16:00"));eq(two.getJSONObject("totals").getDouble("gross"),96,"same-day second shift uses daily OT1.5");eq(two.getJSONObject("totals").getLong("regularMs"),0,"ordinary quota not reset");
  s.removeJournal(Store.id(one),true);eq(s.findJournal(Store.id(two)).getJSONObject("totals").getDouble("gross"),64,"deleting first shift recomputes later tiers");s.removeJournal(Store.id(one),false);eq(s.findJournal(Store.id(two)).getJSONObject("totals").getDouble("gross"),96,"restoring first shift restores tiers");
  JSONObject saturday=s.saveJournal(shift(s,"sat",sat,"06:00","14:00"));eq(saturday.getJSONObject("totals").getLong("overtime15Ms"),7200000,"Saturday first 2h remain OT1.5");eq(saturday.getJSONObject("totals").getLong("overtime2Ms"),19800000,"Saturday meal removed only from OT2");eq(saturday.getJSONObject("totals").getDouble("gross"),448,"Saturday amount");
  JSONObject sunday=s.saveJournal(shift(s,"sun",sun,"07:00","15:00"));eq(sunday.getJSONObject("totals").getDouble("gross"),480,"Sunday 7.5h at double");
  JSONObject live=new JSONObject().put("id","live").put("phase","pause").put("workDate",sun).put("client","Other Marken").put("config",sunday.getJSONObject("config")).put("workedMs",28800000);s.save(live);eq(Store.totals(s.session(),s.config()).getLong("workedMs"),27000000,"live paid time includes meal");s.finishWithLunch("live","pause","45",28800000,System.currentTimeMillis());eq(s.findJournal("live").getLong("workedMs"),27000000,"finalization cannot deduct meal twice or use 45");
  ClientRegistry clients=new ClientRegistry(context,s,new InvoiceStore());JSONObject client=clients.save(new JSONObject().put("client","MARKEN Projects").put("hourly",32));eq("marken".equals(client.getString("payRule"))?1:0,1,"Marken client defaults to contract rule");eq(ClientRegistry.namedMarken("Market")?1:0,0,"unrelated client not migrated");
  System.out.println("PASS: "+checks+" real Store/ClientRegistry integration assertions with in-memory Android preferences.");
 }
}
