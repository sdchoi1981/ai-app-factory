package com.factory.passportkit;
public final class PhotoRules {
 private PhotoRules(){}
 public static final int WIDTH=413, HEIGHT=531, MAX_BYTES=500000;
 public static final double HEAD_MIN=32.0,HEAD_MAX=36.0;
 public static int[] crop(int w,int h,int zoom,int x,int y){
  if(w<=0||h<=0)throw new IllegalArgumentException("Invalid image");
  double z=1+clamp(zoom,0,100)/50.0;
  int ch=Math.max(1,(int)Math.floor(Math.min(h,w*45.0/35)/z));
  int cw=Math.max(1,(int)Math.floor(ch*35.0/45));
  cw=Math.min(cw,w);ch=Math.min(ch,h);
  int left=(int)Math.round((w-cw)*clamp(x,0,100)/100.0);
  int top=(int)Math.round((h-ch)*clamp(y,0,100)/100.0);
  return new int[]{left,top,cw,ch};
 }
 public static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
 public static boolean resolution(int[] crop){return crop[2]>=WIDTH&&crop[3]>=HEIGHT;}
 public static double headMm(int top,int bottom){return (bottom-top)*45.0/100.0;}
 public static boolean headRange(int top,int bottom){double mm=headMm(top,bottom);return top>=0&&bottom<=100&&mm>=HEAD_MIN&&mm<=HEAD_MAX;}
 public static double points(double mm){return mm*72.0/25.4;}
 public static int[] layout(int w,int h){int columns=(int)Math.floor((w-2*points(4)+points(3))/(points(35)+points(3)));int rows=(int)Math.floor((h-2*points(4)+points(3))/(points(45)+points(3)));return new int[]{Math.max(0,columns),Math.max(0,rows)};}
}