package com.neonshift.app;
import java.util.*;
public final class WorksiteBook {
 public static List<String> merge(Collection<String> first,Collection<String> second){
  LinkedHashMap<String,String> out=new LinkedHashMap<>();
  for(Collection<String> values:Arrays.asList(first,second))for(String raw:values){
   if(raw==null)continue;String address=raw.trim();if(address.isEmpty()||address.length()>1000||address.equalsIgnoreCase("Day off"))continue;
   String key=address.replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);if(!out.containsKey(key))out.put(key,address);
  }
  return new ArrayList<>(out.values());
 }
}
