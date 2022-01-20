/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.module;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.ModuleExtensionAnnotation;
import org.hapjs.common.utils.IconUtils;
import org.hapjs.common.utils.QRCodeUtils;
import org.hapjs.common.utils.RouterUtils;
import org.hapjs.logging.Source;
import org.hapjs.model.AppInfo;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONException;
import org.json.JSONObject;

@ModuleExtensionAnnotation(
        name = ApplicationModule.NAME,
        actions = {
                @ActionAnnotation(name = ApplicationModule.ACTION_GET_INFO, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ApplicationModule.ACTION_EXIT, mode = Extension.Mode.SYNC),
                @ActionAnnotation(
                        name = ApplicationModule.ACTION_CREATE_QUICK_APP_QR_CODE,
                        mode = Extension.Mode.ASYNC)
        })
public class ApplicationModule extends ModuleExtension {
    protected static final String NAME = "system.app";
    protected static final String ACTION_GET_INFO = "getInfo";
    protected static final String ACTION_EXIT = "exit";
    protected static final String ACTION_CREATE_QUICK_APP_QR_CODE = "createQuickAppQRCode";

    private static final String RESULT_NAME = "name";
    private static final String RESULT_ICON = "icon";
    private static final String RESULT_PACKAGENAME = "packageName";
    private static final String RESULT_VERSION_NAME = "versionName";
    private static final String RESULT_VERSION_CODE = "versionCode";
    private static final String RESULT_LOG_LEVEL = "logLevel";
    private static final String RESULT_SOURCE = "source";
    private static final String RESULT_URI = "uri";

    private static final String KEY_PATH = "path";
    private static final String URL_QUICK_APP = "https://hapjs.org/app/";

    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_WEIGHT = 300;

    private AppInfo mAppInfo;
    private PageManager mPageManager;
    private Context mContext;

    @Override
    public void attach(RootView rootView, PageManager pageManager, AppInfo appInfo) {
        mAppInfo = appInfo;
        mPageManager = pageManager;
        mContext = rootView.getContext(); // must use an activity context
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_GET_INFO.equals(action)) {
            return getInfo();
        } else if (ACTION_EXIT.equals(action)) {
            return exit();
        } else if (ACTION_CREATE_QUICK_APP_QR_CODE.equals(action)) {
            createQuickAppQRCode(request);
            return Response.SUCCESS;
        } else {
            return Response.NO_ACTION;
        }
    }

    private void createQuickAppQRCode(Request request) throws SerializeException, JSONException {
        Activity activity = request.getNativeInterface().getActivity();
        if (activity == null) {
            request.getCallback().callback(Response.ERROR);
            return;
        }
        SerializeObject reader = request.getSerializeParams();
        String path = reader.optString(KEY_PATH);
        String content = URL_QUICK_APP + request.getApplicationContext().getPackage() + path;
        Bitmap icon = IconUtils.getIconBitmap(activity, request.getApplicationContext().getIcon());
        Bitmap bitmap =
                QRCodeUtils
                        .createQRCodeBitmapWithLogo(content, QR_CODE_WIDTH, QR_CODE_WEIGHT, icon);
        Uri uri =
                QRCodeUtils.saveBitmapToFile(request.getApplicationContext().getCacheDir(), bitmap);
        String internalUri = request.getApplicationContext().getInternalUri(uri);
        if (internalUri != null) {
            JSONObject result = new JSONObject();
            result.put(RESULT_URI, internalUri);
            request.getCallback().callback(new Response(result));
        } else {
            request.getCallback().callback(Response.ERROR);
        }
    }

    private Response getInfo() throws JSONException {
        JSONObject info = new JSONObject();
        info.put(RESULT_NAME, mAppInfo.getName());
        info.put(RESULT_ICON, mAppInfo.getIcon());
        info.put(RESULT_PACKAGENAME, mAppInfo.getPackage());
        info.put(RESULT_VERSION_NAME, mAppInfo.getVersionName());
        info.put(RESULT_VERSION_CODE, mAppInfo.getVersionCode());
        if (mAppInfo.getConfigInfo() != null) {
            info.put(RESULT_LOG_LEVEL, mAppInfo.getConfigInfo().getString("logLevel"));
        }
        Source source = Source.currentSource();
        if (source != null) {
            info.put(RESULT_SOURCE, source.toSafeJson());
        }
        return new Response(info);
    }

    private Response exit() {
        RouterUtils.exit(mContext, mPageManager);
        return Response.SUCCESS;
    }
}
