/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.picker;

import android.content.Context;
import java.lang.ref.WeakReference;
import java.util.Map;
import org.hapjs.common.utils.ThemeUtils;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.ConfigurationManager;
import org.hapjs.runtime.HapConfiguration;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.text.Text;

public abstract class Picker extends Text {

    public static final String WIDGET_NAME = "picker";
    public static final String METHOD_SHOW = "show";

    // event
    protected static final String CANCEL = "cancel";
    private static final String KEY_TEXT = "text";
    protected boolean hasCancelCallBack = false;
    protected OnConfigurationListener mConfigurationListener;

    public Picker(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    protected void setConfigurationListener() {
        if (mConfigurationListener != null) return;
        mConfigurationListener = new OnConfigurationListener(this);
        ConfigurationManager.getInstance().addListener(mConfigurationListener);
    }

    @Override
    protected void onSaveInstanceState(Map<String, Object> outState) {
        super.onSaveInstanceState(outState);
        if (mHost == null) {
            return;
        }
        outState.put(KEY_TEXT, mLayoutBuilder.getText());
    }

    @Override
    protected void onRestoreInstanceState(Map<String, Object> savedState) {
        super.onRestoreInstanceState(savedState);
        if (savedState == null || mHost == null) {
            return;
        }

        if (savedState.containsKey(KEY_TEXT)) {
            mLayoutBuilder.setText((CharSequence) savedState.get(KEY_TEXT));
        }
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        super.invokeMethod(methodName, args);
        if (METHOD_SHOW.equals(methodName)) {
            show();
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (CANCEL.equals(event)) {
            hasCancelCallBack = true;
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (CANCEL.equals(event)) {
            hasCancelCallBack = false;
            return true;
        }

        return super.removeEvent(event);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mConfigurationListener != null) {
            ConfigurationManager.getInstance().removeListener(mConfigurationListener);
        }
    }

    protected void cancelCallBack() {
        if (hasCancelCallBack) {
            mCallback.onJsEventCallback(getPageId(), mRef, CANCEL, Picker.this, null, null);
        }
    }

    public abstract void show();

    protected boolean isShowing() {
        return false;
    }

    protected int getTheme() {
        return ThemeUtils.getAlertDialogTheme();
    }

    protected static class OnConfigurationListener
            implements ConfigurationManager.ConfigurationListener {
        private WeakReference<Picker> reference;

        OnConfigurationListener(Picker picker) {
            reference = new WeakReference<>(picker);
        }

        @Override
        public void onConfigurationChanged(HapConfiguration newConfig) {
            Picker picker = reference.get();
            if (null != picker) {
                if (newConfig.getUiMode() != newConfig.getLastUiMode()) {
                    if (picker.isShowing()) {
                        picker.show();
                    }
                }
            }
        }
    }
}
