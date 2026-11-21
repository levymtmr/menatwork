package com.neonshift.app;
import android.content.*;
import org.json.*;
import java.util.*;
final class ClientRegistry {
 private final SharedPreferences prefs;
 ClientRegistry(Context context,Store shifts,InvoiceStore invoices){
  prefs=context.getSharedPreferences("client_registry",Context.MODE_PRIVATE);
  if(!prefs.getBoolean("importedV1",false)){
   LinkedHashMap<String,JSONObject> found=new LinkedHashMap<>();
   addLegacy(found,invoices.draft().optJSONObject("profile"));JSONArray records=invoices.records();
   for(int n=0;n<records.length();n++){JSONObject r=records.optJSONObject(n);if(r==null)continue;JSONObject draft=r.optJSONObject("draft");addLegacy(found,draft==null?r:draft.optJSONObject("profile"));}
   addLegacy(found,shifts.session());JSONArray history=shifts.history();for(int n=0;n<history.length();n++)addLegacy(found,history.optJSONObject(n));
   if(!prefs.edit().putString("clients",new JSONArray(found.values()).toString()).putBoolean("importedV1",true).commit())throw new IllegalStateException("Não foi possível carregar o cadastro de clientes.");
  }
  if(!prefs.getBoolean("markenRuleV1",false)){JSONArray all=list();for(int n=0;n<all.length();n++){JSONObject c=all.optJSONObject(n);if(c!=null&&namedMarken(c.optString("client"))){Store.put(c,"payRule","marken");Store.put(c,"overtime",true);}}if(!prefs.edit().putString("clients",all.toString()).putBoolean("markenRuleV1",true).commit())throw new IllegalStateException("Não foi possível salvar o cliente.");}
 }
 static boolean namedMarken(String name){return key(name).matches(".*\\bmarken\\b.*");}
 static String rule(JSONObject c){return c.optString("payRule",c.optBoolean("overtime")?"clock":"none");}
 String rule(String name){JSONArray all=list();for(int n=0;n<all.length();n++){JSONObject c=all.optJSONObject(n);if(c!=null&&key(c.optString("client")).equals(key(name)))return rule(c);}return "none";}
 static String key(String name){return name.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
 private static void addLegacy(Map<String,JSONObject> map,JSONObject source){
  if(source==null)return;String name=source.optString("client").trim(),key=key(name);
  if(name.isEmpty()||name.length()>120||Arrays.asList("client name","nome do cliente / client name").contains(key)||map.containsKey(key))return;
  JSONObject client=new JSONObject();Store.put(client,"id",UUID.randomUUID().toString());Store.put(client,"client",name);
  for(String field:new String[]{"client_address","client_email","client_abn"})Store.put(client,field,source.optString(field));map.put(key,client);
 }
 Double hourly(String name){JSONArray all=list();for(int n=0;n<all.length();n++){JSONObject item=all.optJSONObject(n);if(item!=null&&key(item.optString("client")).equals(key(name))&&item.has("hourly"))return item.optDouble("hourly");}return null;}
 boolean overtime(String name){JSONArray all=list();for(int n=0;n<all.length();n++){JSONObject c=all.optJSONObject(n);if(c!=null&&key(c.optString("client")).equals(key(name)))return c.optBoolean("overtime");}return false;}
 JSONArray list(){try{return new JSONArray(prefs.getString("clients","[]"));}catch(Exception e){throw new IllegalStateException("Não foi possível carregar o cadastro de clientes.");}}
 void remember(JSONObject profile) throws Exception {
  String name=profile.optString("client").trim();if(name.isEmpty()||name.length()>120||Arrays.asList("client name","nome do cliente / client name").contains(key(name)))return;
  JSONArray all=list();for(int n=0;n<all.length();n++){JSONObject item=all.getJSONObject(n);if(key(item.getString("client")).equals(key(name))){
   boolean changed=false;for(String field:new String[]{"client_email","client_address","client_abn"})if(item.optString(field).isEmpty()&&!profile.optString(field).trim().isEmpty()){item.put(field,profile.optString(field).trim());changed=true;}
   if(changed)save(item);return;
  }}
  save(profile);
 }
 JSONObject save(JSONObject input) throws Exception {
  String id=input.optString("id"),name=input.optString("client").trim(),address=input.optString("client_address").trim(),email=input.optString("client_email").trim(),abn=input.optString("client_abn").trim();
  if(name.isEmpty()||name.length()>120||address.length()>350||email.length()>160||abn.length()>40)throw new IllegalArgumentException("Confira o nome e os dados do cliente.");
  if(!email.isEmpty()&&!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new IllegalArgumentException("Confira o e-mail do cliente.");
  Double hourly=null;if(input.has("hourly")&&!input.isNull("hourly")){hourly=Double.valueOf(String.valueOf(input.get("hourly")).replace(',','.'));if(!Double.isFinite(hourly)||hourly<=0||hourly>10000)throw new IllegalArgumentException("Confira o valor da hora do cliente.");}
  JSONArray all=list();JSONObject saved=null;
  for(int n=0;n<all.length();n++){JSONObject item=all.getJSONObject(n);if(item.optString("id").equals(id))saved=item;else if(key(item.getString("client")).equals(key(name)))throw new IllegalArgumentException("Já existe um cliente com esse nome. Escolha o cliente cadastrado ou edite os dados dele.");}
  if(saved==null){if(!id.isEmpty())throw new IllegalArgumentException("Cliente não encontrado.");saved=new JSONObject();saved.put("id",UUID.randomUUID().toString());all.put(saved);}
  String payRule=input.has("payRule")?input.getString("payRule"):saved.has("payRule")?saved.getString("payRule"):namedMarken(name)?"marken":input.optBoolean("overtime")?"clock":"none";if(!Arrays.asList("none","clock","marken").contains(payRule))throw new IllegalArgumentException("Regra de pagamento inválida.");saved.put("payRule",payRule);
  if(input.has("overtime"))saved.put("overtime",input.getBoolean("overtime"));
  saved.put("overtime",!"none".equals(payRule));
  if(hourly!=null)saved.put("hourly",hourly);
  saved.put("client",name);saved.put("client_address",address);saved.put("client_email",email);saved.put("client_abn",abn);
  if(!prefs.edit().putString("clients",all.toString()).commit())throw new IllegalStateException("Não foi possível salvar o cliente.");return new JSONObject(saved.toString());
 }
}
