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
  ui=new KitUi(this,0xff08748e,0xff83d8ed);
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

 private int points,fish;
 private long last,feedAnimation;
 private TextView count,collection;
 private Tank tank;
 private Spinner backgrounds;
 private Button unlock;

 private String[] names(){
  return new String[]{t("Silver fish","은빛 물고기","銀色の魚"),t("Clownfish","흰동가리","クマノミ"),
   t("Pufferfish","복어","フグ"),t("Crab","게","カニ"),t("Octopus","문어","タコ"),t("Jellyfish","해파리","クラゲ")};
 }

 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  setup(t("A little illustrated aquarium. Feed your pets and unlock species.","작은 수족관을 꾸미고 먹이를 주며 생물을 모아 보세요.","小さな水槽で餌をあげ、生き物を集めよう。"));
  points=Rules.bound(prefs.getInt("points",0));
  fish=Math.max(1,Math.min(Rules.SPECIES,prefs.getInt("fish",1)));
  last=prefs.getLong("last",0);
  tank=new Tank(this);
  visual=tank;
  hero(tank,340);
  tank.setOnClickListener(view->{buzz(view);feed();});
  setupControls();
  count=label("",20,true);
  button(t("Feed fish +1","먹이 주기 +1","餌をあげる +1"),true,this::feed);
  unlock=button(t("Unlock next species (5 points)","다음 생물 해금 (5점)","次の生き物を解放（5点）"),false,this::unlockSpecies);
  backgrounds=options(t("Tank theme","수조 테마","水槽テーマ"),t("Ocean","바다","海"),t("Lagoon","석호","ラグーン"),t("Night","밤바다","夜の海"));
  backgrounds.setSelection(Math.max(0,Math.min(2,prefs.getInt("theme",0))));
  change(backgrounds,()->{save();tank.invalidate();});
  label(t("Species collection","생물 컬렉션","生き物コレクション"),18,true).setAccessibilityHeading(true);
  collection=label("",16,false);
  refresh();
 }

 private void feed(){
  long now=System.currentTimeMillis();
  if(!Rules.cooldownElapsed(now,last,Rules.FEED_INTERVAL)){
   status.setText(t("Give them a moment","조금 기다려 주세요","少し待ってね"));
   return;
  }
  last=now;
  feedAnimation=android.os.SystemClock.uptimeMillis();
  points=Rules.bound(points+1);
  save();
  refresh();
  status.setText(t("Fish fed.","먹이를 줬어요.","餌をあげました。"));
 }

 private void unlockSpecies(){
  if(!Rules.canUnlock(fish,points)){
   status.setText(t("You need 5 feed points to unlock the next species.",
    "다음 생물을 해금하려면 먹이 점수 5점이 필요해요.","次の生き物の解放には餌ポイントが5点必要です。"));
   return;
  }
  points-=Rules.UNLOCK_COST;
  String species=names()[fish++];
  save();
  refresh();
  status.setText(t("New species: ","새 생물: ","新しい生き物：")+species);
 }

 private void save(){
  prefs.edit().putInt("points",points).putInt("fish",fish).putLong("last",last)
   .putInt("theme",backgrounds.getSelectedItemPosition()).apply();
 }
 private void refresh(){
  count.setText(t("Feed points: ","먹이 점수: ","餌ポイント：")+points+"  ·  "+fish+"/"+Rules.SPECIES);
  String[] species=names();
  StringBuilder list=new StringBuilder();
  for(int i=0;i<fish;i++){
   if(i>0)list.append("\n");
   list.append(species[i]);
  }
  if(fish<Rules.SPECIES)list.append("\n").append(t("Next: ","다음: ","次：")).append(species[fish]);
  collection.setText(list);
  unlock.setEnabled(fish<Rules.SPECIES);
  tank.setContentDescription(t("Aquarium with "+fish+" species. Feed fish","수족관 생물 "+fish+"종. 먹이 주기","水槽の生き物 "+fish+"種。餌をあげる"));
  tank.invalidate();
 }

 private void fishArt(Canvas c,Paint p,float x,float y,float r,int type,boolean left){c.save();c.translate(x,y);if(left)c.scale(-1,1);int[] palette={0xffb4d9f6,0xffff9948,0xfff1cf72,0xffec7167,0xffcf9af0,0xff89f0d8};p.setColor(palette[type]);p.setStyle(Paint.Style.FILL);
 if(type==3){for(int a=0;a<4;a++){float k=(a-1.5f)*r*.35f;p.setStrokeWidth(r*.13f);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(-r*.55f,k,-r*1.05f,k+r*.2f,p);c.drawLine(r*.55f,k,r*1.05f,k+r*.2f,p);}c.drawOval(-r*.7f,-r*.5f,r*.7f,r*.5f,p);c.drawCircle(-r*.75f,-r*.5f,r*.25f,p);c.drawCircle(r*.75f,-r*.5f,r*.25f,p);}
 else if(type==4||type==5){c.drawOval(-r*.7f,-r*.8f,r*.7f,r*.4f,p);p.setStrokeWidth(r*.13f);p.setStrokeCap(Paint.Cap.ROUND);for(int a=0;a<6;a++){float k=(a-2.5f)*r*.21f;Path arm=new Path();arm.moveTo(k,r*.12f);arm.cubicTo(k-r*.3f,r*.5f,k+r*.3f,r*.6f,k,r*1.05f);p.setStyle(Paint.Style.STROKE);c.drawPath(arm,p);}p.setStyle(Paint.Style.FILL);}
 else{Path tail=new Path();tail.moveTo(-r*.62f,0);tail.lineTo(-r*1.25f,-r*.55f);tail.lineTo(-r*1.25f,r*.55f);tail.close();c.drawPath(tail,p);c.drawOval(-r*.85f,-r*.60f,r*.9f,r*.60f,p);p.setColor(0x55ffffff);c.drawOval(-r*.55f,-r*.44f,r*.48f,-r*.05f,p);if(type==1){p.setColor(Color.WHITE);c.drawRoundRect(-r*.28f,-r*.54f,-r*.05f,r*.54f,r*.07f,r*.07f,p);c.drawRoundRect(r*.28f,-r*.45f,r*.49f,r*.45f,r*.07f,r*.07f,p);}}
 p.setColor(Color.WHITE);c.drawCircle(r*.42f,-r*.18f,r*.14f,p);p.setColor(0xff172337);c.drawCircle(r*.46f,-r*.18f,r*.075f,p);c.restore();}

 private class Tank extends View{Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Tank(Context c){super(c);}protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();int theme=backgrounds==null?0:Math.max(0,backgrounds.getSelectedItemPosition());int[][] palettes={{0xff46c3df,0xff063a69},{0xff82dfc1,0xff12665d},{0xff293b76,0xff090e2d}};p.setShader(new LinearGradient(0,0,0,h,palettes[theme],null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);long time=shouldAnimate()?android.os.SystemClock.uptimeMillis():0;p.setColor(0xffd3b07a);c.drawOval(-w*.1f,h*.87f,w*1.1f,h*1.15f,p);for(int k=0;k<8;k++){float x=w*(k+.5f)/8;Path weed=new Path();weed.moveTo(x,h*.94f);weed.quadTo(x+dp(18),h*.79f,x+dp(5),h*(.65f+(k%3)*.08f));weed.quadTo(x+dp(2),h*.83f,x-dp(8),h*.94f);weed.close();p.setColor(k%2==0?0xff27795e:0xff44986c);c.drawPath(weed,p);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.5f));p.setColor(0x80ffffff);for(int k=0;k<9;k++){float x=w*(k+.5f)/9,y=h-((time/18f+k*h/9)%h);c.drawCircle(x,y,dp(3+k%4),p);}p.setStyle(Paint.Style.FILL);long elapsed=time-feedAnimation;if(feedAnimation>0&&elapsed>=0&&elapsed<1400){p.setColor(0xffffd58a);for(int k=0;k<6;k++)c.drawCircle(w*(.26f+k*.095f),h*.12f+elapsed/1400f*h*.62f+dp(k%3*7),dp(3),p);}for(int f=0;f<fish;f++){double phase=time/1700d+f*1.7;float r=dp(25+f%3*4),x=r*1.5f+(float)((Math.sin(phase)+1)*.5*Math.max(0,w-r*3));float y=h*(.23f+(f%3)*.21f)+(float)Math.sin(phase*2)*dp(9);fishArt(c,p,x,y,r,f,Math.cos(phase)<0);}if(isShown()&&shouldAnimate())postInvalidateDelayed(35);}}
 }
