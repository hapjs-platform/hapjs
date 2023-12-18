/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.feature;

import android.app.Activity;
import android.util.Log;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackContextHolder;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.animation.Animation;
import org.hapjs.component.animation.AnimationParser;
import org.hapjs.render.RootView;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.render.vdom.VElement;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = AnimationFeature.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = AnimationFeature.ACTION_ENABLE, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = AnimationFeature.ACTION_PLAY, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = AnimationFeature.ACTION_PAUSE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = AnimationFeature.ACTION_FINISH, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = AnimationFeature.ACTION_CANCEL, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = AnimationFeature.ACTION_REVERSE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = AnimationFeature.ACTION_SET_START_TIME,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = AnimationFeature.ACTION_GET_CUR_TIME,
                        mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(
                        name = AnimationFeature.ACTION_GET_START_TIME,
                        mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(
                        name = AnimationFeature.ACTION_GET_PLAY_STATE,
                        mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(
                        name = AnimationFeature.ACTION_GET_READY,
                        mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(
                        name = AnimationFeature.ACTION_GET_FINISHED,
                        mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(
                        name = AnimationFeature.ACTION_GET_PENDING,
                        mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(
                        name = AnimationFeature.EVENT_ON_ANIMATION_CANCAL,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = AnimationFeature.EVENT_ON_ANIMATION_FINISH,
                        mode = FeatureExtension.Mode.ASYNC)
        })
public class AnimationFeature extends CallbackHybridFeature {
    protected static final String FEATURE_NAME = "system.animation";
    protected static final String ACTION_ENABLE = "enable";
    protected static final String ACTION_PLAY = "play";
    protected static final String ACTION_FINISH = "finish";
    protected static final String ACTION_PAUSE = "pause";
    protected static final String ACTION_CANCEL = "cancel";
    protected static final String ACTION_REVERSE = "reverse";
    protected static final String ACTION_GET_CUR_TIME = "getCurrentTime";
    protected static final String ACTION_SET_START_TIME = "setStartTime";
    protected static final String ACTION_GET_START_TIME = "getStartTime";
    protected static final String ACTION_GET_FINISHED = "getFinished";
    protected static final String ACTION_GET_READY = "getReady";
    protected static final String ACTION_GET_PLAY_STATE = "getPlayState";
    protected static final String ACTION_GET_PENDING = "getPending";
    protected static final String EVENT_ON_ANIMATION_CANCAL = "oncancel";
    protected static final String EVENT_ON_ANIMATION_FINISH = "onfinish";
    private static final String TAG = "AnimationFeature";
    private static final String CONNECTOR = "-";
    private final Map<String, Animation> mAnimations = new ConcurrentHashMap<>();
    private final Map<String, Animation> mPausingAnimations = new ConcurrentHashMap<>();
    private final Object mLock = new Object();
    private HybridManager mHybridManager;
    private final LifecycleListener mLifecycleListener = new LifecycleListener() {
        @Override
        public void onResume() {
            super.onResume();
            synchronized (mLock) {
                Set<Map.Entry<String, Animation>> entries = mPausingAnimations.entrySet();
                for (Map.Entry<String, Animation> entry : entries) {
                    entry.getValue().resume();
                }
                mPausingAnimations.clear();
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            synchronized (mLock) {
                Set<Map.Entry<String, Animation>> entries = mAnimations.entrySet();
                for (Map.Entry<String, Animation> entry : entries) {
                    Animation animation = entry.getValue();
                    //页面 onPause 时，要暂停所有 infinity 的动画，否则会在 Android 13 的设备上崩溃
                    if (animation.getAnimatorSet() != null
                            && Integer.MAX_VALUE == animation.getAnimatorSet().getRepeatCount()) {
                        if (animation.getPlayState().equals("running")) {
                            mPausingAnimations.put(entry.getKey(), entry.getValue());
                            animation.pause();
                        }
                    }
                }
            }
        }
    };

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(final Request request) throws Exception {
        JSONObject data = request.getJSONParams();
        final String compId = data.getString("componentId");
        final String animId = data.getString("animationId");
        final String key = compId + CONNECTOR + animId;
        Activity activity = request.getNativeInterface().getActivity();
        switch (request.getAction()) {
            case ACTION_ENABLE:
                if (mHybridManager == null) {
                    mHybridManager = request.getView().getHybridManager();
                    setLifecycleListener();
                }
                enable(request, compId, animId);
                break;
            case ACTION_PLAY:
                activity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                play(key);
                            }
                        });
                break;
            case ACTION_PAUSE:
                activity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                synchronized (mLock) {
                                    mPausingAnimations.remove(key);
                                }
                                pause(key);
                            }
                        });
                break;
            case ACTION_FINISH:
                activity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                finish(key);
                            }
                        });
                break;
            case ACTION_CANCEL:
                activity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                cancel(key);
                            }
                        });
                break;
            case ACTION_REVERSE:
                activity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                reverse(key);
                            }
                        });
                break;
            case ACTION_SET_START_TIME:
                activity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                setStartTime(key, request);
                            }
                        });
                break;
            case ACTION_GET_CUR_TIME:
                return new Response(System.currentTimeMillis());
            case ACTION_GET_START_TIME:
                return getStartTime(key);
            case ACTION_GET_READY:
                return getReady(key);
            case ACTION_GET_FINISHED:
                return getFinished(key);
            case ACTION_GET_PENDING:
                return getPending(key);
            case ACTION_GET_PLAY_STATE:
                return getPlayState(key);
            case EVENT_ON_ANIMATION_CANCAL:
            case EVENT_ON_ANIMATION_FINISH:
                handleEventRequest(request, key);
                break;
            default:
                break;
        }

        return Response.SUCCESS;
    }

    private void setLifecycleListener() {
        if (mHybridManager != null) {
            mHybridManager.addLifecycleListener(mLifecycleListener);
        }
    }

    private void enable(Request request, String compId, String animId) {
        int ref = Integer.parseInt(compId);
        RootView rootView = request.getNativeInterface().getRootView();
        if (rootView == null) {
            Log.w(TAG, "rootView is null");
            return;
        }
        VDocument document = rootView.getDocument();
        if (document == null) {
            Log.w(TAG, "document is null");
            return;
        }
        VElement vElement = document.getElementById(ref);
        if (vElement == null) {
            Log.w(TAG, "vElement is null");
            return;
        }
        Component target = vElement.getComponent();
        if (target == null) {
            Log.w(TAG, "component may be recycled");
            return;
        }
        Animation animation = null;
        JSONObject data = null;
        try {
            data = request.getJSONParams();
            String keyframes = data.getString("keyframes");
            String options = data.getString("options");
            animation = target.animate(animId, keyframes, options);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (animation != null) {
            String key = compId + CONNECTOR + animId;
            mAnimations.put(key, animation);
        } else {
            Log.e(TAG, "Animation not Create !!");
        }
    }

    private void play(String key) {
        if (mAnimations.containsKey(key)) {
            Animation animation = mAnimations.get(key);
            if (animation == null) {
                Log.e(TAG, "play: animation is null of which key is " + key);
                return;
            }
            animation.play();
        } else {
            Log.e(TAG, "Can not find Animation " + key);
        }
    }

    private void setStartTime(String key, Request request) {
        if (mAnimations.containsKey(key)) {
            Animation animation = mAnimations.get(key);
            int delay = 0;
            try {
                JSONObject data = request.getJSONParams();
                String startTime = data.getString("startTime");
                delay = AnimationParser.getTime(startTime);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (animation == null || animation.getAnimatorSet() == null) {
                Log.e(
                        TAG,
                        "setStartTime: "
                                + (animation == null
                                ? "animation is  null "
                                : "animation.getAnimatorSet() is  null"));
                return;
            }
            animation.getAnimatorSet().setStartTime(delay);
        } else {
            Log.e(TAG, "Can not find Animation " + key);
        }
    }

    private void pause(String key) {
        if (mAnimations.containsKey(key)) {
            Animation animation = mAnimations.get(key);
            if (animation == null) {
                Log.e(TAG, "pause: animation is null of which key is " + key);
                return;
            }
            animation.pause();
        } else {
            Log.e(TAG, "Can not find Animation " + key);
        }
    }

    private void finish(String key) {
        if (mAnimations.containsKey(key)) {
            Animation animation = mAnimations.get(key);
            if (animation == null) {
                Log.e(TAG, "finish: animation is null of which key is " + key);
                return;
            }
            animation.finish();
        } else {
            Log.e(TAG, "Can not find Animation " + key);
        }
    }

    private void cancel(String key) {
        if (mAnimations.containsKey(key)) {
            Animation animation = mAnimations.get(key);
            if (animation == null) {
                Log.e(TAG, "cancel: animation is null of which key is " + key);
                return;
            }
            animation.cancel();
        } else {
            Log.e(TAG, "Can not find Animation " + key);
        }
    }

    private void reverse(String key) {
        if (mAnimations.containsKey(key)) {
            Animation animation = mAnimations.get(key);
            if (animation == null) {
                Log.e(TAG, "reverse: animation is null of which key is " + key);
                return;
            }
            animation.reverse();
        } else {
            Log.e(TAG, "Can not find Animation " + key);
        }
    }

    private Response getStartTime(String key) {
        if (mAnimations.containsKey(key)) {
            Animation animation = mAnimations.get(key);
            if (animation == null) {
                Log.e(TAG, "getStartTime: animation is null of which key is " + key);
                return new Response(0);
            }
            long startTime = animation.getStartTime();
            return new Response(startTime);
        }
        return new Response(0);
    }

    private Response getReady(String key) {
        if (mAnimations.containsKey(key)) {
            Animation animation = mAnimations.get(key);
            if (animation == null) {
                Log.e(TAG, "getReady: animation is null of which key is " + key);
                return new Response(0);
            }
            boolean ready = animation.isReady();
            return new Response(ready);
        }
        return new Response(false);
    }

    private Response getFinished(String key) {
        if (mAnimations.containsKey(key)) {
            Animation animation = mAnimations.get(key);
            if (animation == null) {
                Log.e(TAG, "getFinished: animation is null of which key is " + key);
                return new Response(0);
            }
            boolean finished = animation.isFinished();
            return new Response(finished);
        }
        return new Response(false);
    }

    private Response getPending(String key) {
        if (mAnimations.containsKey(key)) {
            Animation animation = mAnimations.get(key);
            if (animation == null) {
                Log.e(TAG, "getPending: animation is null of which key is " + key);
                return new Response(0);
            }
            boolean pending = animation.isPending();
            return new Response(pending);
        }
        return new Response(false);
    }

    private Response getPlayState(String key) {
        if (mAnimations.containsKey(key)) {
            Animation animation = mAnimations.get(key);
            if (animation == null) {
                Log.e(TAG, "getPlayState: animation is null of which key is " + key);
                return new Response(0);
            }
            String playState = animation.getPlayState();
            return new Response(playState);
        }
        return new Response("idle");
    }

    private void handleEventRequest(Request request, String key) {
        if (!mAnimations.containsKey(key)) {
            return;
        }
        String action = key + CONNECTOR + request.getAction();
        if (request.getCallback().isValid()) {
            final AnimationCallbackContext callbackContext =
                    new AnimationCallbackContext(this, action, request, key);
            putCallbackContext(callbackContext);
            Animation animation = mAnimations.get(key);
            if (animation == null) {
                Log.e(TAG, "handleEventRequest: animation is null of which key is " + key);
                return;
            }
            animation.setAnimationLifecycle(
                    new Animation.AnimationLifecycle() {
                        @Override
                        public void onDestroy(String key) {
                            removeCallbackContext(key);
                        }
                    },
                    action);
        } else {
            removeCallbackContext(action);
        }
    }

    @Override
    public boolean isBuiltInExtension() {
        return true;
    }

    @Override
    public void dispose(boolean force) {
        super.dispose(force);
        if (force && mHybridManager != null) {
            mHybridManager.removeLifecycleListener(mLifecycleListener);
        }
    }

    private class AnimationCallbackContext extends CallbackContext
            implements Animation.OnFinishListener, Animation.OnCancelListener {

        private String mKey;
        private Animation animation;

        public AnimationCallbackContext(
                CallbackContextHolder holder, String action, Request request, String key) {
            super(holder, action, request, false);
            mKey = key;
            animation = mAnimations.get(mKey);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            if (animation == null) {
                return;
            }
            String action = getRequest().getAction();
            switch (action) {
                case EVENT_ON_ANIMATION_CANCAL:
                    animation.setCancelListener(this);
                    break;
                case EVENT_ON_ANIMATION_FINISH:
                    animation.setFinishListener(this);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (animation != null) {
                switch (getRequest().getAction()) {
                    case EVENT_ON_ANIMATION_CANCAL:
                        animation.setCancelListener(null);
                        break;
                    case EVENT_ON_ANIMATION_FINISH:
                        animation.setFinishListener(null);
                        break;
                    default:
                        break;
                }
            }
        }

        @Override
        public void callback(int what, Object obj) {
            getRequest().getCallback().callback((Response) obj);
        }

        @Override
        public void onCancel() {
            runCallbackContext(getAction(), 0, Response.SUCCESS);
        }

        @Override
        public void onFinish() {
            runCallbackContext(getAction(), 0, Response.SUCCESS);
        }
    }
}
