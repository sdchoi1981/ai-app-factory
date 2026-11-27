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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import factory.ui.SystemInsets;

public final class MainActivity extends Activity {
 private EditText text;
 private TextView result;
 private ImageView image;
 private LinearLayout content;
 private boolean ko;
 private String scanned="";
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
  button(tr("카메라로 스캔","Scan with camera"),()->{try{new IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE).setPrompt(tr("QR 코드를 화면에 맞추세요","Align the QR code")).setBeepEnabled(false).setOrientationLocked(false).initiateScan();}catch(RuntimeException e){result.setText(tr("카메라를 사용할 수 없습니다.","Camera unavailable."));}});
  result=label("",17);result.setTextIsSelectable(true);result.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
  button(tr("스캔 결과 복사","Copy scanned text"),()->{if(!scanned.isEmpty()){ClipboardManager c=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);c.setPrimaryClip(ClipData.newPlainText("QR",scanned));}});
  label(tr("QR에 포함된 링크는 자동으로 열지 않습니다. 카메라 이미지는 저장하지 않습니다.","Links are never opened automatically. Camera images are not saved."),14);
  setContentView(scroll);SystemInsets.apply(this,content);
 }
 private void generate(){try{BitMatrix b=QrCodec.encode(text.getText().toString(),512);Bitmap bitmap=Bitmap.createBitmap(QrCodec.pixels(b),b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);image.setImageBitmap(bitmap);result.setText("");}catch(Exception e){image.setImageDrawable(null);result.setText(tr("입력값을 확인하세요 (최대 UTF-8 1200바이트).","Check input (maximum 1200 UTF-8 bytes)."));}}
 @Override protected void onActivityResult(int request,int code,Intent data){IntentResult r=IntentIntegrator.parseActivityResult(request,code,data);if(r==null){super.onActivityResult(request,code,data);return;}if(r.getContents()!=null){scanned=r.getContents();result.setText(scanned);}else{scanned="";result.setText(tr("스캔 취소","Scan cancelled"));}}
}
