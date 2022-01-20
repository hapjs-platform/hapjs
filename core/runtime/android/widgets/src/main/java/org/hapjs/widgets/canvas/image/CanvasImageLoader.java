/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.image;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.hapjs.bridge.Callback;

public interface CanvasImageLoader {

    /**
     * 加载图片，该图片可以是本地图片，网络图片或者base64图片
     *
     * @param uri
     */
    void loadImage(Uri uri, Object imageId, Callback callback);

    /**
     * 加载putImageData的图片数据
     *
     * @param key    该图片的唯一标识。可通过{@link #generateImageDataKey(int, int, int, int, int, int)}创建
     * @param width  图片的宽度
     * @param height 图片的高度
     * @param base64 图片的像素base64编码。jsfw将图片的pixels通过base64传递到native处理。
     */
    Bitmap loadImageData(String key, int width, int height, String base64);

    /**
     * @param pageId   canvas组件所在的页面ID
     * @param canvasId canvas组件的ID
     * @param x        putImageData的起始位置X
     * @param y        putImageData的起始位置Y
     * @param width    putImageData的宽度
     * @param height   putImageData的高度
     * @return
     * @deprecated
     */
    String generateImageDataKey(int pageId, int canvasId, int x, int y, int width, int height);

    /**
     * @param imageDataBase64
     * @return
     */
    String generateImageDataKey(String imageDataBase64);

    /**
     * 获取PutImageData对应的bitmap图片
     *
     * @param key
     * @return
     */
    Bitmap getImageDataBitmap(String key);

    /**
     * 获取Image，CanvasImage与JSFW中的Image对应。
     *
     * @param id image的id
     * @return
     */
    CanvasImage getImage(int id);

    /**
     * 获取image对应的bitmap图片
     *
     * @param image
     * @return
     */
    Bitmap getImageBitmap(CanvasImage image);

    CanvasBitmap getCanvasBitmap(CanvasImage image);

    CanvasBitmap getCanvasBitmap(String key);

    /**
     * 恢复图片，图片可能会在内存不足的情况下被回收。如果在主线程中调用该方法将抛出异常
     *
     * @param image
     * @return
     */
    Bitmap recoverImage(CanvasImage image);

    /**
     * 恢复图片，图片可能会在内存不足的情况下被回收。
     *
     * @param image
     * @param callback
     */
    void recoverImage(CanvasImage image, RecoverImageCallback callback);

    /**
     * 恢复ImageData图片，图片可能会在内存不足的情况下被回收。如果在主线程中调用该方法将抛出异常
     *
     * @param key
     * @return
     */
    Bitmap recoverImageData(String key);

    /**
     * 恢复ImageData图片，图片可能会在内存不足的情况下被回收。如果在主线程中调用该方法将抛出异常
     *
     * @param key
     * @param callback
     */
    void recoverImageData(String key, RecoverImageCallback callback);

    Bitmap createBitmap(@NonNull Bitmap src);

    Bitmap createBitmap(int width, int height, Bitmap.Config config);

    Bitmap createBitmap(
            @NonNull Bitmap source,
            int x,
            int y,
            int width,
            int height,
            @Nullable Matrix m,
            boolean filter);

    interface RecoverImageCallback {
        void onSuccess(Bitmap bitmap);

        void onFailure();
    }
}
