package factory.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared native design tokens. No dependencies, storage, or network. */
public final class KitUi {
    public final Context context;
    public final int background, surface, ink, muted, outline, accent, onAccent, tint;
    public KitUi(Context context, int lightAccent, int darkAccent) {
        this.context=context;
        boolean night=(context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        background=night?0xff101725:0xfff3f5fb;
        surface=night?0xff1c2739:Color.WHITE;
        ink=night?0xfff0f4ff:0xff172338;
        muted=night?0xffb7c4d8:0xff52647a;
        outline=night?0xff48576e:0xffdce3ee;
        accent=night?darkAccent:lightAccent;
        onAccent=night?0xff142039:Color.WHITE;
        tint=night?0xff293957:0xffe8edfa;
    }
    public int dp(float n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    public GradientDrawable shape(int color,int radius,int border){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border!=0)d.setStroke(dp(1),border);return d;
    }
    public LinearLayout.LayoutParams space(int height,int top,int bottom){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height);p.setMargins(0,dp(top),0,dp(bottom));return p;
    }
    public LinearLayout column(){
        LinearLayout v=new LinearLayout(context){
            @Override protected void onMeasure(int w,int h){int max=dp(640);int size=View.MeasureSpec.getSize(w);if(size>max)w=View.MeasureSpec.makeMeasureSpec(max,View.MeasureSpec.EXACTLY);super.onMeasure(w,h);}
        };v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(20),dp(16),dp(20),dp(28));return v;
    }
    public LinearLayout card(LinearLayout parent){
        LinearLayout v=new LinearLayout(context);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(16),dp(14),dp(16),dp(14));v.setBackground(shape(surface,22,outline));parent.addView(v,space(-2,5,10));return v;
    }
    public TextView text(String text,int size,boolean heading){
        TextView v=new TextView(context);v.setText(text);v.setTextSize(size);v.setTextColor(heading?ink:muted);v.setPadding(0,dp(6),0,dp(6));if(heading)v.setTypeface(null,Typeface.BOLD);return v;
    }
    public void button(Button b,boolean primary){
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled},shape(tint,14,outline));
        states.addState(new int[]{},new RippleDrawable(ColorStateList.valueOf(0x22808080),shape(primary?accent:surface,14,primary?0:outline),null));
        b.setBackgroundTintList(null);b.setBackground(states);b.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{muted,primary?onAccent:ink}));
        b.setAllCaps(false);b.setTextSize(15);b.setMinHeight(dp(52));b.setMinimumHeight(dp(52));b.setPadding(dp(12),dp(10),dp(12),dp(10));b.setGravity(Gravity.CENTER);
    }
    public void field(View v){v.setBackground(shape(surface,12,outline));v.setPadding(dp(12),dp(8),dp(12),dp(8));v.setMinimumHeight(dp(52));}
    public void preview(View v){v.setBackground(shape(tint,20,outline));v.setClipToOutline(true);}
}
