/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.widget.SettingViewBehavior;

public class DraggableActivity extends FragmentActivity {
    private static final String TAG = "SettingsActivity";
    private SettingViewBehavior<View> mBehavior;
    private View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_draggable);
        mRootView = findViewById(R.id.root_view);
        mRootView.setOnClickListener(v -> onBackPressed());
        mBehavior = SettingViewBehavior.from(findViewById(R.id.content_wrapper));
        mBehavior.addCallback(new SettingViewBehavior.Callback() {
            @Override
            public void onStateChanged(@NonNull View view, int state) {
                if (state == SettingViewBehavior.STATE_EXPANDED) {
                    mRootView.setBackgroundResource(R.color.floatingBg);
                } else if (state == SettingViewBehavior.STATE_HIDDEN) {
                    mRootView.setBackgroundColor(Color.TRANSPARENT);
                    finish();
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {

            }
        });
        runOnUiThread(() -> mBehavior.setState(SettingViewBehavior.STATE_EXPANDED));
        Log.d(TAG, "behavior state = "+mBehavior.getState());
    }

    @Override
    public void finish() {
        super.finish();
        mRootView.setBackgroundColor(Color.TRANSPARENT);
        overridePendingTransition(0, R.anim.bottom_out);
    }
}
