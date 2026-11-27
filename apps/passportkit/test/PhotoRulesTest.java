import com.factory.passportkit.PhotoRules;
public class PhotoRulesTest {
 static void ok(boolean v){if(!v)throw new AssertionError();}
 public static void main(String[] args){
  for(int w:new int[]{413,531,1000,4000})for(int h:new int[]{413,531,1800,4000})for(int zoom:new int[]{0,50,100})for(int x:new int[]{0,50,100})for(int y:new int[]{0,50,100}){int[] r=PhotoRules.crop(w,h,zoom,x,y);ok(r[0]>=0&&r[1]>=0&&r[0]+r[2]<=w&&r[1]+r[3]<=h&&r[2]>0&&r[3]>0);ok(Math.abs(r[2]-r[3]*35.0/45)<1.01);}
  ok(PhotoRules.resolution(PhotoRules.crop(413,531,0,50,50)));ok(!PhotoRules.resolution(PhotoRules.crop(200,300,0,50,50)));ok(PhotoRules.headRange(5,80));ok(!PhotoRules.headRange(10,70));ok(!PhotoRules.headRange(0,90));ok(!PhotoRules.headRange(90,10));
  ok(PhotoRules.layout(288,432)[0]==2&&PhotoRules.layout(288,432)[1]==3);
  ok(PhotoRules.layout(595,842)[0]==5&&PhotoRules.layout(595,842)[1]==6);
  ok(Math.abs(PhotoRules.points(35)*25.4/72-35)<1e-9);ok(PhotoRules.MAX_BYTES==500000);
  System.out.println("PassportKit crop, head guide, resolution and physical print layout checks passed");
 }
}