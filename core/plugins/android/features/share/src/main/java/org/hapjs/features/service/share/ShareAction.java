/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.share;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.features.service.share.ShareChooserDialog.ShareChooserListener;

public class ShareAction {

    private static final String TAG = "ShareAction";

    private Share mShare;
    private ShareContent mShareContent;
    private Platform mPlatform = null;
    private ShareListener mUserListener;
    private WeakReference<Activity> mActivityRf;
    private List<Platform> mDisplaylist;
    private AbsShareApi mShareHandler;
    private String mDialogTitle;
    private ShareChooserDialog mShareBoard;
    private static final String DIALOG_TAG = "share_action";

    private ShareChooserListener mDefaultShareChooserListener = new ShareChooserListener() {
        @Override
        public void onSelect(Platform platForm) {
            try {
                toShare(platForm);
            } catch (Throwable throwable) {
                Log.d(TAG, "share", throwable);
                onError(platForm, throwable.getMessage());
            }
        }

        @Override
        public void onCancel() {
            if (mListener != null) {
                mListener.onCancel(null);
            }
        }
    };

    private ShareChooserListener mShareChooserListener = mDefaultShareChooserListener;
    private ShareListener mListener = new ShareListener() {
        @Override
        public void onStart(Platform media) {
            if (mUserListener != null) {
                mUserListener.onStart(media);
            }
        }

        @Override
        public void onResult(Platform media) {
            if (mUserListener != null) {
                mUserListener.onResult(media);
            }
            release();
        }

        @Override
        public void onError(Platform media, String message) {
            if (mUserListener != null) {
                mUserListener.onError(media, message);
            }
            release();
        }

        @Override
        public void onCancel(Platform media) {
            if (mUserListener != null) {
                mUserListener.onCancel(media);
            }
            release();
        }
    };

    public ShareAction(Activity activity, Share share) {
        if (activity != null) {
            this.mActivityRf = new WeakReference(activity);
        }
        mShare = share;
    }

    public void share() {
        if (isShowing()) {
            onError(mPlatform, "an action is already sharing");
            return;
        }
        try {
            if (mShareContent == null || !mShareContent.checkArgs()) {
                onError(null, "share params is empty or illegal");
                return;
            }

            if (mDisplaylist == null) {
                onError(null, "no platform");
                return;
            }

            mDisplaylist = getAvailablePlatforms(mDisplaylist, mShareContent);

            if (mDisplaylist.isEmpty()) {
                onError(null, "no available platform");
                return;
            }

            if (mDisplaylist.size() == 1) {
                toShare(mDisplaylist.get(0));
                return;
            }

            showShareDialog();
        } catch (Exception e) {
            Log.d(TAG, "share", e);
            onError(mPlatform, e.getMessage());
        }
    }

    private void showShareDialog() {
        this.mShareBoard = new ShareChooserDialog();
        this.mShareBoard.setTitle(mDialogTitle);
        this.mShareBoard.setShareChooserListener(mShareChooserListener);
        this.mShareBoard.setPlatForms(mDisplaylist);
        this.mShareBoard.setCancelable(false);
        this.mShareBoard.show(mActivityRf.get().getFragmentManager(), DIALOG_TAG);
    }

    private boolean isShowing() {
        DialogFragment fragment =
                (DialogFragment) mActivityRf.get().getFragmentManager().findFragmentByTag(DIALOG_TAG);
        return fragment != null && fragment.getDialog() != null && fragment.getDialog().isShowing();
    }

    private void toShare(Platform platform) {
        mPlatform = platform;
        if (mShareContent == null || !mShareContent.checkArgs()) {
            onError(null, "share params is null");
            return;
        }
        mShareContent.setExtra(String.valueOf(System.currentTimeMillis()));

        if (mPlatform != null) {
            mShareHandler = mShare.createShareAPI(mActivityRf.get(), mShareContent, mPlatform);
            if (mShareHandler == null) {
                onError(mPlatform, "platform isn't support");
            } else {
                mShareHandler.share(mListener);
            }
        }
    }

    public List<Platform> getAvailablePlatforms(List<Platform> platforms,
                                                ShareContent shareContent) {
        List<Platform> result = new ArrayList<>();
        for (Platform platform : platforms) {
            AbsShareApi handler = mShare.createShareAPI(mActivityRf.get(), shareContent, platform);
            if (handler != null && handler.isAvailable()) {
                result.add(platform);
            }
        }
        return result;
    }

    public void release() {
        if (mShareHandler != null) {
            mShareHandler.release();
            mShareHandler = null;
        }
        if (this.mShareBoard != null && this.mShareBoard.isAdded()) {
            this.mShareBoard.dismissAllowingStateLoss();
        }
        mListener = null;
        mUserListener = null;
    }

    public ShareAction setCallback(ShareListener listener) {
        this.mUserListener = listener;
        return this;
    }

    private void onError(Platform platform, String message) {
        if (mListener != null) {
            mListener.onError(platform, message);
        }
    }

    public ShareAction setShareContent(ShareContent shareContent) {
        this.mShareContent = shareContent;
        return this;
    }

    public ShareContent getShareContent() {
        return mShareContent;
    }

    public ShareAction setDisplayList(List<Platform> list) {
        this.mDisplaylist = list;
        return this;
    }

    public ShareAction setDialogTitle(String dialogTitle) {
        this.mDialogTitle = dialogTitle;
        return this;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mShareHandler != null) {
            mShareHandler.onActivityResult(requestCode, resultCode, data);
        }
    }
}