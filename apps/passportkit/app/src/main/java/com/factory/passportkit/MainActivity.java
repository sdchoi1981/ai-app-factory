package com.factory.passportkit;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ClipData;
import android.net.Uri;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.media.ExifInterface;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import androidx.core.content.FileProvider;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.io.*;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import factory.ui.SystemInsets;

public final class MainActivity extends Activity {
 private static final int PICK=101,CAPTURE=102,SAVE_JPG=103,SAVE_PDF=104;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private LinearLayout content; private TextView status,measure,quality;
 private Preview preview; private SeekBar zoom,x,y,headTop,headBottom;
 private CheckBox confirmed; private Button check,jpg,pdf; private Spinner paper;
 private Bitmap source,cropped; private File pendingCamera;
 private FaceDetector detector; private int generation; private boolean busy,closed,ko,updating;
 private byte[] pendingExport; private int pendingRequest;
 private String tr(String k,String e){return ko?k:e;}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setPadding(0,dp(7),0,dp(7));content.addView(t,new LinearLayout.LayoutParams(-1,-2));return t;}
 private Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setMinHeight(dp(48));b.setOnClickListener(v->r.run());content.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;}
 private SeekBar slider(String s,int max,int value,boolean crop){label(s,15);SeekBar bar=new SeekBar(this);bar.setMax(max);bar.setProgress(value);bar.setContentDescription(s);content.addView(bar,new LinearLayout.LayoutParams(-1,dp(44)));bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
  public void onStartTrackingTouch(SeekBar b){}
  public void onStopTrackingTouch(SeekBar b){}
  public void onProgressChanged(SeekBar b,int v,boolean user){if(updating||!user||source==null)return;confirmed.setChecked(false);if(crop)updateCrop();else updateMeasurement();}
 });return bar;}
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);ko=getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ko");
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  File dir=new File(getCacheDir(),"capture");dir.mkdirs();
  if(saved!=null){String n=saved.getString("capture");if(n!=null&&n.matches("photo-[a-zA-Z0-9]+\\.jpg"))pendingCamera=new File(dir,n);}
  File[] stale=dir.listFiles();if(stale!=null)for(File f:stale)if(!f.equals(pendingCamera))f.delete();
  ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(16),dp(20),dp(24));scroll.addView(content,new ViewGroup.LayoutParams(-1,-2));
  label("PassportKit",32);label(tr("대한민국 여권사진 준비 도구","Korean passport photo preparation"),19);
  label(tr("흰 벽 앞에서 정면으로 촬영하세요. 최근 6개월 이내 사진, 모자 없이, 입을 다물고 눈을 뜨세요. 배경 제거·AI 보정은 제공하지 않습니다.","Use a recent frontal photo against a plain white wall, eyes open, mouth closed, no hat. No background removal or AI retouching."),15);
  button(tr("JPG 사진 불러오기","Choose JPEG photo"),this::pick);button(tr("카메라로 촬영","Take photo (camera app)"),this::camera);
  status=label(tr("사진을 선택하세요. 사진은 기기 내부에서 처리합니다.","Choose a photo. Processing stays on this device."),16);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  preview=new Preview();preview.setContentDescription(tr("편집 미리보기 및 머리 길이 표시선","Crop preview and manual head guide"));content.addView(preview,new LinearLayout.LayoutParams(-1,dp(330)));
  label(tr("1. 자르기 · 위치 조절 (35×45mm 비율)","1. Crop and position (35×45 mm)"),18);
  zoom=slider(tr("확대","Zoom"),100,0,true);x=slider(tr("가로 위치","Horizontal position"),100,50,true);y=slider(tr("세로 위치","Vertical position"),100,50,true);
  label(tr("2. 머리 길이 직접 확인","2. Check head length manually"),18);
  headTop=slider(tr("위 표시선: 머리 최상부 (머리카락 제외)","Upper guide: crown, excluding hair"),100,10,false);
  headBottom=slider(tr("아래 표시선: 턱 끝","Lower guide: chin"),100,85,false);
  measure=label("",16);confirmed=new CheckBox(this);confirmed.setText(tr("표시선을 정수리와 턱 끝에 맞췄습니다.","I aligned both guides with the crown and chin."));confirmed.setOnCheckedChangeListener((b,v)->updateMeasurement());content.addView(confirmed);
  check=button(tr("AI 얼굴 및 품질 참고 검사","AI face and quality hints"),this::inspect);
  quality=label("",15);
  label(tr("3. 파일 저장","3. Save output"),18);
  jpg=button(tr("온라인용 JPG 저장 (413×531, 500KB 이하)","Save online JPG (413×531, ≤500 KB)"),()->export(false));
  paper=new Spinner(this);paper.setContentDescription(tr("인화 용지","Print paper"));ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"4 × 6 in","5 × 7 in","A4"});paper.setAdapter(adapter);content.addView(paper,new LinearLayout.LayoutParams(-1,dp(52)));
  pdf=button(tr("인화용 PDF 저장 (35×45mm 반복 배치)","Save print PDF (35×45 mm tiles)"),()->export(true));
  button(tr("사진 지우기","Clear photo"),this::clear);
  label(tr("PDF 인쇄 시 실제 크기 100%로 설정하고 자로 확인하세요. 검사 결과는 참고용이며 여권 심사 통과를 보장하지 않습니다.","Print PDF at actual size (100%) and verify with a ruler. Checks are advisory, not approval."),14);
  label("https://www.passport.go.kr/home/kor/contents.do?menuPos=32\nhttps://www.passport.go.kr/home/kor/contents.do?menuPos=12",12);
  setContentView(scroll);SystemInsets.apply(this,content);updateMeasurement();
 }
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(pendingCamera!=null)out.putString("capture",pendingCamera.getName());}
 private void pick(){if(busy||pendingExport!=null)return;try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/jpeg");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK);}catch(RuntimeException e){message(tr("사진 선택기를 열 수 없습니다.","Cannot open photo picker."));}}
 private void camera(){if(busy||pendingCamera!=null||pendingExport!=null)return;try{
  File dir=new File(getCacheDir(),"capture");if(!dir.exists()&&!dir.mkdirs())throw new IOException();
  pendingCamera=File.createTempFile("photo-",".jpg",dir);
  Uri uri=FileProvider.getUriForFile(this,getPackageName()+".capture",pendingCamera);
  Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(MediaStore.EXTRA_OUTPUT,uri);i.setClipData(ClipData.newRawUri("capture",uri));i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
  startActivityForResult(i,CAPTURE);
 }catch(Exception e){removeCapture();message(tr("카메라 앱을 열 수 없습니다.","Cannot open camera app."));}}
 private void removeCapture(){if(pendingCamera!=null){try{revokeUriPermission(FileProvider.getUriForFile(this,getPackageName()+".capture",pendingCamera),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception ignored){}pendingCamera.delete();pendingCamera=null;}}
 @Override protected void onActivityResult(int request,int code,Intent data){super.onActivityResult(request,code,data);
  if(request==PICK&&code==RESULT_OK&&data!=null&&data.getData()!=null){load(data.getData(),false);return;}
  if(request==CAPTURE){if(code==RESULT_OK&&pendingCamera!=null&&pendingCamera.length()>0)load(Uri.fromFile(pendingCamera),true);else{removeCapture();message(tr("촬영 취소 또는 사진 없음","Capture cancelled or no photo"));}return;}
  if(request==SAVE_JPG||request==SAVE_PDF){byte[] bytes=pendingExport;int expected=pendingRequest;pendingExport=null;pendingRequest=0;if(code!=RESULT_OK)return;
   if(bytes==null||expected!=request||data==null||data.getData()==null){message(tr("사진을 다시 저장해 주세요.","Please export again."));return;}
   try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")){if(out==null)throw new IOException();out.write(bytes);message(tr("저장 완료","Saved"));}catch(Exception e){message(tr("저장하지 못했습니다.","Could not save."));}
  }
 }
 private void message(String s){status.setText(s);}
 private void load(Uri uri,boolean capture){if(busy)return;busy=true;generation++;updateMeasurement();message(tr("사진 읽는 중…","Reading photo…"));
  worker.execute(()->{Bitmap b=null;try{b=decode(uri);}catch(Exception|OutOfMemoryError ignored){}Bitmap decoded=b;runOnUiThread(()->{
   if(capture)removeCapture();if(closed){if(decoded!=null)decoded.recycle();return;}busy=false;
   if(decoded==null){message(tr("JPG 사진만 지원합니다. 파일 형식과 크기를 확인하세요.","Choose a valid JPEG of supported size."));updateMeasurement();return;}
   if(source!=null)source.recycle();source=decoded;updating=true;zoom.setProgress(0);x.setProgress(50);y.setProgress(50);headTop.setProgress(10);headBottom.setProgress(85);confirmed.setChecked(false);updating=false;updateCrop();message(tr("표시선을 정수리와 턱에 맞춘 후 확인하세요.","Align crown and chin guides, then confirm."));
  });});
 }
 private InputStream input(Uri uri)throws IOException{InputStream in=getContentResolver().openInputStream(uri);if(in==null)throw new IOException();return in;}
 private Bitmap decode(Uri uri)throws IOException{
  BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;try(InputStream in=input(uri)){BitmapFactory.decodeStream(in,null,o);}
  if(!"image/jpeg".equals(o.outMimeType)||o.outWidth<1||o.outHeight<1||o.outWidth>50000||o.outHeight>50000)throw new IOException();
  int sample=1;while(o.outWidth/sample>4096||o.outHeight/sample>4096||(long)(o.outWidth/sample)*(o.outHeight/sample)>12000000L)sample*=2;
  o.inJustDecodeBounds=false;o.inSampleSize=sample;o.inPreferredConfig=Bitmap.Config.ARGB_8888;
  Bitmap b;try(InputStream in=input(uri)){b=BitmapFactory.decodeStream(in,null,o);}if(b==null)throw new IOException();
  int orientation=1;try(InputStream in=input(uri)){orientation=new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);}catch(IOException ignored){}
  Matrix m=new Matrix();switch(orientation){case 2:m.setScale(-1,1);break;case 3:m.setRotate(180);break;case 4:m.setScale(1,-1);break;case 5:m.setRotate(90);m.postScale(-1,1);break;case 6:m.setRotate(90);break;case 7:m.setRotate(-90);m.postScale(-1,1);break;case 8:m.setRotate(-90);break;default:break;}
  if(!m.isIdentity()){Bitmap rotated=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true);if(rotated!=b)b.recycle();b=rotated;}
  return b;
 }
 private int[] cropRect(){return PhotoRules.crop(source.getWidth(),source.getHeight(),zoom.getProgress(),x.getProgress(),y.getProgress());}
 private void updateCrop(){generation++;quality.setText("");if(source==null)return;int[] r=cropRect();Bitmap next=Bitmap.createBitmap(PhotoRules.WIDTH,PhotoRules.HEIGHT,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(next);c.drawColor(Color.WHITE);c.drawBitmap(source,new Rect(r[0],r[1],r[0]+r[2],r[1]+r[3]),new Rect(0,0,PhotoRules.WIDTH,PhotoRules.HEIGHT),new Paint(Paint.FILTER_BITMAP_FLAG));if(cropped!=null)cropped.recycle();cropped=next;updateMeasurement();}
 private void updateMeasurement(){if(measure==null)return;boolean valid=source!=null&&PhotoRules.resolution(cropRect());boolean range=PhotoRules.headRange(headTop.getProgress(),headBottom.getProgress());
  measure.setText(source==null?tr("사진 없음","No photo"):String.format(Locale.US,tr("머리 길이 %.1fmm (32~36mm 권장) · 원본 자르기 %s","Head length %.1fmm (32–36mm) · crop %s"),PhotoRules.headMm(headTop.getProgress(),headBottom.getProgress()),valid?tr("해상도 충분","resolution sufficient"):tr("해상도 부족","too small")));
  boolean ready=valid&&range&&confirmed.isChecked()&&!busy;check.setEnabled(source!=null&&!busy);jpg.setEnabled(ready);pdf.setEnabled(ready);preview.invalidate();
 }
 private void inspect(){if(cropped==null||busy)return;int ticket=++generation;busy=true;updateMeasurement();quality.setText(tr("기기 내 검사 중…","Checking locally…"));
  Bitmap snapshot=cropped.copy(Bitmap.Config.ARGB_8888,false);String hint=qualityHint(snapshot);
  try{if(detector==null)detector=FaceDetection.getClient(new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).build());
   detector.process(InputImage.fromBitmap(snapshot,0)).addOnSuccessListener(faces->{if(!closed&&ticket==generation){String face=faces.size()==1?faceHint(faces.get(0)):tr("얼굴이 정확히 한 개 감지되지 않았습니다.","Exactly one face was not detected.");quality.setText(face+"\n"+hint+"\n"+tr("머리 길이·배경·표정은 직접 확인하세요. 합격 판정이 아닙니다.","Verify head size, background and expression yourself. Not an approval."));}}).addOnFailureListener(e->{if(!closed&&ticket==generation)quality.setText(tr("얼굴 검사 실패. 직접 확인하세요.","Face check unavailable. Check manually.")+"\n"+hint);}).addOnCompleteListener(t->{snapshot.recycle();if(!closed){busy=false;updateMeasurement();}});
  }catch(RuntimeException e){snapshot.recycle();busy=false;quality.setText(tr("얼굴 검사 불가","Face check unavailable"));updateMeasurement();}
 }
 private String faceHint(Face f){boolean straight=Math.abs(f.getHeadEulerAngleY())<=10&&Math.abs(f.getHeadEulerAngleZ())<=8&&Math.abs(f.getHeadEulerAngleX())<=10;return straight?tr("얼굴 1개 · 정면 추정","One face · approximately frontal"):tr("얼굴 방향을 다시 확인하세요.","Check head orientation.");}
 private String qualityHint(Bitmap b){long sum=0,edge=0,white=0,corners=0,n=0;for(int yy=4;yy<b.getHeight()-4;yy+=8)for(int xx=4;xx<b.getWidth()-4;xx+=8){int p=b.getPixel(xx,yy),r=Color.red(p),g=Color.green(p),bl=Color.blue(p),l=(r*30+g*59+bl*11)/100;int q=b.getPixel(xx+3,yy);int l2=(Color.red(q)*30+Color.green(q)*59+Color.blue(q)*11)/100;sum+=l;edge+=Math.abs(l-l2);n++;if(yy<65&&(xx<55||xx>b.getWidth()-55)){corners++;if(Math.min(r,Math.min(g,bl))>=225&&Math.max(r,Math.max(g,bl))-Math.min(r,Math.min(g,bl))<=18)white++;}}
  return (sum/n<90?tr("밝기 낮음","Low average brightness"):tr("평균 밝기 확인","Average brightness checked"))+" · "+(edge/n<3?tr("흐림 가능성","Possible blur"):tr("선명도 참고 검사","Sharpness hint"))+"\n"+(corners>0&&white*100/corners>=85?tr("상단 모서리 밝은 중성색 추정 (전체 배경 검사 아님)","Upper corners appear light/neutral (not full background check)"):tr("흰색 배경과 그림자를 직접 확인하세요.","Inspect white background and shadows."));
 }
 private void export(boolean print){if(busy||pendingExport!=null||source==null||!confirmed.isChecked()||!PhotoRules.headRange(headTop.getProgress(),headBottom.getProgress())||!PhotoRules.resolution(cropRect()))return;
  try{byte[] bytes=print?makePdf():makeJpg();if(bytes==null)throw new IOException();pendingExport=bytes;pendingRequest=print?SAVE_PDF:SAVE_JPG;
   Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType(print?"application/pdf":"image/jpeg");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,print?"passport-35x45-print.pdf":"passport-413x531.jpg");startActivityForResult(i,pendingRequest);
  }catch(Exception e){pendingExport=null;pendingRequest=0;message(tr("파일을 만들 수 없습니다.","Unable to create output."));}
 }
 private byte[] makeJpg(){for(int q=100;q>=60;q-=5){ByteArrayOutputStream out=new ByteArrayOutputStream();if(cropped.compress(Bitmap.CompressFormat.JPEG,q,out)&&out.size()<=PhotoRules.MAX_BYTES)return out.toByteArray();}return null;}
 private byte[] makePdf()throws IOException{
  int pos=paper.getSelectedItemPosition(),w=pos==0?288:pos==1?360:595,h=pos==0?432:pos==1?504:842;int[] count=PhotoRules.layout(w,h);
  float pw=(float)PhotoRules.points(35),ph=(float)PhotoRules.points(45),gap=(float)PhotoRules.points(3);
  float left=(w-count[0]*pw-(count[0]-1)*gap)/2,top=(h-count[1]*ph-(count[1]-1)*gap)/2;
  PdfDocument doc=new PdfDocument();try{PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(w,h,1).create());Canvas c=page.getCanvas();c.drawColor(Color.WHITE);Paint p=new Paint(Paint.FILTER_BITMAP_FLAG);
   for(int row=0;row<count[1];row++)for(int col=0;col<count[0];col++){float xx=left+col*(pw+gap),yy=top+row*(ph+gap);c.drawBitmap(cropped,null,new RectF(xx,yy,xx+pw,yy+ph),p);}
   doc.finishPage(page);ByteArrayOutputStream out=new ByteArrayOutputStream();doc.writeTo(out);return out.toByteArray();
  }finally{doc.close();}
 }
 private void clear(){if(busy)return;generation++;if(source!=null){source.recycle();source=null;}if(cropped!=null){cropped.recycle();cropped=null;}pendingExport=null;pendingRequest=0;confirmed.setChecked(false);quality.setText("");message(tr("사진을 지웠습니다.","Photo cleared."));updateMeasurement();}
 @Override protected void onDestroy(){closed=true;generation++;worker.shutdown();if(detector!=null)detector.close();if(source!=null)source.recycle();if(cropped!=null)cropped.recycle();pendingExport=null;if(isFinishing())removeCapture();super.onDestroy();}
 private final class Preview extends View{
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  Preview(){super(MainActivity.this);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float h=Math.min(getHeight()-dp(8),(getWidth()-dp(8))*45f/35f),w=h*35/45;if(w<=0||h<=0)return;float l=(getWidth()-w)/2,t=(getHeight()-h)/2;RectF r=new RectF(l,t,l+w,t+h);p.setColor(Color.WHITE);c.drawRect(r,p);if(cropped!=null&&!cropped.isRecycled())c.drawBitmap(cropped,null,r,p);p.setStyle(Paint.Style.STROKE);p.setColor(0xff327c90);p.setStrokeWidth(dp(2));c.drawRect(r,p);if(headTop!=null){float a=t+h*headTop.getProgress()/100f,b=t+h*headBottom.getProgress()/100f;c.drawLine(l,a,l+w,a,p);c.drawLine(l,b,l+w,b,p);}p.setStyle(Paint.Style.FILL);}
 }
}