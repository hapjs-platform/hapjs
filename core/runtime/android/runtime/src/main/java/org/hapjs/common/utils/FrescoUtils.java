/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import com.facebook.common.internal.Supplier;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.memory.MemoryTrimmable;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.DraweeConfig;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.decoder.ImageDecoderConfig;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.producers.BaseNetworkFetcher;
import com.facebook.imagepipeline.producers.BaseProducerContextCallbacks;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.FetchState;
import com.facebook.imagepipeline.producers.ProducerContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.net.HttpConfig;
import org.hapjs.common.net.NetworkReportManager;
import org.hapjs.logging.RuntimeLogManager;

public class FrescoUtils {

    private static List<MemoryTrimmable> mMemoryTrimmables = new CopyOnWriteArrayList<>();
    private static volatile boolean sInited = false;
    private static List<InitializedCallback> sInitializedCallback = new CopyOnWriteArrayList<>();

    /**
     * Initializes Fresco with the user Drawee config.
     */
    public static void initialize(Context context) {
        if (sInited) {
            return;
        }
        synchronized (FrescoUtils.class) {
            if (sInited) {
                return;
            }

            Context appContext = context.getApplicationContext();
            context = appContext == null ? context : appContext;

            SoLoaderHelper.initialize(context);

            SvgDecoderUtil.setAppContext(context);
            ImagePipelineConfig config =
                    ImagePipelineConfig.newBuilder(context)
                            .setImageDecoderConfig(createImageDecoderConfig())
                            .setMemoryTrimmableRegistry(new DefaultMemoryTrimmableRegistry())
                            .setBitmapMemoryCacheParamsSupplier(
                                    new BitmapMemoryCacheParamsSupplier(context))
                            .setNetworkFetcher(
                                    new OkHttpNetworkFetcher(HttpConfig.get().getOkHttpClient()))
                            .setDownsampleEnabled(true)
                            .build();

            DraweeConfig.Builder draweeConfigBuilder = DraweeConfig.newBuilder();
            addCustomDrawableFactories(draweeConfigBuilder);

            Fresco.initialize(context, config, draweeConfigBuilder.build());
            sInited = true;
        }

        Executors.io().execute(() -> {
            for (InitializedCallback callback : sInitializedCallback) {
                callback.onInitialized();
            }
            sInitializedCallback.clear();
        });
    }

    public static void initializeAsync(final Context context) {
        Executors.io()
                .execute(
                        () -> {
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskStart(context.getPackageName(),
                                            "Fresco#initialize");
                            initialize(context);
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskEnd(context.getPackageName(),
                                            "Fresco#initialize");
                        });
    }

    private static ImageDecoderConfig createImageDecoderConfig() {
        ImageDecoderConfig.Builder config = ImageDecoderConfig.newBuilder();

        config.addDecodingCapability(
                SvgDecoderUtil.SVG_FORMAT,
                new SvgDecoderUtil.SvgFormatChecker(),
                new SvgDecoderUtil.SvgDecoder());

        return config.build();
    }

    private static void addCustomDrawableFactories(DraweeConfig.Builder draweeConfigBuilder) {
        draweeConfigBuilder.addCustomDrawableFactory(new SvgDecoderUtil.SvgDrawableFactory());
    }

    public static void trimOnLowMemory() {
        for (MemoryTrimmable trimmable : mMemoryTrimmables) {
            trimmable.trim(MemoryTrimType.OnSystemLowMemoryWhileAppInForeground);
        }
    }

    public static class DefaultMemoryTrimmableRegistry implements MemoryTrimmableRegistry {

        @Override
        public void registerMemoryTrimmable(MemoryTrimmable trimmable) {
            mMemoryTrimmables.add(trimmable);
        }

        @Override
        public void unregisterMemoryTrimmable(MemoryTrimmable trimmable) {
            mMemoryTrimmables.remove(trimmable);
        }
    }

    public static class BitmapMemoryCacheParamsSupplier implements Supplier<MemoryCacheParams> {

        private static final int MAX_CACHE_ENTRIES = 256;
        private static final int MAX_CACHE_ASHM_ENTRIES = 128;
        private static final int MAX_CACHE_EVICTION_SIZE =
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.M ? 0 : Integer.MAX_VALUE;
        private static final int MAX_CACHE_EVICTION_ENTRIES =
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.M ? 0 : Integer.MAX_VALUE;
        private final ActivityManager mActivityManager;

        public BitmapMemoryCacheParamsSupplier(Context context) {
            mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        }

