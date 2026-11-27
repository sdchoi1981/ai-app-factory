package com.factory.passportkit;
import android.app.Activity;
import android.os.Bundle;
import android.hardware.Camera;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import factory.ui.SystemInsets;
@SuppressWarnings("deprecation")
public final class GuideActivity extends Activity implements SurfaceHolder.Callback {
 private Camera camera;private SurfaceView surface;private TextView status;private Button shutter;private File target;private boolean taking,ko;
 private String tr(String k,String e){return ko?k:e;}
 @Override public void onCreate(Bundle b){super.onCreate(b);ko=getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ko");getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);String name=getIntent().getStringExtra("name");if(name==null||!name.matches("photo-[a-zA-Z0-9]+\\.jpg")){finish();return;}target=new File(new File(getCacheDir(),"capture"),name);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);status=new TextView(this);status.setText(tr("정면을 보고 표시선에 맞추세요. 촬영 후 편집에서 다시 확인합니다.","Face forward and align to the guides. Verify after capture."));status.setPadding(16,16,16,16);root.addView(status);
  FrameLayout frame=new FrameLayout(this);surface=new SurfaceView(this);frame.addView(surface,new FrameLayout.LayoutParams(-1,-1));frame.addView(new Overlay(),new FrameLayout.LayoutParams(-1,-1));root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
  shutter=new Button(this);shutter.setText(tr("촬영","Capture"));shutter.setEnabled(false);shutter.setOnClickListener(v->capture());root.addView(shutter);Button back=new Button(this);back.setText(tr("취소","Cancel"));back.setOnClickListener(v->finish());root.addView(back);setContentView(root);SystemInsets.apply(this,root);surface.getHolder().addCallback(this);
 }
 public void surfaceCreated(SurfaceHolder holder){open(holder);}
 public void surfaceChanged(SurfaceHolder h,int f,int w,int ht){}
 public void surfaceDestroyed(SurfaceHolder h){release();}
 private void open(SurfaceHolder holder){if(camera!=null||taking)return;try{int id=-1;Camera.CameraInfo info=new Camera.CameraInfo();for(int i=0;i<Camera.getNumberOfCameras();i++){Camera.getCameraInfo(i,info);if(info.facing==Camera.CameraInfo.CAMERA_FACING_BACK){id=i;break;}}if(id<0)throw new IOException();Camera.getCameraInfo(id,info);camera=Camera.open(id);Camera.Parameters p=camera.getParameters();int rotation=getWindowManager().getDefaultDisplay().getRotation();int degrees=rotation==Surface.ROTATION_90?90:rotation==Surface.ROTATION_180?180:rotation==Surface.ROTATION_270?270:0;int angle=(info.orientation-degrees+360)%360;camera.setDisplayOrientation(angle);p.setRotation(angle);p.setJpegQuality(95);if(p.getSupportedFocusModes()!=null&&p.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);camera.setParameters(p);camera.setPreviewDisplay(holder);camera.startPreview();shutter.setEnabled(true);}catch(Exception e){release();status.setText(tr("카메라를 열 수 없습니다. 취소 후 JPG를 선택하세요.","Camera unavailable. Cancel and choose a JPEG."));}}
 private void capture(){if(camera==null||taking)return;taking=true;shutter.setEnabled(false);try{camera.takePicture(null,null,(bytes,c)->{try{if(bytes==null||bytes.length==0)throw new IOException();try(FileOutputStream out=new FileOutputStream(target)){out.write(bytes);}setResult(RESULT_OK);finish();}catch(Exception e){taking=false;status.setText(tr("촬영 저장 실패","Capture save failed"));release();open(surface.getHolder());}});}catch(RuntimeException e){taking=false;release();status.setText(tr("촬영 실패","Capture failed"));}}
 private void release(){if(camera!=null){try{camera.stopPreview();}catch(RuntimeException ignored){}camera.release();camera=null;}if(shutter!=null)shutter.setEnabled(false);}
 @Override protected void onPause(){release();super.onPause();}
 @Override protected void onResume(){super.onResume();if(surface!=null&&surface.getHolder().getSurface().isValid())open(surface.getHolder());}
 private final class Overlay extends View{private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Overlay(){super(GuideActivity.this);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),cx=w/2;int i=getIntent().getIntExtra("profile",0);PhotoRules.Profile profile=PhotoRules.Profile.values()[PhotoRules.clamp(i,0,PhotoRules.Profile.values().length-1)];float boxH=Math.min(h*.84f,w*.84f*(float)(profile.mmH/profile.mmW)),boxW=boxH*(float)(profile.mmW/profile.mmH),top=(h-boxH)/2; p.setColor(0xff56e7c7);p.setStrokeWidth(3);p.setStyle(Paint.Style.STROKE);c.drawRect(cx-boxW/2,top,cx+boxW/2,top+boxH,p);c.drawOval(cx-boxW*.32f,top+boxH*.08f,cx+boxW*.32f,top+boxH*.82f,p);p.setStyle(Paint.Style.FILL);}}
}