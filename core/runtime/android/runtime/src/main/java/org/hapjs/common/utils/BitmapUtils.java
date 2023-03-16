/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import com.caverock.androidsvg.SVG;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.postprocessors.IterativeBoxBlurPostProcessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.common.executors.Executors;
import org.hapjs.runtime.Runtime;

public class BitmapUtils {

    public static final String NINE_PATCH_SUFFIX = ".9.png";
    private static final String TAG = "BitmapUtils";
    private static final Map<String, WeakReference<Drawable.ConstantState>> sCache =
            new ConcurrentHashMap<>();

    public static void fetchLocalDrawable(
            Context context, final Uri uri, OnDrawableDecodedListener listener) {
        if (uri == null) {
            return;
        }
        if (uri.getLastPathSegment() != null
                && uri.getLastPathSegment().endsWith(NINE_PATCH_SUFFIX)) {
            // deal with 9.png image
            Executors.io()
                    .execute(
                            () -> {
                                Drawable drawable = getFromCache(uri);
                                if (drawable == null) {
                                    removeOldCache(uri);
                                    drawable = createDrawable(uri);
                                    if (drawable != null) {
                                        Drawable.ConstantState cs = drawable.getConstantState();
                                        String key = getFileCacheKey(uri);
                                        if (key != null) {
                                            sCache.put(key, new WeakReference<>(cs));
                                        }
                                    }
                                }
                                final Drawable postDrawable = drawable;
                                Executors.ui()
                                        .execute(
                                                () -> {
                                                    listener.onDrawableDecoded(postDrawable, uri);
                                                });
                            });
        } else {
            ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(uri).build();
            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            DataSource<CloseableReference<CloseableImage>> backgroundDataSource =
                    imagePipeline.fetchDecodedImage(imageRequest, null);
            backgroundDataSource.subscribe(
                    new BaseBitmapDataSubscriber() {
                        @Override
                        protected void onFailureImpl(
                                DataSource<CloseableReference<CloseableImage>> dataSource) {
                            Log.e(TAG, "onFailureImpl: ", dataSource.getFailureCause());
                            listener.onDrawableDecoded(null, uri);
                        }

                        @Override
                        public void onNewResultImpl(
                                DataSource<CloseableReference<CloseableImage>> dataSource) {
                            if (!dataSource.isFinished()) {
                                return;
                            }
                            CloseableReference<CloseableImage> closeableImageRef =
                                    dataSource.getResult();
                            if (closeableImageRef != null) {
                                CloseableImage closeableImage = closeableImageRef.get();
                                if (closeableImage == null || closeableImage.isClosed()) {
                                    return;
                                }
                                if (closeableImage instanceof CloseableStaticBitmap) {
                                    CloseableStaticBitmap closeableStaticBitmap =
                                            (CloseableStaticBitmap) closeableImage;
                                    Bitmap targetBitmap =
                                            closeableStaticBitmap
                                                    .getUnderlyingBitmap()
                                                    .copy(Bitmap.Config.ARGB_8888, true);
                                    BitmapDrawable drawable = new BitmapDrawable(targetBitmap);
                                    listener.onDrawableDecoded(drawable, uri);
                                    CloseableReference.closeSafely(closeableImageRef);
                                } else {
                                    try {
                                        DrawableFactory animatedDrawableFactory =
                                                ImagePipelineFactory.getInstance()
                                                        .getAnimatedDrawableFactory(context);
                                        if (animatedDrawableFactory != null) {
                                            AnimatedDrawable2 drawable =
                                                    (AnimatedDrawable2)
                                                            animatedDrawableFactory
                                                                    .createDrawable(closeableImage);
                                            listener.onDrawableDecoded(drawable, uri);
                                        } else {
                                            listener.onDrawableDecoded(null, uri);
                                        }
                                    } catch (Exception exception) {
                                        Log.e(TAG, "onNewResultImpl: ", exception);
                                    } finally {
                                        CloseableReference.closeSafely(closeableImageRef);
                                    }
                                }
                            }
                        }

                        @Override
                        protected void onNewResultImpl(Bitmap bitmap) {
                        }
                    },
                    UiThreadImmediateExecutorService.getInstance());
        }
    }

