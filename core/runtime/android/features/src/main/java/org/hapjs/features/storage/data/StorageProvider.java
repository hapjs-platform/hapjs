/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.data;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import org.hapjs.AbstractContentProvider;
import org.hapjs.features.storage.data.internal.IStorage;
import org.hapjs.features.storage.data.internal.StorageFactory;
import org.hapjs.runtime.HapEngine;

public class StorageProvider extends AbstractContentProvider {
    public static final String METHOD_GET = "get";
    public static final String METHOD_SET = "set";
    public static final String METHOD_ENTRIES = "entries";
    public static final String METHOD_KEY = "key";
    public static final String METHOD_LENGTH = "length";
    public static final String METHOD_DELETE = "delete";
    public static final String METHOD_CLEAR = "clear";
    public static final String PARAM_KEY = "key";
    public static final String PARAM_VALUE = "value";
    public static final String PARAM_INDEX = "index";
    protected static final String TAG = "StorageProvider";

    public static Uri getUri(String authority) {
        return new Uri.Builder().scheme("content").authority(authority).build();
    }

    protected static String getPackageName(@NonNull Uri uri) {
        List<String> pathSegments = uri.getPathSegments();
        return pathSegments.get(0);
    }

    @Override
    public boolean onCreate() {
        if (getContext() == null) {
            Log.w(TAG, "onCreate: getContext() == null");
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    public Bundle doCall(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (!checkPermission(getContext())) {
            Log.i(TAG, "call fail: caller not permitted");
            return null;
        }
        if (TextUtils.isEmpty(method) || TextUtils.isEmpty(arg)) {
            Log.i(TAG, "call fail: no method or arg! ");
            return null;
        }
        IStorage storage =
                StorageFactory.getInstance()
                        .create(HapEngine.getInstance(arg).getApplicationContext());
        Bundle result = new Bundle();
        switch (method) {
            case METHOD_GET: {
                if (extras == null) {
                    return null;
                }
                String value = storage.get(extras.getString(PARAM_KEY));
                result.putString(METHOD_GET, value);
                break;
            }
            case METHOD_SET: {
                if (extras == null) {
                    return null;
                }
                String key = extras.getString(PARAM_KEY);
                String value = extras.getString(PARAM_VALUE);
                result.putBoolean(METHOD_SET, storage.set(key, value));
                break;
            }
            case METHOD_ENTRIES: {
                result.putSerializable(
                        METHOD_ENTRIES, ((LinkedHashMap<String, String>) storage.entries()));
                break;
            }
            case METHOD_KEY: {
                if (extras == null) {
                    return null;
                }
                int index = extras.getInt(PARAM_INDEX);
                result.putString(METHOD_KEY, storage.key(index));
                break;
            }
            case METHOD_LENGTH: {
                result.putInt(METHOD_LENGTH, storage.length());
                break;
            }
            case METHOD_DELETE: {
                if (extras == null) {
                    return null;
                }
                String key = extras.getString(PARAM_KEY);
                result.putBoolean(METHOD_DELETE, storage.delete(key));
                break;
            }
            case METHOD_CLEAR: {
                result.putBoolean(METHOD_CLEAR, storage.clear());
                break;
            }
            default:
                break;
        }

        return result;
    }

    protected boolean checkPermission(Context context) {
        return true;
    }
}
