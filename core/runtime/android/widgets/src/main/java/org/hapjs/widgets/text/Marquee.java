/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.text;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import androidx.appcompat.widget.AppCompatTextView;
import java.util.Map;
import java.util.Objects;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.text.TextSpan;

@WidgetAnnotation(
        name = Marquee.WIDGET_NAME,
        methods = {
                Marquee.METHOD_PAUSE,
                Marquee.METHOD_RESUME,
                Marquee.METHOD_START,
                Marquee.METHOD_STOP,
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_FOCUS,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Marquee extends Container<Marquee.MarqueeTextView> {
    // attribute
    public static final String SCROLL_AMOUNT = "scrollamount";
    public static final String LOOP = "loop";
    public static final String DIRECTION = "direction";
    protected static final String WIDGET_NAME = "marquee";
    // method
    protected static final String METHOD_START = "start";
    protected static final String METHOD_RESUME = "resume";
    protected static final String METHOD_PAUSE = "pause";
    protected static final String METHOD_STOP = "stop";

    // marquee
    private static final String EVENT_BOUNCE = "bounce";
    private static final String EVENT_FINISH = "finish";
    private static final String EVENT_START = "start";

    private static final int DEFAULT_SCROLL_AMOUNT = 6;
    private static final int DEFAULT_LOOP = -1;
    private static final String DEFAULT_DIRECTION = "left";

    private int mFontWeight;
    private FontParser mFontParser;
    private String mText;
    private boolean mFontNeedUpdate = true;
    private TextSpan mTextSpan = new TextSpan();
    private Choreographer.FrameCallback mSetTextCallback;

    public Marquee(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        callback.addActivityStateListener(this);
    }

    @Override
    public void onActivityStart() {
        super.onActivityStart();
        MarqueeTextView hostView = getHostView();
        if (hostView == null) {
            return;
        }
        if (hostView.mOnStopPaused) {
            hostView.resumeScroll();
            hostView.mOnStopPaused = false;
        }
    }

    @Override
    public void onActivityStop() {
        super.onActivityStop();
        MarqueeTextView hostView = getHostView();
        if (hostView == null) {
            return;
        }
        if (!hostView.mPaused) {
            hostView.pauseScroll();
            hostView.mOnStopPaused = true;
        }
    }

    @Override
    protected MarqueeTextView createViewImpl() {
        MarqueeTextView view = new MarqueeTextView(mContext);
        view.setComponent(this);
        return view;
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        super.invokeMethod(methodName, args);
        MarqueeTextView hostView = getHostView();
        if (hostView == null) {
            return;
        }
        switch (methodName) {
            case METHOD_RESUME:
                hostView.resumeScroll();
                break;
            case METHOD_PAUSE:
                hostView.pauseScroll();
                break;
            case METHOD_START:
                hostView.startScroll();
                break;
            case METHOD_STOP:
                hostView.stopScroll();
                break;
            default:
                break;
        }
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        MarqueeTextView hostView = getHostView();
        if (hostView == null) {
            return super.setAttribute(key, attribute);
        }
        switch (key) {
            case SCROLL_AMOUNT:
                int scrollAmount =
                        Attributes.getInt(mHapEngine, attribute, getDefaultScrollAmount());
                setScrollSpeed(scrollAmount);
                return true;
            case LOOP:
                int loop = Attributes.getInt(mHapEngine, attribute, getDefaultLoop());
                setLoop(loop);
                return true;
            case DIRECTION:
                String direction = Attributes.getString(attribute, getDefaultDirection());
                setDirection(direction);
                return true;
            case Attributes.Style.COLOR:
                String colorStr = Attributes.getString(attribute, getDefaultColor());
                setColor(colorStr);
                return true;
            case Attributes.Style.FONT_SIZE:
                int defaultFontSize = Attributes.getInt(mHapEngine, getDefaultFontSize(), this);
                int fontSize = Attributes.getInt(mHapEngine, attribute, defaultFontSize, this);
                setFontSize(fontSize);
                return true;
            case Attributes.Style.FONT_WEIGHT:
                String fontWeightStr = Attributes.getString(attribute, "normal");
                setFontWeight(fontWeightStr);
                return true;
            case Attributes.Style.VALUE:
            case Attributes.Style.CONTENT:
                String text = Attributes.getString(attribute, "");
                setText(text);
                return true;
            case Text.FONT_FAMILY_DESC:
                String fontFamily = Attributes.getString(attribute, null);
                setFontFamily(fontFamily);
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
        switch (event) {
            case EVENT_BOUNCE:
                addOnBounceListener();
                return true;
            case EVENT_FINISH:
                addOnFinishListener();
                return true;
            case EVENT_START:
                addOnStartListener();
                return true;
            default:
                break;
        }

        return super.addEvent(event);
    }

    private void addOnBounceListener() {
        if (getHostView().mOnBounceListener == null) {
            getHostView().mOnBounceListener =
                    () -> mCallback
                            .onJsEventCallback(getPageId(), mRef, EVENT_BOUNCE, this, null, null);
        }
    }

    private void addOnFinishListener() {
        if (getHostView().mOnFinishListener == null) {
            getHostView().mOnFinishListener =
                    () -> mCallback
                            .onJsEventCallback(getPageId(), mRef, EVENT_FINISH, this, null, null);
        }
    }

    private void addOnStartListener() {
        if (getHostView().mOnStartListener == null) {
            getHostView().mOnStartListener =
                    () -> mCallback
                            .onJsEventCallback(getPageId(), mRef, EVENT_START, this, null, null);
        }
    }

    private void setScrollSpeed(int scrollSpeed) {
        if (scrollSpeed <= 0) {
            scrollSpeed = 1;
        }
        scrollSpeed =
                Math.round(DisplayUtil.getRealPxByWidth(scrollSpeed, mHapEngine.getDesignWidth()));
        getHostView().setScrollSpeed(scrollSpeed);
    }

    private void setLoop(int loop) {
        getHostView().setLoop(loop);
    }

    private void setDirection(String direction) {
        boolean reverse = Objects.equals(direction, "right");
        getHostView().setReverse(reverse);
    }

    private void setColor(String colorStr) {
        mTextSpan.setColor(colorStr);
        updateSpannable();
    }

    private void setFontSize(int fontSize) {
        mTextSpan.setFontSize(fontSize);
        updateSpannable();
    }

    private void setFontWeight(String fontWeightStr) {
        int weight = TypefaceBuilder.parseFontWeight(fontWeightStr);
        mFontWeight = weight;
        mTextSpan.setFontWeight(weight, null);
        updateSpannable();
    }

    private void setText(String text) {
        if (text.equals(mText)) {
            return;
        }
        mTextSpan.setDirty(true);
        mText = text;
        updateSpannable();
    }

    private void updateSpannable() {
        if (isDirty()) {
            postSetTextCallback();
        }
    }

    private void postSetTextCallback() {
        if (mSetTextCallback != null) {
            return;
        }
        mSetTextCallback =
                frameTimeNanos -> {
                    if (mHost != null) {
                        mHost.setText(applySpannable());
                    }
                    mSetTextCallback = null;
                };
        Choreographer.getInstance().postFrameCallback(mSetTextCallback);
    }

    private void removeSetTextCallback() {
        if (mSetTextCallback != null) {
            Choreographer.getInstance().removeFrameCallback(mSetTextCallback);
            mSetTextCallback = null;
        }
    }

    private CharSequence applySpannable() {
        mTextSpan.setDirty(false);

        // apply host data.
        CharSequence hostText = "";
        if (!TextUtils.isEmpty(mText)) {
            hostText = mTextSpan.createSpanned(mText);
        }
        return hostText;
    }

    private void setFontFamily(String fontFamily) {
        if (TextUtils.equals(fontFamily, mTextSpan.getFontFamily())) {
            return;
        }
        mTextSpan.setFontFamily(fontFamily);
        if (mFontParser == null) {
            mFontParser = new FontParser(mContext, this);
        }
        mFontParser.parse(
                fontFamily,
                typeface -> {
                    mTextSpan.setFontTypeface(typeface, null);
                    if (mFontNeedUpdate) {
                        getHostView().setTypeface(typeface, mFontWeight);
                    }
                });
    }

    private boolean isDirty() {
        return mTextSpan.isDirty();
    }

    private void onViewAttachedToWindow() {
        mFontNeedUpdate = true;
        if (mTextSpan.isDirty()) {
            updateSpannable();
        }
    }

    private void onViewDetachedFromWindow() {
        mFontNeedUpdate = false;
    }

    @Override
    public void destroy() {
        removeSetTextCallback();
        mCallback.removeActivityStateListener(this);
        super.destroy();
    }

    private int getDefaultScrollAmount() {
        return DEFAULT_SCROLL_AMOUNT;
    }

    private int getDefaultLoop() {
        return DEFAULT_LOOP;
    }

    private String getDefaultDirection() {
        return DEFAULT_DIRECTION;
    }

    private String getDefaultColor() {
        return Text.DEFAULT_COLOR;
    }

    private String getDefaultFontSize() {
        return Text.DEFAULT_FONT_SIZE;
    }

    private interface OnBounceListener {
        void onBounce();
    }

    private interface OnFinishListener {
        void onFinish();
    }

    private interface OnStartListener {
        void onStart();
    }

    static class MarqueeTextView extends AppCompatTextView implements ComponentHost, GestureHost {

        /**
         * 滚动器
         */
        private Scroller mScroller;
        /**
         * 滚动的初始 X 位置
         */
        private int mXOffset = 0;
        /**
         * 是否暂停
         */
        private boolean mPaused = true;
        /**
         * 是否第一次滚动
         */
        private boolean mIsFirstTime = true;
        /**
         * 是否 onStop 时被动暂停
         */
        private boolean mOnStopPaused = false;

        private Marquee mComponent;
        private KeyEventDelegate mKeyEventDelegate;
        private IGesture mGesture;
        private boolean mReverse;
        private int mLoop = DEFAULT_LOOP;
        private int mCurrentLoop;
        private int mScrollSpeed = DEFAULT_SCROLL_AMOUNT;

        private OnBounceListener mOnBounceListener;
        private OnFinishListener mOnFinishListener;
        private OnStartListener mOnStartListener;

        public MarqueeTextView(Context context) {
            super(context);
            initView();
        }

        private void initView() {
            setSingleLine();
            setEllipsize(null);
        }

        void startScroll() {
            if (mScroller != null && !mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            mCurrentLoop = 0;
            mXOffset = 0;
            mPaused = true;
            resumeScroll();
        }

        void resumeScroll() {
            if (!mPaused || isLoopEnd()) {
                return;
            }
            if (mOnStartListener != null && mIsFirstTime) {
                mOnStartListener.onStart();
            }
            if (mIsFirstTime && mReverse) {
                int reverseOffset = calculateTextLength() - getWidth();
                mXOffset += reverseOffset;
            }
            mIsFirstTime = false;
            mPaused = false;

            setHorizontallyScrolling(true);

            if (mScroller == null) {
                mScroller = new Scroller(getContext(), new LinearInterpolator());
                setScroller(mScroller);
            }
            float textLength = calculateTextLength();
            int distance = mReverse ? -getWidth() - mXOffset : (int) textLength - mXOffset;
            float rollingInterval = textLength / mScrollSpeed * 1000f;
            int duration =
                    (Float.valueOf(rollingInterval * Math.abs(distance) * 1.0f / textLength))
                            .intValue();
            mScroller.startScroll(mXOffset, 0, distance, 0, duration);
            invalidate();
        }

        /**
         * 暂停滚动
         */
        public void pauseScroll() {
            if (null == mScroller) {
                return;
            }
            if (mPaused) {
                return;
            }
            mPaused = true;
            mXOffset = mScroller.getCurrX();
            mScroller.abortAnimation();
        }

        /**
         * 停止滚动，并回到初始位置
         */
        public void stopScroll() {
            if (null == mScroller) {
                return;
            }
            // reset
            mIsFirstTime = true;
            mPaused = true;
            mOnStopPaused = false;
            mCurrentLoop = 0;
            mScroller.startScroll(0, 0, 0, 0, 0);
        }

        /**
         * 是否循环结束
         */
        private boolean isLoopEnd() {
            return mLoop >= 0 && mLoop == mCurrentLoop;
        }

        /**
         * 计算文本长度
         *
         * @return 文本的长度
         */
        private int calculateTextLength() {
            TextPaint tp = getPaint();
            Rect rect = new Rect();
            String strTxt = getText().toString();
            tp.getTextBounds(strTxt, 0, strTxt.length(), rect);
            return rect.width();
        }

        @Override
        public void computeScroll() {
            super.computeScroll();
            if (null == mScroller) {
                return;
            }
            if (mScroller.isFinished() && (!mPaused)) {
                if (mOnBounceListener != null) {
                    mOnBounceListener.onBounce();
                }
                mCurrentLoop++;
                if (isLoopEnd()) {
                    stopScroll();
                    if (mOnFinishListener != null) {
                        mOnFinishListener.onFinish();
                    }
                    return;
                }
                mPaused = true;
                mXOffset = mReverse ? calculateTextLength() : -getWidth();
                this.resumeScroll();
            }
        }

        @Override
        public Component getComponent() {
            return mComponent;
        }

        @Override
        public void setComponent(Component component) {
            mComponent = (Marquee) component;
        }

        @Override
        public IGesture getGesture() {
            return mGesture;
        }

        public void setGesture(IGesture gestureDelegate) {
            mGesture = gestureDelegate;
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            mComponent.onViewAttachedToWindow();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            mComponent.onViewDetachedFromWindow();
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

        public void setScrollSpeed(int scrollSpeed) {
            if (mScrollSpeed != scrollSpeed) {
                mScrollSpeed = scrollSpeed;
                updateScrolling();
            }
        }

        public void setLoop(int loop) {
            if (mLoop != loop) {
                mLoop = loop;
                updateScrolling();
            }
        }

        public void setReverse(boolean reverse) {
            if (mReverse ^ reverse) {
                mReverse = reverse;
                updateScrolling();
            }
        }

        // refresh scroll speed when scrolling
        private void updateScrolling() {
            if (!mPaused) {
                pauseScroll();
                resumeScroll();
            }
        }
    }
}
