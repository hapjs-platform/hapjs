/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage.file;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.common.utils.FileHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ResourceInfo {
    protected static final String TYPE_FILE = "file";
    protected static final String TYPE_DIR = "dir";
    private static final String TAG = "FileInfo";
    private static final String KEY_FILE_LIST = "fileList";
    private static final String KEY_URI = "uri";
    private static final String KEY_LENGTH = "length";
    private static final String KEY_LAST_MODIFIED_TIME = "lastModifiedTime";
    private static final String KEY_TYPE = "type";
    private static final String KEY_SUB_FIlES = "subFiles";
    private String mUri;
    private long mLength;
    private long mLastModifiedTime;
    private String mType;
    private List<ResourceInfo> mSubFileList;

    public ResourceInfo(
            String uri, long length, long lastModifiedTime, String type,
            List<ResourceInfo> subFiles) {
        mUri = uri;
        mLength = length;
        mLastModifiedTime = lastModifiedTime;
        mType = type;
        mSubFileList = subFiles;
    }

    public static JSONObject toJson(List<ResourceInfo> resourceInfos) {
        JSONArray jsonArray = new JSONArray();
        if (resourceInfos != null && resourceInfos.size() > 0) {
            for (ResourceInfo fileInfo : resourceInfos) {
                jsonArray.put(fileInfo.toJsonObject());
            }
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_FILE_LIST, jsonArray);
        } catch (JSONException e) {
            Log.w(TAG, "Convert to json failed!", e);
        }
        return jsonObject;
    }

    public static ResourceInfo create(String uri, File file) {
        return create(uri, file, false, null);
    }

    public static ResourceInfo create(
            String uri, File file, boolean recursive, ApplicationContext context) {
        String type;
        long length;
        if (file.isFile()) {
            type = TYPE_FILE;
            length = file.length();
        } else if (file.isDirectory()) {
            type = TYPE_DIR;
            length = 0;
        } else {
            return null;
        }
        List<ResourceInfo> list = null;
        if (recursive && file.isDirectory() && context != null) {
            File[] files = file.listFiles();
            if (files != null) {
                list = new ArrayList<>();
                for (File f : files) {
                    list.add(create(context.getInternalUri(f), f, recursive, context));
                }
            }
        }
        return new ResourceInfo(uri, length, file.lastModified(), type, list);
    }

    static ResourceInfo create(Context context, String uri, Uri underlyingUri) {
        Pair<Long, Long> fileInfo = FileHelper.getFileInfoFromContentUri(context, underlyingUri);
        if (fileInfo != null) {
            return new ResourceInfo(uri, fileInfo.first, fileInfo.second, TYPE_FILE, null);
        }
        return null;
    }

    protected static JSONArray toJsonArray(List<ResourceInfo> fileInfos) {
        if (fileInfos == null) {
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        for (ResourceInfo fileInfo : fileInfos) {
            jsonArray.put(fileInfo.toJsonObject());
        }
        return jsonArray;
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_URI, mUri);
            jsonObject.put(KEY_LENGTH, mLength);
            jsonObject.put(KEY_LAST_MODIFIED_TIME, mLastModifiedTime);
            jsonObject.put(KEY_TYPE, mType);
            jsonObject.put(KEY_SUB_FIlES, toJsonArray(mSubFileList));
        } catch (JSONException e) {
            Log.w(TAG, "Convert to json failed!", e);
        }
        return jsonObject;
    }
}
