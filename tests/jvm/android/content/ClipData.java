package android.content;
import android.net.Uri;import java.util.*;
public class ClipData{public static class Item{final Uri uri;public Item(Uri u){uri=u;}public Uri getUri(){return uri;}}private final List<Item> items=new ArrayList<>();public static ClipData newRawUri(String label,Uri uri){ClipData c=new ClipData();c.addItem(new Item(uri));return c;}public void addItem(Item i){items.add(i);}public int getItemCount(){return items.size();}public Item getItemAt(int n){return items.get(n);}}
