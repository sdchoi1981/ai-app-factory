package factory.kit;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.zip.*;
import factory.ui.KitUi;import factory.ui.SystemInsets;
public final class MainActivity extends Activity {

    private static final int PICK=1,SAVE=2;
    private static final int MAX_PHOTOS=20;
    private final ArrayList<Uri> selected=new ArrayList<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LinearLayout root,page,header,photoWorkspace,outputWorkspace,footer,photoActions;
    private ScrollView workspaceScroll;
    private Button photosTab,outputTab;
    private TextView previewFeedback;
    private View previewEmpty;
    private Runnable convertAction;
    private boolean editingOutput;
    private TextView status,selectionInfo,resultInfo,photoPosition;
    private Button pickButton,runButton,previousPhoto,nextPhoto;
    private KitUi ui;
    private ImageView thumbnail;
    private ProgressBar progress;
    private int previewGeneration,previewIndex;
    private Bitmap thumbBitmap;
    private boolean busy,closed;
    private File output;
    private String t(String en,String ko,String ja){String lang=getResources().getConfiguration().getLocales().get(0).getLanguage();return "ko".equals(lang)?ko:"ja".equals(lang)?ja:en;}
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private void setup(){
        ui=new KitUi(this,0xff5543bd,0xffc0b4ff);
        LinearLayout shell=new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(ui.background);
        header=ui.column();
        header.setPadding(dp(20),dp(8),dp(20),dp(4));
        shell.addView(center(header, false));
        TextView title=ui.text(getString(R.string.app_name),28,true);
        title.setAccessibilityHeading(true);
        header.addView(title);
        LinearLayout tabs=new LinearLayout(this);
        photosTab=tab(t("Photos","사진","写真"),false);
        outputTab=tab(t("Output","출력","出力"),true);
        for(Button tab:new Button[]{photosTab,outputTab}){
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);
            params.setMargins(dp(3),0,dp(3),0);
            tabs.addView(tab,params);
        }
        header.addView(tabs);
        workspaceScroll=new ScrollView(this);
        workspaceScroll.setFillViewport(true);
        page=ui.column();
        page.setPadding(dp(20),dp(8),dp(20),dp(12));
        workspaceScroll.addView(center(page, true));
        shell.addView(workspaceScroll,new LinearLayout.LayoutParams(-1,0,1));
        footer=ui.column();
        footer.setPadding(dp(20),dp(2),dp(20),dp(9));
        shell.addView(center(footer, false));
        root=footer;
        status=label("");
        status.setVisibility(View.GONE);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        resultInfo=label("");
        resultInfo.setVisibility(View.GONE);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(ui.accent));
        progress.setVisibility(View.GONE);
        footer.addView(progress,ui.space(dp(4),0,3));
        runButton=button("",()->{
            if(editingOutput)convertAction.run();
            else if(!selected.isEmpty())showWorkspace(true);
            else pick("image/*",true);
        });
        ui.button(runButton,true);
        setContentView(shell);
        SystemInsets.apply(this,shell);
    }
    private FrameLayout center(LinearLayout content, boolean top){
        FrameLayout frame=new FrameLayout(this);
        frame.addView(content,new FrameLayout.LayoutParams(-1,-2,(top?Gravity.TOP:Gravity.CENTER_VERTICAL)|Gravity.CENTER_HORIZONTAL));
        return frame;
    }
    private Button tab(String name,boolean outputTab){
        Button button=new Button(this);
        button.setText(name);
        button.setOnClickListener(v->showWorkspace(outputTab));
        return button;
    }
    private void showWorkspace(boolean output){
        if(busy)return;
        boolean changed=editingOutput!=output;
        editingOutput=output;
        photoWorkspace.setVisibility(output?View.GONE:View.VISIBLE);
        outputWorkspace.setVisibility(output?View.VISIBLE:View.GONE);
        updateWorkspace();
        workspaceScroll.scrollTo(0,0);
        if(changed&&android.animation.ValueAnimator.areAnimatorsEnabled()){
            View visible=output?outputWorkspace:photoWorkspace;
            visible.animate().cancel();
            visible.setAlpha(.75f);
            visible.animate().alpha(1).setDuration(120).start();
        }
    }
    private void updateWorkspace(){
        ui.button(photosTab,!editingOutput);
        ui.button(outputTab,editingOutput);
        photosTab.setSelected(!editingOutput);
        outputTab.setSelected(editingOutput);
        photosTab.setEnabled(!busy);
        outputTab.setEnabled(!busy&&!selected.isEmpty());
        runButton.setText(editingOutput?t("Convert & save","변환 및 저장","変換・保存")
            :selected.isEmpty()?t("Choose photos","사진 선택","写真を選択")
            :t("Configure output","출력 설정","出力を設定"));
        runButton.setEnabled(!busy&&(!editingOutput||!selected.isEmpty()));
        status.setVisibility(status.length()==0?View.GONE:View.VISIBLE);
        resultInfo.setVisibility(!editingOutput||resultInfo.length()==0?View.GONE:View.VISIBLE);
    }
    private void setStatus(String message){status.setText(message);updateWorkspace();}
    private void setResult(String message){resultInfo.setText(message);updateWorkspace();}
    private TextView label(String s){TextView v=ui.text(s,15,false);root.addView(v,ui.space(-2,0,3));return v;}
    private TextView heading(String s){TextView v=ui.text(s,21,true);root.addView(v,ui.space(-2,0,4));return v;}
    private Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);ui.button(b,false);b.setOnClickListener(v->action.run());root.addView(b,ui.space(-2,3,4));return b;}
    private EditText number(String name,String value){label(name);EditText e=new EditText(this);e.setContentDescription(name);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);e.setText(value);e.setSingleLine(true);e.setTextColor(ui.ink);ui.field(e);root.addView(e,ui.space(-2,2,7));return e;}
    private void setWorking(boolean working){
        progress.setVisibility(working?View.VISIBLE:View.GONE);
        pickButton.setEnabled(!working);
        updateWorkspace();
        updatePreviewActions(working);
    }
    private void updatePreviewActions(boolean working){
        int count=selected.size();
        photoActions.setVisibility(count>1?View.VISIBLE:View.GONE);
        previewEmpty.setVisibility(count==0?View.VISIBLE:View.GONE);
        thumbnail.setVisibility(count==0?View.INVISIBLE:View.VISIBLE);
        pickButton.setVisibility(count==0?View.GONE:View.VISIBLE);
        previousPhoto.setEnabled(!working&&previewIndex>0);
        nextPhoto.setEnabled(!working&&previewIndex+1<count);
        photoPosition.setText(count==0?"":String.format(getResources().getConfiguration().getLocales().get(0),
            t("Photo %1$d of %2$d","사진 %1$d / %2$d","写真 %1$d / %2$d"),previewIndex+1,count));
    }
    private void movePreview(int direction){
        int target=previewIndex+direction;
        if(busy||target<0||target>=selected.size())return;
        previewIndex=target;
        updatePreviewActions(false);
        previewPhoto();
    }
    private void previewPhoto(){
        int ticket=++previewGeneration;
        thumbnail.setImageDrawable(null);
        previewFeedback.setVisibility(View.GONE);
        if(thumbBitmap!=null){thumbBitmap.recycle();thumbBitmap=null;}
        if(selected.isEmpty()){
            thumbnail.setContentDescription(t("No photo selected","선택한 사진 없음","写真が未選択です"));
            return;
        }
        thumbnail.setContentDescription(null);
        previewFeedback.setText(t("Loading photo…","사진을 불러오는 중…","写真を読み込み中…"));
        previewFeedback.setVisibility(View.VISIBLE);
        Uri u=selected.get(previewIndex);
        worker.execute(()->{
            Bitmap decoded=null;
            try{decoded=image(u,640);}catch(IOException|RuntimeException|OutOfMemoryError ignored){}
            Bitmap ready=decoded;
            runOnUiThread(()->{
                if(closed||ticket!=previewGeneration){if(ready!=null)ready.recycle();return;}
                if(ready!=null){
                    previewFeedback.setVisibility(View.GONE);
                    thumbnail.setContentDescription(t("Selected photo preview","선택한 사진 미리보기","選択した写真のプレビュー"));
                    thumbBitmap=ready;
                    thumbnail.setImageBitmap(ready);
                }else previewFeedback.setText(t("Preview unavailable","미리보기를 표시할 수 없습니다","プレビューを表示できません"));
            });
        });
    }
    private int parse(EditText e,int min,int max){int n=Integer.parseInt(e.getText().toString().trim());if(n<min||n>max)throw new IllegalArgumentException();return n;}
    private Spinner options(String name,String... items){label(name);Spinner s=new Spinner(this);s.setContentDescription(name);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items);s.setAdapter(a);ui.field(s);root.addView(s,ui.space(dp(52),2,7));return s;}
    private void pick(String mime,boolean multiple){if(busy)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType(mime);i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,multiple);try{startActivityForResult(i,PICK);}catch(ActivityNotFoundException e){setStatus(t("No file picker","파일 선택기 없음","ファイル選択不可"));}}
    private void selectedChanged(){setStatus("");selectionInfo.setText(t("Selected photos: ","선택한 사진: ","選択した写真: ")+selected.size()+"/20");setResult("");previewIndex=0;setWorking(false);previewPhoto();}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);
        if(req==PICK&&result==RESULT_OK&&data!=null){selected.clear();if(data.getClipData()!=null){ClipData clip=data.getClipData();for(int n=0;n<Math.min(clip.getItemCount(),MAX_PHOTOS);n++){Uri u=clip.getItemAt(n).getUri();if(u!=null&&!selected.contains(u))selected.add(u);}}else if(data.getData()!=null)selected.add(data.getData());selectedChanged();}
        if(req==SAVE){
            File source=output;
            output=null;
            Uri destination=result==RESULT_OK&&data!=null?data.getData():null;
            if(source!=null&&destination!=null){
                worker.execute(()->{
                    boolean saved=false;
                    try{
                        try(InputStream in=new FileInputStream(source);OutputStream out=getContentResolver().openOutputStream(destination,"wt")){
                            if(out==null)throw new IOException();
                            copy(in,out);
                        }
                        saved=true;
                    }catch(IOException|RuntimeException e){
                        try{DocumentsContract.deleteDocument(getContentResolver(),destination);}
                        catch(IOException|RuntimeException ignored){ /* Some providers cannot delete. */ }
                    }finally{source.delete();}
                    boolean success=saved;
                    runOnUiThread(()->finishSave(success
                        ?t("Saved successfully","저장 완료","保存完了")
                        :t("Save failed","저장 실패","保存失敗")));
                });
            }else{
                if(source!=null)source.delete();
                finishSave(t("Save canceled","저장 취소됨","保存を中止しました"));
            }
        }
    }
    private void finishSave(String message){
        if(closed)return;
        busy=false;
        setStatus(message);
        setWorking(false);
    }
    private void copy(InputStream in,OutputStream out)throws IOException{byte[] buf=new byte[32768];for(int n;(n=in.read(buf))!=-1;){out.write(buf,0,n);if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();}}
    private ArrayList<Uri> inputs(){if(selected.isEmpty())throw new IllegalArgumentException(t("Select files first","파일을 먼저 선택하세요","ファイルを選択してください"));return new ArrayList<>(selected);}
    private void fail(Exception e){setStatus(t("Check photo selection, format and numeric limits.","사진 선택, 형식과 입력 범위를 확인하세요.","写真の選択、形式、入力範囲を確認してください。"));}
    interface Producer{void write(File file)throws Exception;}
    private void export(String mime,String name,Producer task){if(busy)return;busy=true;setWorking(true);setResult("");setStatus(t("Processing…","처리 중…","処理中…"));worker.execute(()->{File f=null;try{f=File.createTempFile("kit-export-",".tmp",getCacheDir());task.write(f);if(f.length()==0)throw new IOException("empty");final File completed=f;runOnUiThread(()->{if(closed){completed.delete();return;}output=completed;setResult(t("Output size: ","결과 용량: ","出力サイズ: ")+android.text.format.Formatter.formatFileSize(this,completed.length()));Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(mime);i.putExtra(Intent.EXTRA_TITLE,name);try{startActivityForResult(i,SAVE);}catch(RuntimeException e){completed.delete();output=null;busy=false;setWorking(false);setStatus(t("No save picker","저장 앱 없음","保存アプリなし"));}});}catch(Exception|OutOfMemoryError e){if(f!=null)f.delete();runOnUiThread(()->{busy=false;if(!closed){setWorking(false);setStatus(t("Could not process files or meet the requested limit","파일 처리 또는 목표 용량 충족 실패","処理または指定サイズに失敗"));}});}});}
    private Bitmap image(Uri uri,int max)throws IOException{return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(),uri),(decoder,info,src)->{int w=info.getSize().getWidth(),h=info.getSize().getHeight();double scale=Math.min(1d,max/(double)Math.max(w,h));decoder.setTargetSize(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);});}
    private byte[] encode(Bitmap bitmap,Bitmap.CompressFormat format,int quality)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();if(!bitmap.compress(format,quality,b))throw new IOException("encode");return b.toByteArray();}
    private Bitmap white(Bitmap b){Bitmap out=Bitmap.createBitmap(b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(out);c.drawColor(Color.WHITE);c.drawBitmap(b,0,0,null);return out;}
    private byte[] jpegLimit(Bitmap original,int quality,int limit)throws IOException{Bitmap b=white(original);try{while(true){for(int q=quality;q>=10;q-=5){byte[] data=encode(b,Bitmap.CompressFormat.JPEG,q);if(limit==0||data.length<=limit)return data;}int w=b.getWidth(),h=b.getHeight();if(Math.max(w,h)<=32)throw new IOException("limit");Bitmap small=Bitmap.createScaledBitmap(b,Math.max(1,w*3/4),Math.max(1,h*3/4),true);if(small==b)throw new IOException("limit");b.recycle();b=small;}}finally{b.recycle();}}
    @Override protected void onDestroy(){closed=true;previewGeneration++;if(thumbnail!=null)thumbnail.setImageDrawable(null);if(thumbBitmap!=null){thumbBitmap.recycle();thumbBitmap=null;}worker.shutdown();if(output!=null)output.delete();super.onDestroy();}

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        setup();
        photoWorkspace=new LinearLayout(this);
        photoWorkspace.setOrientation(LinearLayout.VERTICAL);
        page.addView(photoWorkspace);
        root=photoWorkspace;
        TextView hero=heading(t("Photo workspace","사진 작업 공간","写真ワークスペース"));
        hero.setAccessibilityHeading(true);
        FrameLayout preview=new FrameLayout(this);
        ui.preview(preview);
        root.addView(preview,ui.space(dp(310),4,10));
        thumbnail=new ImageView(this);
        thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.addView(thumbnail,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout empty=new LinearLayout(this);
        empty.setGravity(Gravity.CENTER);
        empty.setOrientation(LinearLayout.VERTICAL);
        ImageView illustration=new ImageView(this);
        illustration.setImageResource(R.drawable.photo_workbench);
        illustration.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        empty.addView(illustration,new LinearLayout.LayoutParams(dp(160),dp(112)));
        TextView previewPrompt=ui.text(t("Choose up to 20 photos to begin.","사진을 최대 20장 선택하세요.","最大20枚の写真を選択してください。"),16,true);
        previewPrompt.setGravity(Gravity.CENTER);
        previewPrompt.setPadding(dp(12),dp(12),dp(12),0);
        empty.addView(previewPrompt);
        preview.addView(empty,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));
        previewEmpty=empty;
        previewFeedback=ui.text("",16,true);
        previewFeedback.setGravity(Gravity.CENTER);
        previewFeedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        previewFeedback.setVisibility(View.GONE);
        preview.addView(previewFeedback,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));

        selectionInfo=label(t("One photo saves as an image. A batch saves as ZIP.","사진 한 장은 이미지로, 여러 장은 ZIP으로 저장합니다.","1枚は画像、複数枚はZIPで保存されます。"));
        pickButton=button(t("Change photos","사진 바꾸기","写真を変更"),()->pick("image/*",true));
        photoPosition=label("");
        photoPosition.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        photoActions=new LinearLayout(this);
        root.addView(photoActions,ui.space(-2,0,4));
        previousPhoto=new Button(this);previousPhoto.setText(t("Previous photo","이전 사진","前の写真"));
        nextPhoto=new Button(this);nextPhoto.setText(t("Next photo","다음 사진","次の写真"));
        for(Button control:new Button[]{previousPhoto,nextPhoto}){
            ui.button(control,false);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1f);
            params.setMargins(dp(2),0,dp(2),0);
            photoActions.addView(control,params);
        }
        previousPhoto.setOnClickListener(v->movePreview(-1));
        nextPhoto.setOnClickListener(v->movePreview(1));

        outputWorkspace=new LinearLayout(this);
        outputWorkspace.setOrientation(LinearLayout.VERTICAL);
        page.addView(outputWorkspace);
        root=outputWorkspace;
        TextView settingsTitle=heading(t("Output recipe","출력 구성","出力レシピ"));
        settingsTitle.setAccessibilityHeading(true);
        label(t("Apply a preset or customize the format and size.","프리셋을 적용하거나 형식과 크기를 직접 설정하세요.","プリセットを適用するか、形式とサイズを設定してください。"));
        Spinner format=options(t("Output format","출력 형식","出力形式"),"JPEG","PNG","WebP");
        EditText max=number(t("Maximum side (pixels)","최대 가로·세로 (픽셀)","最大辺 (px)"),"2048"),quality=number(t("Quality (10–100)","품질 (10–100)","品質 (10–100)"),"90");
        Spinner limit=options(t("JPEG size limit","JPEG 목표 용량","JPEG 容量上限"),t("No limit","제한 없음","制限なし"),"100 KB","500 KB","1 MB");
        format.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                if(position!=0)limit.setSelection(0);
                limit.setEnabled(position==0);
                quality.setEnabled(position!=1);
            }
            @Override public void onNothingSelected(AdapterView<?> parent){}
        });
        root=ui.card(outputWorkspace);
        heading(t("Quick presets","빠른 프리셋","プリセット"));
        button(t("Small JPEG · 100 KB","작은 JPEG · 100 KB","小さなJPEG · 100 KB"),()->{format.setSelection(0);max.setText("1024");quality.setText("85");limit.setSelection(1);});
        button(t("High quality JPEG","고품질 JPEG","高画質JPEG"),()->{format.setSelection(0);max.setText("4096");quality.setText("95");limit.setSelection(0);});
        button(t("PNG conversion","PNG 변환","PNG変換"),()->{format.setSelection(1);max.setText("4096");quality.setText("90");limit.setSelection(0);});
        convertAction=()->{try{ArrayList<Uri> list=inputs();int f=format.getSelectedItemPosition(),side=parse(max,32,4096),q=parse(quality,10,100),cap=new int[]{0,100000,500000,1000000}[limit.getSelectedItemPosition()];if(f!=0&&cap!=0)throw new IllegalArgumentException(t("Size limit requires JPEG","용량 제한은 JPEG 전용","上限はJPEG専用"));String ext=new String[]{"jpg","png","webp"}[f];String mime=new String[]{"image/jpeg","image/png","image/webp"}[f];
          export(list.size()>1?"application/zip":mime,list.size()>1?"PhotoKit.zip":"PhotoKit."+ext,file->{try(OutputStream out=new FileOutputStream(file)){ZipOutputStream zip=list.size()>1?new ZipOutputStream(out):null;try{int n=0;for(Uri u:list){Bitmap b=image(u,side);try{byte[] bytes=f==0?jpegLimit(b,q,cap):encode(b,f==1?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.WEBP_LOSSY,q);if(zip!=null){zip.putNextEntry(new ZipEntry("photo-"+(++n)+"."+ext));zip.write(bytes);zip.closeEntry();}else out.write(bytes);}finally{b.recycle();}}}finally{if(zip!=null)zip.close();}}});
        }catch(Exception e){fail(e);}};
        root=outputWorkspace;label(t("Re-encoding does not copy source EXIF/GPS metadata. HEIC input depends on device decoding; HEIC output is not supported.","변환 시 원본 EXIF/GPS 정보는 복사하지 않습니다. HEIC 입력은 기기 지원에 따르며 HEIC 출력은 미지원입니다.","元のEXIF/GPSはコピーしません。HEIC入力は端末依存、出力は非対応です。"));
        showWorkspace(false);
        setWorking(false);
    }

}
