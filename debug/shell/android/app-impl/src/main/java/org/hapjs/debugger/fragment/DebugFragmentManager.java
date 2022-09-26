/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.fragment;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import org.hapjs.debugger.utils.PreferenceUtils;

public class DebugFragmentManager {
    private static final String IDE_PATH = "path";
    private static final String CARD_MODE = "cardMode";

    private FragmentActivity mActivity;
    private ViewPager2 mContainerPager;
    private DebugFragment mAppFragment;
    private DebugFragment mCardFragment;

    private class PagerAdapter extends FragmentStateAdapter {

        public PagerAdapter(@NonNull FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                mAppFragment = createAppFragment();
                return mAppFragment;
            } else {
                mCardFragment = createCardFragment();
                return mCardFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    protected AppFragment createAppFragment(){
        return new AppFragment();
    }

    protected  CardFragment createCardFragment(){
        return new CardFragment();
    }

    public DebugFragmentManager(FragmentActivity activity, ViewPager2 pager) {
        this.mActivity = activity;
        this.mContainerPager = pager;
        mContainerPager.setAdapter(new PagerAdapter(activity));
        mContainerPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                PreferenceUtils.setRuntimeMode(mActivity, position);
            }
        });
    }

    public void showDebugFragment(Intent intent) {
        int mode = 0;
        if (isFromToolkit(intent)) {
            boolean isCardMode = "true".equals(intent.getStringExtra(CARD_MODE));
            mode = isCardMode ? 1 : 0;
        } else {
            mode = PreferenceUtils.getRuntimeMode(mActivity);
        }
        showDebugFragment(mode);
    }

    public void showDebugFragment(int mode) {
        if (mode == getMode() && getCurrentFragment() != null) {
            return;
        }
        mContainerPager.setCurrentItem(mode, getCurrentFragment() != null);
        PreferenceUtils.setRuntimeMode(mActivity, mode);
    }

    public boolean onNewIntent(Intent intent) {
        if (isFromToolkit(intent)) {
            boolean isCardMode = "true".equals(intent.getStringExtra(CARD_MODE));
            int mode = isCardMode ? 1 : 0;
            if (mode == getMode()) {
                if (getCurrentFragment() != null) {
                    getCurrentFragment().onNewIntent(intent);
                }
                return false;
            } else {
                showDebugFragment(mode);
                return true;
            }
        } else {
            if (getCurrentFragment() != null) {
                getCurrentFragment().onNewIntent(intent);
            }
            return false;
        }
    }

    private DebugFragment getCurrentFragment() {
        int index = mContainerPager.getCurrentItem();
        return index == 0 ? mAppFragment : mCardFragment;
    }

    public int getMode() {
        return mContainerPager.getCurrentItem();
    }

    private static boolean isFromToolkit(Intent intent) {
        return intent != null && intent.hasExtra(IDE_PATH);
    }

}
