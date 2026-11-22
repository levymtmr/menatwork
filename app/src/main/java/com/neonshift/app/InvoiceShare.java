package com.neonshift.app;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import java.io.File;
import java.util.ArrayList;
import org.json.JSONObject;

final class InvoiceShare {
    static Uri uri(File file) {
        return new Uri.Builder().scheme("content").authority("com.neonshift.app.invoices").appendPath(file.getName()).build();
    }
    static Intent intent(File invoice,File timesheet,JSONObject record)throws Exception{
        if(!invoice.isFile()||!timesheet.isFile())throw new IllegalArgumentException("PDF não encontrado.");
        JSONObject profile=record.getJSONObject("draft").getJSONObject("profile");String name=profile.optString("name"),number=InvoiceEngine.number(record.getInt("number"));boolean signed=!record.optString("signedTimesheetFilename").isEmpty();String period=record.optString("servicePeriod");
        String company=profile.optString("timesheet_company").trim();if(company.isEmpty())company=record.optString("client");
        String subject="Invoice #"+number+" & "+(signed?"Signed Timesheet":"Timesheet")+" - "+name+(period.isEmpty()?"":" - "+period);
        String body="Hi Accounts Team,\n\nPlease find attached my invoice and "+(signed?"signed timesheet":"timesheet for review")+" for services provided at "+company+(period.isEmpty()?"":" from "+period)+".\n\nInvoice number: "+number+"\nTotal amount due: AUD $"+record.getString("total")+"\n\nPlease let me know if you require any further information.\n\nKind regards,\n"+name;
        ArrayList<Uri> uris=new ArrayList<>();uris.add(uri(invoice));uris.add(uri(timesheet));Intent i=new Intent(Intent.ACTION_SEND_MULTIPLE).setType("application/pdf").putExtra(Intent.EXTRA_SUBJECT,subject).putExtra(Intent.EXTRA_TEXT,body).putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String email=record.optString("email");if(email.isEmpty())email="accounts@sarecruitment.com.au";i.putExtra(Intent.EXTRA_EMAIL,new String[]{email});ClipData clip=ClipData.newRawUri("Invoice PDF",uris.get(0));clip.addItem(new ClipData.Item(uris.get(1)));i.setClipData(clip);return i;
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
