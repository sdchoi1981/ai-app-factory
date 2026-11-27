package factory.starter;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import factory.core.LocalPreferences;
import factory.ui.SystemInsets;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        LocalPreferences storage = new LocalPreferences(this, "app-preferences");

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int inset = Math.round(16 * getResources().getDisplayMetrics().density);
        content.setPadding(inset, inset, inset, inset);
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28);
        if (android.os.Build.VERSION.SDK_INT >= 28) title.setAccessibilityHeading(true);
        content.addView(title);

        TextView count = new TextView(this);
        count.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
        count.setText(getString(R.string.count, storage.getInt("count", 0)));
        content.addView(count);

        Button increment = new Button(this);
        increment.setText(R.string.increment);
        increment.setOnClickListener(view -> {
            int previous = storage.getInt("count", 0);
            int next = previous == Integer.MAX_VALUE ? 1 : previous + 1;
            storage.putInt("count", next);
            count.setText(getString(R.string.count, next));
        });
        content.addView(increment);

        setContentView(scroll);
        SystemInsets.apply(this, content);
    }
}
