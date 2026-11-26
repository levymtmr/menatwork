package com.neonshift.app;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;

final class ReceiptStore {
    private final File folder, originals;
    private JSONArray cached;
    ReceiptStore(Context context){folder=new File(context.getFilesDir(),"receipts");originals=new File(folder,"originals");if(!originals.isDirectory()&&!originals.mkdirs())throw new IllegalStateException("Não foi possível abrir os recibos.");}
    static String id(){return UUID.randomUUID().toString();}
    private static void validId(String id){if(!id.matches("[0-9a-f-]{36}"))throw new IllegalArgumentException("Recibo inválido.");}
    private File record(String id){validId(id);return new File(folder,id+".json");}
    File attachment(String name){if(!name.matches("[0-9a-f-]{36}\\.(pdf|img)"))throw new IllegalArgumentException("Anexo inválido.");return new File(originals,name);}
    private JSONObject read(File file)throws Exception{return new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));}
    private void write(File file,JSONObject value)throws Exception{
        byte[] data=value.toString().getBytes(StandardCharsets.UTF_8);if(data.length>300000)throw new IllegalArgumentException("Recibo muito grande.");
        File temp=File.createTempFile("receipt-write-",".tmp",folder);try{try(FileOutputStream out=new FileOutputStream(temp)){out.write(data);out.getFD().sync();}try{Files.move(temp.toPath(),file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}}finally{temp.delete();}
    }
    synchronized JSONObject choices()throws Exception{
        LinkedHashSet<String> shops=new LinkedHashSet<>(),categories=new LinkedHashSet<>();
        File file=new File(folder,"choices.json");if(file.isFile()){JSONObject saved=read(file);for(String key:new String[]{"shops","categories"}){JSONArray list=saved.optJSONArray(key);if(list!=null)for(int n=0;n<list.length();n++)(key.equals("shops")?shops:categories).add(list.getString(n));}}
        JSONArray all=summaries();for(int n=0;n<all.length();n++){JSONObject r=all.getJSONObject(n);shops.add(r.optString("merchant"));categories.add(r.optString("category"));}shops.remove("");categories.remove("");return new JSONObject().put("shops",new JSONArray(shops)).put("categories",new JSONArray(categories));
    }
    private void remember(JSONObject value)throws Exception{JSONObject saved=choices();for(String key:new String[]{"shops","categories"}){String text=value.getString(key.equals("shops")?"merchant":"category");JSONArray values=saved.getJSONArray(key);boolean found=false;for(int n=0;n<values.length();n++)if(text.equals(values.getString(n)))found=true;if(!found)values.put(text);}write(new File(folder,"choices.json"),saved);}
    synchronized JSONObject draft(){try{return read(new File(folder,"draft.json"));}catch(Exception e){return new JSONObject();}}
    synchronized void saveDraft(JSONObject value)throws Exception{String file=value.optString("file");if(!file.isEmpty()&&!attachment(file).isFile())throw new IllegalArgumentException("Anexo não encontrado.");write(new File(folder,"draft.json"),value);}
    synchronized void clearDraft()throws Exception{write(new File(folder,"draft.json"),new JSONObject());}
    synchronized JSONObject get(String id)throws Exception{File file=record(id);if(!file.isFile())throw new IllegalArgumentException("Recibo não encontrado.");return read(file);}
    private static String field(JSONObject value,String key,int max)throws Exception{String text=value.optString(key).trim();if(text.length()>max)throw new IllegalArgumentException("Texto muito longo: "+key);return text;}
    private static BigDecimal amount(String text,boolean negative)throws Exception{
        text=text.trim().replace(',','.');if(!text.matches(negative?"-?\\d+(?:\\.\\d{1,2})?":"\\d+(?:\\.\\d{1,2})?"))throw new IllegalArgumentException("Confira os valores do recibo.");BigDecimal value=new BigDecimal(text);if(value.abs().compareTo(new BigDecimal("1000000"))>0)throw new IllegalArgumentException("Valor do recibo muito alto.");return value.setScale(2,RoundingMode.HALF_UP);
    }
    synchronized JSONObject save(JSONObject value)throws Exception{
        if(!value.optBoolean("reviewed"))throw new IllegalArgumentException("Revise os dados do recibo antes de salvar.");
        String id=field(value,"id",36);if(id.isEmpty())id=id();validId(id);boolean existing=record(id).isFile();
        if(!existing&&summaries().length()>=2000)throw new IllegalArgumentException("Limite de 2.000 recibos atingido.");
        JSONObject out=new JSONObject();out.put("id",id);String merchant=field(value,"merchant",180);if(merchant.isEmpty())throw new IllegalArgumentException("Informe a loja ou fornecedor.");out.put("merchant",merchant);
        String date=field(value,"date",10);LocalDate day=LocalDate.parse(date);if(day.getYear()<1900||day.getYear()>2100)throw new IllegalArgumentException("Confira a data do recibo.");out.put("date",date);
        String currency=field(value,"currency",3);if(!Arrays.asList("AUD","USD","BRL","EUR","NZD","CAD").contains(currency))throw new IllegalArgumentException("Moeda inválida.");out.put("currency",currency);
        String category=field(value,"category",80);if(category.isEmpty())throw new IllegalArgumentException("Categoria inválida.");out.put("category",category);
        BigDecimal total=amount(value.optString("total"),false);out.put("total",total.toPlainString());
        BigDecimal percent;try{percent=new BigDecimal(value.optString("workPercent","100").replace(',','.'));}catch(Exception e){throw new IllegalArgumentException("Confira a porcentagem de uso no trabalho.");}if(percent.signum()<0||percent.compareTo(new BigDecimal("100"))>0||percent.scale()>2)throw new IllegalArgumentException("Uso no trabalho: de 0 a 100%.");out.put("workPercent",percent.stripTrailingZeros().toPlainString());out.put("businessTotal",total.multiply(percent).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP).toPlainString());
        for(String key:new String[]{"client","reference","notes","rawText","file","sourceName","sha256","litres","unitPrice","tax"})out.put(key,field(value,key,key.equals("rawText")?60000:key.equals("notes")?2000:key.equals("file")?40:key.equals("sha256")?64:key.equals("client")?160:250));
        String other=duplicate(out.optString("sha256"));if(!other.isEmpty()&&!other.equals(id))throw new IllegalArgumentException("Este recibo já está cadastrado.");
        if(!out.optString("file").isEmpty()&&!attachment(out.getString("file")).isFile())throw new IllegalArgumentException("Anexo não encontrado.");
        if(!out.optString("tax").isEmpty()){BigDecimal tax=amount(out.getString("tax"),false);if(tax.compareTo(total)>0)throw new IllegalArgumentException("Confira os valores do recibo.");out.put("tax",tax.toPlainString());}
        for(String key:new String[]{"litres","unitPrice"})if(!out.optString(key).isEmpty()){String raw=out.getString(key).replace(',','.');if(!raw.matches("\\d+(?:\\.\\d{1,4})?")||new BigDecimal(raw).compareTo(new BigDecimal("100000"))>0)throw new IllegalArgumentException("Confira litros e preço do combustível.");out.put(key,raw);}
        JSONArray items=value.optJSONArray("items"),clean=new JSONArray();if(items!=null){if(items.length()>200)throw new IllegalArgumentException("Limite de 200 itens por recibo.");for(int n=0;n<items.length();n++){JSONObject item=items.getJSONObject(n),entry=new JSONObject();String name=field(item,"name",200);if(name.isEmpty())throw new IllegalArgumentException("Informe o nome de cada item.");entry.put("name",name);entry.put("description",field(item,"description",500));entry.put("amount",amount(item.optString("amount"),true).toPlainString());entry.put("unit",field(item,"unit",30));for(String key:new String[]{"quantity","unitPrice"}){String raw=field(item,key,24).replace(',','.');if(!raw.isEmpty()&&(!raw.matches("\\d+(?:\\.\\d{1,4})?")||new BigDecimal(raw).compareTo(new BigDecimal("1000000"))>0))throw new IllegalArgumentException("Confira quantidade e preço unitário.");entry.put(key,raw);}clean.put(entry);}}
        out.put("items",clean);out.put("reimbursable",value.optBoolean("reimbursable"));out.put("reimbursed",value.optBoolean("reimbursable")&&value.optBoolean("reimbursed"));out.put("reviewed",true);out.put("reviewedAt",System.currentTimeMillis());out.put("updatedAt",System.currentTimeMillis());out.put("deleted",false);out.put("pageCount",Math.max(1,Math.min(200,value.optInt("pageCount",1))));
        if(existing)out.put("createdAt",get(id).optLong("createdAt",System.currentTimeMillis()));else out.put("createdAt",System.currentTimeMillis());
        remember(out);write(record(id),out);cached=null;clearDraft();return out;
    }
    synchronized void setDeleted(String id,boolean deleted)throws Exception{JSONObject value=get(id);if(!deleted){String other=duplicate(value.optString("sha256"));if(!other.isEmpty()&&!other.equals(id))throw new IllegalArgumentException("Este recibo já está cadastrado.");}value.put("deleted",deleted);value.put("updatedAt",System.currentTimeMillis());write(record(id),value);cached=null;}
    synchronized JSONArray summaries()throws Exception{
        if(cached==null){List<JSONObject> list=new ArrayList<>();File[] files=folder.listFiles((dir,name)->name.matches("[0-9a-f-]{36}\\.json"));if(files!=null)for(File file:files){JSONObject value=read(file),summary=new JSONObject();for(String key:new String[]{"id","merchant","date","currency","category","total","businessTotal","client","reference","reimbursable","reimbursed","deleted","updatedAt","sha256","file","sourceName"})summary.put(key,value.opt(key));JSONArray purchased=new JSONArray(),items=value.optJSONArray("items");if(items!=null)for(int i=0;i<items.length();i++){JSONObject entry=items.getJSONObject(i);purchased.put(new JSONObject().put("name",entry.optString("name")).put("amount",entry.optString("amount")));}summary.put("purchasedItems",purchased);summary.put("itemCount",purchased.length());list.add(summary);}list.sort((a,b)->Long.compare(b.optLong("updatedAt"),a.optLong("updatedAt")));cached=new JSONArray();for(JSONObject value:list)cached.put(value);}
        return new JSONArray(cached.toString());
    }
    synchronized String duplicate(String hash)throws Exception{if(hash.isEmpty())return "";JSONArray all=summaries();for(int n=0;n<all.length();n++){JSONObject r=all.getJSONObject(n);if(!r.optBoolean("deleted")&&hash.equals(r.optString("sha256")))return r.getString("id");}return "";}
}
