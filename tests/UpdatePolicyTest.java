import com.neonshift.app.UpdatePolicy;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class UpdatePolicyTest {
    static int assertions;
    static void check(boolean value) { assertions++; if (!value) throw new AssertionError(); }
    static void bad(Runnable action) { assertions++; try { action.run(); } catch (IllegalArgumentException e) { return; } throw new AssertionError("Expected rejection"); }
    static UpdatePolicy.Release release(long code, String url, String hash, long size) {
        return new UpdatePolicy.Release("com.neonshift.app",code,"1.2.0",url,hash,size,26,"New version");
    }
    public static void main(String[] args) throws Exception {
        String url="https://github.com/levymtmr/menatwork/releases/download/v1.2.0/neon-shift-1.2.0.apk";
        String hash="a".repeat(64);
        UpdatePolicy.Release r=release(3,url,hash,1000);
        check(r.newerThan(2));check(!r.newerThan(3));check(!r.newerThan(4));
        check(UpdatePolicy.allowedDownloadUrl(UpdatePolicy.METADATA_URL));
        check(UpdatePolicy.allowedDownloadUrl("https://release-assets.githubusercontent.com/asset?token=example"));
        check(!UpdatePolicy.allowedDownloadUrl("http://github.com/asset"));
        check(!UpdatePolicy.allowedDownloadUrl("https://github.com.evil.example/asset"));
        check(!UpdatePolicy.allowedDownloadUrl("https://github.com@evil.example/asset"));
        check(!UpdatePolicy.allowedDownloadUrl("https://github.com:8443/asset"));
        check(!UpdatePolicy.allowedDownloadUrl("file:///tmp/update.apk"));
        check(!UpdatePolicy.allowedDownloadUrl("https://github.com/asset#fragment"));
        bad(()->release(0,url,hash,1000));bad(()->release(3,url,"123",1000));
        bad(()->release(3,url,hash,0));bad(()->release(3,url,hash,UpdatePolicy.MAX_APK_BYTES+1));
        bad(()->release(3,url.replace("menatwork","other"),hash,1000));
        bad(()->release(3,url.replace("v1.2.0","v1.3.0"),hash,1000));
        bad(()->release(3,url+"?redirect=evil",hash,1000));
        bad(()->new UpdatePolicy.Release("com.other.app",3,"1.2.0",url,hash,1000,26,""));
        bad(()->new UpdatePolicy.Release("com.neonshift.app",3,"bad",url,hash,1000,26,""));
        File f=File.createTempFile("neon-update-test-",".bin");
        try { Files.write(f.toPath(),"abc".getBytes(StandardCharsets.US_ASCII));check(UpdatePolicy.sha256(f).equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")); }
        finally { f.delete(); }
        System.out.println("PASS: "+assertions+" update policy assertions");
    }
}
