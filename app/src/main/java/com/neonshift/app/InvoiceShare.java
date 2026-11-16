package com.neonshift.app;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import java.io.File;
import org.json.JSONObject;

final class InvoiceShare {
    static Uri uri(File file) {
        return new Uri.Builder().scheme("content").authority("com.neonshift.app.invoices").appendPath(file.getName()).build();
    }
    static Intent intent(File file, JSONObject record) throws Exception {
        if (!file.isFile()) throw new IllegalArgumentException("PDF não encontrado.");
        Uri uri = uri(file);
        String number = InvoiceEngine.number(record.getInt("number"));
        String name = record.getJSONObject("draft").getJSONObject("profile").optString("name");
        Intent i = new Intent(Intent.ACTION_SEND).setType("application/pdf")
            .putExtra(Intent.EXTRA_SUBJECT, "Invoice #" + number + " - " + name)
            .putExtra(Intent.EXTRA_TEXT, "Hello,\n\nPlease find attached invoice #" + number + " for AUD $" + record.getString("total") + ".\n\nKind regards,\n" + name)
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String email = record.optString("email");
        if (!email.isEmpty()) i.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        i.setClipData(ClipData.newRawUri("Invoice PDF", uri));
        return i;
    }
}
