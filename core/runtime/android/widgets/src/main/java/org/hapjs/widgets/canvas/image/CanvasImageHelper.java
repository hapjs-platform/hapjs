/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.image;

import android.content.ComponentCallbacks;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.CloseableImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.Response;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.BitmapUtils;
import org.hapjs.common.utils.DigestUtils;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.runtime.Runtime;
import org.json.JSONObject;

public class CanvasImageHelper implements CanvasImageLoader, ComponentCallbacks {

    private static final String TAG = "CanvasImageHelper";

    private static final String SCHEMA_SHA = "sha://";
    private final CanvasImageCache mImageCache;
    /**
     * 当前imageId对应的ImageLoadInfo
     */
    private HashMap<String, CanvasImage> mCanvasImages;
    private HashMap</*url*/ String, ArrayList<Pair</*imageId*/ Integer, Callback>>> mCallbacks;
    private volatile boolean isDestroyed = false;

    private CanvasImageHelper() {
        mCanvasImages = new HashMap<>();
        mCallbacks = new HashMap<>();
        mImageCache = new CanvasImageCache();

        Runtime.getInstance().getContext().registerComponentCallbacks(this);
    }

    public static CanvasImageHelper getInstance() {
        return Holder.instance;
    }

    @Override
    public void loadImage(Uri uri, Object imageId, Callback callback) {
        if (isDestroyed) {
            return;
        }

        Map<String, Object> result = new HashMap<>();
        if (uri == null || imageId == null) {
            result.put(
                    "error",
                    "invalid param. specify an json array which length is 2, and index 0 is url to load, index 1 is image id.");
            invokeCallback(callback, result);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(imageId.toString());
        } catch (NumberFormatException e) {
            result.put("error", "invalid image id!");
            invokeCallback(callback, result);
            return;
        }

        String url = uri.toString();

        if (url.startsWith("data:image")) {
            String key = buildKey(url);
            CanvasImage image = null;
            if (mCanvasImages.containsKey(key)) {
                image = mCanvasImages.get(key);
            }

            if (image == null || (image.contains(id) && !TextUtils.equals(image.getSrc(), key))) {
                // src改变，释放原来的bitmap
                mCanvasImages.remove(key);
                image = loadBase64Image(key, url);
                mCanvasImages.put(key, image);
            }

            Map<String, Object> resultCallback = new HashMap<>();
            if (image == null) {
                result.put("error", "process base64 failed,url=" + url);
                mCanvasImages.remove(key);
            } else {
                image.bind(id);
                resultCallback.put("id", id);
                resultCallback.put("url", url);
                resultCallback.put("width", image.getWidth());
                resultCallback.put("height", image.getHeight());
            }
            invokeCallback(callback, resultCallback);
            return;
        }

        CanvasImage image = mCanvasImages.get(url);
        if (image == null) {
            image = new CanvasImage(url);
            mCanvasImages.put(url, image);
        }
        image.bind(id);

        ArrayList<Pair<Integer, Callback>> callbacks = mCallbacks.get(url);
        if (callbacks == null) {
            callbacks = new ArrayList<>();
            mCallbacks.put(url, callbacks);
        }
        callbacks.add(new Pair<>(id, callback));

        if (image.status() == CanvasImage.IDLE) {
            image.setStatus(CanvasImage.LOADING);

            BitmapUtils.fetchBitmap(
                    uri,
                    new BitmapUtils.BitmapLoadCallback() {
                        @Override
                        public void onLoadSuccess(CloseableReference<CloseableImage> reference,
                                                  Bitmap bitmap) {
                            if (isDestroyed) {
                                return;
                            }
                            CanvasImage image = mCanvasImages.get(url);
                            if (image == null) {
                                return;
                            }
                            CanvasBitmap canvasBitmap = new CanvasBitmap(reference);
                            image.setWidth(canvasBitmap.getWidth());
                            image.setHeight(canvasBitmap.getHeight());
                            image.setStatus(CanvasImage.LOADED);

                            invokeImageLoadResult(image);
                            mCallbacks.remove(url);

                            mImageCache.put(image.getSrc(), canvasBitmap);
                        }

                        @Override
                        public void onLoadFailure() {
                            if (isDestroyed) {
                                return;
                            }

                            CanvasImage image = mCanvasImages.get(url);
                            image.setStatus(CanvasImage.IDLE);
                            result.put("error", "load image failed!url = " + url);
                            invokeCallback(callback, result);
                            mCallbacks.remove(url);
                        }
                    });
        } else if (image.status() == CanvasImage.LOADED) {
            invokeImageLoadResult(image);
            mCallbacks.remove(image.getSrc());
        }
    }

