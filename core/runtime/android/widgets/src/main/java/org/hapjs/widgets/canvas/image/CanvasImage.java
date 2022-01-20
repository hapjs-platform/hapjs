/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.image;

import android.graphics.Bitmap;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class CanvasImage {

    public static final int IDLE = 0;
    public static final int LOADING = 1;
    public static final int LOADED = 2;
    private String mSrc;
    private int mWidth;
    private int mHeight;
    private String mBase64Src;
    private AtomicInteger mStatus = new AtomicInteger(IDLE);
    private Set<Integer> mIds = new HashSet<>();

    public CanvasImage(String src) {
        mSrc = src;
    }

    public int status() {
        return mStatus.get();
    }

    public void setStatus(@ImageLoadStatus int status) {
        mStatus.set(status);
    }

    public String getSrc() {
        return mSrc;
    }

    public void setSrc(String src) {
        mSrc = src;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    public @Nullable Bitmap get() {
        return null;
    }

    public String getBase64Src() {
        return mBase64Src;
    }

    public void setBase64Src(String base64Src) {
        mBase64Src = base64Src;
    }

    public void bind(int id) {
        mIds.add(id);
    }

    public boolean contains(int id) {
        return mIds.contains(id);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CanvasImage{");
        if (mSrc != null && mSrc.length() > 30) {
            sb.append("mSrc='").append(mSrc.substring(0, 30)).append('\'');
        }
        sb.append(", mWidth=").append(mWidth);
        sb.append(", mHeight=").append(mHeight);
        if (mBase64Src != null && mBase64Src.length() > 100) {
            sb.append(", mBase64Src='").append(mBase64Src.substring(0, 100)).append('\'');
        }
        sb.append(", mStatus=").append(mStatus);
        sb.append(", mIds=").append(mIds);
        sb.append('}');
        return sb.toString();
    }

    @IntDef({IDLE, LOADING, LOADED})
    @Retention(RetentionPolicy.SOURCE)
    @interface ImageLoadStatus {
    }
}
