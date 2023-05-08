/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.hapjs.debugger.app.impl.R;


public class WebActivity extends AppCompatActivity {
    private TitleBar mTitleBar;
    private FrameLayout mFlContainer;
    private WebView mWebView;
    public static final String KEY_URL = "url";
    public static final String KEY_TITLE = "title";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBar();
        setContentView(R.layout.activity_web);
        init();
    }

    private void setStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = getWindow().getDecorView();
            int option = 0;
            option = View.SYSTEM_UI_FLAG_VISIBLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        mTitleBar = findViewById(R.id.title_bar);
        mTitleBar.setStyleAndTitle(TitleBar.STYLE_TITLE_BACK, getIntent().getStringExtra(KEY_TITLE));
        mTitleBar.setBackListener(v -> finish());
        mFlContainer = findViewById(R.id.fl_container);
        mWebView = new WebView(this);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setAllowFileAccessFromFileURLs(false);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(false);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        mWebView.getSettings().setTextZoom(100);
        mWebView.setWebViewClient(new WebViewClient());
        mFlContainer.addView(mWebView);
        String url = getIntent().getStringExtra(KEY_URL);
        mWebView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if(mFlContainer != null) {
            mFlContainer.removeAllViews();
        }
        if(mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }
}