    @Override
    public Bitmap loadImageData(String key, int width, int height, String base64) {
        if (isDestroyed || width <= 0 || height <= 0) {
            return null;
        }
        CanvasImage image = mCanvasImages.get(key);
        if (image != null) {
            String cachedBase64 = image.getBase64Src();
            if (TextUtils.equals(cachedBase64, base64)) {
                CanvasBitmap canvasBitmap = mImageCache.get(key);
                if (canvasBitmap != null && !canvasBitmap.isRecycled()) {
                    return canvasBitmap.get();
                }
            }
        }

        // datas中保存的rgba数据，按照顺序排列
        int[] imageData = null;
        try {
            imageData = getBase64ImageData(base64);
        } catch (OutOfMemoryError e) {
            onLowMemory();
            imageData = getBase64ImageData(base64);
        } catch (Exception e) {
            Log.e(TAG, "parse putImageData error");
            return null;
        }
        if (imageData == null) {
            return null;
        }

        try {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(imageData, 0, width, 0, 0, width, height);

            CanvasBitmap canvasBitmap = createCanvasBitmap(bitmap);
            if (image == null) {
                image = new CanvasImage(key);
                mCanvasImages.put(key, image);
            }
            image.setStatus(CanvasImage.LOADED);
            image.setWidth(canvasBitmap.getWidth());
            image.setHeight(canvasBitmap.getHeight());
            image.setBase64Src(base64);
            mImageCache.put(key, canvasBitmap);
            return canvasBitmap.get();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return null;
    }

    @Override
    public String generateImageDataKey(
            int pageId, int canvasId, int x, int y, int width, int height) {
        StringBuilder builder =
                new StringBuilder("imagedata:")
                        .append(pageId)
                        .append(",")
                        .append(canvasId)
                        .append(",")
                        .append(x)
                        .append(",")
                        .append(y)
                        .append(",")
                        .append(width)
                        .append(",")
                        .append(height);
        return buildKey(builder.toString());
    }

    @Override
    public String generateImageDataKey(String imageDataBase64) {
        return buildKey(imageDataBase64);
    }

    @Override
    public Bitmap getImageDataBitmap(String key) {
        CanvasBitmap bitmap = mImageCache.get(key);
        if (bitmap != null && bitmap.isRecycled()) {
            mImageCache.remove(key);
            return null;
        }
        return bitmap != null ? bitmap.get() : null;
    }

    @Override
    public CanvasImage getImage(int id) {
        CanvasImage canvasImage = null;
        Collection<CanvasImage> images = mCanvasImages.values();
        if (images != null) {
            for (CanvasImage image : images) {
                if (image.contains(id)) {
                    canvasImage = image;
                    break;
                }
            }
        }
        return canvasImage;
    }

    @Override
    public Bitmap getImageBitmap(CanvasImage image) {
        CanvasBitmap bitmap = mImageCache.get(image.getSrc());
        if (bitmap != null && bitmap.isRecycled()) {
            mImageCache.remove(image.getSrc());
            return null;
        }
        return bitmap != null ? bitmap.get() : null;
    }

    @Override
    public CanvasBitmap getCanvasBitmap(CanvasImage image) {
        return mImageCache.get(image.getSrc());
    }

    @Override
    public CanvasBitmap getCanvasBitmap(String key) {
        return mImageCache.get(key);
    }

    private Bitmap recoverBase64ImageInternal(CanvasImage image) {
        String base64Src = image.getBase64Src();
        if (TextUtils.isEmpty(base64Src)) {
            return null;
        }
        Bitmap bmp =
                handleBase64Texture(
                        base64Src.substring(base64Src.indexOf("base64,") + "base64,".length()));
        if (bmp == null) {
            return null;
        }
        CanvasBitmap canvasBitmap = createCanvasBitmap(bmp);
        mImageCache.put(image.getSrc(), canvasBitmap);
        return canvasBitmap.get();
    }

    @Override
    public Bitmap recoverImage(CanvasImage image) {
        if (ThreadUtils.isInMainThread()) {
            throw new RuntimeException("cannot recoverImage in MainThread!");
        }
        CanvasBitmap canvasBitmap = mImageCache.get(image.getSrc());
        if (canvasBitmap != null && !canvasBitmap.isRecycled()) {
            return canvasBitmap.get();
        }

        if (image.getSrc().startsWith(SCHEMA_SHA)) {
            // base64图片
            return recoverBase64ImageInternal(image);
        }

        return BitmapUtils.fetchBitmapSync(Uri.parse(image.getSrc()));
    }

    @Override
    public void recoverImage(CanvasImage image, RecoverImageCallback callback) {
        CanvasBitmap bitmap = mImageCache.get(image.getSrc());

        if (bitmap != null) {
            if (bitmap.isRecycled()) {
                mImageCache.remove(image.getSrc());
            } else {
                if (callback != null) {
                    callback.onSuccess(bitmap.get());
                }
                return;
            }
        }

        Executors.io()
                .execute(
                        () -> {
                            if (isDestroyed) {
                                return;
                            }
                            String src = image.getSrc();
                            if (src.startsWith(SCHEMA_SHA)) {
                                Bitmap bmp = recoverBase64ImageInternal(image);
                                if (callback != null) {
                                    if (bmp == null) {
                                        callback.onFailure();
                                    } else {
                                        callback.onSuccess(bmp);
                                    }
                                }
                            } else {
                                BitmapUtils.fetchBitmap(
                                        Uri.parse(src),
                                        new BitmapUtils.BitmapLoadCallback() {

                                            @Override
                                            public void onLoadSuccess(
                                                    CloseableReference<CloseableImage> reference,
                                                    Bitmap bitmap) {
                                                if (isDestroyed) {
                                                    return;
                                                }
                                                CanvasBitmap canvasBitmap =
                                                        new CanvasBitmap(reference);
                                                mImageCache.put(src, canvasBitmap);

                                                if (callback != null) {
                                                    callback.onSuccess(bitmap);
                                                }
                                            }

                                            @Override
                                            public void onLoadFailure() {
                                                if (callback != null) {
                                                    callback.onFailure();
                                                }
                                            }
                                        });
                            }
                        });
    }

    @Override
    public Bitmap recoverImageData(String key) {
        if (ThreadUtils.isInMainThread()) {
            throw new RuntimeException("cannot recoverImageData in MainThread!");
        }

        CanvasBitmap canvasBitmap = mImageCache.get(key);
        if (canvasBitmap != null && !canvasBitmap.isRecycled()) {
            return canvasBitmap.get();
        }

        CanvasImage image = mCanvasImages.get(key);
        if (image == null) {
            return null;
        }

        String imageDataBase64 = image.getBase64Src();
        if (TextUtils.isEmpty(imageDataBase64)) {
            return null;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        return loadImageData(key, width, height, imageDataBase64);
    }

    @Override
    public void recoverImageData(String key, RecoverImageCallback callback) {
        CanvasBitmap canvasBitmap = mImageCache.get(key);
        if (canvasBitmap != null && !canvasBitmap.isRecycled()) {
            if (callback != null) {
                callback.onSuccess(canvasBitmap.get());
            }
            return;
        }
        CanvasImage image = mCanvasImages.get(key);
        if (image == null) {
            if (callback != null) {
                callback.onFailure();
            }
            return;
        }

        final String imageDataBase64 = image.getBase64Src();
        if (TextUtils.isEmpty(imageDataBase64)) {
            if (callback != null) {
                callback.onFailure();
            }
            return;
        }

        final int width = image.getWidth();
        final int height = image.getHeight();

        Executors.io()
                .execute(
                        () -> {
                            if (isDestroyed) {
                                return;
                            }
                            Bitmap bmp = loadImageData(key, width, height, imageDataBase64);
                            if (callback != null) {
                                if (bmp == null) {
                                    callback.onFailure();
                                } else {
                                    callback.onSuccess(bmp);
                                }
                            }
                        });
    }

    private int[] getBase64ImageData(String base64) {
        byte[] datas = Base64.decode(base64, Base64.NO_WRAP);
        if (datas == null || datas.length % 4 != 0) {
            if (datas != null) {
                Log.d(TAG, "parse putImageData error,datas length is error!");
            }
            return null;
        }
        int length = datas.length / 4;
        int[] imageData = new int[length];
        for (int i = 0; i < length; i++) {
            // 128-255之间的数值byte表示为负数，需要转为正数
            imageData[i] =
                    Color.argb(
                            datas[i * 4 + 3] & 255,
                            datas[i * 4] & 255,
                            datas[i * 4 + 1] & 255,
                            datas[i * 4 + 2] & 255);
        }
        return imageData;
    }

    private CanvasBitmap createCanvasBitmap(Bitmap bitmap) {
        int originWidth = bitmap.getWidth();
        int originHeight = bitmap.getHeight();
        bitmap = preScaleBitmap(bitmap);
        float scaleX = originWidth == 0 ? 1f : bitmap.getWidth() * 1f / originWidth;
        float scaleY = originHeight == 0 ? 1f : bitmap.getHeight() * 1f / originHeight;
        return new CanvasBitmap(bitmap, scaleX, scaleY);
    }

    private Bitmap preScaleBitmap(Bitmap source) {
        if (source.getWidth() <= 0 || source.getHeight() <= 0) {
            return source;
        }
        int screenWidth = DisplayUtil.getScreenWidth(Runtime.getInstance().getContext());
        int screenHeight = DisplayUtil.getScreenHeight(Runtime.getInstance().getContext());
        if (source.getWidth() > screenWidth || source.getHeight() > screenHeight) {
            float ratio =
                    Math.min(screenWidth * 1f / source.getWidth(),
                            screenHeight * 1f / source.getHeight());
            Matrix matrix = new Matrix();
            matrix.preScale(ratio, ratio);
            Bitmap bitmap =
                    Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix,
                            false);
            source.recycle();
            return bitmap;
        }
        return source;
    }

    private CanvasImage loadBase64Image(String key, String url) {
        CanvasImage image = null;
        Bitmap bmp =
                handleBase64Texture(url.substring(url.indexOf("base64,") + "base64,".length()));
        if (bmp != null) {
            CanvasBitmap canvasBitmap = createCanvasBitmap(bmp);

            image = new CanvasImage(key);
            image.setStatus(CanvasImage.LOADED);
            image.setWidth(canvasBitmap.getWidth());
            image.setHeight(canvasBitmap.getHeight());
            image.setBase64Src(url);
            mImageCache.put(image.getSrc(), canvasBitmap);
        }
        return image;
    }

    private Bitmap handleBase64Texture(String url) {
        try {
            byte[] decodedBytes =
                    Base64.decode(url.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
            if (decodedBytes == null) {
                return null;
            }
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (OutOfMemoryError e) {
            onLowMemory();
            byte[] decodedBytes =
                    Base64.decode(url.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
            if (decodedBytes == null) {
                return null;
            }
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "error in processing base64Texture,error=" + e);
        }

        return null;
    }

    private void invokeCallback(Callback callback, Map<String, ?> map) {
        if (callback == null) {
            return;
        }

        callback.callback(new Response(Response.CODE_SUCCESS, new JSONObject(map)));
    }

    private void invokeImageLoadResult(CanvasImage image) {
        ArrayList<Pair<Integer, Callback>> callbacks = mCallbacks.get(image.getSrc());
        if (callbacks == null) {
            return;
        }

        callbacks = new ArrayList<>(callbacks);

        for (Pair<Integer, Callback> pair : callbacks) {
            if (pair == null) {
                continue;
            }
            HashMap<String, Object> result = new HashMap<>();
            int id = pair.first;
            Callback callback = pair.second;
            result.put("id", id);
            result.put("url", image.getSrc());
            result.put("width", image.getWidth());
            result.put("height", image.getHeight());
            invokeCallback(callback, result);
        }
    }

    private String buildKey(String content) {
        String md5 = DigestUtils.getSha256(content.getBytes(StandardCharsets.UTF_8));
        if (TextUtils.isEmpty(md5)) {
            return "";
        }
        return SCHEMA_SHA + md5;
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }

    public void reset() {
        clear();
        isDestroyed = false;
    }

    public void clear() {
        isDestroyed = true;
        mImageCache.clear();
        mCanvasImages.clear();
        mCallbacks.clear();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
        mImageCache.onTrimMemory();
    }

    @Override
    public Bitmap createBitmap(@NonNull Bitmap src) {
        try {
            return Bitmap.createBitmap(src);
        } catch (OutOfMemoryError e) {
            onLowMemory();
            return Bitmap.createBitmap(src);
        }
    }

    @Override
    public Bitmap createBitmap(int width, int height, Bitmap.Config config) {
        try {
            return Bitmap.createBitmap(width, height, config);
        } catch (OutOfMemoryError e) {
            onLowMemory();
            return Bitmap.createBitmap(width, height, config);
        }
    }

    @Override
    public Bitmap createBitmap(
            @NonNull Bitmap source,
            int x,
            int y,
            int width,
            int height,
            @Nullable Matrix m,
            boolean filter) {
        try {
            return Bitmap.createBitmap(source, x, y, width, height, m, filter);
        } catch (OutOfMemoryError e) {
            onLowMemory();
            return Bitmap.createBitmap(source, x, y, width, height, m, filter);
        }
    }

    private static class Holder {
        static CanvasImageHelper instance = new CanvasImageHelper();
    }
}
