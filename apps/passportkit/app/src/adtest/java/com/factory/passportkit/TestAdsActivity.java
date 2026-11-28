package com.factory.passportkit;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
import factory.ads.BannerActivity;

public final class TestAdsActivity extends BannerActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        TextView message = new TextView(this);
        boolean korean = getResources().getConfiguration().getLocales().get(0)
                .getLanguage().equals("ko");
        message.setText(korean
                ? "PassportKit 테스트 광고\n이 테스트 앱에서는 사진을 열거나 촬영할 수 없습니다."
                : "PassportKit test ads\nPhoto import and capture are unavailable in this test app.");
        message.setGravity(Gravity.CENTER);
        message.setTextSize(20);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        message.setPadding(padding, padding, padding, padding);
        setContentView(message);
    }
}
