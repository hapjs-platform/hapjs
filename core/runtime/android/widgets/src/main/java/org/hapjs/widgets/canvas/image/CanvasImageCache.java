/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.image;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import java.util.Map;
import java.util.Set;
import org.hapjs.runtime.Runtime;

/**
 * canvas的图片相关缓存 页面退出后清除bitmap 内存不足清除bitmap 长时间不使用清除bitmap 应用退出后清除bitmap
 */
public class CanvasImageCache {
    private static final String TAG = "CanvasImageCache";

    private static final int BYTES_PER_ARGB_8888_PIXEL = 4;
    private static final int MEMORY_CACHE_TARGET_SCREENS = 2;

    private static final float MAX_SIZE_MULTIPLIER = 0.1f;
    private static final float LOW_MEMORY_MAX_SIZE_MULTIPLIER = 0.07f;

    LruCache<String, CanvasBitmap> mCaches;

    public CanvasImageCache() {
        mCaches =
                new LruCache<String, CanvasBitmap>(calculateMemorySize()) {
                    @Override
                    protected void entryRemoved(
                            boolean evicted, String key, CanvasBitmap oldValue,
                            CanvasBitmap newValue) {
                        super.entryRemoved(evicted, key, oldValue, newValue);
                        if (oldValue != null && oldValue != newValue && !oldValue.isRecycled()) {
                            oldValue.recycle();
                        }
                    }

                    @Override
                    protected int sizeOf(String key, CanvasBitmap value) {
                        return value.getSize();
                    }
                };
    }

    private int calculateMemorySize() {
        Context context = Runtime.getInstance().getContext();
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        boolean isLowMemoryDevice =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                        && activityManager.isLowRamDevice();

        final int memoryClassBytes = activityManager.getMemoryClass() * 1024 * 1024;
        int maxSize =
                Math.round(
                        memoryClassBytes
                                * (isLowMemoryDevice ? MAX_SIZE_MULTIPLIER :
                                LOW_MEMORY_MAX_SIZE_MULTIPLIER));

        int screenSize = screenWidth * screenHeight * BYTES_PER_ARGB_8888_PIXEL;
        int targetMemoryCacheSize = Math.round(screenSize * MEMORY_CACHE_TARGET_SCREENS);

        int memoryCacheSize;
        if (targetMemoryCacheSize <= maxSize) {
            memoryCacheSize = targetMemoryCacheSize;
        } else {
            memoryCacheSize = maxSize;
        }
        return memoryCacheSize;
    }

    public void put(String key, CanvasBitmap bitmap) {
        mCaches.put(key, bitmap);
    }

    public void remove(String key) {
        mCaches.remove(key);
    }

    public CanvasBitmap get(String key) {
        CanvasBitmap bitmap = mCaches.get(key);
        // 更新最近使用
        if (bitmap != null) {
            mCaches.put(key, bitmap);
        }
        return bitmap;
    }

    public void onTrimMemory() {
        Map<String, CanvasBitmap> snapshot = mCaches.snapshot();
        Set<String> keySet = snapshot.keySet();
        for (String key : keySet) {
            CanvasBitmap canvasBitmap = mCaches.get(key);
            if (canvasBitmap != null && !canvasBitmap.isRecycled()) {
                canvasBitmap.recycleBitmap();
            }
        }
    }

    public void clear() {
        try {
            mCaches.evictAll();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
