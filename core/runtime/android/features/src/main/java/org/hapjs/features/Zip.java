/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.storage.file.InternalUriUtils;
import org.hapjs.bridge.storage.file.Resource;
import org.hapjs.cache.utils.ZipUtils;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;

@FeatureExtensionAnnotation(
        name = Zip.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Zip.ACTION_DECOMPRESS, mode = FeatureExtension.Mode.ASYNC)})
public class Zip extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.zip";
    protected static final String ACTION_DECOMPRESS = "decompress";
    private static final String TAG = "Zip";
    private static final String PARAMS_SRC_URI = "srcUri";
    private static final String PARAMS_DST_URI = "dstUri";

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_DECOMPRESS.equals(action)) {
            doDecompress(request);
        }
        return Response.SUCCESS;
    }

    private void doDecompress(Request request) throws SerializeException {
        SerializeObject params = request.getSerializeParams();
        String srcUri = params.optString(PARAMS_SRC_URI);
        if (TextUtils.isEmpty(srcUri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_SRC_URI + " not define"));
            return;
        }
        if (InternalUriUtils.isTmpUri(srcUri)) {
            request
                    .getCallback()
                    .callback(
                            new Response(
                                    Response.CODE_ILLEGAL_ARGUMENT,
                                    PARAMS_SRC_URI + " must not be a temp uri"));
            return;
        }
        String destUri = params.optString(PARAMS_DST_URI);
        if (TextUtils.isEmpty(destUri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_DST_URI + " not define"));
            return;
        }

        ApplicationContext applicationContext = request.getApplicationContext();
        String reason;
        File srcFile = applicationContext.getUnderlyingFile(srcUri);
        if (srcFile == null) {
            reason = "can not resolve srcUri " + srcUri;
            Log.w(TAG, reason);
            request.getCallback().callback(new Response(Response.CODE_IO_ERROR, reason));
            return;
        }

        if (!srcFile.exists()) {
            reason = "srcUri " + srcUri + " is not exists";
            Log.w(TAG, reason);
            request.getCallback().callback(new Response(Response.CODE_IO_ERROR, reason));
            return;
        }

        if (!srcFile.isFile()) {
            reason = "srcUri " + srcUri + " is not a normal file";
            Log.w(TAG, reason);
            request.getCallback().callback(new Response(Response.CODE_IO_ERROR, reason));
            return;
        }

        Resource dstRes = applicationContext.getResourceFactory().create(destUri);
        if (dstRes == null || !dstRes.canWrite()) {
            reason = "dstUri " + destUri + " is not writable";
            Log.w(TAG, reason);
            request.getCallback().callback(new Response(Response.CODE_IO_ERROR, reason));
            return;
        }

        File dstDir = dstRes.getUnderlyingFile();
        if (dstDir == null) {
            reason = "can not resolve dstUri" + destUri;
            Log.w(TAG, reason);
            request.getCallback().callback(new Response(Response.CODE_IO_ERROR, reason));
            return;
        }

        if (dstDir.exists() && !dstDir.isDirectory()) {
            reason = "dstUri " + destUri + " is not a directory";
            Log.i(TAG, reason);
            request.getCallback().callback(new Response(Response.CODE_IO_ERROR, reason));
            return;
        }

        boolean success = ZipUtils.unzip(srcFile, dstDir);
        if (!success) {
            reason = "decompress file failed";
            Log.w(TAG, reason);
            request.getCallback().callback(new Response(Response.CODE_IO_ERROR, reason));
        } else {
            request.getCallback().callback(Response.SUCCESS);
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}
