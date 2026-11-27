package com.factory.passportkit;
public final class PhotoRules {
 private PhotoRules(){}
 public static final int WIDTH=413, HEIGHT=531, MAX_BYTES=500000;
 public enum Profile {
  KR("kr",35,45,413,531,32,36,500000,true),
  US("us",50.8,50.8,600,600,25.4,34.925,10000000,true),
  JP("jp",35,45,413,531,32,36,10000000,true),
  ID("id",30,40,354,472,0,40,10000000,false);
  public final String id;public final double mmW,mmH,minHead,maxHead;public final int width,height,maxBytes;public final boolean official;
  Profile(String id,double w,double h,int pxW,int pxH,double min,double max,int bytes,boolean official){this.id=id;mmW=w;mmH=h;width=pxW;height=pxH;minHead=min;maxHead=max;maxBytes=bytes;this.official=official;}
 }
 public static int[] crop(int w,int h,int zoom,int x,int y){return crop(Profile.KR,w,h,zoom,x,y);}
 public static int[] crop(Profile p,int w,int h,int zoom,int x,int y){
  if(w<=0||h<=0)throw new IllegalArgumentException("Invalid image");
  double z=1+clamp(zoom,0,100)/50.0;
  int ch=Math.max(1,(int)Math.floor(Math.min(h,w*(double)p.height/p.width)/z));
  int cw=Math.max(1,(int)Math.round(ch*(double)p.width/p.height));
  cw=Math.min(cw,w);ch=Math.min(ch,h);
  return new int[]{(int)Math.round((w-cw)*clamp(x,0,100)/100.0),(int)Math.round((h-ch)*clamp(y,0,100)/100.0),cw,ch};
 }
 public static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
 public static boolean resolution(int[] r){return resolution(Profile.KR,r);}
 public static boolean resolution(Profile p,int[] r){return r[2]>=p.width&&r[3]>=p.height;}
 public static double headMm(int top,int bottom){return headMm(Profile.KR,top,bottom);}
 public static double headMm(Profile p,int top,int bottom){return (bottom-top)*p.mmH/100.0;}
 public static boolean headRange(int top,int bottom){return headRange(Profile.KR,top,bottom);}
 public static boolean headRange(Profile p,int top,int bottom){double mm=headMm(p,top,bottom);return top>=0&&bottom<=100&&bottom>top&&mm>=p.minHead&&mm<=p.maxHead;}
 public static double points(double mm){return mm*72.0/25.4;}
 public static int[] layout(int w,int h){return layout(Profile.KR,w,h);}
 public static int[] layout(Profile p,int w,int h){return new int[]{Math.max(0,(int)Math.floor((w-2*points(4)+points(3))/(points(p.mmW)+points(3)))),Math.max(0,(int)Math.floor((h-2*points(4)+points(3))/(points(p.mmH)+points(3))))};}
 public static int pixels(double mm){return (int)Math.round(mm*300/25.4);}
 public static boolean backgroundAllowed(Profile p){return p==Profile.ID;}
}