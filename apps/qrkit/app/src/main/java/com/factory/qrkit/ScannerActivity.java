package com.factory.qrkit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CameraPreview;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import java.util.Collections;
import java.util.List;
import factory.ui.KitUi;
import factory.ui.SystemInsets;

public final class ScannerActivity extends Activity {
    public static final String EXTRA_TEXT="com.factory.qrkit.SCAN_TEXT";
    public static final String EXTRA_ERROR="com.factory.qrkit.SCAN_ERROR";
    private DecoratedBarcodeView scanner;
    private boolean resumed, completed;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){fail();return;}
        try{
            KitUi ui = new KitUi(this, 0xff184b45, 0xffa5ecd6);
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(ui.background);

            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.VERTICAL);
            header.setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(10));
            TextView title = ui.text(getString(R.string.scanner_title), 24, true);
            header.addView(title);
            TextView instructions = ui.text(getString(R.string.scanner_starting), 14, false);
            instructions.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            header.addView(instructions);
            root.addView(header);

            scanner = new DecoratedBarcodeView(this);
            scanner.setStatusText("");
            scanner.setDecoderFactory(new DefaultDecoderFactory(Collections.singletonList(BarcodeFormat.QR_CODE)));
            scanner.getBarcodeView().addStateListener(new CameraPreview.StateListener(){
                public void previewSized(){}
                public void previewStarted(){instructions.setText(R.string.scanner_align);}
                public void previewStopped(){}
                public void cameraClosed(){}
                public void cameraError(Exception error){fail();}
            });
            root.addView(scanner,new LinearLayout.LayoutParams(-1,0,1));

            Button cancel = new Button(this);
            cancel.setText(R.string.scanner_cancel);
            ui.button(cancel, false);
            cancel.setOnClickListener(view -> finish());
            LinearLayout.LayoutParams footer = ui.space(-2, 10, 12);
            footer.setMargins(ui.dp(16), ui.dp(10), ui.dp(16), ui.dp(12));
            root.addView(cancel, footer);

            setContentView(root);
            SystemInsets.apply(this, root);
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
