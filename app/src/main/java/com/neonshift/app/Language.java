package com.neonshift.app;
import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
final class Language {
 private static Map<String,String> english;
 private static synchronized Map<String,String> dictionary(Context c){
  if(english!=null)return english;
  Map<String,String> values=new HashMap<>();
  try(InputStream in=c.getAssets().open("strings-en.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);JSONObject json=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));Iterator<String> keys=json.keys();while(keys.hasNext()){String key=keys.next();values.put(key,json.getString(key));}}
  catch(Exception e){throw new IllegalStateException("Could not load language resources.",e);}
  english=values;return english;
 }
 static String text(Context c,String value){
  if(value==null||"pt".equals(c.getSharedPreferences("neonshift",0).getString("language","en")))return value;
  Map<String,String> words=dictionary(c);if(words.containsKey(value))return words.get(value);
  List<String> keys=new ArrayList<>(words.keySet());keys.sort((a,b)->Integer.compare(b.length(),a.length()));List<String> slots=new ArrayList<>();String out=value;
  for(String key:keys)if(out.contains(key)){String marker="\u0001"+slots.size()+"\u0002";out=out.replace(key,marker);slots.add(words.get(key));}
  for(int n=0;n<slots.size();n++)out=out.replace("\u0001"+n+"\u0002",slots.get(n));return out;
 }
}
