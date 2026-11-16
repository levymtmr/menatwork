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
        return j;
    }
    void saveDraft(JSONObject draft) {
        if (draft.toString().length() > 500000) throw new IllegalArgumentException("Rascunho muito grande.");
        if (!prefs.edit().putString("draft", draft.toString()).commit()) throw new IllegalStateException("Não foi possível salvar o rascunho.");
    }
    static LocalDate today() { return LocalDate.now(ZoneId.of("Australia/Adelaide")); }
    static Map<String, String> profile(JSONObject draft) throws Exception {
        JSONObject p = draft.getJSONObject("profile");
        Map<String, String> out = new HashMap<>();
        for (String key : new String[]{"name", "address", "email", "abn", "bank", "bsb", "account", "account_name", "client", "client_abn", "client_address", "client_email", "description"}) {
            String value = p.optString(key).trim();
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
            items.add(new InvoiceEngine.Item(r.getString("date"), address, hours, rate, source));
        }
        InvoiceEngine.total(items);
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
            JSONArray sources = r.optJSONArray("sourceIds");
            if (sources != null) for (int i = 0; i < sources.length(); i++) for (InvoiceEngine.Item item : items)
                if (!item.sourceId.isEmpty() && item.sourceId.equals(sources.getString(i)))
                    throw new IllegalArgumentException("Uma das jornadas já está no invoice " + InvoiceEngine.number(r.getInt("number")) + ".");
        }
    }
    File preview(JSONObject draft) throws Exception {
        Map<String, String> profile = profile(draft);
        List<InvoiceEngine.Item> items = items(draft);
        byte[] bytes = InvoicePdf.generate(draft.getInt("number"), today(), profile, items);
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
        int number = draft.getInt("number");
        Map<String, String> profile = profile(draft);
        List<InvoiceEngine.Item> items = items(draft);
        available(number, items);
        LocalDate issued = today();
        String date = issued.toString();
        File out = new File(folder(), "invoice_" + InvoiceEngine.number(number) + "_" + date + ".pdf");
        if (out.exists()) throw new IllegalArgumentException("O PDF já existe. Escolha outro número.");
        byte[] bytes = InvoicePdf.generate(number, issued, profile, items);
        File temp = File.createTempFile("invoice-write-", ".tmp", folder());
        try {
            try (FileOutputStream stream = new FileOutputStream(temp)) { stream.write(bytes); stream.getFD().sync(); }
            if (!temp.renameTo(out)) throw new IOException("Não foi possível guardar o PDF.");
            JSONObject record = new JSONObject();
            Store.put(record, "number", number); Store.put(record, "issued", date);
            Store.put(record, "client", profile.get("client")); Store.put(record, "email", profile.get("client_email"));
            Store.put(record, "total", InvoiceEngine.total(items).toPlainString());
            Store.put(record, "filename", out.getName()); Store.put(record, "draft", draft);
            JSONArray sources = new JSONArray();
            for (InvoiceEngine.Item i : items) if (!i.sourceId.isEmpty()) sources.put(i.sourceId);
            Store.put(record, "sourceIds", sources);
            JSONArray all = new JSONArray(); all.put(record);
            JSONArray existing = records(); for (int i = 0; i < existing.length(); i++) all.put(existing.opt(i));
            JSONObject nextDraft = new JSONObject(draft.toString());
            Store.put(nextDraft, "number", Math.max(nextNumber(), number + 1)); Store.put(nextDraft, "rows", new JSONArray());
            if (!prefs.edit().putString("records", all.toString()).putString("draft", nextDraft.toString()).commit()) {
                out.delete(); throw new IOException("Não foi possível salvar o registro do invoice.");
            }
        } finally { temp.delete(); }
        return out;
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
