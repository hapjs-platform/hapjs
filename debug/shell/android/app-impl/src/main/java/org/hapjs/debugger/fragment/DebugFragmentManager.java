/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.fragment;

import android.content.Context;
import android.content.Intent;
import androidx.fragment.app.FragmentActivity;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.utils.PreferenceUtils;

public class DebugFragmentManager {
    private static final String IDE_PATH = "path";
    private static final String CARD_MODE = "cardMode";

    private Mode mMode;
    private FragmentActivity mActivity;
    private int mContainerViewId;
    private DebugFragment mFragment;
    private List<Mode> mModeList;

    public DebugFragmentManager(FragmentActivity activity, int containerViewId) {
        this.mActivity = activity;
        this.mContainerViewId = containerViewId;
        this.mModeList = getAvailableModes(activity);
    }

    public void showDebugFragment(int index) {
        Mode mode = mModeList.get(index);
        showDebugFragment(mode);
    }

    private void showDebugFragment(Mode mode) {
        if (mMode == mode && mFragment != null) {
            return;
        }
        mMode = mode;
        PreferenceUtils.setRuntimeMode(mActivity, mode.getIndex());
        mFragment = createDebugFragment(mMode);
        mActivity.getSupportFragmentManager().beginTransaction().replace(mContainerViewId, mFragment)
                .commitAllowingStateLoss();
    }

    public void showDebugFragment(Intent intent) {
        int modeIndex = 0;
        if (isFromToolkit(intent)) {
            boolean isCardMode = "true".equals(intent.getStringExtra(CARD_MODE));
            if (isCardMode && !PreferenceUtils.isCardModeAdded(mActivity)) {
                PreferenceUtils.setCardModeAdded(mActivity, true);
                refreshModeList();
            }
            modeIndex = isCardMode ? 1 : 0;
        } else {
            modeIndex = PreferenceUtils.getRuntimeMode(mActivity);
        }

        Mode mode = Mode.getMode(modeIndex);
        if (!mModeList.contains(mode)) {
            mode = mModeList.get(0);
        }
        showDebugFragment(mode);
    }

    private static boolean isFromToolkit(Intent intent) {
        return intent != null && intent.hasExtra(IDE_PATH);
    }

    public boolean onNewIntent(Intent intent) {
        if (isFromToolkit(intent)) {
            boolean isCardMode = "true".equals(intent.getStringExtra(CARD_MODE));
            if (isCardMode && !PreferenceUtils.isCardModeAdded(mActivity)) {
                PreferenceUtils.setCardModeAdded(mActivity, true);
                refreshModeList();
            }
            int modeIndex = isCardMode ? 1 : 0;
            Mode mode = Mode.getMode(modeIndex);
            if (mode == mMode) {
                if (mFragment != null) {
                    mFragment.onNewIntent(intent);
                }
                return false;
            } else {
                showDebugFragment(mode);
                return true;
            }
        } else {
            if (mFragment != null) {
                mFragment.onNewIntent(intent);
            }
            return false;
        }
    }

    public int getMode() {
        return mModeList.indexOf(mMode);
    }

    protected DebugFragment createDebugFragment(Mode mode) {
        switch (mode) {
            case MODE_APP:
                return new AppFragment();
            case MODE_CARD:
                return new CardFragment();
            default:
                break;
        }
        return null;
    }

    public List<String> getNamesArray() {
        List<String> namesArray = new ArrayList<>(mModeList.size());
        for (int i = 0; i < mModeList.size(); i++) {
            namesArray.add(mModeList.get(i).getName(mActivity));
        }
        return namesArray;
    }

    /**
     * refresh mode list
     *
     * @return is mode list changed
     */
    public boolean refreshModeList() {
        List<Mode> currentList = getAvailableModes(mActivity);
        if (mModeList.equals(currentList)) {
            return false;
        }
        mModeList = currentList;
        return true;
    }

    private List<Mode> getAvailableModes(Context context) {
        List<Mode> modes = new ArrayList<>();
        for (Mode mode : Mode.values()) {
            if (mode.isAvailable(context)) {
                modes.add(mode);
            }
        }
        return modes;
    }

    public enum Mode {
        MODE_APP(R.string.text_mode_application),
        MODE_CARD(R.string.text_mode_card);

        private int nameRes;

        Mode(int nameRes) {
            this.nameRes = nameRes;
        }

        public static Mode getMode(int index) {
            if (index < 0 || index > values().length) {
                return MODE_APP;
            }
            return values()[index];
        }

        public String getName(Context context) {
            return context.getString(nameRes);
        }

        public int getIndex() {
            return ordinal();
        }

        public boolean isAvailable(Context context) {
            if (this == Mode.MODE_APP) {
                return true;
            }
            if (this == Mode.MODE_CARD) {
                return PreferenceUtils.isCardModeAdded(context);
            }
            return false;
        }
    }
}
