package factory.ads;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.ads.*;
import com.google.android.ump.*;

public abstract class BannerActivity extends Activity {
    private static final String TEST_BANNER="ca-app-pub-3940256099942544/9214589741";
    private LinearLayout slot;
    private Button privacy;
    private TextView label;
    private ConsentInformation consent;
    private AdView ad;
    private boolean active,dead,started,granted,initializing,initialized,loaded,keyboard;
    private int generation;
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private String text(String ko,String en){return getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ko")?ko:en;}
    @Override public void setContentView(View view){
        if(slot!=null)throw new IllegalStateException("Content already attached");
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(view,new LinearLayout.LayoutParams(-1,0,1));
        privacy=new Button(this);privacy.setText(text("개인정보 옵션","Privacy options"));
        privacy.setVisibility(View.GONE);privacy.setOnClickListener(v->changePrivacy());
        root.addView(privacy,new LinearLayout.LayoutParams(-1,-2));
        label=new TextView(this);label.setText(text("테스트 광고 · 수익 없음","Test advertisement · no revenue"));
        label.setGravity(android.view.Gravity.CENTER);label.setPadding(0,dp(8),0,dp(8));label.setVisibility(View.GONE);
        root.addView(label,new LinearLayout.LayoutParams(-1,-2));
        slot=new LinearLayout(this);slot.setGravity(android.view.Gravity.CENTER);slot.setVisibility(View.GONE);
        root.addView(slot,new LinearLayout.LayoutParams(-1,-2));
        super.setContentView(root);
        if(Build.VERSION.SDK_INT>=30){
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v,insets)->{
                Insets s=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
                v.setPadding(s.left,s.top,s.right,s.bottom);
                keyboard=insets.isVisible(WindowInsets.Type.ime());updateVisibility();return WindowInsets.CONSUMED;
            });
            root.requestApplyInsets();
        }
        if(!getPackageName().endsWith(".adtest"))return;
        try{consent=UserMessagingPlatform.getConsentInformation(this);}catch(RuntimeException e){consent=null;}
    }
    private boolean valid(){return !dead&&!isFinishing()&&!isDestroyed();}
    private boolean allowed(){return valid()&&granted&&consent!=null&&consent.canRequestAds();}
    private void refreshConsent(){
        if(started||consent==null||!active||!valid())return;
        started=true;final int g=++generation;
        try{
            consent.requestConsentInfoUpdate(this,new ConsentRequestParameters.Builder().build(),()->{
                if(!valid()||generation!=g)return;
                updatePrivacy();
                if(!active){started=false;return;}
                try{UserMessagingPlatform.loadAndShowConsentFormIfRequired(this,error->{
                    if(!valid()||generation!=g)return;
                    granted=error==null&&consent.canRequestAds();updatePrivacy();maybeLoad();
                });}catch(RuntimeException e){granted=false;clearAd();}
            },error->{if(valid()&&generation==g){granted=false;clearAd();updatePrivacy();}});
        }catch(RuntimeException e){granted=false;clearAd();}
    }
    private void updatePrivacy(){
        if(!valid()||privacy==null)return;
        boolean required=consent!=null&&consent.getPrivacyOptionsRequirementStatus()==ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
        privacy.setVisibility(required?View.VISIBLE:View.GONE);updateVisibility();
    }
    private void changePrivacy(){
        if(!valid()||!active||consent==null)return;
        final int g=++generation;granted=false;clearAd();
        try{UserMessagingPlatform.showPrivacyOptionsForm(this,error->{
            if(!valid()||generation!=g)return;
            granted=error==null&&consent.canRequestAds();updatePrivacy();maybeLoad();
        });}catch(RuntimeException e){granted=false;clearAd();}
    }
    private void maybeLoad(){
        if(!active||!allowed()||ad!=null||slot==null||initializing)return;
        if(initialized){loadBanner();return;}
        initializing=true;
        try{MobileAds.initialize(this,status->runOnUiThread(()->{initializing=false;initialized=true;loadBanner();}));}
        catch(RuntimeException e){initializing=false;clearAd();}
    }
    private void loadBanner(){
        if(!active||!allowed()||ad!=null||slot==null)return;
        slot.post(()->{
            if(!active||!allowed()||ad!=null)return;
            try{
                int width=Math.round(((View)slot.getParent()).getWidth()/getResources().getDisplayMetrics().density);
                if(width<=0)width=getResources().getConfiguration().screenWidthDp-32;
                if(width<120)return;
                AdView next=new AdView(this);
                next.setAdUnitId(TEST_BANNER);
                next.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this,width));
                ad=next;slot.addView(next,new LinearLayout.LayoutParams(-2,-2));
                next.setAdListener(new AdListener(){
                    @Override public void onAdLoaded(){if(ad!=next)return;if(!allowed()){clearAd();return;}loaded=true;updateVisibility();}
                    @Override public void onAdFailedToLoad(LoadAdError error){if(ad==next)clearAd();}
                });
                if(allowed())next.loadAd(new AdRequest.Builder().build());else clearAd();
            }catch(RuntimeException e){clearAd();}
        });
    }
    private void updateVisibility(){
        if(slot==null)return;
        boolean show=active&&!keyboard&&loaded&&allowed();
        slot.setVisibility(show?View.VISIBLE:View.GONE);label.setVisibility(show?View.VISIBLE:View.GONE);
        if(ad!=null){if(show)ad.resume();else ad.pause();}
    }
    private void clearAd(){
        loaded=false;if(ad!=null){AdView old=ad;ad=null;slot.removeView(old);old.destroy();}
        if(slot!=null)slot.setVisibility(View.GONE);if(label!=null)label.setVisibility(View.GONE);
    }
    @Override protected void onResume(){super.onResume();active=true;refreshConsent();maybeLoad();updateVisibility();}
    @Override protected void onPause(){active=false;updateVisibility();super.onPause();}
    @Override protected void onDestroy(){dead=true;generation++;clearAd();super.onDestroy();}
}
