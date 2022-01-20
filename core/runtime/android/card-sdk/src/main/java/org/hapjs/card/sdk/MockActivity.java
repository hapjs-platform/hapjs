/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.InputQueue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public class MockActivity extends Activity {

    private static final String TAG = "MockActivity";

    private final Context mContext;
    private final Window mWindow;

    public MockActivity(Context context) {
        this(context, null);
    }

    public MockActivity(Context context, Window window) {
        super();
        this.mContext = context;
        this.mWindow = window == null ? new MockWindow(context) : window;
        attachBaseContext(context);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WindowManager getWindowManager() {
        return (WindowManager) mContext.getSystemService(WINDOW_SERVICE);
    }

    @Override
    public Object getSystemService(String name) {
        return mContext.getSystemService(name);
    }

    @Override
    public Window getWindow() {
        return mWindow;
    }

    @Override
    public boolean isFinishing() {
        return false;
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

    private static class MockWindow extends Window {

        public MockWindow(Context context) {
            super(context);
        }

        @Override
        public void takeSurface(SurfaceHolder.Callback2 callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void takeInputQueue(InputQueue.Callback callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isFloating() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setContentView(int layoutResID) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setContentView(View view) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setContentView(View view, ViewGroup.LayoutParams params) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addContentView(View view, ViewGroup.LayoutParams params) {
            throw new UnsupportedOperationException();
        }

        @Override
        public View getCurrentFocus() {
            throw new UnsupportedOperationException();
        }

        @Override
        public LayoutInflater getLayoutInflater() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setTitle(CharSequence title) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setTitleColor(int textColor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void openPanel(int featureId, KeyEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void closePanel(int featureId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void togglePanel(int featureId, KeyEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void invalidatePanelMenu(int featureId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean performPanelShortcut(int featureId, int keyCode, KeyEvent event, int flags) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean performPanelIdentifierAction(int featureId, int id, int flags) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void closeAllPanels() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean performContextMenuIdentifierAction(int id, int flags) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setBackgroundDrawable(Drawable drawable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFeatureDrawableResource(int featureId, int resId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFeatureDrawableUri(int featureId, Uri uri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFeatureDrawable(int featureId, Drawable drawable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFeatureDrawableAlpha(int featureId, int alpha) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFeatureInt(int featureId, int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void takeKeyEvents(boolean get) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean superDispatchKeyEvent(KeyEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean superDispatchKeyShortcutEvent(KeyEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean superDispatchTouchEvent(MotionEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean superDispatchTrackballEvent(MotionEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean superDispatchGenericMotionEvent(MotionEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public View getDecorView() {
            throw new UnsupportedOperationException();
        }

        @Override
        public View peekDecorView() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle saveHierarchyState() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void restoreHierarchyState(Bundle savedInstanceState) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onActive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setChildDrawable(int featureId, Drawable drawable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setChildInt(int featureId, int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isShortcutKey(int keyCode, KeyEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getVolumeControlStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setVolumeControlStream(int streamType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getStatusBarColor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setStatusBarColor(int color) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getNavigationBarColor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNavigationBarColor(int color) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setDecorCaptionShade(int decorCaptionShade) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setResizingCaptionDrawable(Drawable drawable) {
            throw new UnsupportedOperationException();
        }
    }
}