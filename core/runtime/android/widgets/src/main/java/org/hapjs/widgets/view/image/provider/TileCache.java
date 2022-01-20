/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.image.provider;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.hapjs.common.utils.BitmapUtils;

public class TileCache {

    private static final int CACHE_COUNT = 12;
    private static TileCache sInstance;
    private Set<SoftReference<Bitmap>> mReuseBits = new HashSet<>();

    private TileCache() {
    }

    public static synchronized TileCache getInstance() {
        if (sInstance == null) {
            sInstance = new TileCache();
        }
        return sInstance;
    }

    private static int getBytesPerPixel(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        } else if (config == Bitmap.Config.RGB_565) {
            return 2;
        } else if (config == Bitmap.Config.ARGB_4444) {
            return 2;
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }

    public synchronized void put(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        trim(bitmap);

        if (mReuseBits.size() >= CACHE_COUNT) {
            bitmap.recycle();
        } else {
            mReuseBits.add(new SoftReference<>(bitmap));
        }
    }

    public synchronized Bitmap get(BitmapFactory.Options options) {
        SoftReference<Bitmap> mostClosely = null;
        Iterator<SoftReference<Bitmap>> iterator = mReuseBits.iterator();
        while (iterator.hasNext()) {
            SoftReference<Bitmap> temp = iterator.next();
            Bitmap bit = temp.get();
            if (BitmapUtils.isValidate(bit)) {
                if (canUseForInBitmap(bit, options)) {
                    mostClosely = temp;
                }
            } else {
                iterator.remove();
            }
        }
        Bitmap target = mostClosely != null ? mostClosely.get() : null;
        if (BitmapUtils.isValidate(target)) {
            mReuseBits.remove(mostClosely);
            return target;
        }
        return null;
    }

    private void trim(Bitmap insert) {
        Iterator<SoftReference<Bitmap>> iterator = mReuseBits.iterator();
        while (iterator.hasNext()) {
            Bitmap bit = iterator.next().get();
            if (bit == null || bit.isRecycled()) {
                iterator.remove();
            } else if (bit.getAllocationByteCount() < insert.getAllocationByteCount()) {
                iterator.remove();
            }
        }
    }

    private int convertToPowerOf2(int inSampleSize) {
        int tarSampleSize = 1;
        while (tarSampleSize <= inSampleSize) {
            if (2 * tarSampleSize > inSampleSize) {
                return tarSampleSize;
            }
            tarSampleSize *= 2;
        }
        return tarSampleSize;
    }

    public synchronized void clear() {
        Iterator<SoftReference<Bitmap>> iterator = mReuseBits.iterator();
        while (iterator.hasNext()) {
            Bitmap bit = iterator.next().get();
            if (bit != null && !bit.isRecycled()) {
                bit.recycle();
            }
        }
        mReuseBits.clear();
    }

    private boolean canUseForInBitmap(Bitmap bitmap, BitmapFactory.Options options) {
        int convertSampleSize = convertToPowerOf2(options.inSampleSize);
        int width = options.outWidth / convertSampleSize;
        int height = options.outHeight / convertSampleSize;
        int byteCount = width * height * getBytesPerPixel(bitmap.getConfig());
        return bitmap.getWidth() == width
                && bitmap.getHeight() == height
                && byteCount == bitmap.getAllocationByteCount();
    }
}
