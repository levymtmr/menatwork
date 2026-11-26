package com.neonshift.app;
import android.content.*;import android.database.Cursor;import android.graphics.*;import android.graphics.pdf.PdfRenderer;import android.media.ExifInterface;import android.net.Uri;import android.os.ParcelFileDescriptor;import android.provider.OpenableColumns;import android.util.Base64;
import org.json.*;import java.io.*;import java.security.*;import java.time.*;
final class ReceiptMedia{
 private final Context context;private final ReceiptStore receipts;
 ReceiptMedia(Context c,ReceiptStore store){context=c;receipts=store;}
 JSONObject importFile(Uri uri)throws Exception{
  String type=context.getContentResolver().getType(uri);if(type!=null&&!type.startsWith("image/")&&!type.equals("application/pdf"))throw new IllegalArgumentException("Escolha uma imagem ou PDF.");
  String name="Receipt";try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}catch(Exception ignored){}
  String id=ReceiptStore.id();File temp=receipts.attachment(id+".img");boolean keep=false;
  try{
   MessageDigest digest=MessageDigest.getInstance("SHA-256");long size=0;byte[] first=new byte[5];int head=0;
   try(InputStream in=context.getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(temp)){if(in==null)throw new IOException();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){size+=n;if(size>20*1024*1024)throw new IllegalArgumentException("Use um arquivo de até 20 MB.");for(int k=0;k<n&&head<5;k++)first[head++]=buffer[k];digest.update(buffer,0,n);out.write(buffer,0,n);}out.getFD().sync();}
   boolean pdf=head==5&&new String(first,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-");String file=id+(pdf?".pdf":".img");if(pdf){File target=receipts.attachment(file);if(!temp.renameTo(target))throw new IOException();temp=target;}
   int pages;if(pdf){try(ParcelFileDescriptor descriptor=ParcelFileDescriptor.open(temp,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(descriptor)){pages=renderer.getPageCount();if(pages<1||pages>200)throw new IllegalArgumentException("Confira as páginas do PDF.");}}
   else{BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;BitmapFactory.decodeFile(temp.getPath(),opts);if(opts.outWidth<=0||opts.outHeight<=0)throw new IllegalArgumentException("Não foi possível abrir a imagem.");pages=1;}
   StringBuilder hash=new StringBuilder();for(byte value:digest.digest())hash.append(String.format(java.util.Locale.ROOT,"%02x",value&255));String duplicate=receipts.duplicate(hash.toString());if(!duplicate.isEmpty())return new JSONObject().put("duplicateId",duplicate);
   JSONObject draft=new JSONObject().put("id",id).put("file",file).put("sourceName",name==null?"Receipt":name.substring(0,Math.min(name.length(),250))).put("sha256",hash.toString()).put("pageCount",pages).put("date",LocalDate.now(ZoneId.of("Australia/Adelaide")).toString()).put("currency","AUD").put("category","materials").put("workPercent","100").put("items",new JSONArray()).put("reviewed",false);
   receipts.saveDraft(draft);keep=true;return draft;
  }finally{if(!keep)temp.delete();}
 }
 String image(String file,int page)throws Exception{
  File source=receipts.attachment(file);if(!source.isFile())throw new IllegalArgumentException("Anexo não encontrado.");Bitmap bitmap=null;
  try{
   if(file.endsWith(".pdf")){try(ParcelFileDescriptor descriptor=ParcelFileDescriptor.open(source,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(descriptor)){if(page<0||page>=Math.min(10,renderer.getPageCount()))throw new IllegalArgumentException("Página inválida.");try(PdfRenderer.Page pdf=renderer.openPage(page)){float scale=Math.min(2.5f,2400f/Math.max(pdf.getWidth(),pdf.getHeight()));bitmap=Bitmap.createBitmap(Math.max(1,Math.round(pdf.getWidth()*scale)),Math.max(1,Math.round(pdf.getHeight()*scale)),Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);pdf.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);}}}
   else{if(page!=0)throw new IllegalArgumentException("Página inválida.");BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(source.getPath(),options);int sample=1;while(Math.max(options.outWidth,options.outHeight)/sample>2400)sample*=2;options.inJustDecodeBounds=false;options.inSampleSize=sample;bitmap=BitmapFactory.decodeFile(source.getPath(),options);if(bitmap==null)throw new IllegalArgumentException("Não foi possível abrir a imagem.");try{int orientation=new ExifInterface(source.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);Matrix matrix=new Matrix();switch(orientation){case 2:matrix.setScale(-1,1);break;case 3:matrix.setRotate(180);break;case 4:matrix.setScale(1,-1);break;case 5:matrix.setRotate(90);matrix.postScale(-1,1);break;case 6:matrix.setRotate(90);break;case 7:matrix.setRotate(270);matrix.postScale(-1,1);break;case 8:matrix.setRotate(270);break;}if(!matrix.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);if(rotated!=bitmap){bitmap.recycle();bitmap=rotated;}}}catch(IOException ignored){}}
   ByteArrayOutputStream out=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,90,out);return "data:image/jpeg;base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
  }finally{if(bitmap!=null)bitmap.recycle();}
 }
}
