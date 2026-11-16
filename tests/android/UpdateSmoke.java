package com.neonshift.app;

import android.app.Instrumentation;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class UpdateSmoke extends Instrumentation {
    private int checks;
    private void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    public void onCreate(Bundle b) { super.onCreate(b); start(); }
    private File copy(String name) throws Exception {
        File f = new File(getTargetContext().getCacheDir(), name);
        try (InputStream in = getContext().getAssets().open(name); FileOutputStream out = new FileOutputStream(f)) {
            byte[] b = new byte[16384]; int n; while ((n = in.read(b)) != -1) out.write(b, 0, n);
        }
        return f;
    }
    private JSONObject manifest(File f, int code, String version) throws Exception {
        return new JSONObject().put("schemaVersion",1).put("applicationId","com.neonshift.app")
            .put("versionCode",code).put("versionName",version).put("minSdk",26)
            .put("apkUrl","https://github.com/levymtmr/menatwork/releases/download/v"+version+"/neon-shift-"+version+".apk")
            .put("sha256",UpdatePolicy.sha256(f)).put("size",f.length());
    }
    public void onStart() {
        Bundle result = new Bundle(); int status = -1;
        if (!"ranchu".equals(android.os.Build.HARDWARE) && !"goldfish".equals(android.os.Build.HARDWARE)) {
            result.putString("stream","Refused: disposable emulator only.\n"); finish(0,result); return;
        }
        Context c = getTargetContext(); File future = null, wrong = null, current = null, ready = null;
        try {
            future=copy("future.apk");wrong=copy("wrong-key.apk");current=copy("current.apk");
            File folder=new File(c.getFilesDir(),"updates");folder.mkdirs();
            ready=new File(folder,"update-4.apk");
            try (InputStream in=new FileInputStream(future);OutputStream out=new FileOutputStream(ready)) {
                byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
            }
            JSONObject meta=manifest(future,4,"1.2.1");
            c.getSharedPreferences("app_updates",0).edit().putString("ready",meta.toString()).commit();
            AppUpdater updater=AppUpdater.get(c);
            check(updater.currentCode==3,"installed baseline version");
            check("ready".equals(updater.snapshot().getString("stage")),"restore validated download");
            updater.validateApk(future,AppUpdater.parse(meta));check(true,"same-key future update accepted");
            boolean badKey=false;try{updater.validateApk(wrong,AppUpdater.parse(manifest(wrong,4,"1.2.1")));}catch(IOException e){badKey=true;}
            check(badKey,"wrong signing key rejected");
            boolean old=false;try{updater.validateApk(current,AppUpdater.parse(manifest(current,3,"1.2.0")));}catch(IOException e){old=true;}
            check(old,"same version rejected");
            JSONObject corrupt=new JSONObject(meta.toString()).put("sha256","0000000000000000000000000000000000000000000000000000000000000000");
            boolean hash=false;try{updater.validateApk(future,AppUpdater.parse(corrupt));}catch(IOException e){hash=true;}
            check(hash,"corrupt hash rejected");
            JSONObject shortSize=new JSONObject(meta.toString()).put("size",future.length()-1);
            boolean size=false;try{updater.validateApk(future,AppUpdater.parse(shortSize));}catch(IOException e){size=true;}
            check(size,"wrong length rejected");
            Uri uri=Uri.parse("content://com.neonshift.app.updates/update-4.apk");
            check("application/vnd.android.package-archive".equals(c.getContentResolver().getType(uri)),"APK MIME");
            try(InputStream in=c.getContentResolver().openInputStream(uri)){byte[] head=new byte[2];check(in.read(head)==2&&"PK".equals(new String(head,StandardCharsets.US_ASCII)),"installer can read actual APK");}
            boolean write=false;try{c.getContentResolver().openFileDescriptor(uri,"w");}catch(Exception e){write=true;}
            check(write,"APK provider read only");
            boolean path=false;try{c.getContentResolver().openInputStream(Uri.parse("content://com.neonshift.app.updates/other.apk"));}catch(Exception e){path=true;}
            check(path,"only ready APK is exposed");
            result.putString("stream","PASS: "+checks+" native update checks. No installation initiated.\n");
        } catch(Throwable e){status=0;result.putString("stream","FAIL: "+e+"\n");}
        finally {
            c.getSharedPreferences("app_updates",0).edit().clear().commit();
            for(File f:new File[]{future,wrong,current,ready})if(f!=null)f.delete();
        }
        finish(status,result);
    }
}
