/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.share;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.NativeInterface;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.storage.file.InternalUriUtils;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.MediaUtils;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.model.AppInfo;
import org.hapjs.system.SysOpProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Share.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Share.ACTION_SHARE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Share.ACTION_GET_PROVIDER, mode =
                        FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = Share.ACTION_GET_AVAILABLE_PLATFORMS, mode =
                        FeatureExtension.Mode.ASYNC)
        }
)
public class Share extends FeatureExtension {

    private static final String TAG = "HybridShare";
    protected static final String FEATURE_NAME = "service.share";
    /**
     * Action supported by this feature.
     */
    protected static final String ACTION_SHARE = "share";
    protected static final String ACTION_GET_PROVIDER = "getProvider";
    protected static final String ACTION_GET_AVAILABLE_PLATFORMS = "getAvailablePlatforms";

    protected static final String PARAM_SHARE_TYPE = "shareType";
    protected static final String PARAM_TITLE = "title";
    protected static final String PARAM_SUMMARY = "summary";
    protected static final String PARAM_TARGET_URL = "targetUrl";
    protected static final String PARAM_IMAGE_PATH = "imagePath";
    protected static final String PARAM_MEDIA_URL = "mediaUrl";
    protected static final String PARAM_DIALOG_TITLE = "dialogTitle";
    protected static final String PARAM_PLATFORMS = "platforms";

    protected static final String RESULT_PLATFORMS = "platforms";
    private boolean isMenubar = false;

    private ShareAction mShareAction;
    private static final int MSG_ONLINE_SHARE = 0x01;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ONLINE_SHARE:
                    Uri uri = (Uri) msg.obj;
                    if (uri != null) {
                        mShareAction.getShareContent().setImageUri(uri);
                        mShareAction.share();
                    } else {
                        Log.i(TAG, "image uri is null");
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private Platform[] mDefaultPlatforms = new Platform[]{Platform.SYSTEM};

    @Override
    public Response invokeInner(Request request) throws JSONException {
        if (ACTION_SHARE.equals(request.getAction())) {
            invokeShareTo(request);
        } else if (ACTION_GET_PROVIDER.equals(request.getAction())) {
            return getProvider(request);
        } else if (ACTION_GET_AVAILABLE_PLATFORMS.equals(request.getAction())) {
            invokeGetAvailablePlatforms(request);
        }
        return Response.SUCCESS;
    }

    protected Response getProvider(Request request) {
        return new Response("");
    }

    private void invokeShareTo(final Request request) throws JSONException {
        final Callback callback = request.getCallback();
        final String pkg = request.getHapEngine().getPackage();
        if (TextUtils.isEmpty(request.getRawParams())) {
            callback.callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }

        JSONObject params = new JSONObject(request.getRawParams());

        if (params == null || params.length() == 0) {
            callback.callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "params is empty"));
            return;
        }

        List<Platform> platforms = parsePlatforms(params);

        if (platforms == null) {
            platforms = new ArrayList<>();
        }
        if (platforms.isEmpty()) {
            Collections.addAll(platforms, mDefaultPlatforms);
        }

        final NativeInterface nativeInterface = request.getNativeInterface();
        Activity activity = nativeInterface.getActivity();
        mShareAction = new ShareAction(activity, this);

