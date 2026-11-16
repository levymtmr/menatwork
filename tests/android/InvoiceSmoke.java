package com.neonshift.app;

import android.app.Instrumentation;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Runs only in the disposable emulator. Does not launch an e-mail app or send mail. */
public class InvoiceSmoke extends Instrumentation {
    private int checks;
    private void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    public void onStart() {
        Bundle result = new Bundle();
        if (!"ranchu".equals(android.os.Build.HARDWARE) && !"goldfish".equals(android.os.Build.HARDWARE)) {
            result.putString("stream", "Refused: this destructive fixture test is restricted to an Android emulator.\n");
            finish(0, result); return;
        }
        int status = -1;
        InvoiceStore store = new InvoiceStore(getTargetContext());
        try {
            // This test APK must only be run on the disposable emulator.
            File[] stale = store.folder().listFiles();
            if (stale != null) for (File f : stale) f.delete();
            store.prefs.edit().clear().commit();
            getTargetContext().getSharedPreferences("neonshift",0).edit().clear().commit();
            JSONObject sourceFixture=new JSONObject().put("id","native-smoke").put("startedAt",1).put("workDate","2026-11-16").put("workedMs",28860000).put("worksite","16 East Pkwy, Fulham SA 5024");
            getTargetContext().getSharedPreferences("neonshift",0).edit().putString("history",new JSONArray().put(sourceFixture).toString()).commit();
            JSONObject draft = store.draft();
            JSONObject profile = draft.getJSONObject("profile");
            profile.put("name", "Example Worker"); profile.put("client", "Example Client");
            profile.put("bank", "Example Bank"); profile.put("account_name", "Example Worker");
            profile.put("bsb", "000-000"); profile.put("account", "00000000");
            profile.put("client_email", "client@example.com");
            JSONObject row = new JSONObject();
            row.put("date", "2026-11-16");row.put("address", "16 East Pkwy, Fulham SA 5024");
            row.put("hours", "08:01");row.put("sourceId", "native-smoke");
            draft.put("rows", new JSONArray().put(row));draft.put("rate", "32");
            store.saveDraft(draft);
            check(store.draft().getJSONArray("rows").length() == 1, "draft persistence");
            File preview = store.preview(draft);
            check(preview.length() > 1000, "native preview PDF");
            check(store.records().length() == 0, "preview is not issued");
            File issued = store.issue(draft);
            check(issued.isFile(), "issued file");
            check(store.records().length() == 1, "issued record");
            JSONObject record = store.records().getJSONObject(0);
            check("264.00".equals(record.getString("total")), "native quarter-hour total");
            check(store.draft().getJSONArray("rows").length() == 0, "draft cleared after issue");
            boolean duplicate = false;
            try { store.issue(draft); } catch (IllegalArgumentException e) { duplicate = true; }
            check(duplicate, "duplicate number blocked");
            draft.put("number", store.nextNumber());
            boolean source = false;
            try { store.issue(draft); } catch (IllegalArgumentException e) { source = true; }
            check(source, "duplicate source blocked");
            Intent share = InvoiceShare.intent(issued, record);
            check(Intent.ACTION_SEND.equals(share.getAction()), "share action");
            check("application/pdf".equals(share.getType()), "PDF MIME");
            check((share.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0, "temporary read grant");
            check(share.getStringArrayExtra(Intent.EXTRA_EMAIL)[0].equals(record.getString("email")), "recipient");
            Uri uri = share.getParcelableExtra(Intent.EXTRA_STREAM);
            check("content".equals(uri.getScheme()), "content URI");
            check(share.getClipData().getItemAt(0).getUri().equals(uri), "clip URI for recipients");
            try (InputStream in = getTargetContext().getContentResolver().openInputStream(uri)) {
                byte[] h = new byte[5]; int count = in.read(h);
                check(count == 5 && "%PDF-".equals(new String(h, StandardCharsets.US_ASCII)), "provider returns actual PDF");
            }
            try (Cursor c = getTargetContext().getContentResolver().query(uri, null, null, null, null)) {
                check(c != null && c.moveToFirst(), "provider metadata");
                check(c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME)).equals(issued.getName()), "attachment filename");
            }
            boolean writeBlocked = false;
            try { getTargetContext().getContentResolver().openFileDescriptor(uri, "w"); }
            catch (FileNotFoundException | SecurityException e) { writeBlocked = true; }
            check(writeBlocked, "provider read only");
            result.putString("stream", "PASS: " + checks + " native Android invoice checks. No e-mail sent.\n");
        } catch (Throwable e) {
            result.putString("stream", "FAIL: " + e + "\n");status = 0;
        } finally {
            store.prefs.edit().clear().commit();
            getTargetContext().getSharedPreferences("neonshift",0).edit().clear().commit();
            try { File[] files = store.folder().listFiles(); if (files != null) for (File f : files) f.delete(); } catch (Exception ignored) { }
        }
        finish(status, result);
    }
}
