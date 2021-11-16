/* Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.screenshot;

import android.Manifest;
import android.os.Handler;
import android.os.Looper;

import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.json.JSONException;

@FeatureExtensionAnnotation(
        name = Screenshot.FEATURE_NAME,
        actions = {
                @ActionAnnotation(
                        name = Screenshot.ACTION_ON_USER_CAPTURE_SCREEN,
                        mode = FeatureExtension.Mode.CALLBACK,
                        permissions = Manifest.permission.READ_EXTERNAL_STORAGE),
                @ActionAnnotation(
                        name = Screenshot.ACTION_OFF_USER_CAPTURE_SCREEN,
                        mode = FeatureExtension.Mode.SYNC),
        }
)

public class Screenshot extends CallbackHybridFeature {

    protected static final String FEATURE_NAME = "system.screenshot";
    protected static final String ACTION_ON_USER_CAPTURE_SCREEN = "onUserCaptureScreen";
    protected static final String ACTION_OFF_USER_CAPTURE_SCREEN = "offUserCaptureScreen";


    private ScreenshotObserver mScreenshotObserver;
    private LifecycleListener mLifecycleListener;

    @Override
    public Response invokeInner(Request request) throws JSONException {
        String action = request.getAction();
        if (ACTION_ON_USER_CAPTURE_SCREEN.equals(action)) {
            onUserCaptureScreen(request);
        } else if (ACTION_OFF_USER_CAPTURE_SCREEN.equals(action)) {
            return offUserCaptureScreen(request);
        }
        return null;
    }


    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    protected String[] getScreenshotKeyWords(){
        return  null;
    }

    private Response onUserCaptureScreen(Request request) {
        UserCaptureScreenCallbackContext callbackContext = new UserCaptureScreenCallbackContext(request, request.getAction());
        putCallbackContext(callbackContext);
        return Response.SUCCESS;
    }

    private Response offUserCaptureScreen(Request request) {
        removeCallbackContext(ACTION_ON_USER_CAPTURE_SCREEN);
        return Response.SUCCESS;
    }

    private class UserCaptureScreenCallbackContext extends CallbackContext {

        public UserCaptureScreenCallbackContext(Request request, String action) {
            super(Screenshot.this, action, request, true);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (mScreenshotObserver == null) {
                    mScreenshotObserver = ScreenshotObserver.newInstance(mRequest.getNativeInterface().getActivity());
                    String[] screenshotKeyWords = getScreenshotKeyWords();
                    if (screenshotKeyWords != null) {
                        mScreenshotObserver.setKeyWords(screenshotKeyWords);
                    }
                    mScreenshotObserver.startListen();
                }
                mScreenshotObserver.addListener(() -> runCallbackContext(ACTION_ON_USER_CAPTURE_SCREEN, 0, Response.SUCCESS));
            });

            if (mLifecycleListener == null) {
                mLifecycleListener = new LifecycleListener() {
                    @Override
                    public void onPause() {
                        if (mScreenshotObserver != null) {
                            mScreenshotObserver.stopListen();
                            mScreenshotObserver = null;
                        }
                    }

                    @Override
                    public void onResume() {
                        if (mScreenshotObserver == null) {
                            mScreenshotObserver = ScreenshotObserver.newInstance(mRequest.getNativeInterface().getActivity());
                            mScreenshotObserver.startListen();
                            mScreenshotObserver.addListener(() -> runCallbackContext(ACTION_ON_USER_CAPTURE_SCREEN, 0, Response.SUCCESS));
                        }
                    }
                };

            }
            mRequest.getNativeInterface().addLifecycleListener(mLifecycleListener);
        }

        @Override
        public void callback(int what, Object obj) {
            mRequest.getCallback().callback(Response.SUCCESS);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (mScreenshotObserver != null) {
                    mScreenshotObserver.stopListen();
                    mScreenshotObserver = null;
                }
            });

            if (mLifecycleListener != null) {
                mRequest.getNativeInterface().removeLifecycleListener(mLifecycleListener);
            }
        }
    }
}

