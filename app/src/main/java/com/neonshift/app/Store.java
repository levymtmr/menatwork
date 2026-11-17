package com.neonshift.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.*;
import java.util.*;

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
    String language() {return "pt".equals(prefs.getString("language","en"))?"pt":"en";}
    void saveLanguage(String value) {
        if(!Arrays.asList("en","pt").contains(value))throw new IllegalArgumentException("Idioma inválido.");
        if(!prefs.edit().putString("language",value).commit())throw new IllegalStateException("Não foi possível salvar o idioma.");
    }
    String theme() {
        String value=prefs.getString("theme","retro");
        return Arrays.asList("retro","pipboy","medieval","cyberpunk").contains(value)?value:"retro";
    }
    void saveTheme(String value) {
        if(!Arrays.asList("retro","pipboy","medieval","cyberpunk").contains(value))throw new IllegalArgumentException("Tema inválido.");
        if(!prefs.edit().putString("theme",value).commit())throw new IllegalStateException("Não foi possível salvar o tema.");
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
    void save(JSONObject j) { withWorksite(prefs.edit(),j.optString("worksite")).putString("session", j.toString()).commit(); }
    private List<String> savedWorksites() {
        List<String> out=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("worksites","[]"));for(int n=0;n<a.length();n++)out.add(a.optString(n));}catch(Exception ignored){}return out;
    }
    private SharedPreferences.Editor withWorksite(SharedPreferences.Editor edit,String address) {
        List<String> known=savedWorksites();
        if(!prefs.getBoolean("worksitesImported",false)){JSONArray legacy=worksites();known=new ArrayList<>();for(int n=0;n<legacy.length();n++)known.add(legacy.optString(n));}
        return edit.putBoolean("worksitesImported",true).putString("worksites",new JSONArray(WorksiteBook.merge(Arrays.asList(address),known)).toString());
    }
    JSONArray worksites() {
        List<String> used=new ArrayList<>();used.add(session().optString("worksite"));JSONArray rows=history();
        for(int n=0;n<rows.length();n++){JSONObject row=rows.optJSONObject(n);if(row!=null&&!row.optBoolean("deleted"))used.add(row.optString("worksite"));}
        return new JSONArray(WorksiteBook.merge(savedWorksites(),used));
    }
    void finish(JSONObject session) {
        if (!session.has("workDate")) put(session,"workDate",date(session));
        JSONArray h = history();
        JSONArray out = new JSONArray();
        out.put(session);
        for (int i = 0; i < h.length(); i++) out.put(h.opt(i));
        // Session archive and active-session reset are one durable transaction.
        withWorksite(prefs.edit(),session.optString("worksite")).putString("history", out.toString()).putString("session", "{}").commit();
    }
    static String id(JSONObject record) { return record.optString("id",String.valueOf(record.optLong("startedAt"))); }
    static String date(JSONObject record) {
        return record.optString("workDate",Instant.ofEpochMilli(record.optLong("workStartedAt",record.optLong("startedAt",System.currentTimeMillis())))
            .atZone(ZoneId.systemDefault()).toLocalDate().toString());
    }
    static String fingerprint(JSONObject record) {
        return date(record)+"|"+record.optLong("workedMs")+"|"+record.optString("worksite","").trim();
    }
    JSONObject findJournal(String id) {
        JSONArray all=history();
        for(int n=0;n<all.length();n++){JSONObject r=all.optJSONObject(n);if(r!=null&&id(r).equals(id))return r;}
        return null;
    }
    JSONArray annotatedHistory() {
        JSONArray out=history();
        for(int n=0;n<out.length();n++){JSONObject r=out.optJSONObject(n);if(r!=null){put(r,"id",id(r));put(r,"workDate",date(r));put(r,"billingFingerprint",fingerprint(r));}}
        return out;
    }
    private void writeHistory(JSONArray rows) {
        if(!prefs.edit().putString("history",rows.toString()).commit())throw new IllegalStateException("Não foi possível salvar as jornadas.");
    }
    void removeJournal(String key, boolean deleted) {
        JSONArray rows=history();boolean found=false;
        for(int n=0;n<rows.length();n++){JSONObject r=rows.optJSONObject(n);if(r!=null&&id(r).equals(key)){
            put(r,"deleted",deleted);put(r,"updatedAt",System.currentTimeMillis());found=true;
        }}
        if(!found)throw new IllegalArgumentException("Jornada não encontrada.");writeHistory(rows);
    }
    void discardSession() {
        JSONObject s=session();if(!s.has("phase"))throw new IllegalArgumentException("Não há jornada em andamento.");
        put(s,"workedMs",elapsed(s));put(s,"phase","done");put(s,"endedAt",System.currentTimeMillis());put(s,"deleted",true);
        put(s,"totals",totals(s,config()));s.remove("lastLat");s.remove("lastLon");s.remove("lastFix");finish(s);
    }
    static void validateConfig(JSONObject p) throws Exception {
        String[] keys={"hourly","tax","consumption","fuelPrice","tyres","maintenance","depreciation"};
        double[] max={10000,100,100,1000,1000,1000,1000};
        for(int n=0;n<keys.length;n++)if(!CostEngine.valid(p.getDouble(keys[n]),max[n]))throw new IllegalArgumentException("Confira os valores da jornada: "+keys[n]);
        if(p.getDouble("hourly")<=0||p.getDouble("consumption")<=0)throw new IllegalArgumentException("Hora e consumo devem ser maiores que zero.");
        if(!Arrays.asList("AUD","USD","BRL","EUR","NZD","CAD").contains(p.getString("currency")))throw new IllegalArgumentException("Moeda inválida.");
    }
    JSONArray skills() {
        try{return new JSONArray(prefs.getString("skills","[]"));}catch(Exception e){return new JSONArray();}
    }
    JSONArray validSkillIds(JSONArray selected) {
        if(selected==null)selected=new JSONArray();
        if(selected.length()>100)throw new IllegalArgumentException("Selecione no máximo 100 habilidades.");
        Set<String> known=new HashSet<>(),used=new HashSet<>();JSONArray all=skills(),out=new JSONArray();
        for(int n=0;n<all.length();n++)known.add(all.optJSONObject(n).optString("id"));
        for(int n=0;n<selected.length();n++){String id=selected.optString(n);if(!known.contains(id))throw new IllegalArgumentException("Uma habilidade não está mais disponível. Atualize a tela.");if(used.add(id))out.put(id);}
        return out;
    }
    void saveSkill(JSONObject payload) throws Exception {
        JSONArray all=skills();String key=payload.optString("id",""),name=payload.optString("name").trim(),description=payload.optString("description").trim();
        if(name.isEmpty()||name.length()>60||description.length()>500)throw new IllegalArgumentException("Informe um nome de até 60 caracteres e uma descrição de até 500.");
        for(int n=0;n<all.length();n++){JSONObject s=all.getJSONObject(n);if(!s.optBoolean("deleted")&&!s.optString("id").equals(key)&&s.optString("name").equalsIgnoreCase(name))throw new IllegalArgumentException("Já existe uma habilidade com esse nome.");}
        JSONObject skill=null;
        for(int n=0;n<all.length();n++)if(all.getJSONObject(n).optString("id").equals(key))skill=all.getJSONObject(n);
        if(skill==null){if(!key.isEmpty())throw new IllegalArgumentException("Habilidade não encontrada.");skill=new JSONObject();put(skill,"id",UUID.randomUUID().toString());all.put(skill);}
        put(skill,"name",name);put(skill,"description",description);
        if(!prefs.edit().putString("skills",all.toString()).commit())throw new IllegalStateException("Não foi possível salvar a habilidade.");
    }
    void removeSkill(String key,boolean deleted) throws Exception {
        JSONArray all=skills();JSONObject found=null;
        for(int n=0;n<all.length();n++)if(all.getJSONObject(n).getString("id").equals(key))found=all.getJSONObject(n);
        if(found==null)throw new IllegalArgumentException("Habilidade não encontrada.");
        if(!deleted)for(int n=0;n<all.length();n++){JSONObject other=all.getJSONObject(n);if(!other.optBoolean("deleted")&&!other.getString("id").equals(key)&&other.getString("name").equalsIgnoreCase(found.getString("name")))throw new IllegalArgumentException("Renomeie a habilidade ativa com o mesmo nome antes de restaurar.");}
        put(found,"deleted",deleted);
        if(!prefs.edit().putString("skills",all.toString()).commit())throw new IllegalStateException("Não foi possível atualizar a habilidade.");
    }
    JSONObject saveJournal(JSONObject p) throws Exception {
        JSONArray all=history();boolean active=p.optBoolean("finishActive");String key=p.optString("id","");
        JSONObject original=active?session():findJournal(key);
        if(active&&(!original.has("phase")||!id(original).equals(key)))throw new IllegalArgumentException("A jornada em andamento mudou. Abra a correção novamente.");
        if(!active&&!key.isEmpty()&&(original==null||original.optBoolean("deleted")))throw new IllegalArgumentException("Jornada indisponível. Restaure pela lixeira antes de editar.");
        if(key.isEmpty()){
            String request=p.optString("requestId",UUID.randomUUID().toString());
            if(!request.matches("[a-zA-Z0-9_-]{1,100}"))throw new IllegalArgumentException("Identificador de rascunho inválido.");
            key="manual-"+request;original=findJournal(key); // Repeated Save is idempotent.
        }
        JSONObject record=original==null?new JSONObject():new JSONObject(original.toString());
        String day=p.getString("workDate"),mode=p.optString("entryMode","hours");
        JournalEngine.Entry e=JournalEngine.plan(day,mode,p.optString("hours","0"),p.optString("startTime"),p.optString("endTime"),p.optBoolean("nextDay"),p.optString("breakMinutes","0"),ZoneId.systemDefault(),System.currentTimeMillis());
        JSONObject cfg=p.getJSONObject("config");validateConfig(cfg);
        double km=p.getDouble("km"),extras=p.getDouble("extras");
        if(!CostEngine.valid(km,100000)||!CostEngine.valid(extras,1000000))throw new IllegalArgumentException("Confira a distância e os gastos extras.");
        String worksite=p.optString("worksite").trim(),notes=p.optString("notes").trim(),client=p.optString("client").trim(),workType=p.optString("workType").trim();
        if(client.length()>160||workType.length()>100)throw new IllegalArgumentException("Confira o cliente e o tipo de trabalho.");
        if(worksite.length()>1000||notes.length()>1000)throw new IllegalArgumentException("Local e observações aceitam até 1.000 caracteres.");
        put(record,"id",key);put(record,"workDate",day);put(record,"entryMode",mode);
        String lunch=p.optString("lunchMinutes","0");long paid=LunchEngine.paid(e.workedMs,lunch);
        put(record,"startedAt",e.startedAt);put(record,"endedAt",e.endedAt);put(record,"trackedWorkedMs",e.workedMs);put(record,"lunchMinutes",new java.math.BigDecimal(lunch.trim().replace(',','.')).stripTrailingZeros().toPlainString());put(record,"workedMs",paid);
        put(record,"client",client);put(record,"workType",workType);
        put(record,"startTime",p.optString("startTime"));put(record,"endTime",p.optString("endTime"));put(record,"nextDay",p.optBoolean("nextDay"));put(record,"breakMinutes",p.optString("breakMinutes","0"));
        put(record,"config",cfg);put(record,"km",km);put(record,"extras",extras);put(record,"worksite",worksite);put(record,"notes",notes);
        put(record,"skillIds",validSkillIds(p.optJSONArray("skillIds")));put(record,"phase","done");put(record,"deleted",false);
        put(record,"manual",true);put(record,"updatedAt",System.currentTimeMillis());put(record,"totals",totals(record,cfg));
        record.remove("lastLat");record.remove("lastLon");record.remove("lastFix");record.remove("segmentStart");record.remove("segmentUptime");
        JSONArray out=new JSONArray();out.put(record);
        for(int n=0;n<all.length();n++){JSONObject old=all.getJSONObject(n);if(!id(old).equals(key))out.put(old);}
        SharedPreferences.Editor edit=withWorksite(prefs.edit(),worksite).putString("history",out.toString());if(active)edit.putString("session","{}");
        if(!edit.commit())throw new IllegalStateException("Não foi possível salvar o dia de trabalho.");
        return record;
    }
    void finishWithLunch(String key,String phase,String minutes,long trackedMs,long endedAt) throws Exception {
        JSONObject s=session();
        if(!s.has("phase")||!id(s).equals(key)||!s.optString("phase").equals(phase))throw new IllegalArgumentException("A jornada em andamento mudou. Abra a correção novamente.");
        if(trackedMs<0||trackedMs>elapsed(s)||endedAt>System.currentTimeMillis())throw new IllegalArgumentException("Confira o tempo registrado.");
        long paid=LunchEngine.paid(trackedMs,minutes);
        put(s,"trackedWorkedMs",trackedMs);put(s,"lunchMinutes",new java.math.BigDecimal(minutes.trim().replace(',','.')).stripTrailingZeros().toPlainString());
        put(s,"workedMs",paid);put(s,"phase","done");put(s,"endedAt",endedAt);put(s,"totals",totals(s,config()));
        s.remove("lastLat");s.remove("lastLon");s.remove("lastFix");finish(s);
    }
    JSONObject progress() {
        JSONArray history=history(),skills=skills();Map<String,Long> perSkill=new HashMap<>();long confirmed=0;
        for(int n=0;n<history.length();n++){
            JSONObject r=history.optJSONObject(n);if(r==null||r.optBoolean("deleted"))continue;
            long ms=Math.max(0,r.optLong("workedMs"));confirmed+=ms;Set<String> seen=new HashSet<>();JSONArray ids=r.optJSONArray("skillIds");
            if(ids!=null)for(int i=0;i<ids.length();i++){String id=ids.optString(i);if(seen.add(id))perSkill.put(id,perSkill.getOrDefault(id,0L)+ms);}
        }
        JSONObject current=session();long provisional=elapsed(current);Set<String> selected=new HashSet<>();JSONArray ids=current.optJSONArray("skillIds");
        if(ids!=null)for(int i=0;i<ids.length();i++)selected.add(ids.optString(i));
        JSONObject out=new JSONObject();put(out,"xp",Progression.xp(confirmed));put(out,"workedMs",confirmed);put(out,"level",Progression.level(Progression.xp(confirmed)));put(out,"within",Progression.withinLevel(Progression.xp(confirmed)));put(out,"pendingXp",Progression.xp(provisional));
        JSONArray stats=new JSONArray();
        for(int n=0;n<skills.length();n++){JSONObject skill=skills.optJSONObject(n);if(skill==null)continue;JSONObject s=obj(skill.toString());long ms=perSkill.getOrDefault(skill.optString("id"),0L);long xp=skill.optBoolean("deleted")?0:Progression.xp(ms);
            put(s,"workedMs",skill.optBoolean("deleted")?0:ms);put(s,"xp",xp);put(s,"level",Progression.level(xp));put(s,"within",Progression.withinLevel(xp));put(s,"pendingXp",!skill.optBoolean("deleted")&&selected.contains(skill.optString("id"))?Progression.xp(provisional):0);stats.put(s);
        }
        put(out,"skills",stats);return out;
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
