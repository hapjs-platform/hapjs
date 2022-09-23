/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import java.util.Map;
import org.hapjs.launch.LauncherManager;
import org.hapjs.logging.LogHelper;
import org.hapjs.logging.Source;
import org.hapjs.runtime.RuntimeActivity;
import org.hapjs.utils.ActivityUtils;
import org.hapjs.utils.ShortcutParamsHelper;

public class DispatcherActivity extends Activity {

    private static final String TAG = "DispatcherActivity";
    private static final String PARAM_SOURCE = "__SRC__";
    private static final String PARAM_SOURCE_SCENE = "__SS__";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Intent intent = getIntent();
            setSource(intent);
            appendShortcutParamsIfNeeded(intent);
            dispatch(intent);
        } catch (Exception e) {
            Log.e(TAG, "onCreate parse intent get error", e);
        }

        super.onCreate(savedInstanceState);

        try {
            finish();
        } catch (Exception e) {
            Log.e(TAG, "finish error", e);
        }
    }

    protected void dispatch(Intent intent) {
        intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        LauncherManager.launch(this, intent);
    }

    private void setSource(Intent intent) {
        Source source = acquireSource(intent);
        appendSourceParams(intent, source);

        intent.putExtra(LauncherActivity.EXTRA_SOURCE, source.toJson().toString());
        String pkg = intent.getStringExtra(LauncherActivity.EXTRA_APP);
        LogHelper.addPackage(pkg, source);
    }

    private Source acquireSource(Intent intent) {
        Source source;
        // 如果在 intent 中携带了 __SRC__ 参数，则优先使用 intent.getData 中的 source
        source = getSourceFromSrcParam(intent);
        if (source != null) {
            return source;
        }
        // 没有获取到 SRC 中的 source ，尝试获取 extra_source
        source = getExtraSource(intent);
        if (source != null) {
            return source;
        }

        return createSource();
    }

    private Source getSourceFromSrcParam(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            String sourceParam = uri.getQueryParameter(PARAM_SOURCE);
            if (!TextUtils.isEmpty(sourceParam)) {
                return Source.fromJson(sourceParam);
            }
        }
        return null;
    }

    private Source getExtraSource(Intent intent) {
        String launchFrom = intent.getStringExtra(LauncherActivity.EXTRA_SOURCE);
        if (TextUtils.isEmpty(launchFrom)) {
            launchFrom = intent.getStringExtra(LauncherActivity.EXTRA_LAUNCH_FROM);
        }
        return Source.fromJson(launchFrom);
    }

    private Source createSource() {
        Source source = new Source();
        if (isHomePackage(getLaunchPackageName())) { // shortcut，兼容以前版本
            source.putExtra(Source.EXTRA_SCENE, Source.SHORTCUT_SCENE_DIALOG);
        }
        return source;
    }

    private void appendSourceParams(Intent intent, Source source) {
        appendSourceScene(intent, source);

        String launchPackage = getLaunchPackageName();
        source.putInternal(Source.INTERNAL_CHANNEL, Source.CHANNEL_INTENT);
        if (TextUtils.isEmpty(source.getType())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && getPackageName().equals(launchPackage)
                    && source.getExtra().containsKey(Source.EXTRA_ORIGINAL)) {
                // 兼容Android O上创建shortcut时没有带type的版本
                source.setType(Source.TYPE_SHORTCUT);
            } else {
                String type =
                        isHomePackage(launchPackage) ? Source.TYPE_SHORTCUT : Source.TYPE_OTHER;
                source.setType(type);
            }
        }
        if (TextUtils.isEmpty(source.getPackageName())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && getPackageName().equals(launchPackage)
                    && Source.TYPE_SHORTCUT.equals(source.getType())) {
                // Android O创建的icon，点击启动之后通过ActivityUtils.getCallingPackage拿到的包名为icon的创建者，而不是桌面
                source.setPackageName(ActivityUtils.getHomePackage(this));
            } else {
                source.setPackageName(launchPackage);
            }
        }
    }

    private void appendSourceScene(Intent intent, Source source) {
        Map<String, String> sourceExtra = source.getExtra();
        if (TextUtils.isEmpty(sourceExtra.get(Source.EXTRA_SCENE))) {
            Uri uri = intent.getData();
            if (uri != null) {
                String sourceScene = uri.getQueryParameter(PARAM_SOURCE_SCENE);
                if (!TextUtils.isEmpty(sourceScene)) {
                    source.putExtra(Source.EXTRA_SCENE, sourceScene);
                }
            }
        }
    }

    private String getLaunchPackageName() {
        String launchPackage = ActivityUtils.getCallingPackage(this);
        if (launchPackage == null) {
            launchPackage = "Unknown";
        }
        return launchPackage;
    }

    private boolean isHomePackage(String pkgName) {
        return ActivityUtils.isHomePackage(this, pkgName);
    }

    private void appendShortcutParamsIfNeeded(Intent intent) {
        Source source = Source.fromJson(intent.getStringExtra(LauncherActivity.EXTRA_SOURCE));
        if (source != null && ActivityUtils.isHomePackage(this, source.getPackageName())) {
            String pkg = intent.getStringExtra(RuntimeActivity.EXTRA_APP);
            String path = intent.getStringExtra(RuntimeActivity.EXTRA_PATH);
            String params = ShortcutParamsHelper.queryShortcutParams(this, pkg, path);
            if (!TextUtils.isEmpty(params)) {
                path = ShortcutParamsHelper.appendParams(path, params);
                intent.putExtra(RuntimeActivity.EXTRA_PATH, path);
            }
        }
    }
}
