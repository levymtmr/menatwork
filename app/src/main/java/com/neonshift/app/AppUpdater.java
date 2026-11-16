package com.neonshift.app;

import android.content.*;
import android.content.pm.*;
import android.os.Build;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

final class AppUpdater {
    private static AppUpdater instance;
    static synchronized AppUpdater get(Context c) {
        if (instance == null) instance = new AppUpdater(c.getApplicationContext());
        return instance;
    }
    private final Context context;
    private final android.content.SharedPreferences prefs;
    private final java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor();
    private String stage = "idle", message = "Toque para procurar uma nova versão no GitHub.";
    private int progress;
    private UpdatePolicy.Release release;
    private JSONObject manifest;
    final long currentCode;
    final String currentName;
    private AppUpdater(Context c) {
        context = c;
        prefs = c.getSharedPreferences("app_updates", Context.MODE_PRIVATE);
        try {
            PackageInfo installed = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            currentCode = code(installed); currentName = installed.versionName;
        } catch (Exception e) { throw new IllegalStateException(e); }
        try {
            String raw = prefs.getString("ready", "");
            if (!raw.isEmpty()) {
                manifest = new JSONObject(raw); release = parse(manifest);
                if (release.newerThan(currentCode)) {
                    validateApk(file(), release);
                    stage = "ready"; message = "Versão " + release.versionName + " pronta para instalar."; progress = 100;
                } else {
                    file().delete(); prefs.edit().remove("ready").apply(); release = null; manifest = null;
                }
            }
        } catch (Exception e) { prefs.edit().remove("ready").apply(); release = null; manifest = null; }
    }
    @SuppressWarnings("deprecation") static long code(PackageInfo p) {
        return Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode;
    }
    static UpdatePolicy.Release parse(JSONObject j) throws Exception {
        if (j.getInt("schemaVersion") != 1) throw new IllegalArgumentException("Formato de atualização não suportado.");
        return new UpdatePolicy.Release(j.getString("applicationId"), j.getLong("versionCode"),
            j.getString("versionName"), j.getString("apkUrl"), j.getString("sha256"),
            j.getLong("size"), j.getInt("minSdk"), j.optString("notes", ""));
    }
    synchronized JSONObject snapshot() {
        JSONObject j = new JSONObject();
        Store.put(j, "stage", stage); Store.put(j, "message", message); Store.put(j, "progress", progress);
        Store.put(j, "currentVersion", currentName); Store.put(j, "currentCode", currentCode);
        if (release != null) { Store.put(j, "newVersion", release.versionName); Store.put(j, "notes", release.notes); }
        return j;
    }
    private synchronized void state(String stage, String message, int progress) {
        this.stage = stage; this.message = message; this.progress = progress;
    }
    synchronized void checkAndDownload() {
        if ("checking".equals(stage) || "downloading".equals(stage)) return;
        state("checking", "Procurando atualização no GitHub…", 0);
        executor.execute(() -> {
            File partial = null;
            try {
                JSONObject data;
                HttpURLConnection conn = open(UpdatePolicy.METADATA_URL);
                try (InputStream in = conn.getInputStream()) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[4096]; int n;
                    while ((n = in.read(b)) != -1) {
                        if (out.size() + n > 65536) throw new IOException("O manifesto de atualização é muito grande.");
                        out.write(b, 0, n);
                    }
                    data = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
                } finally { conn.disconnect(); }
                UpdatePolicy.Release candidate = parse(data);
                if (!candidate.newerThan(currentCode)) {
                    state("current", "Você já usa a versão mais recente: " + currentName + ".", 0); return;
                }
                if (candidate.minSdk > Build.VERSION.SDK_INT) throw new IOException("Essa atualização exige uma versão mais recente do Android.");
                synchronized (this) { release = candidate; manifest = data; }
                state("downloading", "Baixando a versão " + candidate.versionName + "…", 0);
                File folder = folder(); partial = new File(folder, "download.part");
                HttpURLConnection apk = open(candidate.apkUrl);
                long count = 0;
                try (InputStream in = apk.getInputStream(); FileOutputStream out = new FileOutputStream(partial)) {
                    byte[] b = new byte[16384]; int n;
                    while ((n = in.read(b)) != -1) {
                        count += n;
                        if (count > candidate.size || count > UpdatePolicy.MAX_APK_BYTES) throw new IOException("O tamanho do APK não corresponde à versão publicada.");
                        out.write(b, 0, n);
                        int pct = (int)(count * 100 / candidate.size);
                        state("downloading", "Baixando a versão " + candidate.versionName + " · " + pct + "%", pct);
                    }
                    out.getFD().sync();
                } finally { apk.disconnect(); }
                validateApk(partial, candidate);
                File target = file();
                if (!partial.renameTo(target)) throw new IOException("Não foi possível preparar o instalador.");
                if (!prefs.edit().putString("ready", data.toString()).commit()) {
                    target.delete(); throw new IOException("Não foi possível salvar a atualização.");
                }
                state("ready", "Versão " + candidate.versionName + " pronta para instalar.", 100);
            } catch (Exception e) {
                state("error", e instanceof IOException || e instanceof IllegalArgumentException ? e.getMessage() :
                    "Não foi possível conferir a atualização. Verifique sua conexão e tente novamente.", 0);
            } finally { if (partial != null && partial.exists()) partial.delete(); }
        });
    }
    private HttpURLConnection open(String raw) throws Exception {
        for (int redirect = 0; redirect < 6; redirect++) {
            if (!UpdatePolicy.allowedDownloadUrl(raw)) throw new IOException("Destino de atualização não permitido.");
            HttpURLConnection c = (HttpURLConnection)new URL(raw).openConnection();
            c.setInstanceFollowRedirects(false); c.setConnectTimeout(15000); c.setReadTimeout(30000);
            c.setRequestProperty("User-Agent", "NeonShift/" + currentName);
            c.setRequestProperty("Accept", "application/octet-stream, application/json");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = c.getHeaderField("Location"); c.disconnect();
                if (location == null) throw new IOException("Redirecionamento de atualização inválido.");
                raw = new URI(raw).resolve(location).toString(); continue;
            }
            if (code == 200) return c;
            c.disconnect();
            if (code == 404) throw new IOException("Nenhuma versão publicada foi encontrada no GitHub.");
            if (code == 403 || code == 429) throw new IOException("O GitHub limitou as consultas. Tente novamente mais tarde.");
            throw new IOException("Não foi possível baixar a atualização (HTTP " + code + ").");
        }
        throw new IOException("Muitos redirecionamentos ao baixar a atualização.");
    }
    private File folder() throws IOException {
        File folder = new File(context.getFilesDir(), "updates");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Sem espaço para baixar a atualização.");
        return folder;
    }
    private File file() throws IOException {
        if (release == null) throw new IOException("Não há atualização pronta.");
        return new File(folder(), "update-" + release.versionCode + ".apk");
    }
    @SuppressWarnings("deprecation") private static Set<String> signers(PackageInfo info) {
        android.content.pm.Signature[] signatures = Build.VERSION.SDK_INT >= 28 && info.signingInfo != null ?
            info.signingInfo.getApkContentsSigners() : info.signatures;
        Set<String> out = new HashSet<>();
        if (signatures != null) for (android.content.pm.Signature s : signatures) out.add(s.toCharsString());
        return out;
    }
    @SuppressWarnings("deprecation") void validateApk(File file, UpdatePolicy.Release expected) throws Exception {
        if (!file.isFile() || file.length() != expected.size || !UpdatePolicy.sha256(file).equals(expected.sha256))
            throw new IOException("O APK está incompleto ou não passou na verificação SHA-256.");
        PackageManager pm = context.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo next = pm.getPackageArchiveInfo(file.getAbsolutePath(), flags);
        PackageInfo installed = pm.getPackageInfo(context.getPackageName(), flags);
        if (next == null || !context.getPackageName().equals(next.packageName) || code(next) != expected.versionCode ||
            !expected.newerThan(code(installed)) || !expected.versionName.equals(next.versionName) ||
            next.applicationInfo == null || next.applicationInfo.minSdkVersion > Build.VERSION.SDK_INT)
            throw new IOException("O arquivo não é uma atualização compatível do Menatwork.");
        Set<String> trusted = signers(installed);
        if (trusted.isEmpty() || !trusted.equals(signers(next))) throw new IOException("A assinatura do APK não corresponde ao aplicativo instalado.");
    }
    synchronized File readyFile() throws Exception {
        if (!"ready".equals(stage) || release == null) throw new IOException("Procure e baixe uma atualização primeiro.");
        File f = file(); validateApk(f, release); return f;
    }
}
