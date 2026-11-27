package com.factory.snakekit;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;

public final class MainActivity extends Activity {
    private static final String LOCAL_URL = "https://appassets.androidplatform.net/assets/index.html";
    private WebView view;
    private android.speech.tts.TextToSpeech speech;
    private boolean speechReady;
    private boolean closed;
    private boolean visible;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(244, 247, 232));
        view = new WebView(this);
        root.addView(view, new FrameLayout.LayoutParams(-1, -1));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets safe = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return WindowInsets.CONSUMED;
            }
            return insets;
        });
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        root.requestApplyInsets();
        WebSettings s = view.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        WebView.setWebContentsDebuggingEnabled(false);
        view.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView web, WebResourceRequest request) {
                return !LOCAL_URL.equals(request.getUrl().toString());
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView web, WebResourceRequest request) {
                if (LOCAL_URL.equals(request.getUrl().toString()) && "GET".equals(request.getMethod())) {
                    try {
                        return new WebResourceResponse("text/html", "UTF-8", 200, "OK",
                            Collections.singletonMap("Content-Security-Policy", "default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; base-uri 'none'; form-action 'none'"),
                            getAssets().open("index.html"));
                    } catch (IOException e) { return blocked(404, "Not Found"); }
                }
                return blocked(403, "Forbidden");
            }
        });
        view.addJavascriptInterface(new SpeechBridge(), "SnakeSpeech");
        view.loadUrl(LOCAL_URL);
    }
    private final class SpeechBridge {
        @android.webkit.JavascriptInterface public void read(String text) {
            if (text == null || text.length() > 1000) return;
            runOnUiThread(() -> speak(text));
        }
    }
    private void speak(String text) {
        if (closed || !visible) return;
        if (speech == null) {
            speech = new android.speech.tts.TextToSpeech(this, status -> runOnUiThread(() -> {
                if (closed || speech == null) return;
                if (status == android.speech.tts.TextToSpeech.SUCCESS && speech.getVoices() != null) {
                    for (android.speech.tts.Voice voice : speech.getVoices()) {
                        if (voice.getLocale().getLanguage().equals("ko") && !voice.isNetworkConnectionRequired()
                            && !voice.getFeatures().contains(android.speech.tts.TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                            && speech.setVoice(voice) == android.speech.tts.TextToSpeech.SUCCESS) {
                            speechReady = true; break;
                        }
                    }
                }
                if (visible) speak(text);
            }));
            return;
        }
        if (speechReady) speech.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "snake-description");
        else android.widget.Toast.makeText(this, "기기에 한국어 오프라인 음성을 설치해 주세요.", android.widget.Toast.LENGTH_LONG).show();
    }
    @Override protected void onResume() { super.onResume(); visible = true; }
    @Override protected void onPause() { visible = false; if (speech != null) speech.stop(); super.onPause(); }
    private static WebResourceResponse blocked(int code, String reason) {
        return new WebResourceResponse("text/plain", "UTF-8", code, reason,
            Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
    }
    @Override protected void onDestroy() {
        closed = true;
        if (speech != null) { speech.stop(); speech.shutdown(); speech = null; }
        if (view != null) {
            view.removeJavascriptInterface("SnakeSpeech");
            ((android.view.ViewGroup)view.getParent()).removeView(view);
            view.destroy();
            view = null;
        }
        super.onDestroy();
    }
}
