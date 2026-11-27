package factory.kit;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.graphics.pdf.*;import android.net.Uri;import android.view.*;import android.widget.*;import android.hardware.*;import android.provider.DocumentsContract;import java.io.*;import java.util.*;import java.util.concurrent.*;import java.util.zip.*;import java.security.*;
public final class MainActivity extends Activity {

    private static final int PICK=1,SAVE=2,CAMERA=3;
    private final ArrayList<Uri> selected=new ArrayList<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LinearLayout root; private TextView status; private Button pickButton,runButton;
    private boolean busy=false,closed=false; private File output,capture; private Uri captureUri;
    private String outputMime,outputName;
    private String t(String en,String ko,String ja){String lang=getResources().getConfiguration().getLocales().get(0).getLanguage();return "ko".equals(lang)?ko:"ja".equals(lang)?ja:en;}
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private void setup(String intro){
        ScrollView scroll=new ScrollView(this); root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(18),dp(20),dp(28));scroll.setFillViewport(true);scroll.addView(root);setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);return insets;});
        TextView title=new TextView(this);title.setText(getString(R.string.app_name));title.setTextSize(30);title.setTypeface(null,android.graphics.Typeface.BOLD);root.addView(title);
        label(intro); status=label(t("Ready","준비 완료","準備完了"));status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    }
    private TextView label(String s){TextView v=new TextView(this);v.setText(s);v.setTextSize(16);v.setPadding(0,dp(8),0,dp(8));root.addView(v);return v;}
    private Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);b.setMinHeight(dp(48));b.setAllCaps(false);b.setOnClickListener(v->action.run());root.addView(b);return b;}
    private EditText number(String name,String value){label(name);EditText e=new EditText(this);e.setContentDescription(name);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);e.setText(value);e.setSingleLine(true);root.addView(e);return e;}
    private int parse(EditText e,int min,int max){int n=Integer.parseInt(e.getText().toString());if(n<min||n>max)throw new IllegalArgumentException("range "+min+"–"+max);return n;}
    private Spinner options(String name,String... items){label(name);Spinner s=new Spinner(this);s.setContentDescription(name);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items);s.setAdapter(a);root.addView(s);return s;}
    private void pick(String mime,boolean multiple){if(busy)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType(mime);i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,multiple);try{startActivityForResult(i,PICK);}catch(ActivityNotFoundException e){status.setText(t("No file picker","파일 선택기 없음","ファイル選択不可"));}}
    private void selectedChanged(){status.setText(t("Selected: ","선택: ","選択: ")+selected.size());}
    protected void onPicked(){}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);
        if(req==PICK&&result==RESULT_OK&&data!=null){selected.clear();if(data.getClipData()!=null){ClipData clip=data.getClipData();for(int n=0;n<Math.min(clip.getItemCount(),20);n++){Uri u=clip.getItemAt(n).getUri();if(u!=null&&!selected.contains(u))selected.add(u);}}else if(data.getData()!=null)selected.add(data.getData());selectedChanged();onPicked();}
        if(req==SAVE){if(result==RESULT_OK&&data!=null&&data.getData()!=null&&output!=null){final Uri dest=data.getData();final File source=output;busy=true;worker.execute(()->{try(InputStream in=new FileInputStream(source);OutputStream out=getContentResolver().openOutputStream(dest,"wt")){if(out==null)throw new IOException("output");copy(in,out);post(t("Saved successfully","저장 완료","保存完了"));}catch(Exception e){post(t("Save failed","저장 실패","保存失敗"));}finally{source.delete();runOnUiThread(()->{output=null;busy=false;});}});}else{if(output!=null)output.delete();output=null;busy=false;}}
        if(req==CAMERA)cameraResult(result);
    }
    private void post(String message){runOnUiThread(()->{if(!closed)status.setText(message);});}
    private void copy(InputStream in,OutputStream out)throws IOException{byte[] buf=new byte[32768];for(int n;(n=in.read(buf))!=-1;){out.write(buf,0,n);if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();}}
    private ArrayList<Uri> inputs(){if(selected.isEmpty())throw new IllegalArgumentException(t("Select files first","파일을 먼저 선택하세요","ファイルを選択してください"));return new ArrayList<>(selected);}
    private void fail(Exception e){status.setText(t("Check selection or numeric limits: ","선택 및 입력 범위를 확인하세요: ","選択・入力範囲を確認: ")+(e instanceof IllegalArgumentException?e.getMessage():""));}
    interface Producer{void write(File file)throws Exception;}
    private void export(String mime,String name,Producer task){if(busy)return;busy=true;status.setText(t("Processing…","처리 중…","処理中…"));worker.execute(()->{File f=null;try{f=File.createTempFile("kit-export-",".tmp",getCacheDir());task.write(f);if(f.length()==0)throw new IOException("empty");final File completed=f;runOnUiThread(()->{if(closed){completed.delete();return;}output=completed;outputMime=mime;outputName=name;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(mime);i.putExtra(Intent.EXTRA_TITLE,name);try{startActivityForResult(i,SAVE);}catch(ActivityNotFoundException e){completed.delete();output=null;busy=false;status.setText(t("No save picker","저장 앱 없음","保存アプリなし"));}});}catch(Exception|OutOfMemoryError e){if(f!=null)f.delete();runOnUiThread(()->{busy=false;if(!closed)status.setText(t("Could not process files or meet the requested limit","파일 처리 또는 목표 용량 충족 실패","処理または指定サイズに失敗"));});}});}
    private Bitmap image(Uri uri,int max)throws IOException{return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(),uri),(decoder,info,src)->{int w=info.getSize().getWidth(),h=info.getSize().getHeight();double scale=Math.min(1d,max/(double)Math.max(w,h));decoder.setTargetSize(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);});}
    private byte[] encode(Bitmap bitmap,Bitmap.CompressFormat format,int quality)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();if(!bitmap.compress(format,quality,b))throw new IOException("encode");return b.toByteArray();}
    private Bitmap white(Bitmap b){Bitmap out=Bitmap.createBitmap(b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(out);c.drawColor(Color.WHITE);c.drawBitmap(b,0,0,null);return out;}
    private byte[] jpegLimit(Bitmap original,int quality,int limit)throws IOException{Bitmap b=white(original);try{while(true){for(int q=quality;q>=10;q-=5){byte[] data=encode(b,Bitmap.CompressFormat.JPEG,q);if(limit==0||data.length<=limit)return data;}int w=b.getWidth(),h=b.getHeight();if(Math.max(w,h)<=32)throw new IOException("limit");Bitmap small=Bitmap.createScaledBitmap(b,Math.max(1,w*3/4),Math.max(1,h*3/4),true);if(small==b)throw new IOException("limit");b.recycle();b=small;}}finally{b.recycle();}}
    protected void dispose(){closed=true;worker.shutdownNow();if(output!=null)output.delete();if(capture!=null)capture.delete();}

    @Override public void onCreate(Bundle state){super.onCreate(state);setup(t("Capture documents or select photos, then create a PDF.","문서를 촬영하거나 사진을 선택해 PDF로 저장하세요.","文書を撮影・選択してPDFに保存します。"));
      button(t("Capture page","페이지 촬영","ページ撮影"),()->{if(busy||selected.size()>=20)return;try{File dir=new File(getCacheDir(),"scan-capture");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("cache");capture=File.createTempFile("scan-",".jpg",dir);captureUri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".capture",capture);Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,captureUri);i.setClipData(ClipData.newRawUri("scan",captureUri));i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,CAMERA);}catch(Exception e){if(capture!=null)capture.delete();capture=null;status.setText(t("Camera unavailable","카메라 사용 불가","カメラ利用不可"));}});
      button(t("Choose photos (replaces selection)","사진 선택 (기존 선택 교체)","写真を選択（置換）"),()->pick("image/*",true));
      Spinner style=options(t("Document style","문서 스타일","文書スタイル"),t("Color","컬러","カラー"),t("Grayscale","흑백","グレースケール"));
      button(t("Clear pages","페이지 초기화","ページをクリア"),()->{if(!busy){selected.clear();selectedChanged();}});
      button(t("Save PDF","PDF 저장","PDF保存"),()->{try{ArrayList<Uri> list=inputs();boolean gray=style.getSelectedItemPosition()==1;export("application/pdf","ScanKit.pdf",file->{PdfDocument doc=new PdfDocument();try(OutputStream out=new FileOutputStream(file)){int n=0;for(Uri u:list){Bitmap b=image(u,2400);try{boolean landscape=b.getWidth()>b.getHeight();int w=landscape?842:595,h=landscape?595:842;PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(w,h,++n).create());Canvas c=page.getCanvas();c.drawColor(Color.WHITE);float scale=Math.min((w-32f)/b.getWidth(),(h-32f)/b.getHeight());float bw=b.getWidth()*scale,bh=b.getHeight()*scale;Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);if(gray){ColorMatrix matrix=new ColorMatrix();matrix.setSaturation(0);paint.setColorFilter(new ColorMatrixColorFilter(matrix));}c.drawBitmap(b,null,new RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),paint);doc.finishPage(page);}finally{b.recycle();}}doc.writeTo(out);}finally{doc.close();}});}catch(Exception e){fail(e);}});
      label(t("Up to 20 pages. Perspective correction and OCR are not included.","최대 20페이지. 자동 원근 보정과 OCR은 미포함입니다.","最大20ページ。自動台形補正・OCRは非対応です。"));
    }
    private final ArrayList<File> captured=new ArrayList<>();
    protected void cameraResult(int result){if(capture==null)return;if(result==RESULT_OK&&capture.length()>0){captured.add(capture);selected.add(captureUri);selectedChanged();}else capture.delete();if(captureUri!=null)revokeUriPermission(captureUri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);capture=null;captureUri=null;}
    @Override protected void onDestroy(){dispose();for(File f:captured)f.delete();super.onDestroy();}

}
