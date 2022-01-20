/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.card;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import org.hapjs.card.api.AbstractRenderListener;
import org.hapjs.card.api.Card;
import org.hapjs.card.api.CardInfo;
import org.hapjs.card.sdk.CardClient;
import org.hapjs.debugger.app.impl.R;

public class CardActivity extends AppCompatActivity {
    public static final String EXTRA_CARD_URL = "card_url";
    private static final String TAG = "CardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();

        Intent intent = getIntent();
        String cardUrl = intent.getStringExtra(EXTRA_CARD_URL);
        setupCard(cardUrl);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String cardUrl = intent.getStringExtra(EXTRA_CARD_URL);
        setupCard(cardUrl);
    }

    private void initUI() {
        Drawable cardBg = CardBackgroundHelper.getBackground(this);
        if (cardBg == null) {
            setContentView(R.layout.activity_card);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }

            setContentView(R.layout.activity_card);

            findViewById(R.id.hap_panel).setBackground(cardBg);
        }
    }

    private void setupCard(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        CardClient cardClient = CardClient.getInstance();
        if (cardClient == null) {
            Toast.makeText(this, R.string.toast_platform_not_support_debug, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        CardInfo cardInfo = null;
        try {
            cardInfo = cardClient.getCardInfo(url);
        } catch (Exception e) {
            Log.e(TAG, "getCardInfo error", e);
        }
        if (cardInfo == null) {
            Toast.makeText(this, R.string.toast_no_card, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Card card = cardClient.createCard(this);
        FrameLayout hapPanel = (FrameLayout) findViewById(R.id.hap_panel);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        hapPanel.addView(card.getView(), lp);
        card.setRenderListener(new AbstractRenderListener() {
            @Override
            public void onRenderSuccess() {
                Log.i(TAG, "onRenderSuccess");
            }

            @Override
            public boolean onRenderFailed(int errorCode, String message) {
                Log.e(TAG, "onRenderFailed:" + errorCode + "," + message);
                return false;
            }

            @Override
            public boolean onRenderProgress() {
                Log.i(TAG, "onRenderProgress");
                return false;
            }

        });
        card.load(url);
    }

}
