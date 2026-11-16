package com.neonshift.app;

import java.io.*;
import java.net.URI;
import java.security.MessageDigest;
import java.util.*;

/** Validation shared by Android and dependency-free JVM tests. */
public final class UpdatePolicy {
    public static final String REPOSITORY = "levymtmr/menatwork";
    public static final String METADATA_URL = "https://github.com/" + REPOSITORY + "/releases/latest/download/update.json";
    public static final long MAX_APK_BYTES = 200L * 1024 * 1024;
    public static boolean allowedDownloadUrl(String raw) {
        try {
            URI u = new URI(raw);
            return "https".equals(u.getScheme()) && u.getUserInfo() == null && u.getFragment() == null &&
                (u.getPort() == -1 || u.getPort() == 443) && Arrays.asList("github.com",
                    "release-assets.githubusercontent.com", "objects.githubusercontent.com",
                    "github-releases.githubusercontent.com").contains(u.getHost());
        } catch (Exception e) { return false; }
    }
    public static final class Release {
        public final long versionCode, size;
        public final int minSdk;
        public final String versionName, apkUrl, sha256, notes;
        public Release(String applicationId, long versionCode, String versionName, String apkUrl,
                String sha256, long size, int minSdk, String notes) {
            if (!"com.neonshift.app".equals(applicationId) || versionCode < 1 || versionCode > Integer.MAX_VALUE ||
                versionName == null || !versionName.matches("[0-9]+\\.[0-9]+\\.[0-9]+") ||
                sha256 == null || !sha256.matches("[a-fA-F0-9]{64}") || size < 1 || size > MAX_APK_BYTES || minSdk < 26)
                throw new IllegalArgumentException("Os dados da atualização são inválidos.");
            try {
                URI u = new URI(apkUrl);
                if (!allowedDownloadUrl(apkUrl) || !"github.com".equals(u.getHost()) || u.getQuery() != null ||
                    !u.getPath().equals("/" + REPOSITORY + "/releases/download/v" + versionName + "/neon-shift-" + versionName + ".apk"))
                    throw new IllegalArgumentException();
            } catch (Exception e) { throw new IllegalArgumentException("O APK não pertence ao canal de atualização do aplicativo."); }
            this.versionCode = versionCode; this.versionName = versionName; this.apkUrl = apkUrl;
            this.sha256 = sha256.toLowerCase(Locale.ROOT); this.size = size; this.minSdk = minSdk;
            this.notes = notes == null ? "" : notes.substring(0, Math.min(4000, notes.length()));
        }
        public boolean newerThan(long current) { return versionCode > current; }
    }
    public static String sha256(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[16384]; int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
        }
        StringBuilder s = new StringBuilder();
        for (byte b : md.digest()) s.append(String.format(Locale.ROOT, "%02x", b & 255));
        return s.toString();
    }
}
