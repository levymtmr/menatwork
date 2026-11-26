package com.neonshift.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

final class InvoiceStore {
    final Context context;
    final SharedPreferences prefs;
    final JSONObject seed;
    InvoiceStore(Context c) {
        context = c;
        prefs = c.getSharedPreferences("invoice_deck_mobile", Context.MODE_PRIVATE);
        try (InputStream in = c.getAssets().open("invoice-defaults.json")) {
            ByteArrayOutputStream b = new ByteArrayOutputStream(); byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) != -1) b.write(buf, 0, n);
            seed = new JSONObject(new String(b.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) { throw new IllegalStateException("Não foi possível carregar o perfil de invoice.", e); }
        migrateLinks();
    }
    static boolean active(JSONObject record) { return !"cancelled".equals(record.optString("status","issued")); }
    private void migrateLinks() {
        if(prefs.getBoolean("sourceSnapshotsV1",false))return;
        try {
            Store journals=new Store(context);JSONArray records=records();JSONObject draft=draft();
            for(int n=0;n<records.length();n++){
                JSONObject r=records.getJSONObject(n),fingerprints=new JSONObject();JSONArray ids=r.optJSONArray("sourceIds");
                if(ids!=null)for(int i=0;i<ids.length();i++){JSONObject source=journals.findJournal(ids.optString(i));if(source!=null)fingerprints.put(ids.optString(i),Store.fingerprint(source));}
                r.put("sourceFingerprints",fingerprints);
            }
            JSONArray rows=draft.optJSONArray("rows");
            if(rows!=null)for(int n=0;n<rows.length();n++){JSONObject row=rows.getJSONObject(n);JSONObject source=journals.findJournal(row.optString("sourceId"));if(source!=null){row.put("sourceFingerprint",Store.fingerprint(source));row.put("workType",source.optString("workType"));}}
            if(!prefs.edit().putString("records",records.toString()).putString("draft",draft.toString()).putBoolean("sourceSnapshotsV1",true).commit())throw new IOException();
        } catch(Exception e){throw new IllegalStateException("Não foi possível preparar os vínculos dos invoices.");}
    }
    JSONArray records() {
        try { return new JSONArray(prefs.getString("records", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }
    int nextNumber() {
        int next = seed.optInt("nextNumber", 1);
        JSONArray a = records();
        for (int i = 0; i < a.length(); i++) next = Math.max(next, a.optJSONObject(i).optInt("number") + 1);
        return next;
    }
    JSONObject draft() {
        JSONObject j = Store.obj(prefs.getString("draft", "{}"));
        if (!j.has("profile")) Store.put(j, "profile", seed.optJSONObject("profile"));
        if (!j.has("number")) Store.put(j, "number", nextNumber());
        if (!j.has("rate")) Store.put(j, "rate", seed.optJSONObject("profile").optString("rate", "32"));
        if (!j.has("rows")) Store.put(j, "rows", new JSONArray());
        try { JSONObject synced=refreshSources(j);if(!synced.toString().equals(j.toString()))saveDraft(synced);return synced; }
        catch(Exception e){return j;}
    }
    void saveDraft(JSONObject draft) {
        if (draft.toString().length() > 500000) throw new IllegalArgumentException("Rascunho muito grande.");
        if (!prefs.edit().putString("draft", draft.toString()).commit()) throw new IllegalStateException("Não foi possível salvar o rascunho.");
    }
    static LocalDate today() { return LocalDate.now(ZoneId.of("Australia/Adelaide")); }
    static Map<String, String> profile(JSONObject draft) throws Exception {
        JSONObject p = draft.getJSONObject("profile");boolean marken=paired(draft);
        Map<String, String> out = new HashMap<>();
        for (String key : new String[]{"name", "address", "email", "abn", "bank", "bsb", "account", "account_name", "client", "client_abn", "client_address", "client_email", "description", "timesheet_position", "timesheet_company", "timesheet_location"}) {
            String value = marken&&key.equals("description")?MarkenBilling.SERVICE:marken&&key.equals("timesheet_position")?MarkenBilling.POSITION:p.optString(key).trim();
            int limit = key.contains("address") ? 350 : key.equals("description") ? 500 :
                key.contains("email") ? 160 : key.contains("abn") || key.equals("bsb") ? 40 : key.equals("account") ? 80 : 120;
            if (value.length() > limit)
                throw new IllegalArgumentException("Texto muito longo no campo: " + key);
            if (Arrays.asList("name", "client", "description", "account_name", "bank", "bsb", "account").contains(key) && value.isEmpty())
                throw new IllegalArgumentException("Preencha os dados do prestador, cliente e pagamento.");
            if ((key.equals("email") || key.equals("client_email")) && !value.isEmpty() &&
                (!value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0))
                throw new IllegalArgumentException("Confira os endereços de e-mail.");
            out.put(key, value);
        }
        return out;
    }
    static List<InvoiceEngine.Item> items(JSONObject draft) throws Exception {
        JSONArray rows = draft.getJSONArray("rows");
        if (rows.length() > 1000) throw new IllegalArgumentException("Limite de 1.000 lançamentos por invoice.");
        String rate = draft.getString("rate"); InvoiceEngine.rate(rate);
        List<InvoiceEngine.Item> items = new ArrayList<>();
        Set<String> linked = new HashSet<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject r = rows.getJSONObject(i);
            String address = r.optString("address").trim(), hours = r.optString("hours", "0");
            boolean dayOff = address.equalsIgnoreCase("Day off");
            if (!dayOff && InvoiceEngine.hours(hours).signum() == 0) continue;
            String source = r.optString("sourceId", "");
            if (!source.isEmpty() && !linked.add(source)) throw new IllegalArgumentException("Uma jornada foi incluída duas vezes.");
            if(draft.optBoolean("aggregationOnly")&&source.isEmpty())throw new IllegalArgumentException("Selecione apenas jornadas salvas no Histórico.");
            InvoiceEngine.Item item=new InvoiceEngine.Item(r.getString("date"), address, hours, r.optString("sourceRate",rate), source,r.optString("overtimePremium","0"),r.optBoolean("exactHours"),r.optString("billingNote"));
            if(r.has("sourceGross")&&!source.isEmpty())item=InvoiceEngine.withAmount(item,new java.math.BigDecimal(r.optString("sourceGross")));
            items.add(item);
        }
        InvoiceEngine.total(items);
        if(paired(draft)){List<MarkenBilling.Entry> entries=sheetEntries(draft);java.math.BigDecimal target=MarkenBilling.total(entries),difference=target.subtract(InvoiceEngine.total(items));int last=items.size()-1;items.set(last,InvoiceEngine.withAmount(items.get(last),items.get(last).amount.add(difference)));}
        return items;
    }
    void available(int number, List<InvoiceEngine.Item> items) throws Exception {
        InvoiceEngine.number(number);
        JSONArray reserved = seed.optJSONArray("reservedNumbers");
        if (reserved != null) for (int i = 0; i < reserved.length(); i++) if (reserved.optInt(i) == number)
            throw new IllegalArgumentException("Esse número já estava usado no Invoice Deck. Escolha outro.");
        JSONArray records = records();
        for (int n = 0; n < records.length(); n++) {
            JSONObject r = records.getJSONObject(n);
            if (r.getInt("number") == number) throw new IllegalArgumentException("Número já usado neste celular.");
            if (!active(r)) continue;
            JSONArray sources = r.optJSONArray("sourceIds");
            if (sources != null) for (int i = 0; i < sources.length(); i++) for (InvoiceEngine.Item item : items)
                if (!item.sourceId.isEmpty() && item.sourceId.equals(sources.getString(i)))
                    throw new IllegalArgumentException("Uma das jornadas já está no invoice " + InvoiceEngine.number(r.getInt("number")) + ".");
        }
        Store journals=new Store(context);
        for(InvoiceEngine.Item item:items)if(!item.sourceId.isEmpty()){
            JSONObject source=journals.findJournal(item.sourceId);
            if(source==null||source.optBoolean("deleted"))throw new IllegalArgumentException("Uma jornada foi removida. Atualize os vínculos do rascunho antes de emitir.");
        }
    }
    static boolean paired(JSONObject draft){JSONArray rows=draft.optJSONArray("rows");if(rows!=null)for(int n=0;n<rows.length();n++)if(rows.optJSONObject(n)!=null&&rows.optJSONObject(n).has("timesheet"))return true;return ClientRegistry.namedMarken(draft.optJSONObject("profile")==null?"":draft.optJSONObject("profile").optString("client"));}
    static List<MarkenBilling.Entry> sheetEntries(JSONObject draft)throws Exception{List<MarkenBilling.Entry> out=new ArrayList<>();JSONArray rows=draft.getJSONArray("rows");for(int n=0;n<rows.length();n++){JSONObject r=rows.getJSONObject(n);if(r.optString("sourceId").isEmpty()||!r.has("timesheet"))throw new IllegalArgumentException("Para Marken, vincule jornadas com início e término para gerar os dois PDFs.");out.add(TimesheetData.read(r.getJSONObject("timesheet")));}if(out.isEmpty())throw new IllegalArgumentException("Vincule jornadas Marken para gerar o timesheet.");return out;}
    JSONObject linkedTimesheets(JSONObject draft)throws Exception{
        JSONObject out=new JSONObject(draft.toString());if(!paired(out))return out;
        JSONArray rows=out.getJSONArray("rows");Store journals=new Store(context);
        for(int n=0;n<rows.length();n++){
            JSONObject row=rows.getJSONObject(n);String id=row.optString("sourceId");if(id.isEmpty()||row.has("timesheet"))continue;
            JSONObject source=journals.findJournal(id);if(source==null||source.optBoolean("deleted"))throw new IllegalArgumentException("Uma jornada foi removida. Confira o Histórico.");
            row.put("timesheet",TimesheetData.fromSource(source));
        }
        return out;
    }
    File previewTimesheet(JSONObject draft)throws Exception{draft=linkedTimesheets(draft);validateDraftSources(draft);byte[] bytes=InvoicePdf.generateTimesheet(draft.getInt("number"),today(),profile(draft),sheetEntries(draft));File out=File.createTempFile("preview_",".pdf",folder());try(FileOutputStream stream=new FileOutputStream(out)){stream.write(bytes);}return out;}
    File timesheet(JSONObject record,boolean signed)throws Exception{String field=signed&&!record.optString("signedTimesheetFilename").isEmpty()?"signedTimesheetFilename":"timesheetFilename";String name=record.optString(field);if(!name.matches("timesheet_(?:signed_)?[0-9]+_[0-9]{4}-[0-9]{2}-[0-9]{2}(?:_[0-9a-f-]+)?\\.pdf"))throw new IllegalArgumentException("Timesheet não encontrado.");File out=new File(folder(),name);if(!out.isFile())throw new IllegalArgumentException("Timesheet não encontrado.");return out;}
    void attachSigned(String invoice,InputStream input)throws Exception{JSONObject current=find(invoice);if(!active(current)||needsReview(current)||!current.has("timesheetFilename"))throw new IllegalArgumentException("Revise o invoice antes de anexar o timesheet.");String name="timesheet_signed_"+InvoiceEngine.number(current.getInt("number"))+"_"+current.getString("issued")+"_"+UUID.randomUUID()+".pdf";File target=new File(folder(),name);boolean committed=false;try{try(FileOutputStream out=new FileOutputStream(target)){byte[] head=new byte[5];int count=0;while(count<5){int n=input.read(head,count,5-count);if(n<0)break;count+=n;}if(count!=5||!new String(head,StandardCharsets.US_ASCII).equals("%PDF-"))throw new IllegalArgumentException("Selecione um timesheet em PDF.");out.write(head);long size=5;byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1){size+=n;if(size>20*1024*1024)throw new IllegalArgumentException("O timesheet deve ter até 20 MB.");out.write(buffer,0,n);}out.getFD().sync();}JSONArray all=records();for(int n=0;n<all.length();n++)if(all.getJSONObject(n).getString("filename").equals(invoice))all.getJSONObject(n).put("signedTimesheetFilename",name);if(!prefs.edit().putString("records",all.toString()).commit())throw new IOException("Não foi possível salvar o timesheet.");committed=true;}finally{if(!committed)target.delete();}}
    File preview(JSONObject draft) throws Exception {
        draft=linkedTimesheets(draft);
        Map<String, String> profile = profile(draft);
        List<InvoiceEngine.Item> items = items(draft);
        validateDraftSources(draft);
        byte[] bytes = InvoicePdf.generate(draft.getInt("number"), today(), profile, items,paired(draft));
        File folder = folder();
        // Only retain a bounded number of temporary previews; issued files are immutable.
        File[] old = folder.listFiles((dir, name) -> name.startsWith("preview_"));
        if (old != null && old.length > 20) {
            Arrays.sort(old, Comparator.comparingLong(File::lastModified));
            for (int n = 0; n < old.length - 20; n++) old[n].delete();
        }
        File out = new File(folder, "preview_" + UUID.randomUUID() + ".pdf");
        try (FileOutputStream stream = new FileOutputStream(out)) { stream.write(bytes); }
        return out;
    }
    File issue(JSONObject draft) throws Exception {
        draft=linkedTimesheets(draft);
        int number = draft.getInt("number");
        Map<String, String> profile = profile(draft);
        List<InvoiceEngine.Item> items = items(draft);
        available(number, items);
        validateDraftSources(draft);
        LocalDate issued = today();
        String date = issued.toString();
        File out = new File(folder(), "invoice_" + InvoiceEngine.number(number) + "_" + date + ".pdf");
        if (out.exists()) throw new IllegalArgumentException("O PDF já existe. Escolha outro número.");
        boolean pair=paired(draft);byte[] bytes = InvoicePdf.generate(number, issued, profile, items,pair);
        File sheet=pair?new File(folder(),"timesheet_"+InvoiceEngine.number(number)+"_"+date+".pdf"):null;byte[] sheetBytes=pair?InvoicePdf.generateTimesheet(number,issued,profile,sheetEntries(draft)):null;
        File temp = File.createTempFile("invoice-write-", ".tmp", folder());
        boolean committed=false,sheetCreated=false,invoiceCreated=false;
        try {
            if(sheet!=null){if(!sheet.createNewFile())throw new IOException("O timesheet já existe. Escolha outro número.");sheetCreated=true;try(FileOutputStream stream=new FileOutputStream(sheet)){stream.write(sheetBytes);stream.getFD().sync();}}
            try (FileOutputStream stream = new FileOutputStream(temp)) { stream.write(bytes); stream.getFD().sync(); }
            if (!temp.renameTo(out)) throw new IOException("Não foi possível guardar o PDF.");invoiceCreated=true;
            JSONObject record = new JSONObject();
            Store.put(record, "number", number); Store.put(record, "issued", date);
            Store.put(record, "client", profile.get("client")); Store.put(record, "email", profile.get("client_email"));
            Store.put(record, "total", InvoiceEngine.total(items).toPlainString());
            Store.put(record, "filename", out.getName()); Store.put(record, "draft", draft);if(sheet!=null){Store.put(record,"timesheetFilename",sheet.getName());Store.put(record,"servicePeriod",MarkenBilling.period(sheetEntries(draft)));}
            Store.put(record, "status", "issued");
            JSONArray sources = new JSONArray();
            JSONObject fingerprints=new JSONObject();Store journals=new Store(context);
            for (InvoiceEngine.Item i : items) if (!i.sourceId.isEmpty()) sources.put(i.sourceId);
            for(InvoiceEngine.Item i:items)if(!i.sourceId.isEmpty()){JSONObject source=journals.findJournal(i.sourceId);if(source!=null)fingerprints.put(i.sourceId,Store.fingerprint(source));}
            Store.put(record, "sourceIds", sources);
            Store.put(record, "sourceFingerprints", fingerprints);
            JSONArray all = new JSONArray(); all.put(record);
            JSONArray existing = records(); for (int i = 0; i < existing.length(); i++) all.put(existing.opt(i));
            JSONObject nextDraft = new JSONObject(draft.toString());
            Store.put(nextDraft, "number", Math.max(nextNumber(), number + 1)); Store.put(nextDraft, "rows", new JSONArray());
            if (!prefs.edit().putString("records", all.toString()).putString("draft", nextDraft.toString()).commit()) {
                out.delete(); throw new IOException("Não foi possível salvar o registro do invoice.");
            }
            committed=true;
        } finally { temp.delete();if(!committed){if(invoiceCreated)out.delete();if(sheetCreated)sheet.delete();} }
        return out;
    }
    void validateDraftSources(JSONObject draft) throws Exception {
        Store journals=new Store(context);JSONArray rows=draft.getJSONArray("rows");
        for(int n=0;n<rows.length();n++){
            JSONObject row=rows.getJSONObject(n);String id=row.optString("sourceId");if(id.isEmpty())continue;
            JSONObject source=journals.findJournal(id);
            if(source==null||source.optBoolean("deleted"))throw new IllegalArgumentException("Uma jornada foi removida. Toque em Atualizar jornadas vinculadas.");
            if(TimesheetData.supports(source)){if(!row.has("timesheet")||!TimesheetData.read(row.getJSONObject("timesheet")).signature().equals(TimesheetData.read(TimesheetData.fromSource(source)).signature()))throw new IllegalArgumentException("Atualize as jornadas vinculadas para gerar o timesheet correto.");}
            if(Store.isMarken(source)&&!row.optBoolean("exactHours"))throw new IllegalArgumentException("Atualize a jornada Marken vinculada antes de emitir.");
            if(draft.optBoolean("aggregationOnly")){
                if(!ClientRegistry.key(source.optString("client")).equals(ClientRegistry.key(draft.getJSONObject("profile").optString("client"))))throw new IllegalArgumentException("Selecione jornadas de um único cliente por invoice.");
                if(!"AUD".equals(source.getJSONObject("config").optString("currency"))||!row.optString("date").equals(Store.date(source))||!row.optString("address").equals(source.optString("worksite"))||Math.abs(InvoiceEngine.hours(row.optString("hours")).doubleValue()-source.optLong("workedMs")/3600000.0)>0.000001)throw new IllegalArgumentException("Os dados da jornada mudaram. Confira o Histórico.");
                if(!row.has("sourceGross")||Math.abs(row.optDouble("sourceGross")-source.getJSONObject("totals").getDouble("gross"))>0.000001)throw new IllegalArgumentException("O bruto mudou. Atualize as jornadas do rascunho.");
            }
            JSONObject totals=source.optJSONObject("totals");double premium=totals==null?0:totals.optDouble("overtimePremium",0);
            if(source.optJSONObject("config").optBoolean("overtime")&&(Math.abs(row.optDouble("overtimePremium",0)-premium)>0.00001||Math.abs(row.optDouble("sourceRate",0)-source.getJSONObject("config").getDouble("hourly"))>0.00001||Math.abs(InvoiceEngine.hours(row.optString("hours")).doubleValue()-source.optLong("workedMs")/3600000.0)>0.000001))throw new IllegalArgumentException("Atualize a jornada vinculada para incluir o overtime correto.");
            if(row.has("sourceFingerprint")&&!row.getString("sourceFingerprint").equals(Store.fingerprint(source)))
                throw new IllegalArgumentException("Uma jornada foi editada. Atualize os vínculos do rascunho antes de emitir.");
        }
    }
    private Map<String,JSONObject> journalIndex() {
        JSONArray all=new Store(context).history();Map<String,JSONObject> index=new HashMap<>();
        for(int n=0;n<all.length();n++){JSONObject r=all.optJSONObject(n);if(r!=null)index.put(Store.id(r),r);}return index;
    }
    boolean needsReview(JSONObject record) { return needsReview(record,journalIndex()); }
    private boolean needsReview(JSONObject record,Map<String,JSONObject> journals) {
        JSONArray ids=record.optJSONArray("sourceIds");JSONObject snapshots=record.optJSONObject("sourceFingerprints");
        JSONObject saved=record.optJSONObject("draft");JSONArray rows=saved==null?null:saved.optJSONArray("rows");
        if(rows!=null)for(int n=0;n<rows.length();n++){JSONObject row=rows.optJSONObject(n);if(row==null||!row.has("timesheet"))continue;JSONObject source=journals.get(row.optString("sourceId"));try{if(source==null||!TimesheetData.read(row.getJSONObject("timesheet")).signature().equals(TimesheetData.read(TimesheetData.fromSource(source)).signature()))return true;}catch(Exception e){return true;}}
        if(ids!=null)for(int n=0;n<ids.length();n++){
            String id=ids.optString(n);JSONObject source=journals.get(id);
            if(source==null||source.optBoolean("deleted")||snapshots!=null&&snapshots.has(id)&&!snapshots.optString(id).equals(Store.fingerprint(source)))return true;
        }
        return false;
    }
    JSONArray annotatedRecords() {
        JSONArray all=records();Map<String,JSONObject> journals=journalIndex();for(int n=0;n<all.length();n++){JSONObject r=all.optJSONObject(n);if(r!=null)Store.put(r,"needsReview",active(r)&&needsReview(r,journals));}return all;
    }
    JSONObject refreshSources(JSONObject draft) throws Exception {
        JSONObject out=new JSONObject(draft.toString());JSONArray rows=draft.getJSONArray("rows"),updated=new JSONArray();Store journals=new Store(context);
        for(int n=0;n<rows.length();n++){
            JSONObject row=new JSONObject(rows.getJSONObject(n).toString());String id=row.optString("sourceId");
            if(!id.isEmpty()){
                JSONObject source=journals.findJournal(id);if(source==null||source.optBoolean("deleted"))continue;
                if(TimesheetData.supports(source)){try{row.put("timesheet",TimesheetData.fromSource(source));}catch(IllegalArgumentException e){row.remove("timesheet");}}else row.remove("timesheet");
                row.put("exactHours",true);row.put("sourceGross",source.getJSONObject("totals").getDouble("gross"));row.put("billingNote",source.optString("billingNote"));
                row.put("overtimePremium",source.optJSONObject("totals").optDouble("overtimePremium",0));row.put("sourceRate",source.getJSONObject("config").getDouble("hourly"));
                row.put("date",Store.date(source));row.put("hours",java.math.BigDecimal.valueOf(source.optLong("workedMs")).divide(new java.math.BigDecimal("3600000"),12,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
                row.put("address",source.optString("worksite"));
                row.put("sourceFingerprint",Store.fingerprint(source));row.put("workType",source.optString("workType"));
            }
            updated.put(row);
        }
        out.put("rows",updated);return out;
    }
    void setPaid(String filename,boolean paid) throws Exception {
        JSONObject record=find(filename);
        if(!active(record))throw new IllegalArgumentException("Restaure o invoice antes de alterar o pagamento.");
        if(paid&&needsReview(record))throw new IllegalArgumentException("Revise as jornadas alteradas antes de marcar como pago.");
        JSONArray all=records();
        for(int n=0;n<all.length();n++){JSONObject r=all.getJSONObject(n);if(r.getString("filename").equals(filename)){
            r.put("paid",paid);if(paid)r.put("paidAt",System.currentTimeMillis());else r.remove("paidAt");
        }}
        if(!prefs.edit().putString("records",all.toString()).commit())throw new IOException("Não foi possível salvar o pagamento.");
    }
    void cancel(String filename) throws Exception {
        JSONArray all=records();boolean found=false;
        for(int n=0;n<all.length();n++){JSONObject r=all.getJSONObject(n);if(r.getString("filename").equals(filename)){r.put("status","cancelled");r.put("removedAt",System.currentTimeMillis());found=true;}}
        if(!found)throw new IllegalArgumentException("Invoice não encontrado.");
        if(!prefs.edit().putString("records",all.toString()).commit())throw new IOException("Não foi possível remover o invoice dos totais.");
    }
    void restore(String filename) throws Exception {
        JSONObject record=find(filename);
        if(needsReview(record))throw new IllegalArgumentException("As jornadas desse invoice mudaram. Gere uma correção em vez de restaurar o PDF antigo.");
        JSONArray all=records(),sources=record.optJSONArray("sourceIds");
        for(int n=0;n<all.length();n++){
            JSONObject other=all.getJSONObject(n);if(!active(other)||other.getString("filename").equals(filename))continue;
            JSONArray claimed=other.optJSONArray("sourceIds");
            if(sources!=null&&claimed!=null)for(int i=0;i<sources.length();i++)for(int j=0;j<claimed.length();j++)
                if(sources.optString(i).equals(claimed.optString(j)))throw new IllegalArgumentException("Uma jornada já está em outro invoice ativo.");
        }
        for(int n=0;n<all.length();n++)if(all.getJSONObject(n).getString("filename").equals(filename))all.getJSONObject(n).put("status","issued");
        if(!prefs.edit().putString("records",all.toString()).commit())throw new IOException("Não foi possível restaurar o invoice.");
    }
    void correct(String filename) throws Exception {
        JSONObject old=find(filename),draft=refreshSources(old.getJSONObject("draft"));draft.put("number",nextNumber());
        JSONArray all=records();
        for(int n=0;n<all.length();n++)if(all.getJSONObject(n).getString("filename").equals(filename)){
            all.getJSONObject(n).put("status","cancelled");all.getJSONObject(n).put("removedAt",System.currentTimeMillis());
        }
        if(!prefs.edit().putString("records",all.toString()).putString("draft",draft.toString()).commit())throw new IOException("Não foi possível preparar a correção.");
    }
    File folder() throws IOException {
        File folder = new File(context.getFilesDir(), "invoice-pdfs");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Sem espaço para o PDF.");
        return folder;
    }
    JSONObject find(String filename) throws Exception {
        JSONArray a = records();
        for (int i = 0; i < a.length(); i++) if (filename.equals(a.getJSONObject(i).getString("filename"))) return a.getJSONObject(i);
        throw new IllegalArgumentException("Invoice não encontrado.");
    }
}