        final LifecycleListener lifecycleListener = new LifecycleListener() {
            @Override
            public void onDestroy() {
                super.onDestroy();
                nativeInterface.removeLifecycleListener(this);
                mShareAction.release();
            }

            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                super.onActivityResult(requestCode, resultCode, data);
                mShareAction.onActivityResult(requestCode, resultCode, data);
            }
        };
        nativeInterface.addLifecycleListener(lifecycleListener);

        ShareListener listener = new ShareListener() {
            @Override
            public void onStart(Platform media) {
            }

            @Override
            public void onResult(Platform media) {
                callback.callback(new Response(media.toString()));
                nativeInterface.removeLifecycleListener(lifecycleListener);
            }

            @Override
            public void onError(Platform media, String message) {
                callback.callback(new Response(Response.CODE_GENERIC_ERROR, message));
                nativeInterface.removeLifecycleListener(lifecycleListener);
            }

            @Override
            public void onCancel(Platform media) {
                callback.callback(Response.CANCEL);
                nativeInterface.removeLifecycleListener(lifecycleListener);
            }
        };
        try {
            ShareContent content = null;
            isMenubar = (null != params && params.has(SysOpProvider.PARAM_MENUBAR_KEY)
                            && params.optBoolean(SysOpProvider.PARAM_MENUBAR_KEY));
            if (isMenubar) {
                content = parseShareParams(request, params)
                        .setAppName(getAppName(request))
                        .setPackageName(getPackageName(request));
            } else {
                content = parseShareParams(request, params)
                        .setAppName(getAppName(request))
                        .setPackageName(getPackageName(request));
            }
            mShareAction.setCallback(listener)
                    .setShareContent(content)
                    .setDialogTitle(params.optString(PARAM_DIALOG_TITLE))
                    .setDisplayList(platforms);
            if (!content.getIsImgOnline()) {
                mShareAction.share();
            } else {
                String imgPath = params.optString(PARAM_IMAGE_PATH);
                if (UriUtils.isWebUri(imgPath)) {
                    getShareImg(imgPath, request);
                } else {
                    request.getCallback().callback(new Response(Response.CODE_GENERIC_ERROR,
                            "illegal image path"));
                }
            }
        } catch (Exception e) {
            callback.callback(getExceptionResponse(request, e));
        }
    }

    private void invokeGetAvailablePlatforms(Request request) throws JSONException {
        ShareAction shareAction = new ShareAction(request.getNativeInterface().getActivity(), this);
        ShareContent content = new ShareContent();
        List<Platform> platforms =
                shareAction.getAvailablePlatforms(Arrays.asList(mDefaultPlatforms), content);

        JSONArray jsonArray = new JSONArray();
        for (Platform platform : platforms) {
            jsonArray.put(platform.name());
        }
        JSONObject result = new JSONObject();
        result.put(RESULT_PLATFORMS, jsonArray);
        request.getCallback().callback(new Response(result));
    }

    protected String getAppName(Request request) {
        Context context = request.getNativeInterface().getActivity();
        return context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString();
    }

    protected String getPackageName(Request request) {
        return request.getNativeInterface().getActivity().getPackageName();
    }

    protected AbsShareApi createShareAPI(Activity activity, ShareContent shareContent,
                                         Platform platform) {
        return HandleFactory.createShareAPI(activity, shareContent, platform);
    }

    private ShareContent parseShareParams(final Request request,
                                          JSONObject jsonObject) {
        final String imagePath = jsonObject.optString(PARAM_IMAGE_PATH);
        Uri imageUri = null;
        boolean mIsImgOnline = false;
        if (!TextUtils.isEmpty(imagePath)) {
            ApplicationContext appContext = request.getApplicationContext();
            imageUri = getImageUri(appContext, imagePath);
            if (imageUri == null) {
                mIsImgOnline = true;
            }
        }

        return new ShareContent().setShareType(jsonObject.optInt(PARAM_SHARE_TYPE))
                .setTitle(jsonObject.optString(PARAM_TITLE))
                .setSummary(jsonObject.optString(PARAM_SUMMARY))
                .setTargetUrl(jsonObject.optString(PARAM_TARGET_URL))
                .setMediaUrl(jsonObject.optString((PARAM_MEDIA_URL)))
                .setImageUri(imageUri)
                .setIsImgOnline(mIsImgOnline);
    }

    private Uri getImageUri(ApplicationContext applicationContext, String imagePath) {
        if (TextUtils.isEmpty(imagePath)
                || !InternalUriUtils.isValidUri(imagePath)) {
            return null;
        }
        Uri imageUri = applicationContext.getUnderlyingUri(imagePath);
        return MediaUtils.createExternalStorageUri(applicationContext.getContext(),
                applicationContext.getPackage(), imageUri);
    }

    private List<Platform> parsePlatforms(JSONObject json) throws JSONException {
        JSONArray jsonArray = json.optJSONArray((PARAM_PLATFORMS));
        if (jsonArray == null) {
            return null;
        }
        List<Platform> platforms = new ArrayList<>();
        for (int i = 0, l = jsonArray.length(); i < l; i++) {
            Platform platform = Platform.convertToEmun(jsonArray.optString(i));
            if (platform != null) {
                platforms.add(platform);
            }
        }
        return platforms;
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private Uri saveImageAndParseUri(Bitmap bitmap, String imagePath, File shareImageTempDir) {
        if (bitmap == null || shareImageTempDir == null) {
            return null;
        }
        final String sdPath = shareImageTempDir.getPath() + "/" + imagePath.hashCode() + ".jpg";
        File doc = new File(sdPath);
        try {
            FileOutputStream fos = new FileOutputStream(doc);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        File f = new File(sdPath);
        if (f.exists()) {
            return Uri.fromFile(f);
        }
        return null;
    }

    private void getShareImg(final String imagePath, final Request request) {
        ImageRequestBuilder requestBuilder =
                ImageRequestBuilder.newBuilderWithSource(Uri.parse(imagePath));
        ImageRequest imageRequest = requestBuilder.build();
        DataSource<CloseableReference<CloseableImage>> dataSource =
                ImagePipelineFactory.getInstance().getImagePipeline().fetchDecodedImage(imageRequest, null);
        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onNewResultImpl(final Bitmap bitmap) {
                ApplicationContext appContext = request.getApplicationContext();
                final File shareImageTempDir = MediaUtils.getImageTempDir(appContext.getContext(),
                        appContext.getPackage());
                if (bitmap != null && !bitmap.isRecycled()) {
                    final Bitmap resultBitmap = bitmap.copy(bitmap.getConfig(), bitmap.isMutable());
                    Executors.io().execute(() -> {
                        if (resultBitmap != null && !resultBitmap.isRecycled()) {
                            Uri uri = saveImageAndParseUri(resultBitmap, imagePath,
                                    shareImageTempDir);
                            if (uri != null) {
                                mHandler.removeCallbacksAndMessages(null);
                                mHandler.obtainMessage(MSG_ONLINE_SHARE, uri).sendToTarget();
                            } else if (isMenubar) {
                                startDefaultImgShare(appContext);
                            } else {
                                request.getCallback().callback(new Response(Response.CODE_GENERIC_ERROR,
                                        "unknown error : online image uri null"));
                            }
                        } else if (isMenubar) {
                            startDefaultImgShare(appContext);
                        }
                    });
                } else if (isMenubar) {
                    startDefaultImgShare(appContext);
                }
            }

            @Override
            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                if (isMenubar) {
                    startDefaultImgShare(request.getApplicationContext());
                } else {
                    request.getCallback().callback(new Response(Response.CODE_GENERIC_ERROR,
                            "load online image fail"));
                }
            }
        }, UiThreadImmediateExecutorService.getInstance());
    }

    private void startDefaultImgShare(ApplicationContext appContext) {
        if (appContext != null) {
            AppInfo appInfo =
                    CacheStorage.getInstance(appContext.getContext()).getCache(appContext.getPackage()).getAppInfo();
            if (appInfo != null) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler.obtainMessage(MSG_ONLINE_SHARE, getImageUri(appContext,
                        appInfo.getIcon())).sendToTarget();
            }
        }
    }

    interface ImageCallBack<T> {
        void onSuccess(T result);

        void onFail();
    }

    @Override
    public boolean isBuiltInExtension() {
        return true;
    }
}
