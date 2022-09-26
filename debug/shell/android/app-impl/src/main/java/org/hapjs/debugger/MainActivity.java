/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;

import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.debug.AppDebugManager;
import org.hapjs.debugger.debug.CardDebugManager;
import org.hapjs.debugger.fragment.DebugFragmentManager;

public class MainActivity extends FragmentActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final String KEY_RPK_ADDRESS = "rpk_address";
    private DebugFragmentManager mDebugFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //从近期任务栏启动，Intent携带原来的rpk地址数据,需要清空
        if (isUniversalScan(getIntent()) && isLaunchFromRecentsTask(savedInstanceState)) {
            getIntent().setData(null);
        }
        setContentView(R.layout.activity_main);
        initViews();
    }

    private void initViews() {
        ((AppBarLayout) findViewById(R.id.app_bar_layout)).setOutlineProvider(null);

        ViewPager2 pager = findViewById(R.id.pager);
        mDebugFragmentManager = createDebugFragmentManager(pager);
        mDebugFragmentManager.showDebugFragment(getIntent());

        findViewById(R.id.app_mode_title).setOnClickListener(this);
        findViewById(R.id.card_mode_title).setOnClickListener(this);
        int mode = mDebugFragmentManager.getMode();
        updateTitleStyle(mode == 0);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTitleStyle(position == 0);
            }
        });

        findViewById(R.id.setting).setOnClickListener(this);
    }

    protected DebugFragmentManager createDebugFragmentManager(ViewPager2 pager){
        return  new DebugFragmentManager(this, pager);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (mDebugFragmentManager != null) {
            boolean modeChanged = mDebugFragmentManager.onNewIntent(intent);
            if (modeChanged) {
                updateTitleStyle(mDebugFragmentManager.getMode() == 0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppDebugManager.getInstance(this).close();
        CardDebugManager.getInstance(this).close();
    }

    private boolean isLaunchFromRecentsTask(Bundle savedInstanceState) {
        return (savedInstanceState != null) || ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
                == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
    }

    private boolean isUniversalScan(Intent intent) {
        if (intent != null && intent.getData() != null) {
            Uri uri = intent.getData();
            if (!TextUtils.isEmpty(uri.getQueryParameter(KEY_RPK_ADDRESS))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.app_mode_title
                || v.getId() == R.id.card_mode_title) {
            mDebugFragmentManager.showDebugFragment(v.getId() == R.id.app_mode_title ? 0 : 1);
            updateTitleStyle(v.getId() == R.id.app_mode_title);
        } else if (v.getId() == R.id.setting) {
            Intent intent = new Intent();
            intent.setClass(this, SettingsActivity.class);
            startActivity(intent);
        }
    }

    private void updateTitleStyle(boolean isAppMode) {
        TextView appModeTitle = findViewById(R.id.app_mode_title);
        TextView cardModeTitle = findViewById(R.id.card_mode_title);
        View appModeBottomLine = findViewById(R.id.app_mode_bottom_line);
        View cardModeBottomLine = findViewById(R.id.card_mode_bottom_line);
        if (isAppMode) {
            appModeTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.mode_selected_text_size));
            appModeTitle.setTextColor(getResources().getColor(R.color.mode_selected_color));
            appModeTitle.setTypeface(null, Typeface.BOLD);
            cardModeTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.mode_unselected_text_size));
            cardModeTitle.setTextColor(getResources().getColor(R.color.mode_unselected_color));
            appModeTitle.setTypeface(null, Typeface.NORMAL);
            appModeBottomLine.setVisibility(View.VISIBLE);
            cardModeBottomLine.setVisibility(View.GONE);
        } else {
            appModeTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.mode_unselected_text_size));
            appModeTitle.setTextColor(getResources().getColor(R.color.mode_unselected_color));
            appModeTitle.setTypeface(null, Typeface.NORMAL);
            cardModeTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.mode_selected_text_size));
            cardModeTitle.setTextColor(getResources().getColor(R.color.mode_selected_color));
            appModeTitle.setTypeface(null, Typeface.BOLD);
            appModeBottomLine.setVisibility(View.GONE);
            cardModeBottomLine.setVisibility(View.VISIBLE);
        }
    }
}
