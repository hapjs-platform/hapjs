/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.media;

public class MediaQuery {
    private boolean mIsNot = false;

    // and
    private MediaProperty[] mMediaProperties;

    boolean isNot() {
        return mIsNot;
    }

    void setIsNot(boolean isNot) {
        mIsNot = isNot;
    }

    public MediaProperty[] getMediaProperties() {
        return mMediaProperties;
    }

    void setMediaProperties(MediaProperty[] mediaProperties) {
        this.mMediaProperties = mediaProperties;
    }

    boolean getResult() {
        boolean result = true;
        for (MediaProperty mMediaProperty : mMediaProperties) {
            result = result && mMediaProperty.getResult();
        }
        if (mIsNot) {
            result = !result;
        }
        return result;
    }

    void updateMediaPropertyInfo(MediaPropertyInfo info) {
        for (MediaProperty mMediaProperty : mMediaProperties) {
            mMediaProperty.updateMediaPropertyInfo(info);
        }
    }

    public String getMedia() {
        return "screen";
    }

    public MediaProperty item(int i) {
        return mMediaProperties[i];
    }
}
