package com.neonshift.app;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import java.io.*;

/** Narrow read-only content provider; access requires a temporary URI grant. */
public class InvoiceProvider extends ContentProvider {
    public boolean onCreate() { return true; }
    private File file(Uri uri) throws FileNotFoundException {
        if (!"com.neonshift.app.invoices".equals(uri.getAuthority()) || uri.getPathSegments().size() != 1)
            throw new FileNotFoundException();
        String name = uri.getLastPathSegment();
        if (name == null || !name.matches("(?:invoice_[0-9]+_[0-9]{4}-[0-9]{2}-[0-9]{2}|preview_[0-9a-f-]+)\\.pdf"))
            throw new FileNotFoundException();
        File f = new File(new File(getContext().getFilesDir(), "invoice-pdfs"), name);
        if (!f.isFile()) throw new FileNotFoundException();
        return f;
    }
    public String getType(Uri uri) { return "application/pdf"; }
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Somente leitura.");
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        try {
            File f = file(uri);
            if (projection == null) projection = new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
            MatrixCursor c = new MatrixCursor(projection);
            Object[] values = new Object[projection.length];
            for (int i = 0; i < projection.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(projection[i])) values[i] = f.getName();
                else if (OpenableColumns.SIZE.equals(projection[i])) values[i] = f.length();
            }
            c.addRow(values); return c;
        } catch (FileNotFoundException e) { return null; }
    }
    public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
    public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
}