        @Override
        public MemoryCacheParams get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return new MemoryCacheParams(
                        getMaxCacheSize(),
                        MAX_CACHE_ENTRIES,
                        MAX_CACHE_EVICTION_SIZE,
                        MAX_CACHE_EVICTION_ENTRIES,
                        Integer.MAX_VALUE);
            } else {
                return new MemoryCacheParams(
                        getMaxCacheSize(),
                        MAX_CACHE_ASHM_ENTRIES,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE);
            }
        }

        private int getMaxCacheSize() {
            final int maxMemory =
                    Math.min(mActivityManager.getMemoryClass() * ByteConstants.MB,
                            Integer.MAX_VALUE);
            if (maxMemory < 32 * ByteConstants.MB) {
                return 4 * ByteConstants.MB;
            } else if (maxMemory < 64 * ByteConstants.MB) {
                return 6 * ByteConstants.MB;
            } else {
                return maxMemory / 4;
            }
        }
    }

    /**
     * Fresco Network fetcher that uses OkHttp 3 as a backend.
     */
    private static class OkHttpNetworkFetcher
            extends BaseNetworkFetcher<OkHttpNetworkFetcher.OkHttpNetworkFetchState> {

        private static final String TAG = "OkHttpNetworkFetchProducer";
        private static final String QUEUE_TIME = "queue_time";
        private static final String FETCH_TIME = "fetch_time";
        private static final String TOTAL_TIME = "total_time";
        private static final String IMAGE_SIZE = "image_size";
        private final Call.Factory mCallFactory;
        private Executor mCancellationExecutor;

        /**
         * @param okHttpClient client to use
         */
        OkHttpNetworkFetcher(OkHttpClient okHttpClient) {
            this(okHttpClient, okHttpClient.dispatcher().executorService());
        }

        /**
         * @param callFactory          custom {@link Call.Factory} for fetching image from the network
         * @param cancellationExecutor executor on which fetching cancellation is performed if
         *                             cancellation is requested from the UI Thread
         */
        OkHttpNetworkFetcher(Call.Factory callFactory, Executor cancellationExecutor) {
            mCallFactory = callFactory;
            mCancellationExecutor = cancellationExecutor;
        }

        @Override
        public OkHttpNetworkFetchState createFetchState(
                Consumer<EncodedImage> consumer, ProducerContext context) {
            return new OkHttpNetworkFetchState(consumer, context);
        }

        @Override
        public void fetch(final OkHttpNetworkFetchState fetchState, final Callback callback) {
            fetchState.submitTime = SystemClock.elapsedRealtime();
            final Uri uri = fetchState.getUri();

            try {
                NetworkReportManager.getInstance()
                        .reportNetwork(
                                NetworkReportManager.KEY_FRESCO,
                                uri.toString(),
                                NetworkReportManager.REPORT_LEVEL_IMAGE);
                Request request =
                        new Request.Builder()
                                .cacheControl(new CacheControl.Builder().noStore().build())
                                .url(uri.toString())
                                .get()
                                .build();

                fetchWithRequest(fetchState, callback, request);
            } catch (Exception e) {
                // handle error while creating the request
                callback.onFailure(e);
            }
        }

        @Override
        public void onFetchCompletion(OkHttpNetworkFetchState fetchState, int byteSize) {
            fetchState.fetchCompleteTime = SystemClock.elapsedRealtime();
        }

        @Override
        public Map<String, String> getExtraMap(OkHttpNetworkFetchState fetchState, int byteSize) {
            Map<String, String> extraMap = new HashMap<>(4);
            extraMap.put(QUEUE_TIME,
                    Long.toString(fetchState.responseTime - fetchState.submitTime));
            extraMap.put(
                    FETCH_TIME,
                    Long.toString(fetchState.fetchCompleteTime - fetchState.responseTime));
            extraMap.put(TOTAL_TIME,
                    Long.toString(fetchState.fetchCompleteTime - fetchState.submitTime));
            extraMap.put(IMAGE_SIZE, Integer.toString(byteSize));
            return extraMap;
        }

        void fetchWithRequest(
                final OkHttpNetworkFetchState fetchState, final Callback callback,
                final Request request) {
            final Call call = mCallFactory.newCall(request);

            fetchState
                    .getContext()
                    .addCallbacks(
                            new BaseProducerContextCallbacks() {
                                @Override
                                public void onCancellationRequested() {
                                    if (Looper.myLooper() != Looper.getMainLooper()) {
                                        call.cancel();
                                    } else {
                                        mCancellationExecutor.execute(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        call.cancel();
                                                    }
                                                });
                                    }
                                }
                            });

            call.enqueue(
                    new okhttp3.Callback() {
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            fetchState.responseTime = SystemClock.elapsedRealtime();
                            final ResponseBody body = response.body();
                            try {
                                if (!response.isSuccessful()) {
                                    handleException(
                                            call,
                                            new IOException("Unexpected HTTP code " + response),
                                            callback);
                                    return;
                                }
                                if (body == null) {
                                    handleException(call, new Exception("response body is null"),
                                            callback);
                                    return;
                                }
                                long contentLength = body.contentLength();
                                if (contentLength < 0) {
                                    contentLength = 0;
                                }
                                callback.onResponse(body.byteStream(), (int) contentLength);
                            } catch (Exception e) {
                                handleException(call, e, callback);
                            } finally {
                                try {
                                    if (body != null) {
                                        body.close();
                                    }
                                } catch (Exception e) {
                                    FLog.w(TAG, "Exception when closing response body", e);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call call, IOException e) {
                            handleException(call, e, callback);
                        }
                    });
        }

        /**
         * Handles exceptions.
         *
         * <p>
         *
         * <p>OkHttp notifies callers of cancellations via an IOException. If IOException is caught
         * after request cancellation, then the exception is interpreted as successful cancellation and
         * onCancellation is called. Otherwise onFailure is called.
         */
        private void handleException(final Call call, final Exception e, final Callback callback) {
            if (call.isCanceled()) {
                callback.onCancellation();
            } else {
                callback.onFailure(e);
            }
        }

        static class OkHttpNetworkFetchState extends FetchState {
            long submitTime;
            long responseTime;
            long fetchCompleteTime;

            OkHttpNetworkFetchState(Consumer<EncodedImage> consumer,
                                    ProducerContext producerContext) {
                super(consumer, producerContext);
            }
        }
    }

    public static void addInitializedCallback(InitializedCallback callback) {
        synchronized (SoLoaderHelper.class) {
            if (!sInited) {
                sInitializedCallback.add(callback);
                return;
            }
        }
        callback.onInitialized();
    }

    public interface InitializedCallback {
        void onInitialized();
    }
}
