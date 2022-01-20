/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.image;

import android.graphics.Bitmap;
import android.util.Log;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;

public class CanvasBitmap {
    private static final String TAG = "CanvasBitmap";

    private Bitmap mBitmap;
    private CloseableReference<CloseableImage> mReference;
    private int mWidth;
    private int mHeight;

    private float mScaleX;
    private float mScaleY;

    public CanvasBitmap(Bitmap bitmap, float scaleX, float scaleY) {
        if (mReference != null) {
            throw new IllegalArgumentException("CloseableReference is not null");
        }
        mBitmap = bitmap;
        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();
        mScaleX = scaleX;
        mScaleY = scaleY;
    }

    public CanvasBitmap(CloseableReference<CloseableImage> reference) {
        if (mBitmap != null) {
            throw new IllegalArgumentException("mBitmap is not null!");
        }
        if (!(reference.get() instanceof CloseableBitmap)) {
            Log.e(TAG, "only support CloseableBitmap!");
            return;
        }
        mReference = reference;

        CloseableImage closeableImage = reference.get();
        if (closeableImage instanceof CloseableBitmap) {
            CloseableBitmap closeableBitmap = (CloseableBitmap) closeableImage;
            Bitmap bitmap = closeableBitmap.getUnderlyingBitmap();
            if (bitmap != null) {
                mWidth = bitmap.getWidth();
                mHeight = bitmap.getHeight();
            }
        }
        mScaleX = 1f;
        mScaleY = 1f;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public boolean isRecycled() {
        if (mBitmap != null) {
            return mBitmap.isRecycled();
        }

        return mReference == null;
    }

    public void recycle() {
        if (isRecycled()) {
            return;
        }

        recycleBitmap();

        if (mReference != null) {
            mReference.close();
            mReference = null;
        }
    }

    public void recycleBitmap() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    public Bitmap get() {
        if (mBitmap != null) {
            return mBitmap;
        }

        if (mReference != null) {
            CloseableImage closeableImage = mReference.get();
            if (closeableImage instanceof CloseableBitmap) {
                return ((CloseableBitmap) closeableImage).getUnderlyingBitmap();
            }
        }
        return null;
    }

    public int getSize() {
        if (mBitmap != null) {
            return mBitmap.getRowBytes() * mBitmap.getHeight();
        }

        // closeableImage由fresco缓存，这里不计入canvas的缓存大小中
        // 1 means one object,cannot be zero!
        return 1;
    }

    public float getScaleX() {
        return mScaleX;
    }

    public float getScaleY() {
        return mScaleY;
    }
}
