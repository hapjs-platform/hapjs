/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.image.provider;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Build;
import java.io.InputStream;
import org.hapjs.common.utils.BitmapUtils;

public class TileProvider {
    private final Object mLock = new Object();
    private BitmapRegionDecoder mRegionDecoder;
    private int mImageWidth;
    private int mImageHeight;
    private Rect mTempRect = new Rect();

    public Tile.TileData createTile(Rect region, int sampleSize) {
        if (region == null) {
            return null;
        }
        mTempRect.set(0, 0, mImageWidth, mImageHeight);
        if (!mTempRect.intersect(region)) {
            return null;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            options.inPreferredConfig = Bitmap.Config.RGB_565;
        } else {
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        }
        options.inSampleSize = sampleSize;
        options.outWidth = mTempRect.width();
        options.outHeight = mTempRect.height();
        options.inBitmap = TileCache.getInstance().get(options);
        Bitmap bitmap = null;
        synchronized (mLock) {
            if (BitmapUtils.isValidate(mRegionDecoder)) {
                bitmap = BitmapUtils.safeDecodeRegion(mRegionDecoder, mTempRect, options);
            }
        }

        if (bitmap == null) {
            synchronized (mLock) {
                if (BitmapUtils.isValidate(options.inBitmap)) {
                    options.inBitmap.recycle();
                    options.inBitmap = null;
                    bitmap = BitmapUtils.safeDecodeRegion(mRegionDecoder, mTempRect, options);
                }
            }
        }

        if (BitmapUtils.isValidate(bitmap)) {
            Tile.TileData tileData = Tile.TileData.obtain();
            tileData.setBitmap(bitmap);
            tileData.setValidateHeight(mTempRect.height() / sampleSize);
            tileData.setValidateWidth(mTempRect.width() / sampleSize);
            return tileData;
        } else {
            return null;
        }
    }

    public void setInputStream(InputStream stream) {
        mRegionDecoder = BitmapUtils.safeCreateBitmapRegionDecoder(stream);
        if (BitmapUtils.isValidate(mRegionDecoder)) {
            mImageWidth = mRegionDecoder.getWidth();
            mImageHeight = mRegionDecoder.getHeight();
        }
    }

    public void release() {
        TileCache.getInstance().clear();
        synchronized (mLock) {
            if (BitmapUtils.isValidate(mRegionDecoder)) {
                mRegionDecoder.recycle();
            }
            mRegionDecoder = null;
        }
    }

    public int getImageWidth() {
        return mImageWidth;
    }

    public int getImageHeight() {
        return mImageHeight;
    }
}
