package com.neonshift.app;
import android.app.Instrumentation;
import android.os.Bundle;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.time.*;
import java.util.Arrays;
public class ManagementSmoke extends Instrumentation {
 int checks;void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
 interface Run{void run()throws Exception;}void bad(Run r,String label)throws Exception{try{r.run();throw new AssertionError(label);}catch(IllegalArgumentException e){checks++;}}
 public void onCreate(Bundle a){super.onCreate(a);start();}
 public void onStart(){Bundle result=new Bundle();if(!"ranchu".equals(android.os.Build.HARDWARE)&&!"goldfish".equals(android.os.Build.HARDWARE)){result.putString("stream","Refused: emulator only\n");finish(0,result);return;}
 Context c=getTargetContext();int status=-1;InvoiceStore invoices=null;
 try{
  c.getSharedPreferences("neonshift",0).edit().clear().commit();c.getSharedPreferences("invoice_deck_mobile",0).edit().clear().commit();
  Store store=new Store(c);invoices=new InvoiceStore(c);final InvoiceStore inv=invoices;
  for(File f:invoices.folder().listFiles())f.delete();
  check(store.language().equals("en"),"English default");store.saveLanguage("pt");check(new Store(c).language().equals("pt"),"Portuguese persists");check(Language.text(c,"REMOVER").equals("REMOVER"),"Portuguese native text");store.saveLanguage("en");check(Language.text(c,"REMOVER").equals("REMOVE"),"English native text");bad(()->store.saveLanguage("de"),"invalid language blocked");
  check(store.theme().equals("pipboy"),"default theme");
  for(String theme:new String[]{"medieval","cyberpunk","pipboy"}){store.saveTheme(theme);check(new Store(c).theme().equals(theme),"theme persistence "+theme);}
  bad(()->store.saveTheme("unknown"),"invalid theme blocked");
  String date=LocalDate.now().minusDays(3).toString();
  store.saveSkill(new JSONObject().put("name","Cladding").put("description","Installation"));String skill=store.skills().getJSONObject(0).getString("id");
  JSONObject p=new JSONObject().put("requestId","fixture").put("workDate",date).put("entryMode","hours").put("hours","8").put("config",store.config()).put("km",10).put("extras",5).put("worksite","Example site").put("skillIds",new JSONArray().put(skill).put(skill));
  JSONObject saved=store.saveJournal(p);String id=Store.id(saved);check(store.history().length()==1,"add retrospective day");check(store.progress().getLong("xp")==800,"confirmed XP");check(store.progress().getJSONArray("skills").getJSONObject(0).getLong("xp")==800,"duplicate tags do not duplicate XP");
  store.saveJournal(p);check(store.history().length()==1,"idempotent repeated save");
  p.put("id",id).put("hours","6.5");saved=store.saveJournal(p);check(store.history().length()==1,"edit replaces");check(saved.getJSONObject("totals").getDouble("gross")==208,"edited costs");check(store.progress().getLong("xp")==650,"edited XP recalculated");
  store.removeJournal(id,true);check(store.progress().getLong("xp")==0,"removed day excluded");check(store.findJournal(id).getBoolean("deleted"),"trash retained");store.removeJournal(id,false);check(store.progress().getLong("xp")==650,"restore recalculates");
  store.removeSkill(skill,true);check(store.progress().getJSONArray("skills").getJSONObject(0).getLong("xp")==0,"removed skill excluded");check(store.progress().getLong("xp")==650,"global hours unchanged by skill removal");store.removeSkill(skill,false);
  JSONObject draft=invoices.draft(),profile=draft.getJSONObject("profile");for(String k:new String[]{"name","client","bank","account_name","bsb","account"})profile.put(k,"Example");
  JSONObject row=new JSONObject().put("date",date).put("address","Example site").put("hours","6.5").put("sourceId",id).put("sourceFingerprint",Store.fingerprint(saved));draft.put("rows",new JSONArray().put(row));
  store.saveLanguage("pt");byte[] pdfPt=java.nio.file.Files.readAllBytes(invoices.preview(draft).toPath());store.saveLanguage("en");byte[] pdfEn=java.nio.file.Files.readAllBytes(invoices.preview(draft).toPath());check(Arrays.equals(pdfPt,pdfEn),"PDF unaffected by app language");check(new String(pdfEn,java.nio.charset.StandardCharsets.ISO_8859_1).contains("INVOICE FOR SERVICES"),"PDF always English");
  File file=invoices.issue(draft);byte[] bytes=java.nio.file.Files.readAllBytes(file.toPath());String filename=file.getName();
  check(!invoices.needsReview(invoices.find(filename)),"new invoice current");
  invoices.setPaid(filename,true);check(new InvoiceStore(c).find(filename).getBoolean("paid"),"payment persists");check(invoices.find(filename).has("paidAt"),"payment date recorded");check(invoices.find(filename).getJSONArray("sourceIds").getString(0).equals(id),"paid links preserved");check(store.progress().getLong("xp")==650,"payment does not alter XP");
  invoices.setPaid(filename,false);check(!invoices.find(filename).getBoolean("paid"),"payment reversible");check(!invoices.find(filename).has("paidAt"),"unpaid clears date");int next=invoices.nextNumber();invoices.cancel(filename);check(!InvoiceStore.active(invoices.find(filename)),"cancelled excluded");check(invoices.nextNumber()==next,"cancelled number reserved");bad(()->inv.setPaid(filename,true),"cancelled payment blocked");
  invoices.restore(filename);check(InvoiceStore.active(invoices.find(filename)),"restore invoice");
  p.put("hours","7");saved=store.saveJournal(p);check(invoices.needsReview(invoices.find(filename)),"editing linked hours marks review");bad(()->inv.restore(filename),"stale invoice restore blocked");bad(()->inv.setPaid(filename,true),"stale invoice payment blocked");bad(()->inv.validateDraftSources(draft),"stale draft blocked");
  invoices.correct(filename);check(!InvoiceStore.active(invoices.find(filename)),"correction cancels original");check(invoices.draft().getInt("number")==next,"correction gets new number");check(invoices.draft().getJSONArray("rows").getJSONObject(0).getString("hours").equals("7"),"correction refreshes hours");check(Arrays.equals(bytes,java.nio.file.Files.readAllBytes(file.toPath())),"original PDF immutable");
  File corrected=invoices.issue(invoices.draft());check(corrected.exists(),"corrected PDF issued");
  store.removeJournal(id,true);check(invoices.needsReview(invoices.find(corrected.getName())),"deleted source flags invoice");check(invoices.refreshSources(invoices.draft().put("rows",new JSONArray().put(row))).getJSONArray("rows").length()==0,"deleted source removed on refresh");store.removeJournal(id,false);
  JSONObject active=new JSONObject().put("id",123).put("startedAt",System.currentTimeMillis()).put("phase","pause").put("workedMs",3600000).put("config",store.config()).put("skillIds",new JSONArray().put(skill));store.save(active);
  check(store.progress().getLong("xp")==700&&store.progress().getLong("pendingXp")==100,"active XP separate");
  JSONObject fix=new JSONObject(p.toString()).put("id","123").put("finishActive",true).put("entryMode","times").put("startTime","08:00").put("endTime","16:30").put("breakMinutes","30");
  JSONObject ended=store.saveJournal(fix);check(store.session().length()==0,"corrected active clears atomically");check(ended.getLong("workedMs")==28800000,"forgotten stop corrected");check(ended.getLong("endedAt")<System.currentTimeMillis()-86400000,"past end preserved");check(store.progress().getLong("xp")==1500,"corrected active XP");
  store.save(active);store.discardSession();check(store.session().length()==0,"discard clears session");check(store.progress().getLong("xp")==1500,"discard excluded XP");
  // Simulate pre-update records without source snapshots or explicit ISO dates.
  inv.prefs.edit().remove("sourceSnapshotsV1").commit();InvoiceStore migrated=new InvoiceStore(c);check(migrated.find(filename).has("sourceFingerprints"),"legacy links migrated");check(migrated.nextNumber()>next,"all numbers reserved after migration");
  result.putString("stream","PASS: "+checks+" native journal, XP, invoice cancellation/correction and migration checks.\n");
 }catch(Throwable e){status=0;result.putString("stream","FAIL: "+e+"\n");}
 finally{c.getSharedPreferences("neonshift",0).edit().clear().commit();c.getSharedPreferences("invoice_deck_mobile",0).edit().clear().commit();try{if(invoices!=null)for(File f:invoices.folder().listFiles())f.delete();}catch(Exception ignored){}}
 finish(status,result);
 }
}
