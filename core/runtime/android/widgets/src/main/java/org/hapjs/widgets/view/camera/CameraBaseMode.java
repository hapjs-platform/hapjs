/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera;

import android.content.Context;
import android.view.SurfaceView;
import android.view.ViewGroup;
import org.hapjs.component.Component;

public class CameraBaseMode<T extends SurfaceView> {

    public static final String VIDEO_RECORD_TAG = "VIDEO_RECORD_TAG :";
    public static final String VIDEO_RECORD_TAG_TIMESTAMP = "VIDEO_RECORD_TAG_TIMESTAMP :";
    public T mSurfaceView;
    public CameraView mCameraView;
    public Component mComponent;
    public boolean mShowingPreview;
    public boolean mIsDestroy;

    public CameraBaseMode(CameraView cameraView, Component component) {
        mCameraView = cameraView;
        mComponent = component;
    }

    public SurfaceView getModeView(Context context, ViewGroup parentView) {
        return null;
    }

    public void initCameraMode() {
    }

    public void onBackAttachCameraMode() {
    }

    public void onViewDetachFromWindow() {
    }

    public void setUpPreview(boolean isShowingPreview) {
    }

    public void notifyShowingPreview(boolean showingPreview) {
        mShowingPreview = showingPreview;
    }

    public void onActivityPause() {
    }

    public void onActivityResume() {
    }

    public void onCameraDestroy() {
    }
}
