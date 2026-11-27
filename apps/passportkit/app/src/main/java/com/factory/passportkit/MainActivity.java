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
 private static final int PICK=101,CAPTURE=102,SAVE_JPG=103,SAVE_PDF=104,SAVE_SHEET=105,GUIDED=106,CAMERA_PERMISSION=107;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private LinearLayout content; private TextView status,measure,quality;
 private Preview preview; private SeekBar zoom,x,y,headTop,headBottom;
 private CheckBox confirmed; private Button check,jpg,pdf,sheet,background,resetBackground; private Spinner paper,profileSelect; private TextView ruleNote; private PhotoRules.Profile profile=PhotoRules.Profile.KR; private int backgroundColor=0;
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
  label("PassportKit",32);label(tr("여권·일반 증명사진 준비","Passport and personal ID photo preparation"),19);
  profileSelect=new Spinner(this);profileSelect.setContentDescription(tr("사진 종류","Photo type"));profileSelect.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{tr("대한민국 여권","Korea passport"),tr("미국 여권 (인화용)","US passport (print)"),tr("일본 여권 (인화용)","Japan passport (print)"),tr("일반 증명사진 (3×4cm)","Personal ID photo (3×4 cm)")}));content.addView(profileSelect,new LinearLayout.LayoutParams(-1,dp(52)));ruleNote=label("",14);
  label(tr("흰 벽 앞에서 정면으로 촬영하세요. 최근 6개월 이내 사진, 모자 없이, 입을 다물고 눈을 뜨세요. 배경 제거·AI 보정은 제공하지 않습니다.","Use a recent frontal photo against a plain white wall, eyes open, mouth closed, no hat. No background removal or AI retouching."),15);
  button(tr("JPG 사진 불러오기","Choose JPEG photo"),this::pick);button(tr("카메라로 촬영","Take photo (camera app)"),this::camera);button(tr("실시간 촬영 가이드","Live camera guide"),this::guided);
  status=label(tr("사진을 선택하세요. 사진은 기기 내부에서 처리합니다.","Choose a photo. Processing stays on this device."),16);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  preview=new Preview();preview.setContentDescription(tr("편집 미리보기 및 머리 길이 표시선","Crop preview and manual head guide"));content.addView(preview,new LinearLayout.LayoutParams(-1,dp(330)));
  label(tr("1. 자르기 · 위치 조절","1. Crop and position"),18);
  zoom=slider(tr("확대","Zoom"),100,0,true);x=slider(tr("가로 위치","Horizontal position"),100,50,true);y=slider(tr("세로 위치","Vertical position"),100,50,true);
  label(tr("2. 머리 길이 직접 확인","2. Check head length manually"),18);
  headTop=slider(tr("위 표시선: 머리 최상부 (머리카락 제외)","Upper guide: crown, excluding hair"),100,10,false);
  headBottom=slider(tr("아래 표시선: 턱 끝","Lower guide: chin"),100,85,false);
  measure=label("",16);confirmed=new CheckBox(this);confirmed.setText(tr("표시선을 정수리와 턱 끝에 맞췄습니다.","I aligned both guides with the crown and chin."));confirmed.setOnCheckedChangeListener((b,v)->updateMeasurement());content.addView(confirmed);
  check=button(tr("AI 얼굴 및 품질 참고 검사","AI face and quality hints"),this::inspect);
  quality=label("",15);
  background=button(tr("일반 증명사진 배경색 변경","Personal ID background color"),this::chooseBackground);resetBackground=button(tr("배경 편집 되돌리기","Undo background edit"),()->{backgroundColor=0;updateCrop();});
  label(tr("3. 파일 저장","3. Save output"),18);
  jpg=button(tr("JPG 저장","Save JPG"),()->export(SAVE_JPG));
  paper=new Spinner(this);paper.setContentDescription(tr("인화 용지","Print paper"));ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"4 × 6 in","5 × 7 in","A4"});paper.setAdapter(adapter);content.addView(paper,new LinearLayout.LayoutParams(-1,dp(52)));
  pdf=button(tr("인화용 PDF 저장","Save print PDF"),()->export(SAVE_PDF));
  sheet=button(tr("인화용 JPG 저장 (300dpi 기준)","Save print JPG (300 dpi layout)"),()->export(SAVE_SHEET));
  button(tr("사진 지우기","Clear photo"),this::clear);
  button(tr("Pro 기능 안내","Pro features"),()->new android.app.AlertDialog.Builder(this).setTitle("PassportKit Pro").setMessage(tr("현재 모든 기능은 테스트용 무료입니다. 광고 제거 및 인화 기능 유료 상품은 Google Play 등록 후 제공 예정입니다. 결제나 광고는 활성화되지 않았습니다.","All features are free for testing. Paid products and ad removal require Play setup. No purchases or ads are enabled.")).setPositiveButton(android.R.string.ok,null).show());
  label(tr("영국·캐나다 여권: 영국 온라인 신청은 원본 사진 지침을, 캐나다는 상업 사진관 촬영 요건을 확인하세요. 이 앱은 해당 국가의 제출용 파일을 생성하지 않습니다.","UK and Canada: follow official original-photo and commercial-photographer requirements. No submission export for these countries."),14);
  label(tr("PDF 인쇄 시 실제 크기 100%로 설정하고 자로 확인하세요. 검사 결과는 참고용이며 여권 심사 통과를 보장하지 않습니다.","Print PDF at actual size (100%) and verify with a ruler. Checks are advisory, not approval."),14);
  label("https://www.passport.go.kr/home/kor/contents.do?menuPos=32\nhttps://www.passport.go.kr/home/kor/contents.do?menuPos=12",12);
  setContentView(scroll);SystemInsets.apply(this,content);profileSelect.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int i,long id){profile=PhotoRules.Profile.values()[i];backgroundColor=0;confirmed.setChecked(false);updateProfile();if(source!=null)updateCrop();else updateMeasurement();}});updateProfile();updateMeasurement();
 }
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(pendingCamera!=null)out.putString("capture",pendingCamera.getName());}
 private void pick(){if(busy||pendingExport!=null)return;try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/jpeg");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK);}catch(RuntimeException e){message(tr("사진 선택기를 열 수 없습니다.","Cannot open photo picker."));}}
 private void guided(){if(busy||pendingCamera!=null||pendingExport!=null)return;if(checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.CAMERA},CAMERA_PERMISSION);return;}launchGuide();}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==CAMERA_PERMISSION){if(g.length>0&&g[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)launchGuide();else message(tr("카메라 권한이 필요합니다.","Camera permission required."));}}
 private void launchGuide(){try{File dir=new File(getCacheDir(),"capture");dir.mkdirs();pendingCamera=File.createTempFile("photo-",".jpg",dir);Intent i=new Intent(this,GuideActivity.class);i.putExtra("name",pendingCamera.getName());i.putExtra("profile",profile.ordinal());startActivityForResult(i,GUIDED);}catch(Exception e){removeCapture();message(tr("촬영 화면을 열 수 없습니다.","Unable to open live camera."));}}
 private void camera(){if(checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){guided();return;}if(busy||pendingCamera!=null||pendingExport!=null)return;try{
  File dir=new File(getCacheDir(),"capture");if(!dir.exists()&&!dir.mkdirs())throw new IOException();
  pendingCamera=File.createTempFile("photo-",".jpg",dir);
  Uri uri=FileProvider.getUriForFile(this,getPackageName()+".capture",pendingCamera);
  Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(MediaStore.EXTRA_OUTPUT,uri);i.setClipData(ClipData.newRawUri("capture",uri));i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
  startActivityForResult(i,CAPTURE);
 }catch(Exception e){removeCapture();message(tr("카메라 앱을 열 수 없습니다.","Cannot open camera app."));}}
 private void removeCapture(){if(pendingCamera!=null){try{revokeUriPermission(FileProvider.getUriForFile(this,getPackageName()+".capture",pendingCamera),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception ignored){}pendingCamera.delete();pendingCamera=null;}}
 @Override protected void onActivityResult(int request,int code,Intent data){super.onActivityResult(request,code,data);
  if(request==PICK&&code==RESULT_OK&&data!=null&&data.getData()!=null){load(data.getData(),false);return;}
  if(request==CAPTURE||request==GUIDED){if(code==RESULT_OK&&pendingCamera!=null&&pendingCamera.length()>0)load(Uri.fromFile(pendingCamera),true);else{removeCapture();message(tr("촬영 취소 또는 사진 없음","Capture cancelled or no photo"));}return;}
  if(request==SAVE_JPG||request==SAVE_PDF||request==SAVE_SHEET){byte[] bytes=pendingExport;int expected=pendingRequest;pendingExport=null;pendingRequest=0;if(code!=RESULT_OK)return;
   if(bytes==null||expected!=request||data==null||data.getData()==null){message(tr("사진을 다시 저장해 주세요.","Please export again."));return;}
   try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")){if(out==null)throw new IOException();out.write(bytes);message(tr("저장 완료","Saved"));}catch(Exception e){message(tr("저장하지 못했습니다.","Could not save."));}
  }
 }
 private void message(String s){status.setText(s);}
 private void load(Uri uri,boolean capture){if(busy)return;busy=true;generation++;updateMeasurement();message(tr("사진 읽는 중…","Reading photo…"));
  worker.execute(()->{Bitmap b=null;try{b=decode(uri);}catch(Exception|OutOfMemoryError ignored){}Bitmap decoded=b;runOnUiThread(()->{
   if(capture)removeCapture();if(closed){if(decoded!=null)decoded.recycle();return;}busy=false;
   if(decoded==null){message(tr("JPG 사진만 지원합니다. 파일 형식과 크기를 확인하세요.","Choose a valid JPEG of supported size."));updateMeasurement();return;}
   if(source!=null)source.recycle();source=decoded;backgroundColor=0;updating=true;zoom.setProgress(0);x.setProgress(50);y.setProgress(50);headTop.setProgress(10);headBottom.setProgress(85);confirmed.setChecked(false);updating=false;updateCrop();message(tr("표시선을 정수리와 턱에 맞춘 후 확인하세요.","Align crown and chin guides, then confirm."));
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
 private int[] cropRect(){return PhotoRules.crop(profile,source.getWidth(),source.getHeight(),zoom.getProgress(),x.getProgress(),y.getProgress());}
 private void updateCrop(){generation++;quality.setText("");if(source==null)return;int[] r=cropRect();Bitmap next=Bitmap.createBitmap(profile.width,profile.height,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(next);c.drawColor(Color.WHITE);c.drawBitmap(source,new Rect(r[0],r[1],r[0]+r[2],r[1]+r[3]),new Rect(0,0,profile.width,profile.height),new Paint(Paint.FILTER_BITMAP_FLAG));if(backgroundColor!=0&&PhotoRules.backgroundAllowed(profile)){int[] px=new int[profile.width*profile.height];next.getPixels(px,0,profile.width,0,0,profile.width,profile.height);int[] changed=BackgroundEdit.replace(profile,px,profile.width,profile.height,backgroundColor,20);next.setPixels(changed,0,profile.width,0,0,profile.width,profile.height);}if(cropped!=null)cropped.recycle();cropped=next;updateMeasurement();}
 private void updateProfile(){ruleNote.setText(profile==PhotoRules.Profile.KR?tr("35×45mm · 413×531px · JPG 500KB 이하. 배경 변경 금지.","35×45 mm; 413×531 px; JPG ≤500 KB. No background edits."):profile==PhotoRules.Profile.US?tr("미국 인화용 2×2인치, 머리 25.4~34.9mm (머리카락 포함). 온라인 신청용 원본은 별도 지침 확인.","US print 2×2 in; head 25.4–34.9 mm including hair. Check separate online original-photo rules."):profile==PhotoRules.Profile.JP?tr("일본 인화용 35×45mm. 머리 32~36mm. 온라인 규격은 별도 확인.","Japan print 35×45 mm; head 32–36 mm. Check online rules separately."):tr("일반용 30×40mm. 배경 편집 가능, 공식 신분증 제출용 아님.","Personal 30×40 mm. Background edits allowed; not for official ID submission."));background.setVisibility(profile.official?View.GONE:View.VISIBLE);resetBackground.setVisibility(profile.official?View.GONE:View.VISIBLE);}
 private void chooseBackground(){if(source==null||busy||!PhotoRules.backgroundAllowed(profile))return;new android.app.AlertDialog.Builder(this).setTitle(tr("일반 사진 배경 (단색 배경만)","Personal photo background (uniform only)")).setItems(new String[]{tr("흰색","White"),tr("하늘색","Light blue"),tr("회색","Gray")},(d,i)->{if(!PhotoRules.backgroundAllowed(profile))return;backgroundColor=new int[]{Color.WHITE,0xffc9e2f4,0xffd9d9d9}[i];confirmed.setChecked(false);updateCrop();message(tr("단색 배경 편집 결과를 확인하세요. 공식 여권용에는 사용할 수 없습니다.","Inspect the uniform-background edit. Not for passport use."));}).show();}
 private void updateMeasurement(){if(measure==null)return;boolean valid=source!=null&&PhotoRules.resolution(profile,cropRect());boolean range=PhotoRules.headRange(profile,headTop.getProgress(),headBottom.getProgress());
  measure.setText(source==null?tr("사진 없음","No photo"):String.format(Locale.US,tr("머리 길이 %.1fmm · 원본 자르기 %s","Head length %.1fmm · crop %s"),PhotoRules.headMm(profile,headTop.getProgress(),headBottom.getProgress()),valid?tr("해상도 충분","resolution sufficient"):tr("해상도 부족","too small")));
  boolean ready=valid&&range&&confirmed.isChecked()&&!busy;check.setEnabled(source!=null&&!busy);jpg.setEnabled(ready);pdf.setEnabled(ready);sheet.setEnabled(ready);background.setEnabled(source!=null&&!busy);resetBackground.setEnabled(source!=null&&!busy);preview.invalidate();
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
 private void export(int type){if(busy||pendingExport!=null||source==null||!confirmed.isChecked()||!PhotoRules.headRange(profile,headTop.getProgress(),headBottom.getProgress())||!PhotoRules.resolution(profile,cropRect()))return;
  try{byte[] bytes=type==SAVE_PDF?makePdf():type==SAVE_SHEET?makeSheet():makeJpg();if(bytes==null)throw new IOException();pendingExport=bytes;pendingRequest=type;
   Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType(type==SAVE_PDF?"application/pdf":"image/jpeg");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,profile.id+(type==SAVE_PDF?"-print.pdf":type==SAVE_SHEET?"-sheet.jpg":"-photo.jpg"));startActivityForResult(i,pendingRequest);
  }catch(Exception e){pendingExport=null;pendingRequest=0;message(tr("파일을 만들 수 없습니다.","Unable to create output."));}
 }
 private byte[] makeJpg(){for(int q=100;q>=60;q-=5){ByteArrayOutputStream out=new ByteArrayOutputStream();if(cropped.compress(Bitmap.CompressFormat.JPEG,q,out)&&out.size()<=profile.maxBytes)return out.toByteArray();}return null;}
 private byte[] makePdf()throws IOException{
  int pos=paper.getSelectedItemPosition(),w=pos==0?288:pos==1?360:595,h=pos==0?432:pos==1?504:842;int[] count=PhotoRules.layout(profile,w,h);
  float pw=(float)PhotoRules.points(profile.mmW),ph=(float)PhotoRules.points(profile.mmH),gap=(float)PhotoRules.points(3);
  float left=(w-count[0]*pw-(count[0]-1)*gap)/2,top=(h-count[1]*ph-(count[1]-1)*gap)/2;
  PdfDocument doc=new PdfDocument();try{PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(w,h,1).create());Canvas c=page.getCanvas();c.drawColor(Color.WHITE);Paint p=new Paint(Paint.FILTER_BITMAP_FLAG);
   for(int row=0;row<count[1];row++)for(int col=0;col<count[0];col++){float xx=left+col*(pw+gap),yy=top+row*(ph+gap);c.drawBitmap(cropped,null,new RectF(xx,yy,xx+pw,yy+ph),p);}
   doc.finishPage(page);ByteArrayOutputStream out=new ByteArrayOutputStream();doc.writeTo(out);return out.toByteArray();
  }finally{doc.close();}
 }
 private byte[] makeSheet()throws IOException{int pos=paper.getSelectedItemPosition(),w=pos==0?1200:pos==1?1500:2480,h=pos==0?1800:pos==1?2100:3508;int pw=PhotoRules.pixels(profile.mmW),ph=PhotoRules.pixels(profile.mmH),gap=PhotoRules.pixels(3),margin=PhotoRules.pixels(4),cols=(w-2*margin+gap)/(pw+gap),rows=(h-2*margin+gap)/(ph+gap);Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);try{Canvas c=new Canvas(bitmap);c.drawColor(Color.WHITE);int left=(w-cols*pw-(cols-1)*gap)/2,top=(h-rows*ph-(rows-1)*gap)/2;Paint p=new Paint(Paint.FILTER_BITMAP_FLAG);for(int y=0;y<rows;y++)for(int x=0;x<cols;x++){int xx=left+x*(pw+gap),yy=top+y*(ph+gap);c.drawBitmap(cropped,null,new Rect(xx,yy,xx+pw,yy+ph),p);}ByteArrayOutputStream out=new ByteArrayOutputStream();if(!bitmap.compress(Bitmap.CompressFormat.JPEG,95,out))throw new IOException();return out.toByteArray();}finally{bitmap.recycle();}}
 private void clear(){if(busy)return;generation++;if(source!=null){source.recycle();source=null;}if(cropped!=null){cropped.recycle();cropped=null;}pendingExport=null;pendingRequest=0;confirmed.setChecked(false);quality.setText("");message(tr("사진을 지웠습니다.","Photo cleared."));updateMeasurement();}
 @Override protected void onDestroy(){closed=true;generation++;worker.shutdown();if(detector!=null)detector.close();if(source!=null)source.recycle();if(cropped!=null)cropped.recycle();pendingExport=null;if(isFinishing())removeCapture();super.onDestroy();}
 private final class Preview extends View{
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  Preview(){super(MainActivity.this);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float ratio=(float)(profile.mmW/profile.mmH),h=Math.min(getHeight()-dp(8),(getWidth()-dp(8))/ratio),w=h*ratio;if(w<=0||h<=0)return;float l=(getWidth()-w)/2,t=(getHeight()-h)/2;RectF r=new RectF(l,t,l+w,t+h);p.setColor(Color.WHITE);c.drawRect(r,p);if(cropped!=null&&!cropped.isRecycled())c.drawBitmap(cropped,null,r,p);p.setStyle(Paint.Style.STROKE);p.setColor(0xff327c90);p.setStrokeWidth(dp(2));c.drawRect(r,p);if(headTop!=null){float a=t+h*headTop.getProgress()/100f,b=t+h*headBottom.getProgress()/100f;c.drawLine(l,a,l+w,a,p);c.drawLine(l,b,l+w,b,p);}p.setStyle(Paint.Style.FILL);}
 }
}