/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debug;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import org.hapjs.debug.log.DebuggerLogUtil;
import android.text.TextUtils;

import org.hapjs.analyzer.AnalyzerStatisticsManager;
import org.hapjs.analyzer.Analyzer;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.render.RootView;

public class DebugUtils {
    private static final String TAG = "DebugUtils";

    private static final String DEBUG_PARAMS = "__DEBUG_PARAMS__";
    private static final String DEBUG_PARAM_WAIT_DEVTOOLS = "DEBUG_WAIT_DEVTOOLS";
    private static final String DEBUG_PARAM_PLATFORM_VERSION_CODE = "DEBUG_PLATFORM_VERSION_CODE";

    public static String appendDebugParams(
            String url,
            String server,
            String pkg,
            String serialNumber,
            boolean useADB,
            int platformVersionCode,
            boolean waitDevTools,
            String debugTarget,
            String traceId) {
        String debugParams =
                new Uri.Builder()
                        .appendQueryParameter(DebuggerLoader.Params.PARAM_KEY_DEBUG_PACKAGE, pkg)
                        .appendQueryParameter(DebuggerLoader.Params.PARAM_KEY_DEBUG_SERIAL_NUMBER,
                                serialNumber)
                        .appendQueryParameter(DebuggerLoader.Params.PARAM_KEY_DEBUG_SERVER, server)
                        .appendQueryParameter(
                                DebuggerLoader.Params.PARAM_KEY_DEBUG_USE_ADB,
                                String.valueOf(useADB))
                        .appendQueryParameter(DebuggerLoader.Params.PARAM_KEY_DEBUG_TARGET,
                                debugTarget)
                        .appendQueryParameter(DEBUG_PARAM_WAIT_DEVTOOLS,
                                String.valueOf(waitDevTools))
                        .appendQueryParameter(
                                DEBUG_PARAM_PLATFORM_VERSION_CODE,
                                String.valueOf(platformVersionCode))
                        .appendQueryParameter(DebuggerLoader.Params.PARAM_KEY_DEBUG_TRACE_ID,
                                traceId)
                        .toString();
        return Uri.parse(url).buildUpon().appendQueryParameter(DEBUG_PARAMS, debugParams)
                .toString();
    }

    public static String appendAnalyzerParam(String pkg, String path, boolean useAnalyzer) {
        if (!useAnalyzer) {
            return path;
        }
        if (TextUtils.isEmpty(path) || "/".equals(path)) {
            path = new HybridRequest.HapRequest.Builder().pkg(pkg).uri(path).build().getUri();
        }
        return Uri.parse(path)
                .buildUpon()
                .appendQueryParameter(Analyzer.USE_ANALYZER, String.valueOf(true))
                .toString();
    }

    public static String trySetupDebugger(RootView rootView, String url) {
        Uri uri = Uri.parse(url);
        Uri.Builder builder =
                new Uri.Builder()
                        .scheme(uri.getScheme())
                        .authority(uri.getAuthority())
                        .path(uri.getPath())
                        .fragment(uri.getFragment());

        for (String paramName : uri.getQueryParameterNames()) {
            if (!DEBUG_PARAMS.equals(paramName)) {
                builder.appendQueryParameter(paramName, uri.getQueryParameter(paramName));
            } else {
                Context context = rootView.getContext();
                Uri debugParams = Uri.parse(uri.getQueryParameter(paramName));
                DebuggerLoader.Params params =
                        new DebuggerLoader.Params.Builder()
                                .debugServer(
                                        debugParams.getQueryParameter(
                                                DebuggerLoader.Params.PARAM_KEY_DEBUG_SERVER))
                                .debugPackage(
                                        debugParams.getQueryParameter(
                                                DebuggerLoader.Params.PARAM_KEY_DEBUG_PACKAGE))
                                .serialNumber(
                                        debugParams.getQueryParameter(
                                                DebuggerLoader.Params.PARAM_KEY_DEBUG_SERIAL_NUMBER))
                                .useADB(
                                        debugParams.getBooleanQueryParameter(
                                                DebuggerLoader.Params.PARAM_KEY_DEBUG_USE_ADB,
                                                false))
                                .debugTarget(
                                        debugParams.getQueryParameter(
                                                DebuggerLoader.Params.PARAM_KEY_DEBUG_TARGET))
                                .traceId(
                                        debugParams.getQueryParameter(
                                                DebuggerLoader.Params.PARAM_KEY_DEBUG_TRACE_ID))
                                .build();
                String platformVersionString =
                        debugParams.getQueryParameter(DEBUG_PARAM_PLATFORM_VERSION_CODE);
                int platformVersionCode = 1000;
                try {
                    platformVersionCode = Integer.parseInt(platformVersionString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "trySetupDebugger: ", e);
                }
                boolean waitDevTools =
                        Boolean.parseBoolean(
                                debugParams.getQueryParameter(DEBUG_PARAM_WAIT_DEVTOOLS));
                rootView.setWaitDevTools(waitDevTools);
                DebuggerLogUtil.logMessage("ENGINE_INIT_DEBUG_ARGS");
                DebuggerLoader.attachDebugger(context, platformVersionCode, params);
            }
        }
        Analyzer.get().init(rootView, url);
        AnalyzerStatisticsManager.getInstance().recordAnalyzerEvent(AnalyzerStatisticsManager.EVENT_DEBUG_APP);
        return builder.build().toString();
    }

    public static void resetDebugger() {
        DebuggerLoader.resetDebugger(true);
    }
}
