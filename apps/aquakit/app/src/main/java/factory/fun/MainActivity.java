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

 private int points,fish;private long last;private TextView count;private Tank tank;private Spinner backgrounds;private final String[] symbols={"🐟","🐠","🐡","🦀","🐙","🪼"};
 private String[] names(){return new String[]{t("Silver fish","은빛 물고기","銀色の魚"),t("Clownfish","흰동가리","クマノミ"),t("Pufferfish","복어","フグ"),t("Crab","게","カニ"),t("Octopus","문어","タコ"),t("Jellyfish","해파리","クラゲ")};}
 @Override public void onCreate(Bundle state){super.onCreate(state);setup(t("A little illustrated aquarium. Feed your pets and unlock species.","작은 수족관을 꾸미고 먹이를 주며 생물을 모아 보세요.","小さな水槽で餌をあげ、生き物を集めよう。"));points=Rules.bound(prefs.getInt("points",0));fish=Math.max(1,Math.min(6,prefs.getInt("fish",1)));last=prefs.getLong("last",0);count=label("");tank=new Tank(this);tank.setContentDescription(t("Animated aquarium","움직이는 수족관","動く水槽"));root.addView(tank,new LinearLayout.LayoutParams(-1,dp(280)));backgrounds=options(t("Tank theme","수조 테마","水槽テーマ"),t("Ocean","바다","海"),t("Lagoon","석호","ラグーン"),t("Night","밤바다","夜の海"));backgrounds.setSelection(Math.max(0,Math.min(2,prefs.getInt("theme",0))));change(backgrounds,()->{save();tank.invalidate();});button(t("Feed fish +1","먹이 주기 +1","餌をあげる +1"),()->{long now=System.currentTimeMillis();if(now>=last&&now-last<3000){status.setText(t("Give them a moment","조금 기다려 주세요","少し待ってね"));return;}last=now;points=Rules.bound(points+1);save();refresh();});button(t("Unlock next species (5 points)","다음 생물 해금 (5점)","次の生き物を解放（5点）"),()->{if(fish>=6){status.setText(t("Collection complete!","모두 모았어요!","コンプリート！"));return;}if(points<5){status.setText(t("Need 5 points","5점이 필요해요","5点が必要"));return;}points-=5;fish++;save();refresh();});refresh();}
 private void save(){prefs.edit().putInt("points",points).putInt("fish",fish).putLong("last",last).putInt("theme",backgrounds.getSelectedItemPosition()).apply();}
 private void refresh(){StringBuilder s=new StringBuilder(t("Feed points: ","먹이 점수: ","餌ポイント: ")).append(points).append("   ").append(fish).append("/6");for(int i=0;i<fish;i++)s.append("\n").append(symbols[i]).append(" ").append(names()[i]);count.setText(s);tank.invalidate();}
 private class Tank extends View{Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Tank(Context c){super(c);}protected void onDraw(Canvas c){int i=backgrounds==null?0:backgrounds.getSelectedItemPosition();c.drawColor(new int[]{0xff075985,0xff0f766e,0xff172554}[Math.max(0,i)]);p.setColor(0xffe2bc80);c.drawRect(0,getHeight()*.88f,getWidth(),getHeight(),p);p.setTextSize(dp(44));long time=android.os.SystemClock.uptimeMillis();for(int f=0;f<fish;f++){float margin=dp(45),x=(float)((Math.sin(time/1500d+f*1.7)+1)*.5*Math.max(0,getWidth()-margin));float y=dp(55)+(getHeight()-dp(85))*(f%3)/3f;c.drawText(symbols[f],x,y,p);}if(isShown())postInvalidateDelayed(45);}}

}