    private static void removeOldCache(Uri uri) {
        Set<String> keys = sCache.keySet();
        if (keys == null || keys.size() <= 0) {
            return;
        }
        Set<String> keyList = new HashSet<>();
        keyList.addAll(keys);
        for (String key : keyList) {
            if (!TextUtils.isEmpty(key) && key.contains(uri.toString())) {
                sCache.remove(key);
            }
        }
    }

    private static Drawable getFromCache(Uri uri) {
        String cacheKey = getFileCacheKey(uri);
        if (cacheKey == null) {
            return null;
        }
        WeakReference<Drawable.ConstantState> ref = sCache.get(cacheKey);
        if (ref != null) {
            Drawable.ConstantState cs = ref.get();
            if (cs != null) {
                Context context = Runtime.getInstance().getContext();
                // NinePatchDrawable has error padding value if not set resource
                // in Android KK.
                return cs.newDrawable(context.getResources());
            }

            sCache.remove(cacheKey);
        }
        return null;
    }

    private static Drawable createDrawable(Uri uri) {
        if (uri == null) {
            return null;
        }
        InputStream is = null;
        try {
            Context context = Runtime.getInstance().getContext();
            is = context.getContentResolver().openInputStream(uri);
            // NinePatchDrawable has error padding value if not set resource
            // in Android KK.
            return Drawable
                    .createFromResourceStream(context.getResources(), null, is, uri.toString());
        } catch (FileNotFoundException e) {
            Log.e(TAG, "createDrawable: ", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                // Ignore
                Log.e(TAG, "create drawable error", e);
            }
        }

        return null;
    }

    private static String getFileCacheKey(Uri uri) {
        if (uri == null) {
            return null;
        }
        String uriStr = uri.toString();
        if (!UriUtils.isFileUri(uri)) {
            return uriStr;
        }
        String filepath = uri.getPath();
        if (TextUtils.isEmpty(filepath)) {
            return uriStr;
        }
        File file = new File(filepath);
        if (!file.exists() || !file.isFile()) {
            return uriStr;
        }

        return uriStr + ":" + Long.toString(file.lastModified());
    }

    public static Bitmap safeDecodeRegion(
            BitmapRegionDecoder regionDecoder, Rect rect, BitmapFactory.Options options) {
        Bitmap result = null;
        try {
            result = regionDecoder.decodeRegion(rect, options);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "safeDecodeRegion() failed OOM %s", e);
        } catch (Exception e) {
            Log.e(TAG, "safeDecodeRegion() failed %s", e);
        }

