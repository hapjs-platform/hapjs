/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets.view.lottie;

import android.animation.Animator;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.airbnb.lottie.LottieListener;
import com.airbnb.lottie.RenderMode;

import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;

import java.util.HashMap;
import java.util.Map;

@WidgetAnnotation(
        name = Lottie.WIDGET_NAME,
        methods = {
                Lottie.METHOD_PLAY,
                Lottie.METHOD_PAUSE,
                Lottie.METHOD_RESUME,
                Lottie.METHOD_RESET,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        }
)
public class Lottie extends Component<FlexLottie> {
    public static final String TAG = "Lottie";
    protected static final String WIDGET_NAME = "lottie";
    //method
    protected static final String METHOD_PLAY = "play";
    protected static final String METHOD_PAUSE = "pause";
    protected static final String METHOD_RESET = "reset";
    protected static final String METHOD_RESUME = "resume";
    private static final String SPEED = "speed";
    private static final String PROGRESS = "progress";
    private static final String LOOP = "loop";
    private static final String AUTO_PLAY = "autoplay";
    private static final String EVENT_COMPLETE = "complete";
    private static final String EVENT_ERROR = "error";
    private static final String EVENT_CHANGE = "change";
    private static final String RENDER_MODE = "renderMode";
    private static final String START_FRAME = "startFrame";
    private static final String END_FRAME = "endFrame";
    private static final String AUTOMATIC = "AUTOMATIC";
    private static final String HARDWARE = "HARDWARE";
    private static final String SOFTWARE = "SOFTWARE";
    private boolean mHasCompleteListener = false;
    private boolean mHasErrorListener = false;
    private boolean isLoop = true;
    private boolean isRunning = false;
    private String mSpeed = "";


    public Lottie(HapEngine hapEngine, Context context, Container parent, int ref, RenderEventCallback callback, Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        mCallback.addActivityStateListener(this);
    }

