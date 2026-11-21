package android.content;
public interface SharedPreferences {
 String getString(String k,String fallback);boolean getBoolean(String k,boolean fallback);Editor edit();
 interface Editor{Editor putString(String k,String value);Editor putBoolean(String k,boolean value);Editor remove(String k);boolean commit();void apply();}
}
