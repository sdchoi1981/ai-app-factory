package com.factory.qrkit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CameraPreview;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import java.util.Collections;
import java.util.List;
import factory.ui.SystemInsets;

public final class ScannerActivity extends Activity {
    public static final String EXTRA_TEXT="com.factory.qrkit.SCAN_TEXT";
    public static final String EXTRA_ERROR="com.factory.qrkit.SCAN_ERROR";
    private DecoratedBarcodeView scanner;
    private boolean resumed, completed;
    private String tr(String ko,String en){
        return getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ko")?ko:en;
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){fail();return;}
        try{
            LinearLayout root=new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            TextView title=new TextView(this);
            title.setText(tr("QR 코드를 화면에 맞추세요","Align the QR code"));
            title.setTextSize(18);
            int pad=Math.round(16*getResources().getDisplayMetrics().density);
            title.setPadding(pad,pad,pad,pad);
            root.addView(title,new LinearLayout.LayoutParams(-1,-2));
            scanner=new DecoratedBarcodeView(this);
            scanner.setStatusText(tr("QR 코드 스캔","QR code scan"));
            scanner.setDecoderFactory(new DefaultDecoderFactory(Collections.singletonList(BarcodeFormat.QR_CODE)));
            scanner.getBarcodeView().addStateListener(new CameraPreview.StateListener(){
                public void previewSized(){}
                public void previewStarted(){}
                public void previewStopped(){}
                public void cameraClosed(){}
                public void cameraError(Exception error){fail();}
            });
            root.addView(scanner,new LinearLayout.LayoutParams(-1,0,1));
            Button cancel=new Button(this);cancel.setText(tr("취소","Cancel"));
            cancel.setOnClickListener(v->finish());root.addView(cancel,new LinearLayout.LayoutParams(-1,-2));
            setContentView(root);SystemInsets.apply(this,root);
            scanner.decodeSingle(new BarcodeCallback(){
                public void barcodeResult(BarcodeResult result){
                    if(completed||!resumed||isFinishing()||result==null||result.getText()==null)return;
                    completed=true;
                    setResult(RESULT_OK,new Intent().putExtra(EXTRA_TEXT,result.getText()));
                    finish();
                }
                public void possibleResultPoints(List<ResultPoint> points){}
            });
        }catch(RuntimeException e){fail();}
    }
    private void fail(){
        if(completed||isFinishing())return;
        completed=true;setResult(RESULT_CANCELED,new Intent().putExtra(EXTRA_ERROR,true));finish();
    }
    @Override protected void onResume(){
        super.onResume();
        if(completed||isFinishing()||scanner==null)return;
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){fail();return;}
        try{resumed=true;scanner.resume();}catch(RuntimeException e){fail();}
    }
    @Override protected void onPause(){
        resumed=false;
        try{if(scanner!=null)scanner.pause();}catch(RuntimeException ignored){}
        super.onPause();
    }
}
