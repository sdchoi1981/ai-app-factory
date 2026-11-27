package factory.fun;
import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.animation.ValueAnimator;
import factory.ui.KitUi;
import factory.ui.SystemInsets;
public final class MainActivity extends Activity {


 private KitUi ui;
 private LinearLayout root, controls;
 private TextView status;
 private SharedPreferences prefs;

 private String t(String en,String ko,String ja){
  String language=getResources().getConfiguration().getLocales().get(0).getLanguage();
  return "ko".equals(language)?ko:"ja".equals(language)?ja:en;
 }
 private int dp(float value){return ui.dp(value);}
 private LinearLayout.LayoutParams spaced(int height,int top,int bottom){return ui.space(height,top,bottom);}
 private void buzz(View view){view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);}

 private void setup(String intro){
  prefs=getSharedPreferences("local",MODE_PRIVATE);
  ui=new KitUi(this,0xff9d4308,0xffffbe80);
  ScrollView scroll=new ScrollView(this);
  scroll.setFillViewport(true);
  scroll.setBackgroundColor(ui.background);
  root=ui.column();
  FrameLayout centered=new FrameLayout(this);
  centered.addView(root,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL));
  scroll.addView(centered,new ViewGroup.LayoutParams(-1,-2));
  setContentView(scroll);
  SystemInsets.apply(this,scroll);
  TextView title=ui.text(getString(R.string.app_name),32,true);
  title.setAccessibilityHeading(true);
  root.addView(title);
  root.addView(ui.text(intro,16,false));
 }
 private void hero(View view,int height){
  ui.preview(view);
  root.addView(view,spaced(dp(height),8,12));
 }
 private void setupControls(){
  controls=ui.card(root);
  status=ui.text("",15,false);
  status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  controls.addView(status);
 }
 private TextView label(String text,int size,boolean heading){
  TextView view=ui.text(text,size,heading);
  controls.addView(view);
  return view;
 }
 private Button button(String text,boolean primary,Runnable action){
  Button button=new Button(this);
  button.setText(text);
  ui.button(button,primary);
  button.setOnClickListener(view->{buzz(view);action.run();});
  controls.addView(button,spaced(-2,4,4));
  return button;
 }
 private Spinner options(String title,String... items){
  TextView label=label(title,16,true);
  Spinner spinner=new Spinner(this);
  spinner.setId(View.generateViewId());
  label.setLabelFor(spinner.getId());
  spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items));
  ui.field(spinner);
  controls.addView(spinner,spaced(-2,4,8));
  return spinner;
 }
 private void change(Spinner spinner,Runnable action){
  spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
   public void onItemSelected(AdapterView<?> parent,View view,int position,long id){action.run();}
   public void onNothingSelected(AdapterView<?> parent){}
  });
 }
 private boolean shouldAnimate(){return ValueAnimator.areAnimatorsEnabled()&&getWindow().getDecorView().hasWindowFocus();}

 @Override public void onWindowFocusChanged(boolean hasFocus){
  super.onWindowFocusChanged(hasFocus);
  if(hasFocus&&visual!=null)visual.invalidate();
 }
 private View visual;

 private int unlocked,active;
 private final int[] xp=new int[Rules.SLOTS];
 private final boolean[] hatched=new boolean[Rules.SLOTS];
 private long last;
 private TextView pet;
 private Spinner select;
 private Creature creature;
 private ProgressBar evolution;
 private Button hatch,feed,play,unlock;

 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  setup(t("Hatch imaginary creatures, care for them and collect evolutions.","상상 속 몬스터를 부화시키고 돌보며 진화시켜요.","架空のモンスターを孵化・育成・進化させよう。"));
  unlocked=Math.max(1,Math.min(Rules.SLOTS,prefs.getInt("unlocked",1)));
  active=Math.max(0,Math.min(unlocked-1,prefs.getInt("active",0)));
  last=prefs.getLong("last",0);
  for(int i=0;i<Rules.SLOTS;i++){
   xp[i]=Rules.bound(prefs.getInt("xp"+i,0));
   hatched[i]=prefs.getBoolean("hatched"+i,false);
  }
  creature=new Creature(this);
  visual=creature;
  hero(creature,295);
  creature.setOnClickListener(view->{buzz(view);care(1);});
  setupControls();
  pet=label("",20,true);
  evolution=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
  evolution.setMax(Rules.EVOLUTION_XP);
  evolution.setProgressTintList(android.content.res.ColorStateList.valueOf(ui.accent));
  evolution.setContentDescription(t("Evolution progress","진화 진행률","進化進捗"));
  controls.addView(evolution,spaced(dp(18),4,14));
  select=options(t("Select creature","몬스터 선택","モンスター選択"),"1","2","3","4","5","6");
  select.setSelection(active);
  change(select,()->{
   int next=select.getSelectedItemPosition();
   if(next<0||next>=unlocked){
    select.setSelection(active);
    status.setText(t("Locked creature","아직 잠겨 있어요","未解放"));
    return;
   }
   if(active!=next)status.setText("");
   active=next;
   save();
   refresh();
  });
  hatch=button(t("Hatch egg","알 부화","卵を孵化"),true,this::hatchEgg);
  feed=button(t("Feed +1 XP","먹이 주기 +1 XP","餌 +1 XP"),true,()->care(1));
  play=button(t("Play +2 XP","놀아 주기 +2 XP","遊ぶ +2 XP"),false,()->care(2));
  unlock=button(t("Unlock next egg","다음 알 해금","次の卵を解放"),false,this::unlockEgg);
  refresh();
 }

 private void hatchEgg(){
  if(hatched[active])return;
  hatched[active]=true;
  save();
  refresh();
  status.setText(t("The egg hatched!","알이 부화했어요!","卵が孵化しました！"));
 }

 private void unlockEgg(){
  if(!Rules.canUnlock(unlocked,hatched[unlocked-1],xp[unlocked-1])){
   status.setText(t("Evolve the previous creature first (10 XP)","이전 몬스터를 먼저 진화시키세요 (10 XP)","前のモンスターを進化させよう（10 XP）"));
   return;
  }
  active=unlocked++;
  select.setSelection(active);
  save();
  refresh();
  status.setText(t("New egg unlocked.","새 알을 해금했어요.","新しい卵を解放しました。"));
 }

 private void care(int amount){
  if(!hatched[active]){
   status.setText(t("Hatch the egg first","알을 먼저 부화시키세요","先に孵化させよう"));
   return;
  }
  long now=System.currentTimeMillis();
  if(!Rules.cooldownElapsed(now,last)){
   status.setText(t("Rest for a moment","잠깐 쉬어요","少し休もう"));
   return;
  }
  last=now;
  boolean before=Rules.evolved(hatched[active],xp[active]);
  xp[active]=Rules.bound(xp[active]+amount);
  save();
  refresh();
  status.setText(!before&&Rules.evolved(hatched[active],xp[active])
   ?t("Your creature evolved!","몬스터가 진화했어요!","モンスターが進化しました！")
   :t("Care complete.","돌봄을 완료했어요.","お世話しました。"));
 }

 private void save(){
  SharedPreferences.Editor edit=prefs.edit().putInt("unlocked",unlocked).putInt("active",active).putLong("last",last);
  for(int i=0;i<Rules.SLOTS;i++)edit.putInt("xp"+i,xp[i]).putBoolean("hatched"+i,hatched[i]);
  edit.apply();
 }

 private void refresh(){
  boolean ready=hatched[active];
  boolean evolved=Rules.evolved(ready,xp[active]);
  String stage=!ready?t("Egg","알","卵"):evolved?t("Evolved","진화 완료","進化済み"):t("Baby","아기 몬스터","赤ちゃん");
  creature.setContentDescription(t("Creature ","몬스터 ","モンスター ")+(active+1)+" · "+stage
   +(ready?" · "+t("Feed","먹이 주기","餌をあげる"):""));
  creature.invalidate();
  evolution.setProgress(ready?Math.min(Rules.EVOLUTION_XP,xp[active]):0);
  pet.setText(stage+"  ·  XP "+xp[active]+(evolved?"":" / "+Rules.EVOLUTION_XP)
   +"\n"+t("Collection ","수집 ","コレクション ")+unlocked+"/"+Rules.SLOTS);
  hatch.setVisibility(ready?View.GONE:View.VISIBLE);
  feed.setVisibility(ready?View.VISIBLE:View.GONE);
  play.setVisibility(ready?View.VISIBLE:View.GONE);
  unlock.setEnabled(unlocked<Rules.SLOTS);
 }

 private class Creature extends View {Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Creature(Context c){super(c);}protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),r=Math.min(w,h)*.26f;float x=w/2,y=h*.48f+(shouldAnimate()?(float)Math.sin(android.os.SystemClock.uptimeMillis()/550d)*dp(4):0);p.setShader(new LinearGradient(0,0,w,h,new int[]{0xffffe8b6,0xfff3c1c7,0xffd9c8ff},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);p.setColor(0x26000000);c.drawOval(x-r,y+r*.8f,x+r,y+r*1.05f,p);c.save();c.translate(x,y);int[] palette={0xff6da5dd,0xfff0a051,0xffd986ae,0xff7cbe83,0xff9d8de5,0xff5abfb9};p.setColor(palette[active]);if(!hatched[active]){c.drawOval(-r*.72f,-r,r*.72f,r,p);p.setColor(0x66ffffff);c.drawOval(-r*.49f,-r*.75f,-r*.12f,-r*.12f,p);p.setColor(0xfffef3c7);c.drawCircle(r*.2f,r*.2f,r*.19f,p);c.drawCircle(-r*.26f,r*.45f,r*.11f,p);}else{boolean evolved=Rules.evolved(hatched[active],xp[active]);Path ear=new Path();ear.moveTo(-r*.8f,-r*.2f);ear.lineTo(-r*(evolved?1.12f:.82f),-r*1.14f);ear.lineTo(-r*.15f,-r*.62f);ear.close();c.drawPath(ear,p);c.save();c.scale(-1,1);c.drawPath(ear,p);c.restore();c.drawOval(-r,-r*.76f,r,r*.86f,p);p.setColor(0x66ffffff);c.drawOval(-r*.56f,r*.05f,r*.56f,r*.73f,p);p.setColor(Color.WHITE);c.drawOval(-r*.6f,-r*.35f,-r*.12f,r*.16f,p);c.drawOval(r*.12f,-r*.35f,r*.6f,r*.16f,p);p.setColor(0xff172337);c.drawCircle(-r*.32f,-r*.05f,r*.12f,p);c.drawCircle(r*.32f,-r*.05f,r*.12f,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(r*.055f);c.drawArc(-r*.2f,r*.15f,r*.2f,r*.39f,0,180,false,p);p.setStyle(Paint.Style.FILL);if(evolved){p.setColor(0xffffd85c);c.drawCircle(0,-r*.76f,r*.18f,p);}}c.restore();if(isShown()&&shouldAnimate())postInvalidateDelayed(40);}}

}
