package factory.kit;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.*;
import android.graphics.pdf.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import factory.ui.KitUi;
import factory.ui.SystemInsets;

public final class MainActivity extends Activity {

    private static final int PICK=1,SAVE=2,CAMERA=3;
    private final ArrayList<Uri> selected=new ArrayList<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LinearLayout page,root;
    private TextView status,previewText;
    private ImageView preview;
    private Bitmap shownPreview;
    private KitUi ui;
    private volatile int previewGeneration;
    private boolean busy,closed;
    private File output;
    private File capture;
    private Uri captureUri;
    private String t(String en,String ko,String ja){String lang=getResources().getConfiguration().getLocales().get(0).getLanguage();return "ko".equals(lang)?ko:"ja".equals(lang)?ja:en;}
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private void setup(String intro){
        ui=new KitUi(this,0xff3155b7,0xffa9bfff);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ui.background);
        FrameLayout frame=new FrameLayout(this);
        page=ui.column();
        frame.addView(page,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL));
        scroll.addView(frame);
        root=page;
        TextView title=ui.text(getString(R.string.app_name),30,true);
        title.setAccessibilityHeading(true);
        page.addView(title,ui.space(-2,0,0));
        label(intro);
        status=label(t("Ready","준비 완료","準備完了"));
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        setContentView(scroll);
        SystemInsets.apply(this,scroll);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        root=ui.card(page);
        TextView heading=ui.text(t("Preview","미리보기","プレビュー"),20,true);
        heading.setAccessibilityHeading(true);
        root.addView(heading,ui.space(-2,0,2));
        preview=new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setPadding(dp(8),dp(8),dp(8),dp(8));
        ui.preview(preview);
        root.addView(preview,ui.space(dp(280),4,4));
        previewText=label(t("Choose a file to preview","미리 볼 파일을 선택하세요","プレビューするファイルを選択"));
        previewText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    }
    private TextView label(String text){
        TextView view=ui.text(text,16,false);
        root.addView(view,ui.space(-2,0,2));
        return view;
    }
    private Button button(String text,boolean primary,Runnable action){
        Button view=new Button(this);
        view.setText(text);
        ui.button(view,primary);
        view.setOnClickListener(v->action.run());
        root.addView(view,ui.space(-2,3,4));
        return view;
    }
    private void displayPreview(Bitmap image,String description){
        preview.setImageBitmap(image);
        preview.setContentDescription(image==null?null:description);
        preview.setImportantForAccessibility(image==null?View.IMPORTANT_FOR_ACCESSIBILITY_NO:View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        if(shownPreview!=null)shownPreview.recycle();
        shownPreview=image;
        previewText.setText(description);
    }
    private Spinner options(String name,String... items){label(name);Spinner s=new Spinner(this);s.setContentDescription(name);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items);s.setAdapter(a);ui.field(s);root.addView(s,ui.space(-2,0,6));return s;}
    private void pick(String mime,boolean multiple){if(busy)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType(mime);i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,multiple);try{startActivityForResult(i,PICK);}catch(ActivityNotFoundException e){status.setText(t("No file picker","파일 선택기 없음","ファイル選択不可"));}}
    private void selectedChanged(){status.setText(t("Selected: ","선택: ","選択: ")+selected.size());refreshPreview();}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);
        if(req==PICK&&result==RESULT_OK&&data!=null){clearCaptured();selected.clear();if(data.getClipData()!=null){ClipData clip=data.getClipData();for(int n=0;n<Math.min(clip.getItemCount(),20);n++){Uri u=clip.getItemAt(n).getUri();if(u!=null&&!selected.contains(u))selected.add(u);}}else if(data.getData()!=null)selected.add(data.getData());pageIndex=0;selectedChanged();}
        if(req==SAVE){if(result==RESULT_OK&&data!=null&&data.getData()!=null&&output!=null){final Uri dest=data.getData();final File source=output;busy=true;worker.execute(()->{try{try(InputStream in=new FileInputStream(source);OutputStream out=getContentResolver().openOutputStream(dest,"wt")){if(out==null)throw new IOException("output");copy(in,out);}post(t("Saved successfully","저장 완료","保存完了"));}catch(Exception e){try{DocumentsContract.deleteDocument(getContentResolver(),dest);}catch(IOException|RuntimeException ignored){}post(t("Save failed","저장 실패","保存失敗"));}finally{source.delete();runOnUiThread(()->{output=null;busy=false;});}});}else{if(output!=null)output.delete();output=null;busy=false;}}
        if(req==CAMERA)cameraResult(result);
    }
    private void post(String message){runOnUiThread(()->{if(!closed)status.setText(message);});}
    private void copy(InputStream in,OutputStream out)throws IOException{byte[] buf=new byte[32768];for(int n;(n=in.read(buf))!=-1;){out.write(buf,0,n);if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();}}
    private ArrayList<Uri> inputs(){if(selected.isEmpty())throw new IllegalArgumentException(t("Select files first","파일을 먼저 선택하세요","ファイルを選択してください"));return new ArrayList<>(selected);}
    private void fail(Exception e){status.setText(t("Check selection or numeric limits: ","선택 및 입력 범위를 확인하세요: ","選択・入力範囲を確認: ")+(e instanceof IllegalArgumentException?e.getMessage():""));}
    interface Producer{void write(File file)throws Exception;}
    private void export(String mime,String name,Producer task){if(busy)return;busy=true;status.setText(t("Processing…","처리 중…","処理中…"));worker.execute(()->{File f=null;try{f=File.createTempFile("kit-export-",".tmp",getCacheDir());task.write(f);if(f.length()==0)throw new IOException("empty");final File completed=f;runOnUiThread(()->{if(closed){completed.delete();return;}output=completed;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(mime);i.putExtra(Intent.EXTRA_TITLE,name);try{startActivityForResult(i,SAVE);}catch(ActivityNotFoundException e){completed.delete();output=null;busy=false;status.setText(t("No save picker","저장 앱 없음","保存アプリなし"));}});}catch(Exception|OutOfMemoryError e){if(f!=null)f.delete();runOnUiThread(()->{busy=false;if(!closed)status.setText(t("Could not process files or meet the requested limit","파일 처리 또는 목표 용량 충족 실패","処理または指定サイズに失敗"));});}});}
    private Bitmap image(Uri uri,int max)throws IOException{return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(),uri),(decoder,info,src)->{int w=info.getSize().getWidth(),h=info.getSize().getHeight();double scale=Math.min(1d,max/(double)Math.max(w,h));decoder.setTargetSize(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);});}
    private void dispose(){
        closed=true;
        previewGeneration++;
        worker.shutdownNow();
        displayPreview(null,"");
        if(output!=null)output.delete();
    }

    @Override public void onCreate(Bundle state){super.onCreate(state);setup(t("Capture documents or select photos, then create a PDF.","문서를 촬영하거나 사진을 선택해 PDF로 저장하세요.","文書を撮影・選択してPDFに保存します。"));
      back=button(t("Previous page","이전 페이지","前のページ"),false,()->{if(!busy&&pageIndex>0){pageIndex--;refreshPreview();}});
      next=button(t("Next page","다음 페이지","次のページ"),false,()->{if(!busy&&pageIndex+1<selected.size()){pageIndex++;refreshPreview();}});
      root=ui.card(page);
      button(t("Capture page","페이지 촬영","ページ撮影"),false,()->{if(busy||capture!=null)return;if(selected.size()>=20){status.setText(t("Up to 20 pages","최대 20페이지","最大20ページ"));return;}try{File dir=new File(getCacheDir(),"scan-capture");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("cache");capture=File.createTempFile("scan-",".jpg",dir);captureUri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".capture",capture);Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,captureUri);i.setClipData(ClipData.newRawUri("scan",captureUri));i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,CAMERA);}catch(Exception e){releaseCapture();status.setText(t("Camera unavailable","카메라 사용 불가","カメラ利用不可"));}});
      button(t("Choose photos (replaces selection)","사진 선택 (기존 선택 교체)","写真を選択（置換）"),false,()->pick("image/*",true));
      documentStyle=options(t("Document style","문서 스타일","文書スタイル"),t("Color","컬러","カラー"),t("Grayscale","흑백","グレースケール"));
      documentStyle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
          @Override public void onNothingSelected(AdapterView<?> parent){applyPreviewStyle();}
          @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id){applyPreviewStyle();}
      });
      button(t("Clear pages","페이지 초기화","ページをクリア"),false,()->{if(!busy){clearCaptured();selected.clear();pageIndex=0;selectedChanged();}});
      button(t("Save PDF","PDF 저장","PDF保存"),true,()->{try{ArrayList<Uri> list=inputs();boolean gray=documentStyle.getSelectedItemPosition()==1;export("application/pdf","ScanKit.pdf",file->{PdfDocument doc=new PdfDocument();try(OutputStream out=new FileOutputStream(file)){int n=0;for(Uri u:list){Bitmap b=image(u,2400);try{boolean landscape=b.getWidth()>b.getHeight();int w=landscape?842:595,h=landscape?595:842;PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(w,h,++n).create());Canvas c=page.getCanvas();c.drawColor(Color.WHITE);float scale=Math.min((w-32f)/b.getWidth(),(h-32f)/b.getHeight());float bw=b.getWidth()*scale,bh=b.getHeight()*scale;Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);if(gray){ColorMatrix matrix=new ColorMatrix();matrix.setSaturation(0);paint.setColorFilter(new ColorMatrixColorFilter(matrix));}c.drawBitmap(b,null,new RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),paint);doc.finishPage(page);}finally{b.recycle();}}doc.writeTo(out);}finally{doc.close();}});}catch(Exception e){fail(e);}});
      refreshPreview();
      label(t("Up to 20 pages. Perspective correction and OCR are not included.","최대 20페이지. 자동 원근 보정과 OCR은 미포함입니다.","最大20ページ。自動台形補正・OCRは非対応です。"));
    }
    private final ArrayList<File> captured=new ArrayList<>();
    protected void cameraResult(int result){if(capture==null)return;if(result==RESULT_OK&&capture.length()>0){
        captured.add(capture);
        selected.add(captureUri);
        capture=null;
        pageIndex=selected.size()-1;
        selectedChanged();
    }else if(result==RESULT_OK){
        status.setText(t("No image was captured","촬영한 이미지가 없습니다","撮影画像がありません"));
    }
    releaseCapture();}
    private void releaseCapture(){
        if(captureUri!=null)revokeUriPermission(captureUri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if(capture!=null)capture.delete();
        capture=null;
        captureUri=null;
    }
    private void clearCaptured(){
        for(File file:captured)file.delete();
        captured.clear();
    }
    @Override protected void onDestroy(){
        dispose();
        releaseCapture();
        clearCaptured();
        super.onDestroy();
    }


    private void applyPreviewStyle(){
        if(documentStyle==null||documentStyle.getSelectedItemPosition()!=1){
            preview.clearColorFilter();
            return;
        }
        ColorMatrix grayscale=new ColorMatrix();
        grayscale.setSaturation(0);
        preview.setColorFilter(new ColorMatrixColorFilter(grayscale));
    }

    private int pageIndex;
    private Button back,next;
    private Spinner documentStyle;
    private void refreshPreview(){
        int ticket=++previewGeneration;
        back.setEnabled(pageIndex>0);
        next.setEnabled(pageIndex+1<selected.size());
        if(selected.isEmpty()){
            displayPreview(null,t("Capture or choose photos to preview","미리 볼 사진을 촬영하거나 선택하세요","写真を撮影するか選択してください"));
            return;
        }
        displayPreview(null,t("Loading page…","페이지 여는 중…","ページを読み込み中…"));
        Uri uri=selected.get(pageIndex);
        String description=String.format(t("Page %d of %d","%d / %d 페이지","%d / %dページ"),pageIndex+1,selected.size());
        worker.execute(()->{
            if(ticket!=previewGeneration)return;
            Bitmap image=null;
            try{image=image(uri,900);}catch(IOException|RuntimeException|OutOfMemoryError ignored){}
            Bitmap result=image;
            runOnUiThread(()->{
                if(closed||ticket!=previewGeneration){if(result!=null)result.recycle();return;}
                displayPreview(result,result==null?description+" · "+t("Preview unavailable","미리보기 불가","プレビュー不可"):description);
            });
        });
    }
}
