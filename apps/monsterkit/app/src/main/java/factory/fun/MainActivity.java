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

 private int unlocked,active;private int[] xp=new int[6];private boolean[] hatched=new boolean[6];private long last;private TextView pet;private Spinner select;
 private final String[] baby={"🐉","🐲","🦊","🦎","🐸","🐙"},grown={"🐲","🐉","🦁","🦖","🐊","🦑"};
 @Override public void onCreate(Bundle state){super.onCreate(state);setup(t("Hatch imaginary creatures, care for them and collect evolutions.","상상 속 몬스터를 부화시키고 돌보며 진화시켜요.","架空のモンスターを孵化・育成・進化させよう。"));unlocked=Math.max(1,Math.min(6,prefs.getInt("unlocked",1)));active=Math.max(0,Math.min(unlocked-1,prefs.getInt("active",0)));last=prefs.getLong("last",0);for(int i=0;i<6;i++){xp[i]=Rules.bound(prefs.getInt("xp"+i,0));hatched[i]=prefs.getBoolean("hatched"+i,false);}pet=label("");pet.setTextSize(36);select=options(t("Select creature","몬스터 선택","モンスター選択"),"1","2","3","4","5","6");select.setSelection(active);change(select,()->{int i=select.getSelectedItemPosition();if(i>=unlocked){select.setSelection(active);status.setText(t("Locked creature","아직 잠겨 있어요","未解放"));return;}active=i;save();refresh();});button(t("Hatch egg","알 부화","卵を孵化"),()->{if(!hatched[active]){hatched[active]=true;save();refresh();}});button(t("Feed +1 XP","먹이 주기 +1 XP","餌 +1 XP"),()->care(1));button(t("Play +2 XP","놀아 주기 +2 XP","遊ぶ +2 XP"),()->care(2));button(t("Unlock next egg","다음 알 해금","次の卵を解放"),()->{if(unlocked==6){status.setText(t("All unlocked","모두 해금했어요","すべて解放"));return;}if(!hatched[unlocked-1]||xp[unlocked-1]<10){status.setText(t("Evolve the previous creature first (10 XP)","이전 몬스터를 먼저 진화시키세요 (10 XP)","前のモンスターを進化させよう（10 XP）"));return;}unlocked++;active=unlocked-1;select.setSelection(active);save();refresh();});refresh();}
 private void care(int n){if(!hatched[active]){status.setText(t("Hatch the egg first","알을 먼저 부화시키세요","先に孵化させよう"));return;}long now=System.currentTimeMillis();if(now>=last&&now-last<2000){status.setText(t("Rest for a moment","잠깐 쉬어요","少し休もう"));return;}last=now;xp[active]=Rules.bound(xp[active]+n);save();refresh();}
 private void save(){SharedPreferences.Editor e=prefs.edit().putInt("unlocked",unlocked).putInt("active",active).putLong("last",last);for(int i=0;i<6;i++)e.putInt("xp"+i,xp[i]).putBoolean("hatched"+i,hatched[i]);e.apply();}
 private void refresh(){String icon=!hatched[active]?"🥚":xp[active]>=10?grown[active]:baby[active];pet.setText(icon+"\n"+(!hatched[active]?t("Egg","알","卵"):xp[active]>=10?t("Evolved!","진화 완료!","進化！"):t("Baby","아기 몬스터","赤ちゃん"))+"\nXP "+xp[active]+" / 10\n"+t("Collection ","수집 ","コレクション ")+unlocked+"/6");}

}
