/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.hapjs.runtime.R;

public class IconUtils {
    private static final String TAG = "IconUtils";

    private static final float[] densities = new float[] {1.5f, 2f, 2.75f, 3f, 4f};
    private static final int[] full_sizes = new int[] {90, 136, 168, 192, 224};
    private static final int[] content_sizes = new int[] {80, 120, 150, 164, 200};
    private static final float RADIUS_RATIO = 0.15f;
    private static Executor sExecutor = Executors.newCachedThreadPool();

    public static Drawable getIconDrawable(Context context, Uri uri) {
        Bitmap bitmap = getIconBitmap(context, uri);
        if (bitmap == null) {
            return null;
        }

        return getIconDrawable(context, bitmap);
    }

    @NonNull
    public static Drawable getIconDrawable(Context context, Bitmap bitmap) {
        RoundedBitmapDrawable iconDrawable =
                RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);
        iconDrawable.setCornerRadius(bitmap.getWidth() * RADIUS_RATIO);
        return iconDrawable;
    }

    public static Bitmap getIconBitmap(Context context, Uri uri) {
        return getIconBitmap(context, uri, true);
    }

    public static Bitmap getIconBitmap(Context context, Uri uri, boolean drawFlag) {
        if (uri == null) {
            return null;
        }

        int densityIndex = chooseBestDensity(context);
        int fullSize = full_sizes[densityIndex];
        Bitmap bitmap = loadIconBitmap(context, uri, fullSize);
        if (drawFlag) {
            Bitmap result = drawFlagOnIconBitmap(context, bitmap);
            if (bitmap != null) {
                bitmap.recycle();
            }
            return result;
        }
        return bitmap;
    }

    public static Bitmap drawFlagOnIconBitmap(Context context, Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Drawable flagDrawable = null;
        try {
            flagDrawable = context.getResources().getDrawable(R.drawable.flag);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "icon flag resource not found.", e);
        }
        if (flagDrawable == null) {
            Log.e(TAG, "flagDrawable is null.");
            return bitmap;
        }
        int densityIndex = chooseBestDensity(context);
        int fullSize = full_sizes[densityIndex];
        Bitmap result = Bitmap.createBitmap(fullSize, fullSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.setDrawFilter(
                new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        Rect fromRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Rect toRect = new Rect(0, 0, fullSize, fullSize);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawBitmap(bitmap, fromRect, toRect, paint);
        Rect flagRect = new Rect(0, 0, fullSize, fullSize);
        flagDrawable.setBounds(flagRect);
        flagDrawable.draw(canvas);
        return result;
    }

    public static Bitmap getCircleIconNoFlagBitmap(Context context, Uri uri) {
        Bitmap bitmap = getIconBitmap(context, uri, false);
        return getCircleBitmap(bitmap);
    }

    public static Bitmap getRoundIconBitmap(Context context, Uri uri) {
        Bitmap bitmap = getIconBitmap(context, uri);
        return getRoundIconBitmap(bitmap);
    }

    public static Bitmap getRoundIconBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        return getRoundIconBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight());
    }

    public static Bitmap getRoundIconBitmap(Bitmap bitmap, int expectedWidth, int expectedHeight) {
        if (bitmap == null) {
            return null;
        }
        Bitmap output = Bitmap.createBitmap(expectedWidth, expectedHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF dst = new RectF(0, 0, expectedWidth, expectedHeight);

        canvas.drawRoundRect(dst, expectedWidth * RADIUS_RATIO, expectedWidth * RADIUS_RATIO,
                paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, src, dst, paint);
        return output;
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Bitmap output =
                Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        canvas.drawCircle(
                bitmap.getWidth() / (float) 2,
                bitmap.getHeight() / (float) 2,
                bitmap.getWidth() / (float) 2,
                paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    private static int chooseBestDensity(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        if (density <= densities[0]) {
            return 0;
        }
        for (int i = 1; i < densities.length; ++i) {
            if (density <= densities[i]) {
                if (densities[i] - density < density - densities[i - 1]) {
                    return i;
                } else {
                    return i - 1;
                }
            }
        }
        return densities.length - 1;
    }

    private static Bitmap loadIconBitmap(Context context, Uri uri, int expectSize) {
        Point size = resolveIconSize(context, uri);
        if (size == null) {
            return null;
        }

        int actualWidth = size.x;
        int actualHeight = size.y;
        if (actualWidth == expectSize && actualHeight == expectSize) {
            return decodeIconBitmap(context, uri, null);
        }

        int sampleSize = Math.min(actualWidth, actualHeight) / expectSize;
        if (sampleSize == 0) {
            sampleSize = 1;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        return decodeIconBitmap(context, uri, options);
    }

    private static Point resolveIconSize(Context context, Uri uri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        decodeIconBitmap(context, uri, options);
        if (options.outWidth == 0 || options.outHeight == 0) {
            return null;
        } else {
            return new Point(options.outWidth, options.outHeight);
        }
    }

    private static Bitmap decodeIconBitmap(Context context, Uri uri,
                                           BitmapFactory.Options options) {
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(in, null, options);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Can't open icon: " + uri.getPath(), e);
            return null;
        } finally {
            FileUtils.closeQuietly(in);
        }
    }

    public static void getIconBitmapAsync(
            Context context, Uri iconUri, final OnBitmapCallback callBack) {
        final Context applicationContext = context.getApplicationContext();
        int densityIndex = chooseBestDensity(context);
        int fullSize = full_sizes[densityIndex];
        ImageRequest request =
                ImageRequestBuilder.newBuilderWithSource(iconUri)
                        .setResizeOptions(new ResizeOptions(fullSize, fullSize))
                        .build();
        FrescoUtils.initialize(context);
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>> data =
                imagePipeline.fetchDecodedImage(request, null);
        data.subscribe(
                new BaseBitmapDataSubscriber() {
                    @Override
                    protected void onNewResultImpl(Bitmap bitmap) {
                        if (bitmap == null) {
                            callBack.onError(new Exception("Download bitmap error"));
                            return;
                        }

                        Log.d(TAG, "onNewResultImpl:" + bitmap);
                        Bitmap result = drawFlagOnIconBitmap(applicationContext, bitmap);
                        callBack.onResult(result);
                    }

                    @Override
                    protected void onFailureImpl(
                            DataSource<CloseableReference<CloseableImage>> dataSource) {
                        callBack.onError(dataSource.getFailureCause());
                    }
                },
                sExecutor);
    }

    public static void getIconBitmapWithoutResize(Context context, Uri iconUri, final OnBitmapCallback callBack) {
        final Context applicationContext = context.getApplicationContext();
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(iconUri).build();
        FrescoUtils.initialize(applicationContext);
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>>
                data = imagePipeline.fetchDecodedImage(request, null);
        data.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onNewResultImpl(Bitmap bitmap) {
                if (bitmap == null) {
                    callBack.onError(new Exception("Download bitmap error"));
                    return;
                }

                Log.d(TAG, "onNewResultImpl:" + bitmap);
                callBack.onResult(bitmap);
            }

            @Override
            protected void onFailureImpl(
                    DataSource<CloseableReference<CloseableImage>> dataSource) {
                callBack.onError(dataSource.getFailureCause());
            }
        }, sExecutor);
    }

    public static void getIconDrawableAsync(
            Context context, Uri iconUri, final OnDrawableCallback callBack) {
        final Context applicationContext = context.getApplicationContext();
        getIconBitmapAsync(
                context,
                iconUri,
                new OnBitmapCallback() {
                    @Override
                    public void onResult(Bitmap bitmap) {
                        RoundedBitmapDrawable iconDrawable =
                                RoundedBitmapDrawableFactory
                                        .create(applicationContext.getResources(), bitmap);
                        iconDrawable.setCornerRadius(bitmap.getWidth() * RADIUS_RATIO);
                        callBack.onResult(iconDrawable);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        callBack.onError(throwable);
                    }
                });
    }

    public interface OnDrawableCallback {
        void onResult(Drawable drawable);

        void onError(Throwable throwable);
    }

    public interface OnBitmapCallback {
        void onResult(Bitmap bitmap);

        void onError(Throwable throwable);
    }
}