    @Override
    protected FlexLottie createViewImpl() {
        FlexLottie lottieView = new FlexLottie(mContext);
        lottieView.setComponent(this);
        lottieView.setLoop(isLoop);
        lottieView.setFailureListener(new LottieListener<Throwable>() {
            @Override
            public void onResult(Throwable result) {
                Log.e(TAG, "mHasErrorListener result: " + result);
                callError(result.toString());
            }
        });
        lottieView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                Log.d(TAG, "onAnimationStart");
                isRunning = true;
                mHost.setMaxFrame();

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                Log.d(TAG, "mHasCompleteListener: " + mHasCompleteListener + "  isLoop:  " + isLoop);
                isRunning = false;
                if (!isLoop) {
                    if (mHasCompleteListener) {
                        // Map<String, Object> params = new HashMap<>();
                        mCallback.onJsEventCallback(getPageId(), mRef, EVENT_COMPLETE,
                                Lottie.this, null, null);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                isRunning = false;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });


        return lottieView;
    }


    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.SOURCE:
                String source = Attributes.getString(attribute);
                setSource(source);
                return true;
            case Attributes.Style.WIDTH:
                String width = Attributes.getString(attribute, "");
                setWidth(width);
                return true;
            case Attributes.Style.HEIGHT:
                String height = Attributes.getString(attribute, "");
                setHeight(height);
                return true;
            case AUTO_PLAY:
                boolean autoplay = Attributes.getBoolean(attribute, false);
                setAutoPlay(autoplay);
                return true;
            case SPEED:
                mSpeed = Attributes.getString(attribute, "1");
                Log.d(TAG, "speed: " + mSpeed);
                setSpeed(mSpeed, false);
                return true;
            case PROGRESS:
                String progress = Attributes.getString(attribute, "1");
                setProgress(progress);
                return true;
            case LOOP:
                boolean loop = Attributes.getBoolean(attribute, true);
                isLoop = loop;
                setLoop(loop);
                return true;
            case RENDER_MODE:
                String renderMode = Attributes.getString(attribute, "");
                setRenderMode(renderMode);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }


    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        if (mHost == null) {
            return;
        }
        if (METHOD_PLAY.equals(methodName)) {
            if (args == null || args.get(START_FRAME) == null || args.get(END_FRAME) == null || !isNumber(args.get(START_FRAME)) || !isNumber(args.get(END_FRAME))) {
                setSpeed(mSpeed, false);
                start();
                return;
            }
            try {
                int starFrame = (int) args.get(START_FRAME);
                int endFrame = (int) args.get(END_FRAME);
                if (starFrame < 0 || endFrame < 0) {
                    setSpeed(mSpeed, false);
                    start();
                    return;
                }
                start(starFrame, endFrame);
            } catch (Exception e) {
                callError(e.toString());
            }
        } else if (METHOD_PAUSE.equals(methodName)) {
            stop();
        } else if (METHOD_RESET.equals(methodName)) {
            reset();
        } else if (METHOD_RESUME.equals(methodName)) {
            resume();
        } else if (METHOD_GET_BOUNDING_CLIENT_RECT.equals(methodName)) {
            super.invokeMethod(methodName, args);
        }
    }


    public boolean isNumber(Object obj) {
        if (obj instanceof Number) {
            return true;
        } else if (obj instanceof String) {
            try {
                Double.parseDouble((String) obj);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public void setAutoPlay(boolean autoplay) {
        if (mHost == null) {
            return;
        }
        mHost.setAutoPlay(autoplay);
    }


    public void start(int startFrame, int endFrame) {
        if (startFrame < endFrame) {
            setSpeed(mSpeed, false);
            mHost.start(startFrame, endFrame);
        } else if (startFrame > endFrame) {
            setSpeed(mSpeed, true);
            mHost.start(endFrame, startFrame);
        } else {
            setSpeed(mSpeed, false);
            start();
            Log.d(TAG, "startFrame endFrame");
        }
    }

    public void start() {
        if (mHost == null) {
            return;
        }
        mHost.start();
    }

    public void stop() {
        if (mHost == null) {
            return;
        }
        isRunning = false;
        mHost.stop();
    }


    public boolean isRunning() {
        return isRunning;
    }

    private void setLoop(boolean loop) {
        mHost.setLoop(loop);
    }

    private void resume() {
        mHost.resume();
    }

    private void reset() {
        mHost.reset();
    }

    private void setRenderMode(String renderMode) {
        Log.d(TAG, "renderMode " + renderMode);
        if (mHost == null || renderMode == null) {
            Log.d(TAG, "host is null");
            return;
        }
        if (AUTOMATIC.equals(renderMode)) {
            mHost.setRenderMode(RenderMode.AUTOMATIC);

        } else if (HARDWARE.equals(renderMode)) {
            mHost.setRenderMode(RenderMode.HARDWARE);

        } else if (SOFTWARE.equals(renderMode)) {
            mHost.setRenderMode(RenderMode.SOFTWARE);
        } else {
            Log.d(TAG, "renderMode is exception");
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        if (EVENT_COMPLETE.equals(event)) {
            mHasCompleteListener = true;
            return true;
        } else if (EVENT_ERROR.equals(event)) {
            mHasErrorListener = true;
            mHost.setOnErrorListener(new FlexLottie.OnErrorListener() {
                @Override
                public void onError(String error) {
                    callError(error);
                }
            }, mHasErrorListener);
            return true;
        } else if (EVENT_CHANGE.equals(event)) {
            mHost.setOnChangeListener(new FlexLottie.OnChangeListener() {
                @Override
                public void onChange(float progress) {
                    Log.d(TAG, "onChange: " + progress);
                    Map<String, Object> params = new HashMap<>();
                    params.put(PROGRESS, progress);
                    mCallback.onJsEventCallback(getPageId(), mRef, EVENT_CHANGE, Lottie.this, params,
                            null);
                }
            });
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        switch (event) {
            case EVENT_COMPLETE:
                mHasCompleteListener = false;
                return true;
            case EVENT_ERROR:
                mHasErrorListener = false;
                return true;
            default: {
                break;
            }

        }
        return super.removeEvent(event);
    }

    public void setSource(String source) {
        if (mHost == null) {
            Log.d(TAG, "host is null");
            return;
        }

        if (TextUtils.isEmpty(source)) {
            return;
        }
        Uri uri = tryParseUri(source);
        if (uri != null) {
            mHost.setSource(uri);
        }
        if (uri == null) {
            callError("source uri is null");
        }
    }

    /**
     * Returns a float representing the current value of the animation from 0 to 1
     * regardless of the animation speed, direction, or min and max frames.
     */
    public void setSpeed(String speed, Boolean isEndStar) {
        if (mHost == null) {
            Log.d(TAG, "host is null");
            return;
        }
        if (TextUtils.isEmpty(speed)) {
            Log.d(TAG, "set lottie speed null");
            mHost.setSpeed(1f);
            return;
        }
        try {
            float fSpeed = Float.parseFloat(speed);
            mHost.setMethSpeed(isEndStar ? fSpeed * -1 : fSpeed);
        } catch (Exception e) {
            Log.e(TAG, "setSpeed Exception");
            callError(e.toString());
        }
    }

    private void callError(String error) {
        if (mHasErrorListener) {
            Map<String, Object> params = new HashMap<>();
            params.put(EVENT_ERROR, error);
            mCallback.onJsEventCallback(getPageId(), mRef, EVENT_ERROR,
                    Lottie.this, params, null);
        }
    }

    public void setProgress(String progress) {
        if (mHost == null) {
            Log.d(TAG, "host is null");
            return;
        }
        Log.d(TAG, "set lottie progress:  " + progress);
        if (TextUtils.isEmpty(progress)) {
            return;
        }
        try {
            float fProgress = Float.parseFloat(progress);
            mHost.setMethProgress(fProgress);
        } catch (Exception e) {
            Log.e(TAG, "setProgress Exception");
        }
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        if (mHost != null && isRunning) {
            mHost.onActivityResume();
        }
    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
        if (mHost != null && isRunning) {
            mHost.onActivityPaused();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        mCallback.removeActivityStateListener(this);
        if (mHost != null) {
            mHost.release();
        }
    }
}
