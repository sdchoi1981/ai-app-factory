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

 private final Random random=new Random();private TextView result;private EditText choices;
 @Override public void onCreate(Bundle state){super.onCreate(state);setup(t("Choose, roll or play with friends. No accounts or network.","친구와 함께 선택, 주사위, 게임을 즐기세요.","友達と選択・サイコロ・ゲームを楽しもう。"));choices=edit(t("Options, one per line (2–30)","선택지 (한 줄에 하나, 2~30개)","候補（1行ずつ、2〜30個）"),t("Pizza\nChicken\nPasta","피자\n치킨\n파스타","ピザ\nチキン\nパスタ"));choices.setMinLines(3);choices.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2000)});result=label("🎲");result.setTextSize(30);result.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);button(t("Pick randomly","랜덤 선택","ランダム選択"),()->{try{String[] a=Rules.choices(choices.getText().toString());result.setText("🎯 "+a[random.nextInt(a.length)]);}catch(IllegalArgumentException e){status.setText(t("Enter 2–30 different options","서로 다른 선택지를 2~30개 입력하세요","異なる候補を2〜30個入力"));}});button(t("Coin toss","동전 던지기","コイントス"),()->result.setText(random.nextBoolean()?t("Heads","앞면","表"):t("Tails","뒷면","裏")));button(t("Roll two dice","주사위 두 개","サイコロ2個"),()->{int a=1+random.nextInt(6),b=1+random.nextInt(6);result.setText("🎲 "+a+" + "+b+" = "+(a+b));});button(t("Conversation card","대화 카드","会話カード"),()->{String[] cards={t("What is your dream holiday?","꿈꾸는 여행지는?","夢の旅行先は？"),t("Which food would you eat every day?","매일 먹고 싶은 음식은?","毎日食べたい料理は？"),t("Name a skill you want to learn.","배우고 싶은 기술은?","学びたいことは？"),t("What made you laugh recently?","최근 가장 웃겼던 일은?","最近笑ったことは？"),t("Invent a funny superhero name.","재미있는 영웅 이름을 지어 봐요.","面白いヒーローの名前を考えよう。")};result.setText(cards[random.nextInt(cards.length)]);});button(t("Family challenge","가족 미션","家族チャレンジ"),()->{String[] cards={t("Draw a cat in 20 seconds.","20초 동안 고양이를 그려요.","20秒で猫を描こう。"),t("Name five fruits.","과일 이름 다섯 개를 말해요.","果物を5つ言おう。"),t("Tell a kind joke.","기분 좋은 농담을 해요.","優しい冗談を言おう。")};result.setText(cards[random.nextInt(cards.length)]);});}

}
