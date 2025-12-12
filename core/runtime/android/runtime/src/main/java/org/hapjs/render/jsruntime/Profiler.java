/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

import android.util.Log;
import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.executors.ScheduledExecutor;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.Runtime;
import org.hapjs.system.SysOpProvider;

public class Profiler extends V8Object {

    private static final String TAG = "Profiler";

    private static Map<String, Long> mMsgMap = new HashMap<>();

    private static boolean sIsAllowProfiler;
    private static boolean sIsCached;

    private JsThread mJsThread;
    private long mThreadId;
    private IJavaNative mNative;

    public final JavaCallback isEnabled =
            (v8Object, v8Array) -> {
                if (!sIsCached) {
                    try {
                        sIsAllowProfiler = mJsThread.postAndWait(() -> mNative.profilerIsEnabled());
                    } catch (Exception e) {
                        Log.e(TAG, "failed to check profilerIsEnabled");
                    }
                    sIsCached = true;
                }
                return sIsAllowProfiler;
            };
    public final JavaVoidCallback saveProfilerData =
            (v8Object, args) -> {
                if (!sIsAllowProfiler) {
                    Log.d(TAG, "saveProfilerData : not allow profiler");
                    return;
                }
                String profilerData = toMsg(args);
                mJsThread.post(() -> mNative.profilerSaveProfilerData(profilerData));
            };
    public final JavaVoidCallback record =
            (v8Object, args) -> {
                if (!sIsAllowProfiler) {
                    Log.d(TAG, "record: not allow profiler");
                    return;
                }
                String msg = toMsg(args);
                mJsThread.post(() -> mNative.profilerRecord(msg, mThreadId));
            };
    public final JavaVoidCallback time =
            (v8Object, args) -> {
                if (!sIsAllowProfiler) {
                    Log.d(TAG, "time: not allow profiler");
                    return;
                }
                long currentTime = System.nanoTime();
                String msg = toMsg(args);
                mJsThread.post(
                        () -> {
                            String key = mThreadId + "_" + msg;
                            mMsgMap.put(key, currentTime);
                        });
            };

    public final JavaVoidCallback timeEnd =
            (v8Object, args) -> {
                if (!sIsAllowProfiler) {
                    Log.d(TAG, "timeEnd: not allow profiler");
                    return;
                }
                long currentTime = System.nanoTime();
                String msg = toMsg(args);
                mJsThread.post(
                        () -> {
                            String key = mThreadId + "_" + msg;
                            String content;
                            if (mMsgMap.containsKey(key)) {
                                content = getContent("timeEnd", msg, mThreadId) + ": "
                                        + getCost(currentTime, key);
                            } else {
                                content = getContent("timeEnd", msg, mThreadId)
                                        + ": don't match the time flag";
                            }
                            mNative.profilerTimeEnd(content);
                        });
            };

    public Profiler(V8 v8, long threadId, IJavaNative javaNative, JsThread jsThread) {
        super(v8);
        mThreadId = threadId;
        mNative = javaNative;
        mJsThread = jsThread;
    }

    private static String getCost(long time, String key) {
        Long value = mMsgMap.remove(key);
        if (value != null) {
            long cost = time - value;
            return String.format(Locale.getDefault(), "%.1f", cost / 1000000f) + "ms";
        }
        return "";
    }

    private static String getContent(String prefix, String msg, long threadId) {
        return getCurrentTime() + " " + threadId + " " + prefix + ": " + msg;
    }

    private static String getCurrentTime() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        return sdf.format(date);
    }

    private static String toMsg(V8Array args) {
        if (args == null || args.length() == 0) {
            return "";
        }

        Object first = args.get(0);
        try {
            int length = args.length();
            if (length == 1) {
                return first.toString();
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < length; i++) {
                    Object obj = args.get(i);
                    try {
                        sb.append(obj);
                        if (i != length - 1) {
                            sb.append(' ');
                        }
                    } finally {
                        tryRelease(obj);
                    }
                }
                return sb.toString();
            }
        } finally {
            tryRelease(first);
        }
    }

    private static void tryRelease(Object obj) {
        if (obj instanceof V8Value) {
            JsUtils.release((V8Value) obj);
        }
    }
}