        return result;
    }

    public static BitmapRegionDecoder safeCreateBitmapRegionDecoder(InputStream stream) {
        BitmapRegionDecoder decoder = null;
        try {
            decoder = BitmapRegionDecoder.newInstance(stream, false);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "safeCreateBitmapRegionDecoder() failed OOM %s", e);
        } catch (Exception e) {
            Log.e(TAG, "safeCreateBitmapRegionDecoder() failed %s", e);
        }
        return decoder;
    }

    public static boolean isValidate(Bitmap bitmap) {
        return bitmap != null && !bitmap.isRecycled();
    }

    public static boolean isValidate(BitmapRegionDecoder regionDecoder) {
        return regionDecoder != null && !regionDecoder.isRecycled();
    }

    public static void fetchBitmap(Uri uri, BitmapLoadCallback callback) {
        fetchBitmap(uri, callback, 0, 0);
    }

    public static void fetchBitmap(final Uri imgUri, final BitmapLoadCallback bitmapLoadCallback,
                                   int width, int height) {
        fetchBitmap(imgUri, bitmapLoadCallback, width, height, false);
    }

    public static void fetchBitmap(final Uri imgUri, final BitmapLoadCallback bitmapLoadCallback,
                                   int width, int height, boolean setBlur) {
        if (imgUri == null || TextUtils.isEmpty(imgUri.toString()) || bitmapLoadCallback == null) {
            return;
        }
        ImageDecodeOptions options =
                ImageDecodeOptions.newBuilder()
                        .setForceStaticImage(true)
                        .setDecodePreviewFrame(true)
                        .build();
        ResizeOptions resizeOptions = null;
        int realWidth = width;
        int realHeight = height;
        if (realWidth > 0 && realHeight > 0) {
            if (width == IntegerUtil.UNDEFINED || height == IntegerUtil.UNDEFINED) {
                realWidth = 0;
                realHeight = 0;
            }
            boolean doResize = (realWidth > 0 && realHeight > 0);
            resizeOptions = doResize ? new ResizeOptions(realWidth, realHeight) : null;
        }
        ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder
                .newBuilderWithSource(imgUri)
                .setImageDecodeOptions(options)
                .setAutoRotateEnabled(true)
                .setResizeOptions(resizeOptions);
        if (setBlur) {
            //给图片加高斯模糊
            imageRequestBuilder.setPostprocessor(new IterativeBoxBlurPostProcessor(6,25));
        }
        ImageRequest imageRequest = imageRequestBuilder.build();

        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>> mBackgroundDataSource =
                imagePipeline.fetchDecodedImage(imageRequest, null);
        mBackgroundDataSource.subscribe(
                new BaseBitmapDataSubscriber() {
                    @Override
                    protected void onFailureImpl(
                            DataSource<CloseableReference<CloseableImage>> dataSource) {
                        bitmapLoadCallback.onLoadFailure();
                    }

                    @Override
                    public void onNewResultImpl(
                            DataSource<CloseableReference<CloseableImage>> dataSource) {
                        if (!dataSource.isFinished()) {
                            return;
                        }
                        CloseableReference<CloseableImage> closeableImageRef =
                                dataSource.getResult();
                        Bitmap bitmap = null;
                        if (closeableImageRef != null
                                && closeableImageRef.get() instanceof CloseableBitmap) {
                            bitmap = ((CloseableBitmap) closeableImageRef.get())
                                    .getUnderlyingBitmap();
                        }
                        if (bitmap != null) {
                            bitmapLoadCallback.onLoadSuccess(closeableImageRef, bitmap);
                        } else {
                            bitmapLoadCallback.onLoadFailure();
                        }
                    }

                    @Override
                    protected void onNewResultImpl(Bitmap bitmap) {

                    }
                }, UiThreadImmediateExecutorService.getInstance());
    }

    public static void fetchBitmapForImageSpan(final Uri imgUri, final BitmapLoadCallback bitmapLoadCallback,
                                   int width, int height) {
        if (imgUri == null || TextUtils.isEmpty(imgUri.toString()) || bitmapLoadCallback == null) {
            return;
        }
        ImageDecodeOptions options = ImageDecodeOptions.newBuilder().setForceStaticImage(true)
                .setDecodePreviewFrame(true).build();
        ResizeOptions resizeOptions = null;
        int realWidth = width;
        int realHeight = height;
        if (realWidth > 0 && realHeight > 0) {
            if (width == IntegerUtil.UNDEFINED || height == IntegerUtil.UNDEFINED) {
                realWidth = 0;
                realHeight = 0;
            }
            boolean doResize = (realWidth > 0 && realHeight > 0);
            resizeOptions = doResize ? new ResizeOptions(realWidth, realHeight) : null;
        }
        ImageRequest imageRequest = ImageRequestBuilder
                .newBuilderWithSource(imgUri)
                .setImageDecodeOptions(options)
                .setRotationOptions(RotationOptions.autoRotate())
                .setResizeOptions(resizeOptions)
                .build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>> bitmapDataSource = imagePipeline
                .fetchDecodedImage(imageRequest, null);
        bitmapDataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                bitmapLoadCallback.onLoadFailure();
            }

            @Override
            public void onNewResultImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                if (!dataSource.isFinished()) {
                    return;
                }
                CloseableReference<CloseableImage> closeableImageRef = dataSource.getResult();
                Bitmap bitmap = null;
                if (closeableImageRef != null) {
                    CloseableImage closeableImage = closeableImageRef.get();
                    if (closeableImage instanceof CloseableBitmap) {
                        bitmap = ((CloseableBitmap) closeableImage).getUnderlyingBitmap().copy(Bitmap.Config.ARGB_8888, true);
                    } else if (closeableImage instanceof SvgDecoderUtil.CloseableSvgImage){
                        SVG svg = ((SvgDecoderUtil.CloseableSvgImage) closeableImage).getSvg();
                        int svgWidth = width > 0 ? width : closeableImage.getWidth();
                        int svgHeight = height > 0 ? height : closeableImage.getHeight();
                        bitmap = Bitmap.createBitmap(svgWidth, svgHeight, Bitmap.Config.ARGB_8888);
                        Canvas bitmapCanvas = new Canvas(bitmap);
                        svg.renderToCanvas(bitmapCanvas);
                    }
                }
                if (bitmap != null) {
                    bitmapLoadCallback.onLoadSuccess(closeableImageRef, bitmap);
                } else {
                    bitmapLoadCallback.onLoadFailure();
                }
            }

            @Override
            protected void onNewResultImpl(Bitmap bitmap) {

            }

        }, UiThreadImmediateExecutorService.getInstance());
    }

    public static Bitmap fetchBitmapSync(Uri uri) {
        return fetchBitmapSync(uri, 0, 0);
    }

    public static Bitmap fetchBitmapSync(Uri uri, int width, int height) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("cannot fetch Bitmap on MainThread!");
        }
        if (uri == null || TextUtils.isEmpty(uri.toString())) {
            return null;
        }
        ImageDecodeOptions options =
                ImageDecodeOptions.newBuilder()
                        .setForceStaticImage(true)
                        .setDecodePreviewFrame(true)
                        .build();
        ResizeOptions resizeOptions = null;
        int realWidth = width;
        int realHeight = height;
        if (realWidth > 0 && realHeight > 0) {
            if (width == IntegerUtil.UNDEFINED || height == IntegerUtil.UNDEFINED) {
                realWidth = 0;
                realHeight = 0;
            }
            boolean doResize = (realWidth > 0 && realHeight > 0);
            resizeOptions = doResize ? new ResizeOptions(realWidth, realHeight) : null;
        }
        ImageRequest imageRequest =
                ImageRequestBuilder.newBuilderWithSource(uri)
                        .setImageDecodeOptions(options)
                        .setAutoRotateEnabled(true)
                        .setResizeOptions(resizeOptions)
                        .build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>> dataSource =
                imagePipeline.fetchDecodedImage(imageRequest, null);
        Bitmap bitmap = null;
        try {
            CloseableReference<CloseableImage> result = DataSources.waitForFinalResult(dataSource);
            if (result != null && result.get() instanceof CloseableBitmap) {
                bitmap = ((CloseableBitmap) result.get()).getUnderlyingBitmap();
            }
        } catch (Throwable ignored) {
            Log.e(TAG, "fetch bitmap sync error", ignored);
        } finally {
            dataSource.close();
        }
        return bitmap;
    }

    public interface OnDrawableDecodedListener {
        void onDrawableDecoded(Drawable drawable, Uri uri);
    }

    public interface BitmapLoadCallback {
        void onLoadSuccess(CloseableReference<CloseableImage> reference, Bitmap bitmap);

        void onLoadFailure();
    }

    public static Bitmap resizeBitmap(Bitmap origin, int newWidth, int newHeight) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        if (width == newWidth && height == newHeight) {
            return origin;
        }

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        return Bitmap.createBitmap(origin, 0, 0, width, height, matrix, true);
    }
}
