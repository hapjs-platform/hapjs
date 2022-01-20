/*
 * Copyright (C) 2012 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hapjs.features.barcode.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.executors.Future;

final class AutoFocusManager implements Camera.AutoFocusCallback {

    private static final String TAG = AutoFocusManager.class.getSimpleName();
    private static final long AUTO_FOCUS_INTERVAL_MS = 1500L;
    private final boolean useAutoFocus;
    private final Camera camera;
    private boolean active;
    private Future mFuture;

    AutoFocusManager(Context context, Camera camera) {
        this.camera = camera;
        String currentFocusMode = camera.getParameters().getFocusMode();
        useAutoFocus = true;
        Log.i(TAG,
                "Current focus mode '" + currentFocusMode + "'; use auto focus? " + useAutoFocus);
        start();
    }

    @Override
    public synchronized void onAutoFocus(boolean success, Camera theCamera) {
        if (active) {
            mFuture =
                    Executors.scheduled()
                            .executeWithDelay(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            synchronized (AutoFocusManager.this) {
                                                if (active) {
                                                    start();
                                                }
                                            }
                                        }
                                    },
                                    AUTO_FOCUS_INTERVAL_MS);
        }
    }

    synchronized void start() {
        if (useAutoFocus) {
            active = true;
            try {
                camera.autoFocus(this);
            } catch (RuntimeException re) {
                // Have heard RuntimeException reported in Android 4.0.x+; continue?
                Log.w(TAG, "Unexpected exception while focusing", re);
            }
        }
    }

    synchronized void stop() {
        if (useAutoFocus) {
            try {
                camera.cancelAutoFocus();
            } catch (RuntimeException re) {
                // Have heard RuntimeException reported in Android 4.0.x+; continue?
                Log.w(TAG, "Unexpected exception while cancelling focusing", re);
            }
        }
        if (mFuture != null) {
            mFuture.cancel(true);
            mFuture = null;
        }
        active = false;
    }
}
