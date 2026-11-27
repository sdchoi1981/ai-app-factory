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
    protected void cameraResult(int result){}
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
    @Override protected void onDestroy(){closed=true;worker.shutdownNow();if(output!=null)output.delete();if(capture!=null)capture.delete();super.onDestroy();}

    @Override public void onCreate(Bundle state){super.onCreate(state);setup(t("Compress and convert up to 20 photos. Batch results are saved as ZIP.","사진 20장까지 압축·변환합니다. 일괄 결과는 ZIP으로 저장합니다.","写真を最大20枚変換し、一括結果をZIPに保存します。"));
        pickButton=button(t("Choose photos","사진 선택","写真を選択"),()->pick("image/*",true));
        Spinner format=options(t("Output format","출력 형식","出力形式"),"JPEG","PNG","WebP");
        EditText max=number(t("Maximum side (pixels)","최대 가로·세로 (픽셀)","最大辺 (px)"),"2048"),quality=number(t("Quality (10–100)","품질 (10–100)","品質 (10–100)"),"90");
        Spinner limit=options(t("JPEG size limit","JPEG 목표 용량","JPEG 容量上限"),t("No limit","제한 없음","制限なし"),"100 KB","500 KB","1 MB");
        button(t("Convert & save","변환 및 저장","変換・保存"),()->{try{ArrayList<Uri> list=inputs();int f=format.getSelectedItemPosition(),side=parse(max,32,4096),q=parse(quality,10,100),cap=new int[]{0,100000,500000,1000000}[limit.getSelectedItemPosition()];if(f!=0&&cap!=0)throw new IllegalArgumentException(t("Size limit requires JPEG","용량 제한은 JPEG 전용","上限はJPEG専用"));String ext=new String[]{"jpg","png","webp"}[f];String mime=new String[]{"image/jpeg","image/png","image/webp"}[f];
          export(list.size()>1?"application/zip":mime,list.size()>1?"PhotoKit.zip":"PhotoKit."+ext,file->{try(OutputStream out=new FileOutputStream(file)){ZipOutputStream zip=list.size()>1?new ZipOutputStream(out):null;try{int n=0;for(Uri u:list){Bitmap b=image(u,side);try{byte[] bytes=f==0?jpegLimit(b,q,cap):encode(b,f==1?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.WEBP_LOSSY,q);if(zip!=null){zip.putNextEntry(new ZipEntry("photo-"+(++n)+"."+ext));zip.write(bytes);zip.closeEntry();}else out.write(bytes);}finally{b.recycle();}}}finally{if(zip!=null)zip.close();}}});
        }catch(Exception e){fail(e);}});
        label(t("Re-encoding does not copy source EXIF/GPS metadata. HEIC input depends on device decoding; HEIC output is not supported.","변환 시 원본 EXIF/GPS 정보는 복사하지 않습니다. HEIC 입력은 기기 지원에 따르며 HEIC 출력은 미지원입니다.","元のEXIF/GPSはコピーしません。HEIC入力は端末依存、出力は非対応です。"));
    }

}
