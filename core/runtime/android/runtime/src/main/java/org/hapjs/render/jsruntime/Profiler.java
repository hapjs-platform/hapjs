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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    // 基于时间点和事件的 Profiler 文本文件
    private static final String FILE_NAME_TIMELINE = "profiler_log.txt";

    // 基于火焰图的 Profile 火焰图文件
    private static final String FILE_NAME_FLAME = "framework.cpuprofile";

    private static final String KEY_APP_START = "app_start";

    private static ScheduledExecutor sExecutor = Executors.createSingleThreadExecutor();
    private static Map<String, Long> mMsgMap = new HashMap<>();

    private static boolean sIsAllowProfiler;
    private static boolean sIsCached;
    public final JavaCallback isEnabled = (v8Object, v8Array) -> sIsAllowProfiler;
    public final JavaVoidCallback saveProfilerData =
            (v8Object, args) -> {
                if (!sIsAllowProfiler) {
                    Log.d(TAG, "saveProfilerData : not allow profiler");
                    return;
                }
                String profilerData = toMsg(args);
                sExecutor.execute(() -> writeFlameDataToFile(profilerData));
            };
    private long mThreadId;
    public final JavaVoidCallback record =
            (v8Object, args) -> {
                if (!sIsAllowProfiler) {
                    Log.d(TAG, "record: not allow profiler");
                    return;
                }
                String msg = toMsg(args);
                sExecutor.execute(() -> writeToFile(getContent("record", msg, mThreadId)));
            };
    public final JavaVoidCallback time =
            (v8Object, args) -> {
                if (!sIsAllowProfiler) {
                    Log.d(TAG, "time: not allow profiler");
                    return;
                }
                long currentTime = System.nanoTime();
                String msg = toMsg(args);
                sExecutor.execute(
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
                sExecutor.execute(
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
                            writeToFile(content);
                        });
            };

    public Profiler(V8 v8, long threadId) {
        super(v8);
        mThreadId = threadId;
    }

    public static void recordAppStart(long time) {
        mMsgMap.put(KEY_APP_START, time);
    }

    public static void recordFirstFrameRendered(long time, long threadId) {
        if (!sIsAllowProfiler) {
            Log.d(TAG, "recordFirstFrameRendered: not allow profiler");
            return;
        }
        sExecutor.execute(
                () -> {
                    if (mMsgMap.containsKey(KEY_APP_START)) {
                        writeToFile(getContent("first frame rendered", getCost(time, KEY_APP_START),
                                threadId));
                    }
                });
    }

    private static String getCost(long time, String key) {
        Long value = mMsgMap.remove(key);
        if (value != null) {
            long cost = time - value;
            return String.format(Locale.getDefault(), "%.1f", cost / 1000000f) + "ms";
        }
        return "";
    }

    private static void writeToFile(String content) {
        try {
            File logFile =
                    new File(
                            Runtime.getInstance().getContext().getExternalFilesDir(null),
                            FILE_NAME_TIMELINE);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            String finalContent = content + "\n";
            FileUtils.saveToFile(finalContent.getBytes(StandardCharsets.UTF_8), logFile, true);
        } catch (IOException e) {
            Log.e(TAG, "write to file failed", e);
        }
    }

    private static void writeFlameDataToFile(String content) {
        try {
            File logFile =
                    new File(Runtime.getInstance().getContext().getExternalFilesDir(null),
                            FILE_NAME_FLAME);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            String finalContent = content;
            FileUtils.saveToFile(finalContent.getBytes(StandardCharsets.UTF_8), logFile, false);
        } catch (IOException e) {
            Log.e(TAG, "write to file failed", e);
        }
    }

    public static void checkProfilerState() {
        if (sIsCached) {
            return;
        }
        sExecutor.execute(
                () -> {
                    SysOpProvider provider =
                            ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
                    if (provider != null) {
                        sIsAllowProfiler = provider.isAllowProfiler();
                        sIsCached = true;
                    }
                });
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
