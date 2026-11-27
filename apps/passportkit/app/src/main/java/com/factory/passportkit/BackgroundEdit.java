package com.factory.passportkit;
public final class BackgroundEdit {
 private BackgroundEdit(){}
 // Conservative edge-connected, near-uniform background replacement. Never enabled for official photos.
 public static int[] replace(PhotoRules.Profile profile,int[] input,int w,int h,int color,int tolerance){
  if(!PhotoRules.backgroundAllowed(profile))throw new IllegalArgumentException("Official photo");
  if(w<1||h<1||(long)w*h!=input.length||input.length>2000000)throw new IllegalArgumentException("Image size");
  int[] out=input.clone(),q=new int[input.length];boolean[] visited=new boolean[input.length];
  int seed=input[0],limit=PhotoRules.clamp(tolerance,8,35),head=0,tail=0;
  for(int i=0;i<input.length;i++){int x=i%w,y=i/w;if(x==0||x==w-1||y==0||y==h-1){if(near(input[i],seed,limit)){visited[i]=true;q[tail++]=i;}}}
  while(head<tail){int i=q[head++];out[i]=color;int x=i%w,y=i/w;int[] next={x>0?i-1:-1,x<w-1?i+1:-1,y>0?i-w:-1,y<h-1?i+w:-1};for(int j:next)if(j>=0&&!visited[j]&&near(input[j],seed,limit)){visited[j]=true;q[tail++]=j;}}
  return out;
 }
 private static boolean near(int a,int b,int t){return Math.abs((a>>16&255)-(b>>16&255))<=t&&Math.abs((a>>8&255)-(b>>8&255))<=t&&Math.abs((a&255)-(b&255))<=t;}
}