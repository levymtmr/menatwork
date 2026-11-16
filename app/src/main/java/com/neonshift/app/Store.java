package com.neonshift.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

final class Store {
    private static int currentBoot = -1;
    private final SharedPreferences prefs;
    Store(Context c) {
        prefs = c.getSharedPreferences("neonshift", Context.MODE_PRIVATE);
        currentBoot = android.provider.Settings.Global.getInt(c.getContentResolver(), "boot_count", -1);
    }
    static JSONObject obj(String value) {
        try { return new JSONObject(value); } catch (Exception e) { return new JSONObject(); }
    }
    static void put(JSONObject j, String key, Object value) {
        try { j.put(key, value); } catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    JSONObject config() {
        JSONObject j = obj(prefs.getString("config", "{}"));
        String[] keys = {"hourly", "tax", "consumption", "fuelPrice", "tyres", "maintenance", "depreciation"};
        double[] values = {32, 15, 8, 1.85, 0.025, 0.05, 0.10};
        for (int i = 0; i < keys.length; i++) if (!j.has(keys[i])) put(j, keys[i], values[i]);
        if (!j.has("currency")) put(j, "currency", "AUD");
        return j;
    }
    JSONObject session() { return obj(prefs.getString("session", "{}")); }
    JSONArray history() {
        try { return new JSONArray(prefs.getString("history", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }
    void saveConfig(JSONObject j) { prefs.edit().putString("config", j.toString()).commit(); }
    void save(JSONObject j) { prefs.edit().putString("session", j.toString()).commit(); }
    void finish(JSONObject session) {
        JSONArray h = history();
        JSONArray out = new JSONArray();
        out.put(session);
        for (int i = 0; i < h.length(); i++) out.put(h.opt(i));
        // Session archive and active-session reset are one durable transaction.
        prefs.edit().putString("history", out.toString()).putString("session", "{}").commit();
    }
    static long elapsed(JSONObject s) {
        long ms = s.optLong("workedMs", 0);
        if ("work".equals(s.optString("phase"))) {
            ms += TimeLedger.segment(s.optLong("segmentStart", System.currentTimeMillis()),
                s.optLong("segmentUptime", -1), s.optInt("segmentBoot", -2),
                System.currentTimeMillis(), android.os.SystemClock.elapsedRealtime(), currentBoot);
        }
        return ms;
    }
    static void startSegment(JSONObject s) {
        put(s, "segmentStart", System.currentTimeMillis());
        put(s, "segmentUptime", android.os.SystemClock.elapsedRealtime());
        put(s, "segmentBoot", currentBoot);
    }
    static JSONObject totals(JSONObject s, JSONObject cfg) {
        JSONObject c = s.optJSONObject("config");
        if (c == null) c = cfg;
        double[] a = CostEngine.calculate(elapsed(s), c.optDouble("hourly"), c.optDouble("tax"),
            s.optDouble("km", 0), c.optDouble("consumption"), c.optDouble("fuelPrice"),
            c.optDouble("tyres"), c.optDouble("maintenance"), c.optDouble("depreciation"), s.optDouble("extras", 0));
        String[] keys = {"gross", "tax", "fuel", "tyres", "maintenance", "depreciation", "extras", "carCosts", "net"};
        JSONObject j = new JSONObject();
        for (int i = 0; i < keys.length; i++) put(j, keys[i], a[i]);
        put(j, "workedMs", elapsed(s));
        return j;
    }
}
