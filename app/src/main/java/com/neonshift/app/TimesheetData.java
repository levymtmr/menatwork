package com.neonshift.app;
import org.json.*;import java.time.*;import java.time.format.*;
final class TimesheetData {
 static JSONObject fromSource(JSONObject s)throws Exception{
  if(!Store.isMarken(s))throw new IllegalArgumentException("Vincule jornadas Marken para gerar o timesheet.");
  ZoneId zone=ZoneId.of(s.optString("workZone",ZoneId.systemDefault().getId()));long from=s.optLong("workStartedAt",s.optLong("startedAt")),to=s.optLong("endedAt");JSONArray spans=s.optJSONArray("workIntervals");
  if(!s.optBoolean("manual")&&spans!=null&&spans.length()>0){from=spans.getJSONArray(0).getLong(0);to=spans.getJSONArray(spans.length()-1).getLong(1);}
  if(from<=0||to<from||"hours".equals(s.optString("entryMode")))throw new IllegalArgumentException("Informe início e término da jornada para gerar o timesheet.");
  JSONObject t=s.getJSONObject("totals"),cfg=s.getJSONObject("config");if(!"AUD".equals(cfg.optString("currency")))throw new IllegalArgumentException("O timesheet Marken usa AUD. Confira a moeda da jornada.");long paid=s.getLong("workedMs"),unpaid=to-from-paid;if(unpaid<0)throw new IllegalArgumentException("Confira as pausas e os horários da jornada.");
  JSONObject out=new JSONObject();Store.put(out,"date",Store.date(s));Store.put(out,"start",clock(from,zone));Store.put(out,"end",clock(to,zone)+(Instant.ofEpochMilli(from).atZone(zone).toLocalDate().equals(Instant.ofEpochMilli(to).atZone(zone).toLocalDate())?"":" (+1 day)"));
  Store.put(out,"site",s.optString("worksite"));Store.put(out,"position",s.optString("workType"));Store.put(out,"company",s.optString("client"));Store.put(out,"breakMs",unpaid);Store.put(out,"regularMs",t.getLong("regularMs"));Store.put(out,"ot15Ms",t.getLong("overtime15Ms"));Store.put(out,"ot2Ms",t.getLong("overtime2Ms"));Store.put(out,"rate",String.valueOf(cfg.get("hourly")));return out;
 }
 private static String clock(long time,ZoneId zone){java.time.LocalTime t=Instant.ofEpochMilli(time).atZone(zone).toLocalTime();return t.format(DateTimeFormatter.ofPattern(t.getSecond()==0?"HH:mm":"HH:mm:ss"));}
 static MarkenBilling.Entry read(JSONObject j)throws Exception{return new MarkenBilling.Entry(j.getString("date"),j.getString("start"),j.getString("end"),j.optString("site"),j.optString("position"),j.optString("company"),j.getLong("breakMs"),j.getLong("regularMs"),j.getLong("ot15Ms"),j.getLong("ot2Ms"),j.getString("rate"));}
}
