package android.content;
import java.util.*;
public class Context {
 private final String filesToken=java.util.UUID.randomUUID().toString();
 public java.io.File getFilesDir(){java.io.File f=new java.io.File(System.getProperty("java.io.tmpdir"),"menatwork-pair-tests-"+filesToken);f.mkdirs();return f;}public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}
 public static final int MODE_PRIVATE=0;private final Map<String,SharedPreferences> stores=new HashMap<>();public Object getContentResolver(){return null;}
 public SharedPreferences getSharedPreferences(String name,int mode){return stores.computeIfAbsent(name,k->new Memory());}
 private static final class Memory implements SharedPreferences {
  final Map<String,Object> values=new HashMap<>();public String getString(String k,String fallback){return (String)values.getOrDefault(k,fallback);}public boolean getBoolean(String k,boolean fallback){return (Boolean)values.getOrDefault(k,fallback);}
  public Editor edit(){return new Editor(){final Map<String,Object> writes=new HashMap<>();public Editor putString(String k,String v){writes.put(k,v);return this;}public Editor putBoolean(String k,boolean v){writes.put(k,v);return this;}public Editor remove(String k){writes.put(k,null);return this;}public boolean commit(){writes.forEach((k,v)->{if(v==null)values.remove(k);else values.put(k,v);});return true;}public void apply(){commit();}};}
 }
}
