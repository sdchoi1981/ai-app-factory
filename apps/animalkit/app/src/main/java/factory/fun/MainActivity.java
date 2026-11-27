package factory.fun;
import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import java.util.*;
public final class MainActivity extends Activity {

 private LinearLayout root;private ScrollView scroll;private TextView status;private SharedPreferences prefs;
 private String t(String en,String ko,String ja){String l=getResources().getConfiguration().getLocales().get(0).getLanguage();return "ko".equals(l)?ko:"ja".equals(l)?ja:en;}
 private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private void setup(String intro){prefs=getSharedPreferences("local",MODE_PRIVATE);scroll=new ScrollView(this);scroll.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(16),dp(20),dp(28));scroll.addView(root);setContentView(scroll);scroll.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets a=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(a.left,a.top,a.right,a.bottom);return i;});TextView title=label(getString(R.string.app_name));title.setTextSize(32);title.setTypeface(null,Typeface.BOLD);label(intro);status=label(t("Ready","준비 완료","準備完了"));status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);}
 private TextView label(String s){TextView v=new TextView(this);v.setText(s);v.setTextSize(17);v.setPadding(0,dp(8),0,dp(8));root.addView(v);return v;}
 private Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setMinHeight(dp(48));b.setOnClickListener(v->action.run());root.addView(b);return b;}
 private EditText edit(String name,String value){label(name);EditText e=new EditText(this);e.setContentDescription(name);e.setText(value);root.addView(e);return e;}
 private Spinner options(String title,String... items){label(title);Spinner s=new Spinner(this);s.setContentDescription(title);s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,items));root.addView(s);return s;}
 private void change(Spinner s,Runnable r){s.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){r.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});}

 private final String[] emoji={"🐘","🐬","🐯","🐧","🦒","🐙","🐍","🦉","🦘","🐝","🐢","🐼"};private String[] names,facts;private TextView card;private LinearLayout answers;private int idx=0,question=-1,score=0;private boolean answered;private final Random rng=new Random();
 @Override public void onCreate(Bundle state){super.onCreate(state);setup(t("Explore animal facts and test your knowledge. Emoji are illustrations, not photos.","동물의 특징을 알아보고 퀴즈를 풀어요. 그림은 이모지이며 실제 사진은 아닙니다.","動物の特徴を学びクイズに挑戦。絵文字は写真ではありません。"));
 names=new String[]{t("Elephant","코끼리","ゾウ"),t("Dolphin","돌고래","イルカ"),t("Tiger","호랑이","トラ"),t("Penguin","펭귄","ペンギン"),t("Giraffe","기린","キリン"),t("Octopus","문어","タコ"),t("Snake","뱀","ヘビ"),t("Owl","올빼미","フクロウ"),t("Kangaroo","캥거루","カンガルー"),t("Honey bee","꿀벌","ミツバチ"),t("Turtle","거북","カメ"),t("Giant panda","자이언트 판다","ジャイアントパンダ")};
 facts=new String[]{t("Uses its long trunk to smell, drink and grasp objects.","긴 코로 냄새를 맡고 물을 마시며 물건을 집어요.","長い鼻で匂いを嗅ぎ、水を飲み、物をつかむ。"),t("Uses echolocation to locate objects in water.","반향정위로 물속 물체의 위치를 알아내요.","反響定位で水中の物体を見つける。"),t("A large cat with a striped coat.","줄무늬가 있는 큰 고양잇과 동물이에요.","縞模様の大型ネコ科動物。"),t("A flightless bird that swims using flippers.","날지 못하며 날개를 지느러미처럼 써서 헤엄쳐요.","飛べない鳥で、翼をひれのように使って泳ぐ。"),t("Has a very long neck and browses leaves high in trees.","목이 길어 높은 나무의 잎을 먹어요.","長い首で高い木の葉を食べる。"),t("Has eight arms with suckers.","빨판이 달린 다리가 여덟 개예요.","吸盤のある腕が8本ある。"),t("A reptile without legs; many species swallow prey whole.","다리가 없는 파충류로 많은 종이 먹이를 통째로 삼켜요.","脚のない爬虫類。多くの種は獲物を丸のみする。"),t("Many species hunt at night and have excellent hearing.","많은 종이 밤에 사냥하며 청각이 뛰어나요.","多くの種は夜に狩りをし、聴覚が優れている。"),t("A marsupial known for hopping; young develop in a pouch.","뛰어다니는 유대류로 새끼는 주머니에서 자라요.","跳ねて移動する有袋類。子は袋で育つ。"),t("Collects nectar and helps pollinate flowers.","꽃의 꿀을 모으며 꽃가루받이를 도와요.","花の蜜を集め、受粉を助ける。"),t("A reptile protected by a shell.","등딱지로 몸을 보호하는 파충류예요.","甲羅で体を守る爬虫類。"),t("A bear whose diet consists mostly of bamboo.","주로 대나무를 먹는 곰이에요.","主に竹を食べるクマ。")};
 card=label("");card.setTextSize(23);button(t("Previous animal","이전 동물","前の動物"),()->{idx=(idx+names.length-1)%names.length;showAnimal();});button(t("Next animal","다음 동물","次の動物"),()->{idx=(idx+1)%names.length;showAnimal();});button(t("New quiz","새 퀴즈","新しいクイズ"),()->quiz());answers=new LinearLayout(this);answers.setOrientation(LinearLayout.VERTICAL);root.addView(answers);showAnimal();}
 private void showAnimal(){question=-1;answers.removeAllViews();card.setText(emoji[idx]+"  "+names[idx]+"\n"+facts[idx]+"\n"+(idx+1)+"/"+names.length);}
 private void quiz(){question=rng.nextInt(names.length);answered=false;answers.removeAllViews();card.setText(t("Which animal?","어떤 동물일까요?","どの動物？")+"\n"+facts[question]);int[] options=Rules.options(question,names.length,rng);for(int i:options){Button b=new Button(this);b.setText(emoji[i]+" "+names[i]);b.setAllCaps(false);b.setMinHeight(dp(48));b.setOnClickListener(v->{if(answered)return;answered=true;if(i==question)score++;status.setText((i==question?t("Correct!","정답!","正解！"):t("Answer: ","정답: ","答え: ")+names[question])+"  "+t("Score ","점수 ","得点 ")+score);for(int n=0;n<answers.getChildCount();n++)answers.getChildAt(n).setEnabled(false);});answers.addView(b);}}

}
