/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONException;
import org.json.JSONObject;

public class InternalConfig {
    private static final String TAG = "InternalConfig";

    private static final String INTERNAL_CONFIG_NAME = "hap/card_internal.json";

    private static final String KEY_CARD_SERVICE = "CardService";
    private static final String KEY_CARD_VIEW = "CardView";

    private static volatile InternalConfig sInstance;
    private JSONObject mConfig;

    private InternalConfig(Context context) {
        init(context);
    }

    public static InternalConfig getInstance(Context context) {
        if (sInstance == null) {
            synchronized (InternalConfig.class) {
                if (sInstance == null) {
                    sInstance = new InternalConfig(context);
                }
            }
        }
        return sInstance;
    }

    private void init(Context context) {
        try {
            InputStream input = context.getAssets().open(INTERNAL_CONFIG_NAME);
            String config = FileUtils.readStreamAsString(input, true);
            if (!TextUtils.isEmpty(config)) {
                mConfig = new JSONObject(config);
            }
        } catch (IOException | JSONException e) {
            Log.w(TAG, "Fail to load " + INTERNAL_CONFIG_NAME, e);
        }
    }

    public String getCardServiceName() {
        if (mConfig != null) {
            return mConfig.optString(KEY_CARD_SERVICE);
        }
        return null;
    }

    public String getCardViewName() {
        if (mConfig != null) {
            return mConfig.optString(KEY_CARD_VIEW);
        }
        return null;
    }
}
