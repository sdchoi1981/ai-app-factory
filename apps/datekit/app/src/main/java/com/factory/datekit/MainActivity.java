package com.factory.datekit;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import factory.ui.SystemInsets;

public final class MainActivity extends Activity {
    private LocalDate start, end;
    private Button startButton, endButton;
    private EditText offset;
    private TextView days, inclusive, countdown, offsetResult;
    private LinearLayout content;
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        start = LocalDate.now();
        end = start.plusDays(30);
        if (state != null) {
            try { start=DateMath.checked(LocalDate.parse(state.getString("start"))); end=DateMath.checked(LocalDate.parse(state.getString("end"))); }
            catch (RuntimeException ignored) { start=LocalDate.now(); end=start.plusDays(30); }
        }
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20),dp(20),dp(20),dp(28));
        scroll.addView(content,new ViewGroup.LayoutParams(-1,-2));
        TextView title=label(getString(R.string.app_name),32);
        if(android.os.Build.VERSION.SDK_INT>=28) title.setAccessibilityHeading(true);
        label(getString(R.string.subtitle),16);
        heading(R.string.difference_title);
        label(getString(R.string.start_date),15);
        startButton=button("",()->pick(true));
        label(getString(R.string.end_date),15);
        endButton=button("",()->pick(false));
        button(getString(R.string.swap),()->{LocalDate tmp=start;start=end;end=tmp;refresh();});
        days=result();
        inclusive=result();
        countdown=result();
        heading(R.string.offset_title);
        TextView hint=label(getString(R.string.offset_hint),15);
        offset=new EditText(this);
        offset.setId(android.view.View.generateViewId());
        hint.setLabelFor(offset.getId());
        offset.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        offset.setSingleLine(true);
        offset.setText(state==null?"100":state.getString("offset","100"));
        content.addView(offset,new LinearLayout.LayoutParams(-1,-2));
        offsetResult=result();
        offset.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int c,int f) {}
            public void onTextChanged(CharSequence s,int a,int b,int c) { refresh(); }
            public void afterTextChanged(Editable e) {}
        });
        button(getString(R.string.reset),()->{start=LocalDate.now();end=start.plusDays(30);offset.setText("100");refresh();});
        label(getString(R.string.offline),14);
        setContentView(scroll);
        SystemInsets.apply(this, content);
        refresh();
    }
    private TextView label(String value,int size) {
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);
        t.setPadding(0,dp(6),0,dp(6));content.addView(t,new LinearLayout.LayoutParams(-1,-2));return t;
    }
    private void heading(int id) {
        TextView t=label(getString(id),22);t.setPadding(0,dp(22),0,dp(8));
        if(android.os.Build.VERSION.SDK_INT>=28)t.setAccessibilityHeading(true);
    }
    private TextView result() { TextView t=label("",19);t.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);return t; }
    private Button button(String text,Runnable action) {
        Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setMinHeight(dp(48));
        b.setOnClickListener(v->action.run());content.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;
    }
    private String date(LocalDate d) { return d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(getResources().getConfiguration().getLocales().get(0))); }
    private void pick(boolean first) {
        LocalDate d=first?start:end;
        DatePickerDialog dialog=new DatePickerDialog(this,(v,y,m,day)->{
            LocalDate selected=LocalDate.of(y,m+1,day);
            if(first)start=selected;else end=selected;refresh();
        },d.getYear(),d.getMonthValue()-1,d.getDayOfMonth());
        java.util.Calendar min=java.util.Calendar.getInstance();min.clear();min.set(1900,0,1);
        java.util.Calendar max=java.util.Calendar.getInstance();max.clear();max.set(9999,11,31);
        dialog.getDatePicker().setMinDate(min.getTimeInMillis());
        dialog.getDatePicker().setMaxDate(max.getTimeInMillis());
        dialog.show();
    }
    private void refresh() {
        startButton.setText(date(start));endButton.setText(date(end));
        days.setText(getString(R.string.days_result,DateMath.difference(start,end)));
        inclusive.setText(getString(R.string.inclusive_result,DateMath.inclusive(start,end)));
        countdown.setText(getString(R.string.dday_result,DateMath.dday(LocalDate.now(),end)));
        try {
            long n=Long.parseLong(offset.getText().toString().trim());
            offsetResult.setText(getString(R.string.offset_result,date(DateMath.offset(start,n))));
        } catch(RuntimeException e) {offsetResult.setText(R.string.invalid_offset);}
    }
    @Override protected void onResume(){super.onResume();if(offset!=null)refresh();}
    @Override protected void onSaveInstanceState(Bundle out){
        super.onSaveInstanceState(out);out.putString("start",start.toString());out.putString("end",end.toString());out.putString("offset",offset.getText().toString());
    }
}
