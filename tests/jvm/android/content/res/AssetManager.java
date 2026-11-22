package android.content.res;import java.io.*;public class AssetManager{public InputStream open(String name)throws IOException{return new FileInputStream(new File("app/src/main/assets",name));}}
