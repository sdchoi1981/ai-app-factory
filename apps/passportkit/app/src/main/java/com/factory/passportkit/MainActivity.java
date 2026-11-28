package com.factory.passportkit;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.DocumentsContract;
import android.os.Bundle;
import android.content.Intent;
import android.content.ClipData;
import android.net.Uri;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.media.ExifInterface;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
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
 private static final int PICK=101,CAPTURE=102,SAVE_JPG=103,SAVE_PDF=104,SAVE_SHEET=105,GUIDED=106,CAMERA_PERMISSION=107,GALLERY_PERMISSION=108;
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
 private byte[] pendingExport;
 private int pendingGalleryType;
 private Uri savedGalleryImage;
 private TextView exportStatus;
 private Button openGallery,shareGallery;
 private Button importPhoto,advance,more;
 private LinearLayout cropTools,guideTools;
 private Button cropTab,guideTab;
 private boolean guideSelected;
 private int toolbarStage=-1;
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView label(String s,int size){TextView t=ui.text(s,size,size>=18);content.addView(t,ui.space(-2,0,2));return t;}
 private Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);ui.button(b,false);b.setOnClickListener(v->r.run());content.addView(b,ui.space(-2,3,4));return b;}
 private void showStage(int next){
  if(next!=0&&source==null)next=0;
  boolean changed=stage!=next;
  stage=next;
  sourcePanel.setVisibility(stage==0?View.VISIBLE:View.GONE);
  previewPanel.setVisibility(stage==1?View.VISIBLE:View.GONE);
  editPanel.setVisibility(stage==1?View.VISIBLE:View.GONE);
  exportPanel.setVisibility(stage==2?View.VISIBLE:View.GONE);
  refreshWorkflowActions();
  if(printPreview!=null)printPreview.invalidate();
  if(changed){
   screen.scrollTo(0,0);
   screen.post(()->screen.scrollTo(0,0));
   page.animate().cancel();
   page.setAlpha(1f);
   if(ValueAnimator.areAnimatorsEnabled()){
    page.setAlpha(.78f);
    page.animate().alpha(1f).setDuration(160).start();
   }
  }
 }

 private void refreshWorkflowActions(){
  if(toolbarStage!=stage){
   for(int i=0;i<steps.length;i++){
    ui.button(steps[i],i==stage);
    steps[i].setSelected(i==stage);
   }
   toolbarStage=stage;
  }
  for(int i=0;i<steps.length;i++)steps[i].setEnabled(i==0||source!=null);
  importPhoto.setVisibility(stage==0?View.VISIBLE:View.GONE);
  advance.setVisibility(stage==1?View.VISIBLE:View.GONE);
  jpg.setVisibility(stage==2?View.VISIBLE:View.GONE);
  importPhoto.setEnabled(!exportBlocked());
  advance.setEnabled(!exportBlocked());
 }

 private void showGuideTools(boolean guides){
  guideSelected=guides;
  cropTools.setVisibility(guides?View.GONE:View.VISIBLE);
  guideTools.setVisibility(guides?View.VISIBLE:View.GONE);
  cropTab.setSelected(!guides);
  guideTab.setSelected(guides);
  ui.button(cropTab,!guides);
  ui.button(guideTab,guides);
  advance.setText(guides?R.string.stage_save:R.string.studio_guides);
 }

 private LinearLayout rail(){
  LinearLayout row=new LinearLayout(this);
  row.setGravity(Gravity.CENTER_VERTICAL);
  return row;
 }

 private Button railButton(LinearLayout row,CharSequence title,Runnable action){
  Button b=new Button(this);
  b.setText(title);
  b.setOnClickListener(v->action.run());
  ui.button(b,false);
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);
  params.setMargins(dp(2),0,dp(2),0);
  row.addView(b,params);
  return b;
 }

 private void showMenu(){
  PopupMenu menu=new PopupMenu(this,more);
  menu.getMenu().add(0,1,0,R.string.pk_029).setEnabled(source!=null&&!exportBlocked());
  menu.getMenu().add(0,2,1,R.string.studio_references);
  menu.getMenu().add(0,3,2,R.string.pk_030);
  menu.setOnMenuItemClickListener(item->{
   switch(item.getItemId()){
    case 1: clear(); return true;
    case 2:
     new android.app.AlertDialog.Builder(this).setTitle(R.string.studio_references)
      .setMessage(getString(R.string.pk_032)+"\n\nhttps://www.passport.go.kr/home/kor/contents.do?menuPos=32\nhttps://www.passport.go.kr/home/kor/contents.do?menuPos=12")
      .setPositiveButton(android.R.string.ok,null).show();
     return true;
    case 3:
     new android.app.AlertDialog.Builder(this).setTitle(R.string.pk_030)
      .setMessage(R.string.pk_031).setPositiveButton(android.R.string.ok,null).show();
     return true;
    default: return false;
   }
  });
  menu.show();
 }

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
  ui=new KitUi(this,0xff274c90,0xffaec8ff);
  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setGravity(Gravity.CENTER_HORIZONTAL);
  root.setBackgroundColor(ui.background);
  LinearLayout header=ui.column();
  header.setPadding(dp(18),dp(4),dp(18),dp(2));
  header.setBackgroundColor(ui.surface);
  root.addView(header,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout identity=rail();
  header.addView(identity,new LinearLayout.LayoutParams(-1,-2));
  ImageView icon=new ImageView(this);
  icon.setImageResource(R.drawable.ic_launcher);
  icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  identity.addView(icon,new LinearLayout.LayoutParams(dp(32),dp(32)));
  TextView title=ui.text(getString(R.string.app_name),21,true);
  ViewCompat.setAccessibilityHeading(title,true);
  LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(0,-2,1);
  titleParams.setMarginStart(dp(10));
  identity.addView(title,titleParams);
  more=new Button(this);
  more.setText("⋯");
  more.setContentDescription(getString(R.string.studio_more));
  ui.button(more,false);
  more.setMinWidth(dp(48));
  more.setMinimumWidth(dp(48));
  more.setOnClickListener(v->showMenu());
  identity.addView(more,new LinearLayout.LayoutParams(dp(52),-2));

  LinearLayout navigation=rail();
  steps=new Button[3];
  int[] stepIds={R.string.stage_source,R.string.stage_edit,R.string.stage_save};
  for(int i=0;i<steps.length;i++){
   final int step=i;
   steps[i]=railButton(navigation,getString(R.string.studio_step,i+1,getString(stepIds[i])),()->showStage(step));
  }
  header.addView(navigation,ui.space(-2,4,6));
  status=ui.text("",14,false);
  status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  status.setVisibility(View.GONE);
  header.addView(status,ui.space(-2,0,2));
  ScrollView scroll=new ScrollView(this);screen=scroll;scroll.setFillViewport(true);scroll.setBackgroundColor(ui.background);
  page=ui.column();android.widget.FrameLayout frame=new android.widget.FrameLayout(this);frame.addView(page,new android.widget.FrameLayout.LayoutParams(-1,-2,android.view.Gravity.TOP|android.view.Gravity.CENTER_HORIZONTAL));scroll.addView(frame,new ViewGroup.LayoutParams(-1,-2));content=page;
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  sourcePanel=new LinearLayout(this);
  sourcePanel.setOrientation(LinearLayout.VERTICAL);
  page.addView(sourcePanel,new LinearLayout.LayoutParams(-1,-2));
  content=sourcePanel;
  ViewCompat.setAccessibilityHeading(label(getString(R.string.pk_001),24),true);
  View intro=new PassportArtwork();
  ui.preview(intro);
  intro.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  content.addView(intro,ui.space(dp(208),6,12));
  content=ui.card(sourcePanel);
  ViewCompat.setAccessibilityHeading(label(getString(R.string.pk_002),18),true);
  profileSelect=new Spinner(this);
  profileSelect.setContentDescription(getString(R.string.pk_002));
  profileSelect.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{
   getString(R.string.pk_003),getString(R.string.pk_004),getString(R.string.pk_005),getString(R.string.pk_006)}));
  ui.field(profileSelect);
  content.addView(profileSelect,ui.space(dp(56),4,4));
  ruleNote=label("",14);
  content=sourcePanel;
  label(getString(R.string.pk_007),14);
  button(getString(R.string.pk_010),this::guided);
  button(getString(R.string.pk_009),this::camera);

  previewPanel=new LinearLayout(this);
  previewPanel.setOrientation(LinearLayout.VERTICAL);
  page.addView(previewPanel,ui.space(-2,0,6));
  content=previewPanel;
  preview=new Preview();
  preview.setContentDescription(getString(R.string.pk_012));
  ui.preview(preview);
  content.addView(preview,ui.space(dp(280),0,0));

  editPanel=new LinearLayout(this);
  editPanel.setOrientation(LinearLayout.VERTICAL);
  page.addView(editPanel,ui.space(-2,0,0));
  LinearLayout inspector=rail();
  editPanel.addView(inspector,ui.space(-2,4,6));
  cropTab=railButton(inspector,getString(R.string.studio_crop),()->showGuideTools(false));
  guideTab=railButton(inspector,getString(R.string.studio_guides),()->showGuideTools(true));

  cropTools=ui.card(editPanel);
  content=cropTools;
  ViewCompat.setAccessibilityHeading(label(getString(R.string.pk_013),20),true);
  zoom=slider(getString(R.string.pk_014),100,0,true);
  x=slider(getString(R.string.pk_015),100,50,true);
  y=slider(getString(R.string.pk_016),100,50,true);
  background=button(getString(R.string.pk_022),this::chooseBackground);
  resetBackground=button(getString(R.string.pk_023),()->{backgroundColor=0;updateCrop();});

  guideTools=ui.card(editPanel);
  content=guideTools;
  ViewCompat.setAccessibilityHeading(label(getString(R.string.pk_017),20),true);
  headTop=slider(getString(R.string.pk_018),100,10,false);
  headBottom=slider(getString(R.string.pk_019),100,85,false);
  measure=label("",16);
  measure.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  confirmed=new CheckBox(this);
  confirmed.setText(getString(R.string.pk_020));
  confirmed.setMinHeight(dp(52));
  confirmed.setOnCheckedChangeListener((b,v)->updateMeasurement());
  content.addView(confirmed);
  check=button(getString(R.string.pk_021),this::inspect);
  quality=label("",15);
  quality.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);

  exportPanel=new LinearLayout(this);
  exportPanel.setOrientation(LinearLayout.VERTICAL);
  page.addView(exportPanel,ui.space(-2,0,0));
  content=exportPanel;
  ViewCompat.setAccessibilityHeading(label(getString(R.string.pk_024),24),true);
  label(getString(R.string.stage_save_hint),15);
  exportStatus=label("",15);
  exportStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  openGallery=button(getString(R.string.gallery_open),()->openSavedImage(false));
  shareGallery=button(getString(R.string.gallery_share),()->openSavedImage(true));
  refreshGalleryActions();

  content=ui.card(exportPanel);
  ViewCompat.setAccessibilityHeading(label(getString(R.string.studio_print),18),true);
  printPreview=new PrintPreview();
  label(getString(R.string.pk_026),15);paper=new Spinner(this);paper.setContentDescription(getString(R.string.pk_026));ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"4 × 6 in","5 × 7 in","A4"});paper.setAdapter(adapter);ui.field(paper);content.addView(paper,ui.space(dp(52),4,6));paper.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int i,long id){printPreview.invalidate();}});
  printPreview.setContentDescription(getString(R.string.stage_paper_preview));
  ui.preview(printPreview);
  content.addView(printPreview,ui.space(dp(260),6,8));
  sheet=button(getString(R.string.pk_028),()->export(SAVE_SHEET));
  pdf=button(getString(R.string.pk_027),()->export(SAVE_PDF));
  label(getString(R.string.pk_033),14);

  LinearLayout footer=ui.column();
  footer.setPadding(dp(18),dp(3),dp(18),dp(5));
  footer.setBackgroundColor(ui.surface);
  root.addView(footer,new LinearLayout.LayoutParams(-1,-2));
  content=footer;
  importPhoto=button(getString(R.string.pk_008),this::pick);
  advance=button(getString(R.string.studio_guides),()->{
   if(guideSelected)showStage(2);
   else showGuideTools(true);
  });
  jpg=button(getString(R.string.pk_025),()->export(SAVE_JPG));
  ui.button(importPhoto,true);
  ui.button(advance,true);
  ui.button(jpg,true);
  showGuideTools(false);
  showStage(0);
  if(holdSplash)holdSplashUntil(scroll,startedAt+SPLASH_MIN_MS);
  setContentView(root);
  SystemInsets.apply(this,root);
  profileSelect.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int i,long id){profile=PhotoRules.Profile.values()[i];backgroundColor=0;confirmed.setChecked(false);updateProfile();if(source!=null)updateCrop();else updateMeasurement();}});updateProfile();updateMeasurement();
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
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
  super.onRequestPermissionsResult(request,permissions,grants);
  boolean granted=grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED;
  if(request==CAMERA_PERMISSION){
   if(granted)launchGuide();else message(getString(R.string.pk_035));
  }else if(request==GALLERY_PERMISSION){
   int type=pendingGalleryType;
   pendingGalleryType=0;
   updateMeasurement();
   if(granted&&type!=0)export(type);
   else if(type!=0)exportStatus.setText(R.string.gallery_permission_denied);
  }
 }
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
  if(request!=SAVE_PDF)return;
  byte[] bytes=pendingExport;
  pendingExport=null;
  if(code!=RESULT_OK){updateMeasurement();return;}
  if(bytes==null||data==null||data.getData()==null){
   updateMeasurement();
   exportStatus.setText(R.string.pk_039);
   return;
  }
  Uri destination=data.getData();
  busy=true;
  updateMeasurement();
  worker.execute(()->{
   int result=R.string.pk_040;
   try(OutputStream output=getContentResolver().openOutputStream(destination,"wt")){
    if(output==null)throw new IOException();
    output.write(bytes);
   }catch(IOException|RuntimeException error){
    result=R.string.pk_041;
    try{DocumentsContract.deleteDocument(getContentResolver(),destination);}
    catch(IOException|RuntimeException ignored){ /* Some document providers cannot delete a failed export. */ }
   }
   finishExport(result,null);
  });
 }
 private void message(String s){
  status.setText(s);
  status.setVisibility(s.isEmpty()?View.GONE:View.VISIBLE);
 }
 private void load(Uri uri,boolean capture){if(busy)return;busy=true;generation++;updateMeasurement();message(getString(R.string.pk_042));
  worker.execute(()->{Bitmap b=null;try{b=decode(uri);}catch(Exception|OutOfMemoryError ignored){}Bitmap decoded=b;runOnUiThread(()->{
   if(capture)removeCapture();if(closed){if(decoded!=null)decoded.recycle();return;}busy=false;
   if(decoded==null){message(getString(R.string.pk_043));updateMeasurement();return;}
   savedGalleryImage=null;refreshGalleryActions();exportStatus.setText("");
   if(source!=null)source.recycle();source=decoded;backgroundColor=0;updating=true;zoom.setProgress(0);x.setProgress(50);y.setProgress(50);headTop.setProgress(10);headBottom.setProgress(85);confirmed.setChecked(false);updating=false;updateCrop();message(getString(R.string.pk_044));showGuideTools(false);showStage(1);
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
  boolean ready=valid&&range&&confirmed.isChecked()&&!exportBlocked();
  if(exportBlocked())exportStatus.setText(R.string.export_in_progress);
  else if(!ready)exportStatus.setText(R.string.export_disabled);
  else if(exportStatus.getText().toString().equals(getString(R.string.export_in_progress))||
    exportStatus.getText().toString().equals(getString(R.string.export_disabled)))exportStatus.setText("");check.setEnabled(source!=null&&!busy);jpg.setEnabled(ready);pdf.setEnabled(ready);sheet.setEnabled(ready);background.setEnabled(source!=null&&!busy);resetBackground.setEnabled(source!=null&&!busy);refreshWorkflowActions();preview.invalidate();if(printPreview!=null)printPreview.invalidate();
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
 private boolean exportBlocked(){
  return busy||pendingExport!=null||pendingGalleryType!=0;
 }

 private void export(int type){
  if(exportBlocked())return;
  if(source==null||cropped==null||!confirmed.isChecked()||
    !PhotoRules.headRange(profile,headTop.getProgress(),headBottom.getProgress())||
    !PhotoRules.resolution(profile,cropRect())){
   exportStatus.setText(R.string.export_disabled);
   return;
  }
  if(type!=SAVE_PDF&&Build.VERSION.SDK_INT<Build.VERSION_CODES.Q&&
    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
   pendingGalleryType=type;
   updateMeasurement();
   requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},GALLERY_PERMISSION);
   return;
  }

  Bitmap image;
  try{image=cropped.copy(Bitmap.Config.ARGB_8888,false);}
  catch(RuntimeException|OutOfMemoryError error){image=null;}
  if(image==null){
   exportStatus.setText(type==SAVE_PDF?R.string.pk_071:R.string.gallery_save_failed);
   return;
  }
  Bitmap snapshot=image;
  PhotoRules.Profile exportProfile=profile;
  int paperSize=paper.getSelectedItemPosition();
  if(type!=SAVE_PDF){savedGalleryImage=null;refreshGalleryActions();}
  busy=true;
  updateMeasurement();
  worker.execute(()->{
   try{
    if(type==SAVE_PDF){
     byte[] bytes=makePdf(snapshot,exportProfile,paperSize);
     runOnUiThread(()->choosePdfDestination(bytes,exportProfile));
    }else{
     byte[] bytes=type==SAVE_SHEET?makeSheet(snapshot,exportProfile,paperSize):makeJpg(snapshot,exportProfile);
     if(bytes==null)throw new IOException();
     Uri uri=GalleryExport.save(getApplicationContext(),bytes,type==SAVE_SHEET);
     finishExport(R.string.gallery_saved,uri);
    }
   }catch(IOException|RuntimeException|OutOfMemoryError error){
    finishExport(type==SAVE_PDF?R.string.pk_071:R.string.gallery_save_failed,null);
   }finally{snapshot.recycle();}
  });
 }

 private void choosePdfDestination(byte[] bytes,PhotoRules.Profile exportProfile){
  if(closed)return;
  busy=false;
  pendingExport=bytes;
  updateMeasurement();
  try{
   Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/pdf");
   intent.addCategory(Intent.CATEGORY_OPENABLE);
   intent.putExtra(Intent.EXTRA_TITLE,exportProfile.id+"-print.pdf");
   startActivityForResult(intent,SAVE_PDF);
  }catch(RuntimeException error){
   pendingExport=null;
   updateMeasurement();
   exportStatus.setText(R.string.pk_071);
  }
 }

 private void finishExport(int result,Uri uri){
  runOnUiThread(()->{
   if(closed)return;
   busy=false;
   if(uri!=null)savedGalleryImage=uri;
   refreshGalleryActions();
   updateMeasurement();
   exportStatus.setText(result);
  });
 }

 private void refreshGalleryActions(){
  int visibility=savedGalleryImage==null?View.GONE:View.VISIBLE;
  openGallery.setVisibility(visibility);
  shareGallery.setVisibility(visibility);
 }

 private void openSavedImage(boolean share){
  Uri uri=savedGalleryImage;
  if(uri==null)return;
  Intent intent=share?new Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM,uri)
    :new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"image/jpeg");
  intent.setClipData(ClipData.newRawUri("PassportKit JPEG",uri));
  intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  try{startActivity(share?Intent.createChooser(intent,getText(R.string.gallery_share)):intent);}
  catch(RuntimeException error){exportStatus.setText(R.string.gallery_unavailable);}
 }
 private static byte[] makeJpg(Bitmap image,PhotoRules.Profile profile){for(int q=100;q>=60;q-=5){ByteArrayOutputStream out=new ByteArrayOutputStream();if(image.compress(Bitmap.CompressFormat.JPEG,q,out)&&out.size()<=profile.maxBytes)return out.toByteArray();}return null;}
 private static byte[] makePdf(Bitmap image,PhotoRules.Profile profile,int pos)throws IOException{
  int w=pos==0?288:pos==1?360:595,h=pos==0?432:pos==1?504:842;int[] count=PhotoRules.layout(profile,w,h);
  float pw=(float)PhotoRules.points(profile.mmW),ph=(float)PhotoRules.points(profile.mmH),gap=(float)PhotoRules.points(3);
  float left=(w-count[0]*pw-(count[0]-1)*gap)/2,top=(h-count[1]*ph-(count[1]-1)*gap)/2;
  PdfDocument doc=new PdfDocument();try{PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(w,h,1).create());Canvas c=page.getCanvas();c.drawColor(Color.WHITE);Paint p=new Paint(Paint.FILTER_BITMAP_FLAG);
   for(int row=0;row<count[1];row++)for(int col=0;col<count[0];col++){float xx=left+col*(pw+gap),yy=top+row*(ph+gap);c.drawBitmap(image,null,new RectF(xx,yy,xx+pw,yy+ph),p);}
   doc.finishPage(page);ByteArrayOutputStream out=new ByteArrayOutputStream();doc.writeTo(out);return out.toByteArray();
  }finally{doc.close();}
 }
 private static byte[] makeSheet(Bitmap image,PhotoRules.Profile profile,int pos)throws IOException{int w=pos==0?1200:pos==1?1500:2480,h=pos==0?1800:pos==1?2100:3508;int pw=PhotoRules.pixels(profile.mmW),ph=PhotoRules.pixels(profile.mmH),gap=PhotoRules.pixels(3),margin=PhotoRules.pixels(4),cols=(w-2*margin+gap)/(pw+gap),rows=(h-2*margin+gap)/(ph+gap);Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);try{Canvas c=new Canvas(bitmap);c.drawColor(Color.WHITE);int left=(w-cols*pw-(cols-1)*gap)/2,top=(h-rows*ph-(rows-1)*gap)/2;Paint p=new Paint(Paint.FILTER_BITMAP_FLAG);for(int y=0;y<rows;y++)for(int x=0;x<cols;x++){int xx=left+x*(pw+gap),yy=top+y*(ph+gap);c.drawBitmap(image,null,new Rect(xx,yy,xx+pw,yy+ph),p);}ByteArrayOutputStream out=new ByteArrayOutputStream();if(!bitmap.compress(Bitmap.CompressFormat.JPEG,95,out))throw new IOException();return out.toByteArray();}finally{bitmap.recycle();}}
 private void clear(){if(exportBlocked())return;generation++;if(source!=null){source.recycle();source=null;}if(cropped!=null){cropped.recycle();cropped=null;}savedGalleryImage=null;refreshGalleryActions();confirmed.setChecked(false);quality.setText("");message(getString(R.string.pk_072));updateMeasurement();showStage(0);}
 @Override protected void onDestroy(){releaseSplashGate();closed=true;generation++;worker.shutdown();if(detector!=null)detector.close();if(source!=null)source.recycle();if(cropped!=null)cropped.recycle();pendingExport=null;if(isFinishing())removeCapture();super.onDestroy();}
 private final class PassportArtwork extends View {
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  PassportArtwork(){super(MainActivity.this);}

  @Override protected void onDraw(Canvas canvas){
   float width=getWidth(),height=getHeight(),scale=Math.min(width/320f,height/207f);
   if(scale<=0)return;
   canvas.drawColor(ui.tint);
   canvas.save();
   canvas.translate(width/2,height/2);
   canvas.scale(scale,scale);

   paint.setColor(ui.outline);
   for(int column=-7;column<=7;column++){
    for(int row=-4;row<=4;row++)canvas.drawCircle(column*20,row*20,1.1f,paint);
   }
   canvas.save();
   canvas.rotate(-12,-37,0);
   paint.setColor(0xff172c53);
   canvas.drawRoundRect(-101,-66,26,70,11,11,paint);
   paint.setColor(0xffc8a572);
   paint.setStyle(Paint.Style.STROKE);
   paint.setStrokeWidth(1.6f);
   canvas.drawRoundRect(-93,-58,18,62,8,8,paint);
   canvas.drawCircle(-38,-5,20,paint);
   canvas.drawOval(-48,-25,-28,15,paint);
   canvas.drawLine(-58,-5,-18,-5,paint);
   canvas.drawLine(-59,38,-17,38,paint);
   paint.setStyle(Paint.Style.FILL);
   canvas.restore();

   canvas.save();
   canvas.rotate(8,65,0);
   paint.setColor(0x260f1b34);
   canvas.drawRoundRect(14,-56,116,75,10,10,paint);
   paint.setColor(ui.surface);
   canvas.drawRoundRect(8,-61,110,68,10,10,paint);
   paint.setColor(ui.tint);
   canvas.drawRoundRect(18,-51,100,46,7,7,paint);
   paint.setColor(ui.accent);
   canvas.drawCircle(59,-15,14,paint);
   canvas.drawRoundRect(35,7,83,38,14,14,paint);
   paint.setStrokeWidth(2f);
   for(int x=25;x<=93;x+=68){
    for(int y=-44;y<=39;y+=83){
     canvas.drawLine(x,y,x+(x==25?9:-9),y,paint);
     canvas.drawLine(x,y,x,y+(y==-44?9:-9),paint);
    }
   }
   paint.setColor(ui.outline);
   canvas.drawRoundRect(31,54,87,58,2,2,paint);
   canvas.restore();
   canvas.restore();
  }
 }

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