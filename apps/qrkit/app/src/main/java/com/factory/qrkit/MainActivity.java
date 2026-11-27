package com.factory.qrkit;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Bitmap;
import android.widget.*;
import android.view.ViewGroup;
import com.google.zxing.common.BitMatrix;
import android.content.pm.PackageManager;
import android.Manifest;
import factory.ui.SystemInsets;

public final class MainActivity extends Activity {
 private EditText text;
 private TextView result;
 private ImageView image;
 private LinearLayout content;
 private boolean ko;
 private String scanned="";
 private static final int SCAN_REQUEST=421;
 private static final int CAMERA_REQUEST=422;
 private String tr(String k,String e){return ko?k:e;}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setPadding(0,dp(8),0,dp(8));content.addView(t,new LinearLayout.LayoutParams(-1,-2));return t;}
 private void button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setMinHeight(dp(48));b.setOnClickListener(v->r.run());content.addView(b,new LinearLayout.LayoutParams(-1,-2));}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);ko=getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ko");
  ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(20),dp(20),dp(24));scroll.addView(content,new ViewGroup.LayoutParams(-1,-2));
  label("QRKit",32);label(tr("QR 생성 및 카메라 스캔","QR generator and camera scanner"),18);
  text=new EditText(this);text.setHint(tr("텍스트 또는 URL","Text or URL"));text.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);text.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1200)});content.addView(text,new LinearLayout.LayoutParams(-1,-2));
  button(tr("QR 생성","Generate QR"),this::generate);
  image=new ImageView(this);image.setContentDescription(tr("생성된 QR 이미지","Generated QR image"));image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);content.addView(image,new LinearLayout.LayoutParams(-1,dp(300)));
  button(tr("카메라로 스캔","Scan with camera"),this::requestScan);
  result=label("",17);result.setTextIsSelectable(true);result.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
  button(tr("스캔 결과 복사","Copy scanned text"),()->{if(!scanned.isEmpty()){ClipboardManager c=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);c.setPrimaryClip(ClipData.newPlainText("QR",scanned));}});
  label(tr("QR에 포함된 링크는 자동으로 열지 않습니다. 카메라 이미지는 저장하지 않습니다.","Links are never opened automatically. Camera images are not saved."),14);
  setContentView(scroll);SystemInsets.apply(this,content);
 }
 private void generate(){try{BitMatrix b=QrCodec.encode(text.getText().toString(),512);Bitmap bitmap=Bitmap.createBitmap(QrCodec.pixels(b),b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);image.setImageBitmap(bitmap);result.setText("");}catch(Exception e){image.setImageDrawable(null);result.setText(tr("입력값을 확인하세요 (최대 UTF-8 1200바이트).","Check input (maximum 1200 UTF-8 bytes)."));}}
 private void requestScan(){
  if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
   result.setText(tr("사용할 수 있는 카메라가 없습니다.","No camera is available."));return;
  }
  if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
   try{requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_REQUEST);}
   catch(RuntimeException e){result.setText(tr("카메라 권한을 요청할 수 없습니다.","Unable to request camera permission."));}
   return;
  }
  launchScan();
 }
 private void launchScan(){
  try{startActivityForResult(new Intent(this,ScannerActivity.class),SCAN_REQUEST);}
  catch(RuntimeException e){result.setText(tr("카메라 화면을 열 수 없습니다.","Unable to open scanner."));}
 }
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
  super.onRequestPermissionsResult(request,permissions,grants);
  if(request!=CAMERA_REQUEST)return;
  if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)launchScan();
  else result.setText(tr("카메라 권한이 필요합니다. 설정에서 QRKit의 카메라 권한을 허용하세요.","Camera permission is required. Allow QRKit camera access in Settings."));
 }
 @Override protected void onActivityResult(int request,int code,Intent data){
  super.onActivityResult(request,code,data);if(request!=SCAN_REQUEST)return;
  if(code==RESULT_OK&&data!=null&&data.getStringExtra(ScannerActivity.EXTRA_TEXT)!=null){
   scanned=data.getStringExtra(ScannerActivity.EXTRA_TEXT);result.setText(scanned);
  }else{
   scanned="";result.setText(data!=null&&data.getBooleanExtra(ScannerActivity.EXTRA_ERROR,false)?
    tr("카메라를 시작할 수 없습니다. 다른 카메라 앱을 닫고 다시 시도하세요.","Could not start camera. Close other camera apps and retry."):
    tr("스캔 취소","Scan cancelled"));
  }
 }
}
