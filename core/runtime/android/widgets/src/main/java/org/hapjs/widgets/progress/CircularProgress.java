/*
 * Copyright (c) 2021-present,  the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.progress;

import android.content.Context;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ProgressBar;
import java.util.Map;
import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.R;

@WidgetAnnotation(
        name = Progress.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        },
        types = {@TypeAnnotation(name = CircularProgress.TYPE_CIRCULAR)})
public class CircularProgress extends Progress<ProgressBar> {

    protected static final String TYPE_CIRCULAR = "circular";

    private Wrapper mProgressBar;

    private int mProgressColor = DEFAULT_COLOR;

    public CircularProgress(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected ProgressBar createViewImpl() {
        mProgressBar = new Wrapper(mContext, null, android.R.attr.progressBarStyleSmall);
        mProgressBar.getIndeterminateDrawable()
                .setColorFilter(mProgressColor, PorterDuff.Mode.SRC_IN);
        ViewGroup.LayoutParams lp = generateDefaultLayoutParams();
        lp.width = mDefaultDimension;
        lp.height = mDefaultDimension;
        mProgressBar.setLayoutParams(lp);

        mProgressBar.setComponent(this);

        return mProgressBar;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.COLOR:
                String colorStr =
                        Attributes.getString(attribute, ColorUtil.getColorStr(mProgressColor));
                setColor(colorStr);
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    private void setColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || mHost == null) {
            return;
        }

        mProgressColor = ColorUtil.getColor(colorStr, mProgressColor);
        mHost.getIndeterminateDrawable().setColorFilter(mProgressColor, PorterDuff.Mode.SRC_IN);
    }

    private class Wrapper extends ProgressBar implements ComponentHost, GestureHost {

        private Component mComponent;
        private KeyEventDelegate mKeyEventDelegate;
        private IGesture mGesture;

        public Wrapper(Context context) {
            super(context);
        }

        public Wrapper(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public Component getComponent() {
            return mComponent;
        }

        @Override
        public void setComponent(Component component) {
            mComponent = component;
        }

        @Override
        public IGesture getGesture() {
            return mGesture;
        }

        @Override
        public void setGesture(IGesture gestureDelegate) {
            mGesture = gestureDelegate;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            boolean result = super.onTouchEvent(event);
            if (mGesture != null) {
                result |= mGesture.onTouch(event);
            }
            return result;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            boolean result = super.onKeyDown(keyCode, event);
            return onKey(KeyEvent.ACTION_DOWN, keyCode, event, result);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            boolean result = super.onKeyUp(keyCode, event);
            return onKey(KeyEvent.ACTION_UP, keyCode, event, result);
        }

        private boolean onKey(int keyAction, int keyCode, KeyEvent event, boolean result) {
            if (mKeyEventDelegate == null) {
                mKeyEventDelegate = new KeyEventDelegate(mComponent);
            }
            result |= mKeyEventDelegate.onKey(keyAction, keyCode, event);
            return result;
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName("");
            info.setClickable(false);
            info.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
            if (getProgress() >= 0) {
                info.setText(mContext.getResources().getString(R.string.talkback_progress_percent)
                        + getProgress()
                        + " "
                        + mContext.getResources().getString(R.string.talkback_progress));
            }
        }
    }
}
