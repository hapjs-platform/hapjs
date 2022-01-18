/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;

import org.hapjs.common.executors.Executor;
import org.hapjs.common.executors.Executors;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Clipboard.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Clipboard.ACTION_SET, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Clipboard.ACTION_GET, mode = FeatureExtension.Mode.ASYNC)
        })
public class Clipboard extends FeatureExtension {

    public static final String FEATURE_NAME = "system.clipboard";
    public static final String ACTION_SET = "set";
    public static final String ACTION_GET = "get";

    protected static final String PARAM_KEY_TEXT = "text";

    protected volatile ClipboardManager mClipboard;

    @Override
    protected Response invokeInner(final Request request) throws Exception {
        if (mClipboard == null) {
            // android 7上ClipboardManager需要在ui线程获取
            Executors.ui().execute(() -> {
                if (mClipboard == null) {
                    Context context = request.getNativeInterface().getActivity();
                    mClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                }

                Executor executor = getExecutor(request);
                executor = executor == null ? Executors.io() : executor;
                executor.execute(() -> {
                    try {
                        invokeInner(request);
                    } catch (Exception e) {
                        request.getCallback().callback(getExceptionResponse(request, e));
                    }
                });
            });
        } else {
            String action = request.getAction();
            if (ACTION_SET.equals(action)) {
                set(request);
            } else {
                get(request);
            }
            recordClipboardFeature(request);
        }
        return Response.SUCCESS;
    }

    protected void set(final Request request) throws JSONException {
        JSONObject params = new JSONObject(request.getRawParams());
        String text = params.getString(PARAM_KEY_TEXT);
        ClipData clip = ClipData.newPlainText("text", text);
        mClipboard.setPrimaryClip(clip);
        request.getCallback().callback(Response.SUCCESS);
    }

    protected void get(final Request request) throws JSONException {
        String text = null;
        ClipData clip = mClipboard.getPrimaryClip();
        if (clip != null) {
            ClipData.Item item = clip.getItemAt(0);
            if (item != null) {
                CharSequence textSeq = item.getText();
                if (textSeq != null) {
                    text = textSeq.toString();
                }
            }
        }
        JSONObject result = new JSONObject();
        result.put(PARAM_KEY_TEXT, text);
        request.getCallback().callback(new Response(result));
    }

    protected void recordClipboardFeature(Request request) {
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}
