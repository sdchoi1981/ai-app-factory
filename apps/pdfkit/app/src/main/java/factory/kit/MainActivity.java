package factory.kit;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.*;
import android.graphics.pdf.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import factory.ui.KitUi;
import factory.ui.SystemInsets;

public final class MainActivity extends Activity {

    private static final int PICK=1,SAVE=2;
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
    private EditText number(String name,String value){
        TextView label=label(name);
        EditText field=new EditText(this);
        field.setId(View.generateViewId());
        label.setLabelFor(field.getId());
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        field.setText(value);
        field.setSingleLine(true);
        field.setTextColor(ui.ink);
        ui.field(field);
        root.addView(field,ui.space(-2,0,6));
        return field;
    }
    private void displayPreview(Bitmap image,String description){
        preview.setImageBitmap(image);
        preview.setContentDescription(image==null?null:description);
        preview.setImportantForAccessibility(image==null?View.IMPORTANT_FOR_ACCESSIBILITY_NO:View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        if(shownPreview!=null)shownPreview.recycle();
        shownPreview=image;
        previewText.setText(description);
    }
    private int parse(EditText e,int min,int max){int n=Integer.parseInt(e.getText().toString());if(n<min||n>max)throw new IllegalArgumentException("range "+min+"–"+max);return n;}
    private Spinner options(String name,String... items){label(name);Spinner s=new Spinner(this);s.setContentDescription(name);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items);s.setAdapter(a);ui.field(s);root.addView(s,ui.space(-2,0,6));return s;}
    private void pick(String mime,boolean multiple){if(busy)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType(mime);i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,multiple);try{startActivityForResult(i,PICK);}catch(ActivityNotFoundException e){status.setText(t("No file picker","파일 선택기 없음","ファイル選択不可"));}}
    private void selectedChanged(){status.setText(t("Selected: ","선택: ","選択: ")+selected.size());refreshPreview();}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);
        if(req==PICK&&result==RESULT_OK&&data!=null){selected.clear();if(data.getClipData()!=null){ClipData clip=data.getClipData();for(int n=0;n<Math.min(clip.getItemCount(),20);n++){Uri u=clip.getItemAt(n).getUri();if(u!=null&&!selected.contains(u))selected.add(u);}}else if(data.getData()!=null)selected.add(data.getData());selectedChanged();}
        if(req==SAVE){if(result==RESULT_OK&&data!=null&&data.getData()!=null&&output!=null){final Uri dest=data.getData();final File source=output;busy=true;worker.execute(()->{try{try(InputStream in=new FileInputStream(source);OutputStream out=getContentResolver().openOutputStream(dest,"wt")){if(out==null)throw new IOException("output");copy(in,out);}post(t("Saved successfully","저장 완료","保存完了"));}catch(Exception e){try{DocumentsContract.deleteDocument(getContentResolver(),dest);}catch(IOException|RuntimeException ignored){}post(t("Save failed","저장 실패","保存失敗"));}finally{source.delete();runOnUiThread(()->{output=null;busy=false;});}});}else{if(output!=null)output.delete();output=null;busy=false;}}
    }
    private void post(String message){runOnUiThread(()->{if(!closed)status.setText(message);});}
    private void copy(InputStream in,OutputStream out)throws IOException{byte[] buf=new byte[32768];for(int n;(n=in.read(buf))!=-1;){out.write(buf,0,n);if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();}}
    private ArrayList<Uri> inputs(){if(selected.isEmpty())throw new IllegalArgumentException(t("Select files first","파일을 먼저 선택하세요","ファイルを選択してください"));return new ArrayList<>(selected);}
    private void fail(Exception e){status.setText(t("Check selection or numeric limits: ","선택 및 입력 범위를 확인하세요: ","選択・入力範囲を確認: ")+(e instanceof IllegalArgumentException?e.getMessage():""));}
    interface Producer{void write(File file)throws Exception;}
    private void export(String mime,String name,Producer task){if(busy)return;busy=true;status.setText(t("Processing…","처리 중…","処理中…"));worker.execute(()->{File f=null;try{f=File.createTempFile("kit-export-",".tmp",getCacheDir());task.write(f);if(f.length()==0)throw new IOException("empty");final File completed=f;runOnUiThread(()->{if(closed){completed.delete();return;}output=completed;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(mime);i.putExtra(Intent.EXTRA_TITLE,name);try{startActivityForResult(i,SAVE);}catch(ActivityNotFoundException e){completed.delete();output=null;busy=false;status.setText(t("No save picker","저장 앱 없음","保存アプリなし"));}});}catch(Exception|OutOfMemoryError e){if(f!=null)f.delete();runOnUiThread(()->{busy=false;if(!closed)status.setText(t("Could not process files or meet the requested limit","파일 처리 또는 목표 용량 충족 실패","処理または指定サイズに失敗"));});}});}
    private byte[] encode(Bitmap bitmap,Bitmap.CompressFormat format,int quality)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();if(!bitmap.compress(format,quality,b))throw new IOException("encode");return b.toByteArray();}
    private void dispose(){
        closed=true;
        previewGeneration++;
        worker.shutdownNow();
        displayPreview(null,"");
        if(output!=null)output.delete();
    }

    @Override public void onCreate(Bundle state){super.onCreate(state);setup(t("Merge or extract PDF pages. Output is a rasterized PDF, not searchable text.","PDF 합치기 및 페이지 추출. 결과는 이미지 기반 PDF이며 검색 가능한 텍스트는 유지되지 않습니다.","PDFを結合・抽出。出力は画像化され、検索可能な文字は保持されません。"));
       root=ui.card(page);
       button(t("Choose PDFs in order","PDF 순서대로 선택","PDFを順番に選択"),false,()->pick("application/pdf",true));
       Spinner mode=options(t("Action","작업","操作"),t("Merge all pages","전체 페이지 합치기","全ページ結合"),t("Extract from first PDF","첫 PDF 페이지 추출","最初のPDFから抽出"),t("Render first PDF to images","첫 PDF를 이미지로 변환","最初のPDFを画像化"));
       EditText first=number(t("First page (extract/render)","시작 페이지 (추출/변환)","開始ページ"),"1"),last=number(t("Last page (extract/render)","끝 페이지 (추출/변환)","終了ページ"),"1"),side=number(t("Maximum rendered side (px)","렌더링 최대 변 (px)","最大辺 (px)"),"1600");
       button(t("Process & save","처리 및 저장","処理・保存"),true,()->{try{ArrayList<Uri> files=inputs();int m=mode.getSelectedItemPosition(),a=m==0?1:parse(first,1,10000),z=m==0?1:parse(last,a,10000),s=parse(side,300,2400);if(m!=0&&z-a>=100)throw new IllegalArgumentException(t("100 pages maximum","최대 100페이지","最大100ページ"));
          export(m==2?"application/zip":"application/pdf",m==2?"PDFKit-images.zip":"PDFKit.pdf",file->{PdfDocument doc=new PdfDocument();try(OutputStream out=new FileOutputStream(file)){ZipOutputStream zip=m==2?new ZipOutputStream(out):null;int count=0;try{for(int k=0;k<(m==0?files.size():1);k++){try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(files.get(k),"r")){if(fd==null)throw new IOException("PDF");try(PdfRenderer renderer=new PdfRenderer(ParcelFileDescriptor.dup(fd.getFileDescriptor()))){int start=m==0?0:a-1,end=m==0?renderer.getPageCount():z;if(end>renderer.getPageCount()||start>=end)throw new IOException("page range");for(int p=start;p<end;p++){if(++count>100)throw new IOException("page cap");try(PdfRenderer.Page page=renderer.openPage(p)){int w=page.getWidth(),h=page.getHeight();Bitmap b=render(page,s);try{if(zip!=null){zip.putNextEntry(new ZipEntry("page-"+count+".jpg"));zip.write(encode(b,Bitmap.CompressFormat.JPEG,90));zip.closeEntry();}else{PdfDocument.Page dest=doc.startPage(new PdfDocument.PageInfo.Builder(w,h,count).create());dest.getCanvas().drawBitmap(b,null,new Rect(0,0,w,h),new Paint(Paint.FILTER_BITMAP_FLAG));doc.finishPage(dest);}}finally{b.recycle();}}}}}}if(zip==null)doc.writeTo(out);}finally{if(zip!=null)zip.close();}}finally{doc.close();}});
       }catch(Exception e){fail(e);}});
       label(t("Up to 100 output pages. Password-protected PDFs are unsupported. Originals are not modified.","최대 100페이지. 암호화 PDF 미지원. 원본은 변경하지 않습니다.","最大100ページ。暗号化PDFは非対応。原本は変更しません。"));
    }


    private void refreshPreview(){
        int ticket=++previewGeneration;
        if(selected.isEmpty()){
            displayPreview(null,t("Choose a PDF to preview","미리 볼 PDF를 선택하세요","PDFを選択してください"));
            return;
        }
        displayPreview(null,t("Rendering the first page…","첫 페이지를 표시하는 중…","先頭ページを表示中…"));
        Uri uri=selected.get(0);
        worker.execute(()->{
            if(ticket!=previewGeneration)return;
            Bitmap image=null;
            int count=0;
            try(ParcelFileDescriptor descriptor=getContentResolver().openFileDescriptor(uri,"r")){
                if(descriptor==null)throw new IOException("PDF");
                try(PdfRenderer renderer=new PdfRenderer(ParcelFileDescriptor.dup(descriptor.getFileDescriptor()))){
                    count=renderer.getPageCount();
                    if(count>0) try(PdfRenderer.Page first=renderer.openPage(0)){
                        image=render(first,900);
                    }
                }
            }catch(IOException|RuntimeException|OutOfMemoryError ignored){}
            Bitmap result=image;
            int pages=count;
            runOnUiThread(()->{
                if(closed||ticket!=previewGeneration){if(result!=null)result.recycle();return;}
                String description=result==null?t("Preview unavailable","미리보기 불가","プレビュー不可"):
                    String.format(t("First PDF · page 1 of %d","첫 PDF · 1 / %d 페이지","最初のPDF · 1 / %dページ"),pages);
                displayPreview(result,description);
            });
        });
    }

    private static Bitmap render(PdfRenderer.Page page,int maxSide){
        int width=page.getWidth(),height=page.getHeight();
        double scale=maxSide/(double)Math.max(width,height);
        Bitmap bitmap=Bitmap.createBitmap(Math.max(1,(int)(width*scale)),Math.max(1,(int)(height*scale)),Bitmap.Config.ARGB_8888);
        try{
            bitmap.eraseColor(Color.WHITE);
            page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bitmap;
        }catch(RuntimeException|OutOfMemoryError failure){
            bitmap.recycle();
            throw failure;
        }
    }

    @Override protected void onDestroy(){
        dispose();
        super.onDestroy();
    }
}
