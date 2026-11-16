package com.neonshift.app;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

public class UpdateProvider extends ContentProvider {
    public boolean onCreate() { return true; }
    private File file(Uri uri) throws FileNotFoundException {
        try {
            if (!"com.neonshift.app.updates".equals(uri.getAuthority()) || uri.getPathSegments().size() != 1) throw new IOException();
            File file = AppUpdater.get(getContext()).readyFile();
            if (!file.getName().equals(uri.getLastPathSegment())) throw new IOException();
            return file;
        } catch (Exception e) { throw new FileNotFoundException("Atualização indisponível."); }
    }
    public String getType(Uri uri) { return "application/vnd.android.package-archive"; }
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Somente leitura.");
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        try {
            File file = file(uri);
            if (projection == null) projection = new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
            MatrixCursor c = new MatrixCursor(projection); Object[] values = new Object[projection.length];
            for (int i = 0; i < projection.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(projection[i])) values[i] = file.getName();
                if (OpenableColumns.SIZE.equals(projection[i])) values[i] = file.length();
            }
            c.addRow(values); return c;
        } catch (Exception e) { return null; }
    }
    public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
    public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
}
