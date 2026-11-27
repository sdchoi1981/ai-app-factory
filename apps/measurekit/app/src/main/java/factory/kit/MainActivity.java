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

    private SensorManager manager;private Sensor gravity;private TextView readout;private float x,y,z,zeroX,zeroY;private boolean available;private LevelView level;
    @Override public void onCreate(Bundle state){super.onCreate(state);setup(t("Place phone on a surface or against an edge. Calibrate on a known level reference.","휴대전화를 평면이나 모서리에 놓고 기준면에서 영점을 보정하세요.","端末を面・辺に置き、基準面でゼロ補正します。"));
      readout=label("—");readout.setTextSize(25);level=new LevelView();level.setContentDescription(t("Bubble level","수평계","水準器"));root.addView(level,new LinearLayout.LayoutParams(-1,dp(220)));
      button(t("Set zero","영점 설정","ゼロ設定"),()->{if(available){zeroX=x;zeroY=y;update();}});button(t("Reset calibration","보정 초기화","補正リセット"),()->{zeroX=zeroY=0;update();});
      manager=(SensorManager)getSystemService(SENSOR_SERVICE);gravity=manager.getDefaultSensor(Sensor.TYPE_GRAVITY);if(gravity==null)gravity=manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
      label(t("Sensor readings are approximate, not for safety-critical or professional measurements.","센서 측정값은 참고용이며 전문·안전 측정용이 아닙니다.","参考値です。安全・専門測定には使用しないでください。"));
    }
    private final SensorEventListener listener=new SensorEventListener(){public void onAccuracyChanged(Sensor sensor,int accuracy){}public void onSensorChanged(SensorEvent e){float a=e.values[0],b=e.values[1],c=e.values[2];if(!Float.isFinite(a+b+c)||Math.abs(a)+Math.abs(b)+Math.abs(c)<0.1)return;x=(float)Math.toDegrees(Math.atan2(a,Math.sqrt(b*b+c*c)));y=(float)Math.toDegrees(Math.atan2(b,Math.sqrt(a*a+c*c)));z=c;available=true;update();}};
    @Override protected void onResume(){super.onResume();if(gravity!=null&&manager.registerListener(listener,gravity,SensorManager.SENSOR_DELAY_UI))status.setText(t("Sensor active","센서 작동 중","センサー作動中"));else status.setText(t("Sensor unavailable","센서 없음","センサーなし"));}
    @Override protected void onPause(){if(manager!=null)manager.unregisterListener(listener);super.onPause();}
    private void update(){if(!available)return;readout.setText(String.format(getResources().getConfiguration().getLocales().get(0),"X: %+.1f°   Y: %+.1f°",x-zeroX,y-zeroY));level.invalidate();}
    private class LevelView extends View{Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);LevelView(){super(MainActivity.this);}protected void onDraw(Canvas c){float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-dp(12);if(r<=0)return;p.setColor(0xffb9c6ce);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));c.drawCircle(cx,cy,r,p);c.drawCircle(cx,cy,r/4,p);c.drawLine(cx-r,cy,cx+r,cy,p);c.drawLine(cx,cy-r,cx,cy+r,p);p.setStyle(Paint.Style.FILL);p.setColor(0xff218c68);if(available){float a=Math.max(-1,Math.min(1,(x-zeroX)/45)),b=Math.max(-1,Math.min(1,(y-zeroY)/45));double length=Math.hypot(a,b);if(length>1){a/=length;b/=length;}c.drawCircle(cx+a*r*.8f,cy-b*r*.8f,dp(13),p);}}}

}
