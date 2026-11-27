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
import factory.ui.KitUi;

public final class MainActivity extends Activity {
 private static final int PICK=101,CAPTURE=102,SAVE_JPG=103,SAVE_PDF=104,SAVE_SHEET=105,GUIDED=106,CAMERA_PERMISSION=107;
 private static final long SPLASH_MIN_MS=1500L;
 private static boolean firstActivityCreated;
 private View splashGateView;
 private android.view.ViewTreeObserver.OnPreDrawListener splashGate;
 private final android.os.Handler splashHandler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable releaseSplash=this::releaseSplashGate;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private LinearLayout content, page, previewPanel, sourcePanel, editPanel, exportPanel; private ScrollView screen; private KitUi ui; private Button[] steps; private int stage; private PrintPreview printPreview; private TextView status,measure,quality;
 private Preview preview; private SeekBar zoom,x,y,headTop,headBottom;
 private CheckBox confirmed; private Button check,jpg,pdf,sheet,background,resetBackground; private Spinner paper,profileSelect; private TextView ruleNote; private PhotoRules.Profile profile=PhotoRules.Profile.KR; private int backgroundColor=0;
 private Bitmap source,cropped; private File pendingCamera;
 private FaceDetector detector; private int generation; private boolean busy,closed,updating;
 private byte[] pendingExport; private int pendingRequest;
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView label(String s,int size){TextView t=ui.text(s,size,size>=18);content.addView(t,ui.space(-2,0,2));return t;}
 private Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);ui.button(b,false);b.setOnClickListener(v->r.run());content.addView(b,ui.space(-2,3,4));return b;}
 private void showStage(int next){if(next!=0&&source==null)next=0;stage=next;sourcePanel.setVisibility(stage==0?View.VISIBLE:View.GONE);previewPanel.setVisibility(stage==1?View.VISIBLE:View.GONE);editPanel.setVisibility(stage==1?View.VISIBLE:View.GONE);exportPanel.setVisibility(stage==2?View.VISIBLE:View.GONE);for(int i=0;i<3;i++){ui.button(steps[i],i==stage);steps[i].setSelected(i==stage);steps[i].setEnabled(i==0||source!=null);}if(printPreview!=null)printPreview.invalidate();screen.post(()->screen.scrollTo(0,0));}
 private SeekBar slider(String s,int max,int value,boolean crop){label(s,15);SeekBar bar=new SeekBar(this);bar.setMax(max);bar.setProgress(value);bar.setContentDescription(s);bar.setProgressTintList(android.content.res.ColorStateList.valueOf(ui.accent));bar.setThumbTintList(android.content.res.ColorStateList.valueOf(ui.accent));content.addView(bar,new LinearLayout.LayoutParams(-1,dp(52)));bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
  public void onStartTrackingTouch(SeekBar b){}
  public void onStopTrackingTouch(SeekBar b){}
  public void onProgressChanged(SeekBar b,int v,boolean user){if(updating||!user||source==null)return;confirmed.setChecked(false);if(crop)updateCrop();else updateMeasurement();}
 });return bar;}
 @Override public void onCreate(Bundle saved){
  setTheme(R.style.FactoryTheme);
  super.onCreate(saved);
  final long startedAt=android.os.SystemClock.uptimeMillis();
  final boolean holdSplash=!firstActivityCreated&&saved==null;
  firstActivityCreated=true;
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  File dir=new File(getCacheDir(),"capture");dir.mkdirs();
  if(saved!=null){String n=saved.getString("capture");if(n!=null&&n.matches("photo-[a-zA-Z0-9]+\\.jpg"))pendingCamera=new File(dir,n);}
  File[] stale=dir.listFiles();if(stale!=null)for(File f:stale)if(!f.equals(pendingCamera))f.delete();
  ui=new KitUi(this,0xff3155b7,0xffa9bfff);
  ScrollView scroll=new ScrollView(this);screen=scroll;scroll.setFillViewport(true);scroll.setBackgroundColor(ui.background);
  page=ui.column();android.widget.FrameLayout frame=new android.widget.FrameLayout(this);frame.addView(page,new android.widget.FrameLayout.LayoutParams(-1,-2,android.view.Gravity.TOP|android.view.Gravity.CENTER_HORIZONTAL));scroll.addView(frame,new ViewGroup.LayoutParams(-1,-2));content=page;
  label("PassportKit",32);label(getString(R.string.pk_001),16);
  LinearLayout navigation=new LinearLayout(this);navigation.setOrientation(LinearLayout.HORIZONTAL);steps=new Button[3];int[] stepIds={R.string.stage_source,R.string.stage_edit,R.string.stage_save};
  for(int i=0;i<3;i++){final int step=i;Button b=new Button(this);b.setText(getString(stepIds[i]));ui.button(b,i==0);b.setOnClickListener(v->showStage(step));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(2),0,dp(2),0);navigation.addView(b,lp);steps[i]=b;}page.addView(navigation,ui.space(-2,4,8));
  status=label(getString(R.string.pk_011),15);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  sourcePanel=ui.card(page);content=sourcePanel;
  label(getString(R.string.pk_002),22);
  profileSelect=new Spinner(this);profileSelect.setContentDescription(getString(R.string.pk_002));profileSelect.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{getString(R.string.pk_003),getString(R.string.pk_004),getString(R.string.pk_005),getString(R.string.pk_006)}));ui.field(profileSelect);content.addView(profileSelect,ui.space(dp(56),4,4));ruleNote=label("",14);
  View intro=new View(this){final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);@Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setColor(ui.tint);c.drawRect(0,0,w,h,p);float x=w/2,y=h/2,r=Math.min(w,h)*.35f;p.setColor(ui.accent);c.drawRoundRect(x-r*1.2f,y-r*.8f,x+r*.25f,y+r*.9f,dp(8),dp(8),p);p.setColor(ui.surface);c.drawRoundRect(x-r*.25f,y-r*.65f,x+r*1.3f,y+r*.65f,dp(9),dp(9),p);p.setColor(ui.accent);c.drawCircle(x+r*.18f,y-r*.19f,r*.19f,p);c.drawRoundRect(x-r*.09f,y+r*.06f,x+r*.46f,y+r*.42f,dp(5),dp(5),p);p.setStrokeWidth(dp(2));c.drawLine(x+r*.60f,y-r*.15f,x+r*1.04f,y-r*.15f,p);c.drawLine(x+r*.60f,y+r*.12f,x+r*1.04f,y+r*.12f,p);}};
  ui.preview(intro);content.addView(intro,ui.space(dp(136),6,8));label(getString(R.string.pk_007),14);
  ui.button(button(getString(R.string.pk_008),this::pick),true);button(getString(R.string.pk_010),this::guided);button(getString(R.string.pk_009),this::camera);
  previewPanel=ui.card(page);content=previewPanel;preview=new Preview();preview.setContentDescription(getString(R.string.pk_012));ui.preview(preview);content.addView(preview,ui.space(dp(330),0,0));
  editPanel=ui.card(page);content=editPanel;label(getString(R.string.pk_013),20);
  zoom=slider(getString(R.string.pk_014),100,0,true);x=slider(getString(R.string.pk_015),100,50,true);y=slider(getString(R.string.pk_016),100,50,true);
  label(getString(R.string.pk_017),20);headTop=slider(getString(R.string.pk_018),100,10,false);headBottom=slider(getString(R.string.pk_019),100,85,false);
  measure=label("",16);confirmed=new CheckBox(this);confirmed.setText(getString(R.string.pk_020));confirmed.setMinHeight(dp(52));confirmed.setOnCheckedChangeListener((b,v)->updateMeasurement());content.addView(confirmed);
  check=button(getString(R.string.pk_021),this::inspect);quality=label("",15);
  background=button(getString(R.string.pk_022),this::chooseBackground);resetBackground=button(getString(R.string.pk_023),()->{backgroundColor=0;updateCrop();});
  ui.button(button(getString(R.string.stage_save),()->showStage(2)),true);
  exportPanel=ui.card(page);content=exportPanel;label(getString(R.string.pk_024),22);
  label(getString(R.string.stage_save_hint),15);printPreview=new PrintPreview();printPreview.setContentDescription(getString(R.string.stage_paper_preview));ui.preview(printPreview);content.addView(printPreview,ui.space(dp(250),4,8));
  jpg=button(getString(R.string.pk_025),()->export(SAVE_JPG));ui.button(jpg,true);
  label(getString(R.string.pk_026),15);paper=new Spinner(this);paper.setContentDescription(getString(R.string.pk_026));ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"4 × 6 in","5 × 7 in","A4"});paper.setAdapter(adapter);ui.field(paper);content.addView(paper,ui.space(dp(52),4,6));paper.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int i,long id){printPreview.invalidate();}});
  pdf=button(getString(R.string.pk_027),()->export(SAVE_PDF));sheet=button(getString(R.string.pk_028),()->export(SAVE_SHEET));button(getString(R.string.stage_edit),()->showStage(1));label(getString(R.string.pk_033),14);
  content=page;button(getString(R.string.pk_029),this::clear);button(getString(R.string.pk_030),()->new android.app.AlertDialog.Builder(this).setTitle("PassportKit Pro").setMessage(getString(R.string.pk_031)).setPositiveButton(android.R.string.ok,null).show());
  label(getString(R.string.pk_032),14);label("https://www.passport.go.kr/home/kor/contents.do?menuPos=32\nhttps://www.passport.go.kr/home/kor/contents.do?menuPos=12",12);
  showStage(0);
  if(holdSplash)holdSplashUntil(scroll,startedAt+SPLASH_MIN_MS);
  setContentView(scroll);SystemInsets.apply(this,scroll);profileSelect.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int i,long id){profile=PhotoRules.Profile.values()[i];backgroundColor=0;confirmed.setChecked(false);updateProfile();if(source!=null)updateCrop();else updateMeasurement();}});updateProfile();updateMeasurement();
 }
 private void holdSplashUntil(View view,long deadline){
  splashGateView=view;
  splashGate=()->{
   if(android.os.SystemClock.uptimeMillis()<deadline)return false;
   releaseSplashGate();
   return true;
  };
  view.getViewTreeObserver().addOnPreDrawListener(splashGate);
  splashHandler.postAtTime(releaseSplash,deadline);
 }
 private void releaseSplashGate(){
  splashHandler.removeCallbacks(releaseSplash);
  if(splashGateView!=null){
   android.view.ViewTreeObserver observer=splashGateView.getViewTreeObserver();
   if(observer.isAlive()&&splashGate!=null)observer.removeOnPreDrawListener(splashGate);
   splashGateView.invalidate();
  }
  splashGateView=null;splashGate=null;
 }
 @Override protected void onPause(){releaseSplashGate();super.onPause();}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(pendingCamera!=null)out.putString("capture",pendingCamera.getName());}
 private void pick(){if(busy||pendingExport!=null)return;try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/jpeg");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK);}catch(RuntimeException e){message(getString(R.string.pk_034));}}
 private void guided(){if(busy||pendingCamera!=null||pendingExport!=null)return;if(checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.CAMERA},CAMERA_PERMISSION);return;}launchGuide();}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==CAMERA_PERMISSION){if(g.length>0&&g[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)launchGuide();else message(getString(R.string.pk_035));}}
 private void launchGuide(){try{File dir=new File(getCacheDir(),"capture");dir.mkdirs();pendingCamera=File.createTempFile("photo-",".jpg",dir);Intent i=new Intent(this,GuideActivity.class);i.putExtra("name",pendingCamera.getName());i.putExtra("profile",profile.ordinal());startActivityForResult(i,GUIDED);}catch(Exception e){removeCapture();message(getString(R.string.pk_036));}}
 private void camera(){if(checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){guided();return;}if(busy||pendingCamera!=null||pendingExport!=null)return;try{
  File dir=new File(getCacheDir(),"capture");if(!dir.exists()&&!dir.mkdirs())throw new IOException();
  pendingCamera=File.createTempFile("photo-",".jpg",dir);
  Uri uri=FileProvider.getUriForFile(this,getPackageName()+".capture",pendingCamera);
  Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(MediaStore.EXTRA_OUTPUT,uri);i.setClipData(ClipData.newRawUri("capture",uri));i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
  startActivityForResult(i,CAPTURE);
 }catch(Exception e){removeCapture();message(getString(R.string.pk_037));}}
 private void removeCapture(){if(pendingCamera!=null){try{revokeUriPermission(FileProvider.getUriForFile(this,getPackageName()+".capture",pendingCamera),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception ignored){}pendingCamera.delete();pendingCamera=null;}}
 @Override protected void onActivityResult(int request,int code,Intent data){super.onActivityResult(request,code,data);
  if(request==PICK&&code==RESULT_OK&&data!=null&&data.getData()!=null){load(data.getData(),false);return;}
  if(request==CAPTURE||request==GUIDED){if(code==RESULT_OK&&pendingCamera!=null&&pendingCamera.length()>0)load(Uri.fromFile(pendingCamera),true);else{removeCapture();message(getString(R.string.pk_038));}return;}
  if(request==SAVE_JPG||request==SAVE_PDF||request==SAVE_SHEET){byte[] bytes=pendingExport;int expected=pendingRequest;pendingExport=null;pendingRequest=0;if(code!=RESULT_OK)return;
   if(bytes==null||expected!=request||data==null||data.getData()==null){message(getString(R.string.pk_039));return;}
   try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")){if(out==null)throw new IOException();out.write(bytes);message(getString(R.string.pk_040));}catch(Exception e){message(getString(R.string.pk_041));}
  }
 }
 private void message(String s){status.setText(s);}
 private void load(Uri uri,boolean capture){if(busy)return;busy=true;generation++;updateMeasurement();message(getString(R.string.pk_042));
  worker.execute(()->{Bitmap b=null;try{b=decode(uri);}catch(Exception|OutOfMemoryError ignored){}Bitmap decoded=b;runOnUiThread(()->{
   if(capture)removeCapture();if(closed){if(decoded!=null)decoded.recycle();return;}busy=false;
   if(decoded==null){message(getString(R.string.pk_043));updateMeasurement();return;}
   if(source!=null)source.recycle();source=decoded;backgroundColor=0;updating=true;zoom.setProgress(0);x.setProgress(50);y.setProgress(50);headTop.setProgress(10);headBottom.setProgress(85);confirmed.setChecked(false);updating=false;updateCrop();message(getString(R.string.pk_044));showStage(1);
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
 private void updateProfile(){ruleNote.setText(profile==PhotoRules.Profile.KR?getString(R.string.pk_045):profile==PhotoRules.Profile.US?getString(R.string.pk_046):profile==PhotoRules.Profile.JP?getString(R.string.pk_047):getString(R.string.pk_048));background.setVisibility(profile.official?View.GONE:View.VISIBLE);resetBackground.setVisibility(profile.official?View.GONE:View.VISIBLE);}
 private void chooseBackground(){if(source==null||busy||!PhotoRules.backgroundAllowed(profile))return;new android.app.AlertDialog.Builder(this).setTitle(getString(R.string.pk_049)).setItems(new String[]{getString(R.string.pk_050),getString(R.string.pk_051),getString(R.string.pk_052)},(d,i)->{if(!PhotoRules.backgroundAllowed(profile))return;backgroundColor=new int[]{Color.WHITE,0xffc9e2f4,0xffd9d9d9}[i];confirmed.setChecked(false);updateCrop();message(getString(R.string.pk_053));}).show();}
 private void updateMeasurement(){if(measure==null)return;boolean valid=source!=null&&PhotoRules.resolution(profile,cropRect());boolean range=PhotoRules.headRange(profile,headTop.getProgress(),headBottom.getProgress());
  measure.setText(source==null?getString(R.string.pk_054):String.format(Locale.US,getString(R.string.pk_055),PhotoRules.headMm(profile,headTop.getProgress(),headBottom.getProgress()),valid?getString(R.string.pk_056):getString(R.string.pk_057)));
  boolean ready=valid&&range&&confirmed.isChecked()&&!busy;check.setEnabled(source!=null&&!busy);jpg.setEnabled(ready);pdf.setEnabled(ready);sheet.setEnabled(ready);background.setEnabled(source!=null&&!busy);resetBackground.setEnabled(source!=null&&!busy);preview.invalidate();if(printPreview!=null)printPreview.invalidate();
 }
 private void inspect(){if(cropped==null||busy)return;int ticket=++generation;busy=true;updateMeasurement();quality.setText(getString(R.string.pk_058));
  Bitmap snapshot=cropped.copy(Bitmap.Config.ARGB_8888,false);String hint=qualityHint(snapshot);
  try{if(detector==null)detector=FaceDetection.getClient(new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).build());
   detector.process(InputImage.fromBitmap(snapshot,0)).addOnSuccessListener(faces->{if(!closed&&ticket==generation){String face=faces.size()==1?faceHint(faces.get(0)):getString(R.string.pk_059);quality.setText(face+"\n"+hint+"\n"+getString(R.string.pk_060));}}).addOnFailureListener(e->{if(!closed&&ticket==generation)quality.setText(getString(R.string.pk_061)+"\n"+hint);}).addOnCompleteListener(t->{snapshot.recycle();if(!closed){busy=false;updateMeasurement();}});
  }catch(RuntimeException e){snapshot.recycle();busy=false;quality.setText(getString(R.string.pk_062));updateMeasurement();}
 }
 private String faceHint(Face f){boolean straight=Math.abs(f.getHeadEulerAngleY())<=10&&Math.abs(f.getHeadEulerAngleZ())<=8&&Math.abs(f.getHeadEulerAngleX())<=10;return straight?getString(R.string.pk_063):getString(R.string.pk_064);}
 private String qualityHint(Bitmap b){long sum=0,edge=0,white=0,corners=0,n=0;for(int yy=4;yy<b.getHeight()-4;yy+=8)for(int xx=4;xx<b.getWidth()-4;xx+=8){int p=b.getPixel(xx,yy),r=Color.red(p),g=Color.green(p),bl=Color.blue(p),l=(r*30+g*59+bl*11)/100;int q=b.getPixel(xx+3,yy);int l2=(Color.red(q)*30+Color.green(q)*59+Color.blue(q)*11)/100;sum+=l;edge+=Math.abs(l-l2);n++;if(yy<65&&(xx<55||xx>b.getWidth()-55)){corners++;if(Math.min(r,Math.min(g,bl))>=225&&Math.max(r,Math.max(g,bl))-Math.min(r,Math.min(g,bl))<=18)white++;}}
  return (sum/n<90?getString(R.string.pk_065):getString(R.string.pk_066))+" · "+(edge/n<3?getString(R.string.pk_067):getString(R.string.pk_068))+"\n"+(corners>0&&white*100/corners>=85?getString(R.string.pk_069):getString(R.string.pk_070));
 }
 private void export(int type){if(busy||pendingExport!=null||source==null||!confirmed.isChecked()||!PhotoRules.headRange(profile,headTop.getProgress(),headBottom.getProgress())||!PhotoRules.resolution(profile,cropRect()))return;
  try{byte[] bytes=type==SAVE_PDF?makePdf():type==SAVE_SHEET?makeSheet():makeJpg();if(bytes==null)throw new IOException();pendingExport=bytes;pendingRequest=type;
   Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType(type==SAVE_PDF?"application/pdf":"image/jpeg");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,profile.id+(type==SAVE_PDF?"-print.pdf":type==SAVE_SHEET?"-sheet.jpg":"-photo.jpg"));startActivityForResult(i,pendingRequest);
  }catch(Exception e){pendingExport=null;pendingRequest=0;message(getString(R.string.pk_071));}
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
 private void clear(){if(busy)return;generation++;if(source!=null){source.recycle();source=null;}if(cropped!=null){cropped.recycle();cropped=null;}pendingExport=null;pendingRequest=0;confirmed.setChecked(false);quality.setText("");message(getString(R.string.pk_072));updateMeasurement();showStage(0);}
 @Override protected void onDestroy(){releaseSplashGate();closed=true;generation++;worker.shutdown();if(detector!=null)detector.close();if(source!=null)source.recycle();if(cropped!=null)cropped.recycle();pendingExport=null;if(isFinishing())removeCapture();super.onDestroy();}
 private final class PrintPreview extends View{
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  PrintPreview(){super(MainActivity.this);}
  @Override protected void onDraw(Canvas c){if(paper==null||cropped==null||cropped.isRecycled())return;int pos=paper.getSelectedItemPosition(),w=pos==0?288:pos==1?360:595,h=pos==0?432:pos==1?504:842;float scale=Math.min((getWidth()-dp(24))/(float)w,(getHeight()-dp(24))/(float)h);if(scale<=0)return;float l=(getWidth()-w*scale)/2,t=(getHeight()-h*scale)/2;c.save();c.translate(l,t);c.scale(scale,scale);p.setColor(Color.WHITE);c.drawRect(0,0,w,h,p);int[] count=PhotoRules.layout(profile,w,h);float pw=(float)PhotoRules.points(profile.mmW),ph=(float)PhotoRules.points(profile.mmH),gap=(float)PhotoRules.points(3),left=(w-count[0]*pw-(count[0]-1)*gap)/2,top=(h-count[1]*ph-(count[1]-1)*gap)/2;for(int row=0;row<count[1];row++)for(int col=0;col<count[0];col++){float x=left+col*(pw+gap),y=top+row*(ph+gap);c.drawBitmap(cropped,null,new RectF(x,y,x+pw,y+ph),p);}c.restore();}
 }
 private final class Preview extends View{
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  Preview(){super(MainActivity.this);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float ratio=(float)(profile.mmW/profile.mmH),h=Math.min(getHeight()-dp(8),(getWidth()-dp(8))/ratio),w=h*ratio;if(w<=0||h<=0)return;float l=(getWidth()-w)/2,t=(getHeight()-h)/2;RectF r=new RectF(l,t,l+w,t+h);p.setColor(Color.WHITE);c.drawRect(r,p);if(cropped!=null&&!cropped.isRecycled())c.drawBitmap(cropped,null,r,p);p.setStyle(Paint.Style.STROKE);p.setColor(0xff327c90);p.setStrokeWidth(dp(2));c.drawRect(r,p);if(headTop!=null){float a=t+h*headTop.getProgress()/100f,b=t+h*headBottom.getProgress()/100f;c.drawLine(l,a,l+w,a,p);c.drawLine(l,b,l+w,b,p);}p.setStyle(Paint.Style.FILL);}
 }
}