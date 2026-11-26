package com.neonshift.app;
import org.json.*;import java.time.*;import java.time.format.*;
final class TimesheetData {
 static boolean supports(JSONObject s){return Store.isMarken(s)||ClientRegistry.namedMarken(s.optString("client"));}
 static JSONObject fromSource(JSONObject s)throws Exception{
  if(!supports(s))throw new IllegalArgumentException("Vincule jornadas Marken para gerar o timesheet.");
  ZoneId zone=ZoneId.of(s.optString("workZone",ZoneId.systemDefault().getId()));long from=s.optLong("workStartedAt",s.optLong("startedAt")),to=s.optLong("endedAt");JSONArray spans=s.optJSONArray("workIntervals");
  boolean recorded=!s.optBoolean("manual")&&spans!=null&&spans.length()>0;
  if(recorded){from=spans.getJSONArray(0).getLong(0);to=spans.getJSONArray(spans.length()-1).getLong(1);}
  if((from<=0||to<=0)&&"times".equals(s.optString("entryMode"))&&!s.optString("startTime").isEmpty()&&!s.optString("endTime").isEmpty()){
   LocalDate date=LocalDate.parse(Store.date(s));from=date.atTime(LocalTime.parse(s.getString("startTime"))).atZone(zone).toInstant().toEpochMilli();to=date.plusDays(s.optBoolean("nextDay")?1:0).atTime(LocalTime.parse(s.getString("endTime"))).atZone(zone).toInstant().toEpochMilli();
  }
  if(from<=0||to<from||("hours".equals(s.optString("entryMode"))&&!recorded))throw new IllegalArgumentException("Informe início e término da jornada para gerar o timesheet.");
  JSONObject t=s.getJSONObject("totals"),cfg=s.getJSONObject("config");if(!"AUD".equals(cfg.optString("currency")))throw new IllegalArgumentException("O timesheet Marken usa AUD. Confira a moeda da jornada.");long paid=s.getLong("workedMs"),unpaid=to-from-paid;if(unpaid<0)throw new IllegalArgumentException("Confira as pausas e os horários da jornada.");
  long ot15=t.optLong("overtime15Ms"),ot2=t.optLong("overtime2Ms"),regular=t.optLong("regularMs",paid-ot15-ot2);if(regular<0||regular+ot15+ot2!=paid)throw new IllegalArgumentException("Confira as horas pagas da jornada no Histórico.");
  JSONObject out=new JSONObject();Store.put(out,"date",Store.date(s));Store.put(out,"start",clock(from,zone));Store.put(out,"end",clock(to,zone)+(Instant.ofEpochMilli(from).atZone(zone).toLocalDate().equals(Instant.ofEpochMilli(to).atZone(zone).toLocalDate())?"":" (+1 day)"));
  Store.put(out,"site",s.optString("worksite"));Store.put(out,"position",s.optString("workType"));Store.put(out,"company",s.optString("client"));Store.put(out,"breakMs",unpaid);Store.put(out,"regularMs",regular);Store.put(out,"ot15Ms",ot15);Store.put(out,"ot2Ms",ot2);Store.put(out,"rate",String.valueOf(cfg.get("hourly")));return out;
 }
 private static String clock(long time,ZoneId zone){java.time.LocalTime t=Instant.ofEpochMilli(time).atZone(zone).toLocalTime();return t.format(DateTimeFormatter.ofPattern(t.getSecond()==0?"HH:mm":"HH:mm:ss"));}
 static MarkenBilling.Entry read(JSONObject j)throws Exception{return new MarkenBilling.Entry(j.getString("date"),j.getString("start"),j.getString("end"),j.optString("site"),j.optString("position"),j.optString("company"),j.getLong("breakMs"),j.getLong("regularMs"),j.getLong("ot15Ms"),j.getLong("ot2Ms"),j.getString("rate"));}
}
