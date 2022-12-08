/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.SwipeObserver;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.SliderView;

@WidgetAnnotation(
        name = Slider.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Slider extends Component<SliderView> implements SwipeObserver {

    protected static final String WIDGET_NAME = "slider";

    // style
    protected static final String BLOCK_COLOR = "blockColor";

    private static final String KEY_CHANGE_EVENT_STATE = "change_event_state";
    private static final String KEY_PROGRESS = "progress";
    private static final String DEFAULT_COLOR = "#fff0f0f0";
    private static final String DEFAULT_SELECTED_COLOR = "ff33b4ff";
    private static final String DEFAULT_PADDING = "32px";
    private ProgressChangeRunnable mProgressChangeRunnable = new ProgressChangeRunnable();
    private boolean mIsChangeEventRegistered = false;

    public Slider(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected SliderView createViewImpl() {
        SliderView sliderView =
                (SliderView) LayoutInflater.from(mContext).inflate(R.layout.slider, null);
        sliderView.setComponent(this);
        int padding = Attributes.getInt(mHapEngine, DEFAULT_PADDING, this);
        setPadding(Attributes.Style.PADDING_LEFT, padding);
        setPadding(Attributes.Style.PADDING_RIGHT, padding);
        sliderView.setPadding(padding, 0, padding, 0);
        initOnchangeListener(sliderView);
        return sliderView;
    }

    private void initOnchangeListener(SliderView sliderView) {
        if (sliderView == null) {
            return;
        }
        sliderView.setOnProgressChangeListener(
                new SliderView.OnProgressChangeListener() {
                    @Override
                    public void onChange(int progress, boolean fromUser) {
                        if (fromUser) {
                            changeAttrDomData(Attributes.Style.VALUE, progress);
                        }
                        if (mIsChangeEventRegistered) {
                            Handler handler = mHost.getHandler();
                            if (handler != null) {
                                handler.removeCallbacks(mProgressChangeRunnable);
                                mProgressChangeRunnable.mProgress = progress;
                                mProgressChangeRunnable.mIsFromUser = fromUser;
                                handler.postDelayed(mProgressChangeRunnable, 16);
                            }
                        }
                    }
                });
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.MIN:
                int min = Attributes.getInt(mHapEngine, attribute, 0);
                setMin(min);
                return true;
            case Attributes.Style.MAX:
                int max = Attributes.getInt(mHapEngine, attribute, 100);
                setMax(max);
                return true;
            case Attributes.Style.STEP:
                int step = Attributes.getInt(mHapEngine, attribute, 1);
                setStep(step);
                return true;
            case Attributes.Style.ENABLE:
                boolean enabled = Attributes.getBoolean(attribute, true);
                setEnabled(enabled);
                return true;
            case Attributes.Style.VALUE:
                int progress = Attributes.getInt(mHapEngine, attribute, 0);
                setProgress(progress);
                return true;
            case Attributes.Style.COLOR:
                String colorStr = Attributes.getString(attribute, DEFAULT_COLOR);
                setColor(colorStr);
                return true;
            case Attributes.Style.SELECTED_COLOR:
                String selectedColorStr = Attributes.getString(attribute, DEFAULT_SELECTED_COLOR);
                setSelectedColor(selectedColorStr);
                return true;
            case BLOCK_COLOR:
                String blockColorStr = Attributes.getString(attribute, BLOCK_COLOR);
                setBlockColor(blockColorStr);
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.CHANGE.equals(event)) {
            mIsChangeEventRegistered = true;
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.CHANGE.equals(event)) {
            mIsChangeEventRegistered = false;
            return true;
        }

        return super.removeEvent(event);
    }

    public void setMin(int min) {
        if (mHost == null) {
            return;
        }
        mHost.setMin(min);
    }

    public void setMax(int max) {
        if (mHost == null) {
            return;
        }
        mHost.setMax(max);
    }

    public void setStep(int step) {
        if (mHost == null) {
            return;
        }
        mHost.setStep(step);
    }

    public void setEnabled(boolean enabled) {
        if (mHost == null) {
            return;
        }
        mHost.setEnabled(enabled);
    }

    public void setProgress(int progress) {
        if (mHost == null) {
            return;
        }
        mHost.setProgress(progress);
    }

    public void setColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || mHost == null) {
            return;
        }

        int color = ColorUtil.getColor(colorStr);
        mHost.setColor(color);
    }

    public void setSelectedColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || mHost == null) {
            return;
        }

        int color = ColorUtil.getColor(colorStr);
        mHost.setSelectedColor(color);
    }

    private void setBlockColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || mHost == null) {
            return;
        }

        int color = ColorUtil.getColor(colorStr);
        mHost.setBlockColor(color);
    }

    @Override
    protected void onSaveInstanceState(Map<String, Object> outState) {
        super.onSaveInstanceState(outState);
        if (mHost == null) {
            return;
        }
        outState.put(KEY_CHANGE_EVENT_STATE, mIsChangeEventRegistered);
        outState.put(KEY_PROGRESS, mHost.getProgress());
    }

    @Override
    protected void onRestoreInstanceState(Map<String, Object> savedState) {
        super.onRestoreInstanceState(savedState);
        if (savedState == null) {
            return;
        }
        if (savedState.get(KEY_CHANGE_EVENT_STATE) != null) {
            mIsChangeEventRegistered = (boolean) savedState.get(KEY_CHANGE_EVENT_STATE);
        }
        if (savedState.get(KEY_PROGRESS) != null) {
            mHost.setProgress((int) savedState.get(KEY_PROGRESS));
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mHost != null) {
            Handler handler = mHost.getHandler();
            if (handler != null) {
                handler.removeCallbacks(mProgressChangeRunnable);
            }
            mHost.setOnProgressChangeListener(null);
        }
    }

    private class ProgressChangeRunnable implements Runnable {

        int mProgress;
        boolean mIsFromUser;

        @Override
        public void run() {
            Map<String, Object> params = new HashMap<>();
            params.put("progress", mProgress);
            params.put(Attributes.EventParams.IS_FROM_USER, mIsFromUser);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("value", mProgress);
            mCallback.onJsEventCallback(
                    getPageId(), mRef, Attributes.Event.CHANGE, Slider.this, params, attributes);
        }
    }
}
