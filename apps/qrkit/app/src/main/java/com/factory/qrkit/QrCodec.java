package com.factory.qrkit;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.common.HybridBinarizer;
import java.util.Map;
public final class QrCodec {
 private QrCodec(){}
 private static Map<EncodeHintType,Object> hints(){Map<EncodeHintType,Object> m=new java.util.EnumMap<>(EncodeHintType.class);m.put(EncodeHintType.CHARACTER_SET,"UTF-8");m.put(EncodeHintType.MARGIN,4);return m;}
 public static BitMatrix encode(String value,int size) throws WriterException {
  if(value==null||value.trim().isEmpty()||value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>1200||size<256||size>1024)throw new IllegalArgumentException("Invalid QR input");
  return new QRCodeWriter().encode(value,BarcodeFormat.QR_CODE,size,size,hints());
 }
 public static String decode(int width,int height,int[] pixels) throws NotFoundException,ChecksumException,FormatException {
  return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width,height,pixels))),java.util.Collections.singletonMap(DecodeHintType.CHARACTER_SET,"UTF-8")).getText();
 }
 public static int[] pixels(BitMatrix bits){int w=bits.getWidth(),h=bits.getHeight();int[] p=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++)p[y*w+x]=bits.get(x,y)?0xff000000:0xffffffff;return p;}
}
