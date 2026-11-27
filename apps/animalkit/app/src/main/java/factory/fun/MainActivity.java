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
 private final String[] emoji={"🐘","🐬","🐯","🐧","🦒","🐙","🐍","🦉","🦘","🐝","🐢","🐼"};
 private String[] names,facts;
 private TextView title,detail,status;
 private LinearLayout answers;
 private Button quizButton;
 private AnimalGallery gallery;
 private int index,question=-1,lastQuestion=-1,selected=-1,score,attempts;
 private int[] options;
 private final Random random=new Random();

 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  setup(t("Explore animal facts and try a quiz. The pictures are emoji illustrations.",
          "동물의 특징을 알아보고 퀴즈를 풀어요. 그림은 이모지입니다.",
          "動物の特徴を学びクイズに挑戦。絵は絵文字のイラストです。"),0xff1a6e47,0xff9ce3b9);
 names=new String[]{t("Elephant","코끼리","ゾウ"),t("Dolphin","돌고래","イルカ"),t("Tiger","호랑이","トラ"),t("Penguin","펭귄","ペンギン"),t("Giraffe","기린","キリン"),t("Octopus","문어","タコ"),t("Snake","뱀","ヘビ"),t("Owl","올빼미","フクロウ"),t("Kangaroo","캥거루","カンガルー"),t("Honey bee","꿀벌","ミツバチ"),t("Turtle","거북","カメ"),t("Giant panda","자이언트 판다","ジャイアントパンダ")};
 facts=new String[]{t("Uses its long trunk to smell, drink and grasp objects.","긴 코로 냄새를 맡고 물을 마시며 물건을 집어요.","長い鼻で匂いを嗅ぎ、水を飲み、物をつかむ。"),t("Uses echolocation to locate objects in water.","반향정위로 물속 물체의 위치를 알아내요.","反響定位で水中の物体を見つける。"),t("A large cat with a striped coat.","줄무늬가 있는 큰 고양잇과 동물이에요.","縞模様の大型ネコ科動物。"),t("A flightless bird that swims using flippers.","날지 못하며 날개를 지느러미처럼 써서 헤엄쳐요.","飛べない鳥で、翼をひれのように使って泳ぐ。"),t("Has a very long neck and browses leaves high in trees.","목이 길어 높은 나무의 잎을 먹어요.","長い首で高い木の葉を食べる。"),t("Has eight arms with suckers.","빨판이 달린 다리가 여덟 개예요.","吸盤のある腕が8本ある。"),t("A reptile without legs; many species swallow prey whole.","다리가 없는 파충류로 많은 종이 먹이를 통째로 삼켜요.","脚のない爬虫類。多くの種は獲物を丸のみする。"),t("Many species hunt at night and have excellent hearing.","많은 종이 밤에 사냥하며 청각이 뛰어나요.","多くの種は夜に狩りをし、聴覚が優れている。"),t("A marsupial known for hopping; young develop in a pouch.","뛰어다니는 유대류로 새끼는 주머니에서 자라요.","跳ねて移動する有袋類。子は袋で育つ。"),t("Collects nectar and helps pollinate flowers.","꽃의 꿀을 모으며 꽃가루받이를 도와요.","花の蜜を集め、受粉を助ける。"),t("A reptile protected by a shell.","등딱지로 몸을 보호하는 파충류예요.","甲羅で体を守る爬虫類。"),t("A bear whose diet consists mostly of bamboo.","주로 대나무를 먹는 곰이에요.","主に竹を食べるクマ。")};

  LinearLayout feature=ui.card(root);
  gallery=new AnimalGallery(this);
  ui.preview(gallery);
  feature.addView(gallery,ui.space(ui.dp(260),0,8));
  title=ui.text("",24,true);
  title.setAccessibilityHeading(true);
  feature.addView(title);
  detail=ui.text("",17,false);
  feature.addView(detail);
  status=ui.text("",16,true);
  status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  feature.addView(status);

  LinearLayout actions=ui.card(root);
  button(actions,t("Previous animal","이전 동물","前の動物"),false,()->browse(-1));
  button(actions,t("Next animal","다음 동물","次の動物"),false,()->browse(1));
  quizButton=button(actions,t("New quiz","새 퀴즈","新しいクイズ"),true,this::quiz);
  answers=new LinearLayout(this);
  answers.setOrientation(LinearLayout.VERTICAL);
  feature.addView(answers);

  if(state!=null){
   index=state.getInt("index",0);
   question=state.getInt("question",-1);
   lastQuestion=state.getInt("lastQuestion",-1);
   selected=state.getInt("selected",-1);
   options=state.getIntArray("options");
   score=state.getInt("score",0);
   attempts=state.getInt("attempts",0);
  }
  render();
 }

 @Override protected void onSaveInstanceState(Bundle state){
  super.onSaveInstanceState(state);
  state.putInt("index",index);
  state.putInt("question",question);
  state.putInt("lastQuestion",lastQuestion);
  state.putInt("selected",selected);
  state.putIntArray("options",options);
  state.putInt("score",score);
  state.putInt("attempts",attempts);
 }

 private void browse(int shift){
  index=(index+shift+names.length)%names.length;
  question=-1;
  selected=-1;
  options=null;
  render();
 }

 private void quiz(){
  question=Rules.nextQuestion(lastQuestion,names.length,random);
  lastQuestion=question;
  options=Rules.options(question,names.length,random);
  selected=-1;
  render();
 }

 private void answer(int animal){
  if(selected>=0)return;
  selected=animal;
  attempts++;
  if(animal==question)score++;
  render();
  quizButton.requestFocus();
 }

 private void render(){
  answers.removeAllViews();
  boolean quizzing=question>=0;
  boolean mystery=quizzing&&selected<0;
  int animal=quizzing?question:index;
  title.setText(mystery?t("Which animal?","어떤 동물일까요?","どの動物？"):names[animal]);
  detail.setText(facts[animal]);
  gallery.setContentDescription(mystery
      ?t("Mystery animal","수수께끼 동물","謎の動物"):names[animal]);
  gallery.invalidate();
  if(mystery){
   status.setText(t("Choose one answer","정답을 하나 골라 보세요","答えを一つ選ぼう"));
   for(int option:options)button(answers,emoji[option]+" "+names[option],false,()->answer(option));
  }else if(quizzing){
   String feedback=selected==question?t("Correct!","정답!","正解！")
       :t("Answer: ","정답: ","答え: ")+names[question];
   status.setText(feedback+" · "+t("Score ","점수 ","得点 ")+score+" / "+attempts);
  }else{
   status.setText((index+1)+" / "+names.length);
  }
  quizButton.setText(quizzing?t("Next question","다음 문제","次の問題"):t("New quiz","새 퀴즈","新しいクイズ"));
 }

 private class AnimalGallery extends View {
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path hill=new Path();
  private Shader sky;
  AnimalGallery(Context context){super(context);}
  @Override protected void onSizeChanged(int w,int h,int oldWidth,int oldHeight){
   super.onSizeChanged(w,h,oldWidth,oldHeight);
   if(w>0&&h>0)sky=new LinearGradient(0,0,w,h,ui.tint,ui.outline,Shader.TileMode.CLAMP);
   hill.reset();
   hill.moveTo(0,h);
   hill.lineTo(0,h*.76f);
   hill.quadTo(w*.24f,h*.48f,w*.48f,h*.76f);
   hill.quadTo(w*.74f,h*.59f,w,h*.72f);
   hill.lineTo(w,h);
   hill.close();
  }
  @Override protected void onDraw(Canvas canvas){
   float w=getWidth(),h=getHeight();
   paint.setShader(sky);
   canvas.drawRect(0,0,w,h,paint);
   paint.setShader(null);
   paint.setColor(ui.surface);
   canvas.drawCircle(w*.8f,h*.2f,h*.14f,paint);
   paint.setColor(ui.accent);
   paint.setAlpha(80);
   canvas.drawPath(hill,paint);
   paint.setAlpha(255);
   boolean mystery=question>=0&&selected<0;
   paint.setColor(ui.ink);
   paint.setTypeface(Typeface.DEFAULT_BOLD);
   paint.setTextSize(Math.min(h*.55f,w*.53f));
   String image=mystery?"?":emoji[question>=0?question:index];
   canvas.drawText(image,(w-paint.measureText(image))/2,h*.70f,paint);
  }
 }
}
