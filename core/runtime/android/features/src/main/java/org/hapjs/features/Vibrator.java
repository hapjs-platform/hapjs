/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;

@FeatureExtensionAnnotation(
        name = Vibrator.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Vibrator.ACTION_VIBRATE, mode = FeatureExtension.Mode.SYNC)
        })
public class Vibrator extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.vibrator";
    protected static final String ACTION_VIBRATE = "vibrate";

    private static final String PARAMS_KEY_MODE = "mode";

    private static final String MODE_SHORT = "short";
    private static final String MODE_LONG = "long";

    private static final long LENGTH_SHORT = 35;
    private static final long LENGTH_LONG = 1000;

    @Override
    protected Response invokeInner(Request request) throws Exception {
        return vibrate(request);
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private Response vibrate(Request request) throws SerializeException {
        String mode = null;

        SerializeObject params = request.getSerializeParams();
        if (params != null) {
            mode = params.optString(PARAMS_KEY_MODE);
            if (!TextUtils.isEmpty(mode) && !MODE_SHORT.equals(mode) && !MODE_LONG.equals(mode)) {
                return new Response(Response.CODE_ILLEGAL_ARGUMENT, "Unsupported mode");
            }
        }

        Activity activity = request.getNativeInterface().getActivity();
        android.os.Vibrator vibrator =
                (android.os.Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        if (MODE_SHORT.equals(mode)) {
            vibrator.vibrate(LENGTH_SHORT);
        } else {
            vibrator.vibrate(LENGTH_LONG);
        }
        return Response.SUCCESS;
    }
}
