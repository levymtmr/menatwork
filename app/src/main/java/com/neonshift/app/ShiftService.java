package com.neonshift.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.*;
import android.os.*;
import org.json.JSONObject;
import java.util.Locale;

public class ShiftService extends Service implements LocationListener {
    static final String CHANNEL = "journey";
    static volatile boolean running = false;
    private Store store;
    private LocationManager gps;
    private Location previous;
    private long lastReceivedNanos;
    private boolean listening;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        public void run() {
            JSONObject s = store.session();
            if (!s.has("phase")) { stopSelf(); return; }
            try { ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(7, notification(s)); }
            catch (SecurityException ignored) { }
            handler.postDelayed(this, 15000);
        }
    };
    static boolean travelling(JSONObject s) {
        return "commute".equals(s.optString("phase")) || "return".equals(s.optString("phase"));
    }
    public void onCreate() {
        super.onCreate();
        store = new Store(this);
        gps = (LocationManager)getSystemService(LOCATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL, Language.text(this,"Jornada e trajeto"), NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Tempo, ganho líquido estimado e registro do trajeto iniciado por você.");
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
    }
    private Notification notification(JSONObject s) {
        JSONObject t = Store.totals(s, store.config());
        JSONObject cfg = s.optJSONObject("config");
        String currency = cfg == null ? "AUD" : cfg.optString("currency", "AUD");
        String phase = s.optString("phase");
        String title = travelling(s) ? "Menatwork · registrando trajeto" : "pause".equals(phase) ? "Menatwork · pausa" : "Menatwork · trabalhando";
        long mins = t.optLong("workedMs") / 60000;
        String text = String.format("pt".equals(store.language())?new Locale("pt", "BR"):Locale.US, "%s $ %.2f líquidos · %02dh %02dmin · %.1f km", currency,
            t.optDouble("net"), mins / 60, mins % 60, s.optDouble("km", 0));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(com.neonshift.app.R.drawable.ic_notification).setContentTitle(Language.text(this,title)).setContentText(Language.text(this,text))
            .setStyle(new Notification.BigTextStyle().bigText(Language.text(this,text))).setContentIntent(open)
            .setOngoing(true).setOnlyAlertOnce(true).setColor(0xff92ee73).setShowWhen(false);
        if ("work".equals(phase) || "pause".equals(phase)) {
            String action = "work".equals(phase) ? "pause" : "resume";
            PendingIntent pi = PendingIntent.getService(this, 1, new Intent(this, ShiftService.class).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            b.addAction(new Notification.Action.Builder(null, Language.text(this,"work".equals(phase) ? "Pausar" : "Retomar"), pi).build());
        }
        return b.build();
    }
    public int onStartCommand(Intent intent, int flags, int id) {
        String action = intent == null ? "recover" : intent.getAction();
        JSONObject s = store.session();
        String phase = s.optString("phase", "idle");
        long now = System.currentTimeMillis();
        if (("commute".equals(action) || "work".equals(action)) && "idle".equals(phase)) {
            s = new JSONObject();
            Store.put(s, "id", now);
            Store.put(s, "startedAt", now);
            Store.put(s, "km", 0);
            Store.put(s, "extras", 0);
            Store.put(s, "workedMs", 0);
            Store.put(s, "config", store.config());
            Store.put(s,"client",store.config().optString("client"));Store.put(s,"clientId",store.config().optString("clientId"));
            Store.put(s, "skillIds", new org.json.JSONArray());
            Store.put(s, "phase", "commute".equals(action) ? "commute" : "work");
            if ("work".equals(action)) { Store.startSegment(s); Store.put(s,"workStartedAt",now);Store.put(s,"workDate",java.time.LocalDate.now().toString()); }
        } else if ("work".equals(action) && "commute".equals(phase)) {
            Store.put(s, "phase", "work");
            Store.startSegment(s);
            Store.put(s,"workStartedAt",now);Store.put(s,"workDate",java.time.LocalDate.now().toString());
        } else if ("pause".equals(action) && "work".equals(phase)) {
            Store.put(s, "workedMs", Store.elapsed(s));
            Store.put(s, "phase", "pause");
        } else if ("resume".equals(action) && "pause".equals(phase)) {
            Store.put(s, "phase", "work");
            Store.startSegment(s);
        } else if ("return".equals(action) && ("work".equals(phase) || "pause".equals(phase))) {
            Store.put(s, "workedMs", Store.elapsed(s));
            Store.put(s, "phase", "return");
        } else if ("finish".equals(action) && !"idle".equals(phase)) {
            Store.put(s, "workedMs", Store.elapsed(s));
            Store.put(s, "phase", "done");
            Store.put(s, "endedAt", now);
            Store.put(s, "totals", Store.totals(s, store.config()));
            s.remove("lastLat"); s.remove("lastLon"); s.remove("lastFix");
            store.finish(s);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!s.has("phase")) { stopSelf(); return START_NOT_STICKY; }
        store.save(s);
        try {
            int type = travelling(s) ? ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION :
                (Build.VERSION.SDK_INT >= 34 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE : 0);
            if (Build.VERSION.SDK_INT >= 29) startForeground(7, notification(s), type);
            else startForeground(7, notification(s));
            running = true;
            updateGps(s);
        } catch (RuntimeException e) {
            Store.put(s, "gpsNote", "Serviço interrompido. Abra o app e confira as permissões para continuar.");
            store.save(s);
            stopSelf();
            return START_NOT_STICKY;
        }
        handler.removeCallbacks(refresh);
        handler.post(refresh);
        return START_STICKY;
    }
    private void updateGps(JSONObject s) {
        if (travelling(s) && !listening) {
            previous = null;
            lastReceivedNanos = 0;
            s.remove("lastFix");
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
            if (gps.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, this);
                listening = true;
            }
            Store.put(s, "gpsNote", listening ? "Buscando sinal GPS…" : "Ative a localização do celular ou informe os km manualmente.");
            store.save(s);
        } else if (!travelling(s) && listening) {
            gps.removeUpdates(this);
            listening = false;
            previous = null;
        }
    }
    public void onLocationChanged(Location loc) {
        JSONObject s = store.session();
        if (!travelling(s)) return;
        if (!loc.hasAccuracy() || loc.getAccuracy() > 35 ||
            SystemClock.elapsedRealtimeNanos() - loc.getElapsedRealtimeNanos() > 20000000000L || loc.isFromMockProvider()) return;
        double added = 0;
        if (previous != null) {
            float distance = previous.distanceTo(loc);
            double seconds = (loc.getElapsedRealtimeNanos() - previous.getElapsedRealtimeNanos()) / 1e9;
            if (seconds <= 0) return;
            if (lastReceivedNanos > 0 && (loc.getElapsedRealtimeNanos() - lastReceivedNanos) / 1e9 > 120) {
                Store.put(s, "hasGpsGap", true);
                previous = loc;
            } else if (distance / seconds > 65) {
                return; // Ignore impossible jumps, preserving the last valid anchor.
            } else if (distance >= Math.max(8, (loc.getAccuracy() + previous.getAccuracy()) * .6)) {
                added = distance / 1000.0;
                previous = loc;
            }
        } else previous = loc;
        lastReceivedNanos = loc.getElapsedRealtimeNanos();
        Store.put(s, "km", s.optDouble("km", 0) + added);
        Store.put(s, "lastFix", System.currentTimeMillis());
        Store.put(s, "lastLat", loc.getLatitude());
        Store.put(s, "lastLon", loc.getLongitude());
        Store.put(s, "gpsNote", s.optBoolean("hasGpsGap") ? "Houve perda de sinal. Confira os km antes de salvar." : "GPS ativo · precisão de " + Math.round(loc.getAccuracy()) + " m");
        store.save(s);
    }
    public void onProviderDisabled(String provider) {
        JSONObject s = store.session();
        if (travelling(s)) { Store.put(s, "gpsNote", "GPS desativado. Ative a localização e confira os km."); store.save(s); }
    }
    public void onProviderEnabled(String provider) { }
    @SuppressWarnings("deprecation") public void onStatusChanged(String p, int status, Bundle extras) { }
    public IBinder onBind(Intent i) { return null; }
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(refresh);
        if (gps != null && listening) gps.removeUpdates(this);
        super.onDestroy();
    }
}
