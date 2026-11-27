package factory.fun;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import factory.ui.KitUi;
import factory.ui.SystemInsets;
import java.util.Random;

public final class MainActivity extends Activity {
 private KitUi ui;
 private LinearLayout root;
 private String t(String en,String ko,String ja){
  String language=getResources().getConfiguration().getLocales().get(0).getLanguage();
  return "ko".equals(language)?ko:"ja".equals(language)?ja:en;
 }
 private void setup(String intro,int lightAccent,int darkAccent){
  ui=new KitUi(this,lightAccent,darkAccent);
  ScrollView scroll=new ScrollView(this);
  scroll.setFillViewport(true);
  scroll.setBackgroundColor(ui.background);
  root=ui.column();
  FrameLayout frame=new FrameLayout(this);
  frame.addView(root,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL));
  scroll.addView(frame);
  setContentView(scroll);
  SystemInsets.apply(this,scroll);
  TextView title=ui.text(getString(R.string.app_name),32,true);
  title.setAccessibilityHeading(true);
  root.addView(title);
  root.addView(ui.text(intro,16,false));
 }
 private Button button(LinearLayout parent,String text,boolean primary,Runnable action){
  Button button=new Button(this);
  button.setText(text);
  ui.button(button,primary);
  button.setOnClickListener(view->{
   view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
   action.run();
  });
  parent.addView(button,ui.space(-2,4,4));
  return button;
 }
 private final Random random=new Random();
 private TextView result,status,quickResult;
 private EditText choices;
 private Button spinButton;
 private Wheel wheel;
 private android.animation.ValueAnimator spin;

 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  setup(t("Choose, roll or play with friends. No accounts or network.",
          "친구와 함께 선택하고 주사위를 던져 보세요. 가입이나 인터넷 연결이 필요 없어요.",
          "友達と選択やサイコロを楽しもう。登録やネット接続は不要です。"),0xff6b2acd,0xffc7adff);
  LinearLayout decision=ui.card(root);
  wheel=new Wheel(this);
  wheel.setContentDescription(t("Decision wheel","선택 룰렛","ルーレット"));
  ui.preview(wheel);
  decision.addView(wheel,ui.space(ui.dp(265),0,8));
  result=ui.text(t("Choose a game to begin","게임을 선택해 시작하세요","ゲームを選んで始めよう"),24,true);
  result.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  decision.addView(result);
  status=ui.text("",15,false);
  status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  decision.addView(status);

  String prompt=t("Options, one per line (2–30)","선택지 (한 줄에 하나, 2~30개)","候補（1行ずつ、2〜30個）");
  decision.addView(ui.text(prompt,16,false));
  String defaults=t("Pizza\nChicken\nPasta","피자\n치킨\n파스타","ピザ\nチキン\nパスタ");
  choices=new EditText(this);
  choices.setContentDescription(prompt);
  choices.setSaveEnabled(false);
  choices.setText(defaults);
  choices.setTextColor(ui.ink);
  choices.setGravity(Gravity.TOP|Gravity.START);
  choices.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
  choices.setMinLines(3);
  choices.setMaxLines(6);
  choices.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2000)});
  ui.field(choices);
  decision.addView(choices,ui.space(-2,4,8));
  wheel.labels=Rules.choices(defaults);
  spinButton=button(decision,t("Spin the wheel","룰렛 돌리기","ルーレットを回す"),true,this::spinWheel);

  LinearLayout quick=ui.card(root);
  TextView quickTitle=ui.text(t("Quick games","빠른 게임","クイックゲーム"),21,true);
  quickTitle.setAccessibilityHeading(true);
  quick.addView(quickTitle);
  button(quick,t("Coin toss","동전 던지기","コイントス"),false,()->showResult(random.nextBoolean()
      ?t("Heads","앞면","表"):t("Tails","뒷면","裏")));
  button(quick,t("Roll two dice","주사위 두 개","サイコロ2個"),false,()->{
   int first=1+random.nextInt(6),second=1+random.nextInt(6);
   showResult("🎲 "+first+" + "+second+" = "+(first+second));
  });
  button(quick,t("Conversation card","대화 카드","会話カード"),false,()->chooseCard(
      t("What is your dream holiday?","꿈꾸는 여행지는?","夢の旅行先は？"),
      t("Which food would you eat every day?","매일 먹고 싶은 음식은?","毎日食べたい料理は？"),
      t("Name a skill you want to learn.","배우고 싶은 기술은?","学びたいことは？"),
      t("What made you laugh recently?","최근 가장 웃겼던 일은?","最近笑ったことは？"),
      t("Invent a funny superhero name.","재미있는 영웅 이름을 지어 봐요.","面白いヒーローの名前を考えよう。")));
  button(quick,t("Family challenge","가족 미션","家族チャレンジ"),false,()->chooseCard(
      t("Draw a cat in 20 seconds.","20초 동안 고양이를 그려요.","20秒で猫を描こう。"),
      t("Name five fruits.","과일 이름 다섯 개를 말해요.","果物を5つ言おう。"),
      t("Tell a kind joke.","기분 좋은 농담을 해요.","優しい冗談を言おう。")));
  quickResult=ui.text("",22,true);
  quickResult.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  quick.addView(quickResult);
 }

 private void chooseCard(String... cards){
  showResult(cards[random.nextInt(cards.length)]);
 }

 private void showResult(String text){
  if(spin!=null)spin.cancel();
  status.setText("");
  quickResult.setText(text);
  quickResult.post(()->quickResult.requestRectangleOnScreen(
      new Rect(0,0,quickResult.getWidth(),quickResult.getHeight()),false));
 }

 private void spinWheel(){
  if(spin!=null)return;
  String[] options;
  try{
   options=Rules.choices(choices.getText().toString());
  }catch(IllegalArgumentException failure){
   status.setText(t("Enter 2–30 different options","서로 다른 선택지를 2~30개 입력하세요","異なる候補を2〜30個入力"));
   choices.requestFocus();
   return;
  }
  int selected=random.nextInt(options.length);
  wheel.labels=options;
  spinButton.setEnabled(false);
  status.setText(t("Spinning…","룰렛을 돌리는 중…","回転中…"));
  float start=Rules.normalizedAngle(wheel.angle);
  float end=Rules.spinEnd(start,selected,options.length);
  spin=android.animation.ValueAnimator.ofFloat(start,end);
  spin.setDuration(1600);
  spin.setInterpolator(new android.view.animation.DecelerateInterpolator());
  spin.addUpdateListener(animation->{
   wheel.angle=(Float)animation.getAnimatedValue();
   wheel.invalidate();
  });
  spin.addListener(new android.animation.AnimatorListenerAdapter(){
   private boolean canceled;
   @Override public void onAnimationCancel(android.animation.Animator animation){canceled=true;}
   @Override public void onAnimationEnd(android.animation.Animator animation){
    spin=null;
    if(isFinishing()||isDestroyed())return;
    spinButton.setEnabled(true);
    status.setText("");
    if(!canceled){
     result.setText("🎯 "+options[selected]);
     wheel.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }
   }
  });
  spin.start();
 }

 @Override protected void onDestroy(){
  if(spin!=null){
   spin.removeAllListeners();
   spin.cancel();
  }
  super.onDestroy();
 }

 private class Wheel extends View {
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final int[] colors={0xff6d28d9,0xff0e687e,0xffa71555,0xffa74512,0xff067456};
  private final Path pointer=new Path();
  private final RectF bounds=new RectF();
  private float angle;
  private String[] labels;
  Wheel(Context context){super(context);}
  @Override protected void onDraw(Canvas canvas){
   float x=getWidth()/2f,y=getHeight()/2f,radius=Math.min(x,y)-ui.dp(14);
   if(radius<=0||labels==null)return;
   float arc=360f/labels.length;
   bounds.set(x-radius,y-radius,x+radius,y+radius);
   for(int i=0;i<labels.length;i++){
    float start=angle+i*arc;
    paint.setColor(colors[i%colors.length]);
    canvas.drawArc(bounds,start,arc,true,paint);
    String label=Rules.abbreviate(labels[i],12);
    paint.setColor(Color.WHITE);
    paint.setTypeface(Typeface.DEFAULT_BOLD);
    paint.setTextSize(ui.dp(labels.length>10?10:14));
    float maxWidth=radius*.64f,width=paint.measureText(label);
    if(width>maxWidth)paint.setTextSize(paint.getTextSize()*maxWidth/width);
    canvas.save();
    canvas.rotate(start+arc/2,x,y);
    canvas.drawText(label,x+radius*.25f,y+ui.dp(4),paint);
    canvas.restore();
   }
   paint.setColor(ui.ink);
   canvas.drawCircle(x,y,ui.dp(10),paint);
   pointer.reset();
   pointer.moveTo(x,y-radius+ui.dp(19));
   pointer.lineTo(x-ui.dp(12),y-radius-ui.dp(5));
   pointer.lineTo(x+ui.dp(12),y-radius-ui.dp(5));
   pointer.close();
   canvas.drawPath(pointer,paint);
  }
 }
}
