/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */

package org.hapjs.render.jsruntime;

import android.util.Log;
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

public class ProfilerHelper {

    private static final String TAG = "ProfilerHelper";

    // 基于时间点和事件的 Profiler 文本文件
    private static final String FILE_NAME_TIMELINE = "profiler_log.txt";

    // 基于火焰图的 Profile 火焰图文件
    private static final String FILE_NAME_FLAME = "framework.cpuprofile";

    private static final String KEY_APP_START = "app_start";

    private static ScheduledExecutor sExecutor = Executors.createSingleThreadExecutor();
    private static Map<String, Long> mMsgMap = new HashMap<>();

    private static boolean sIsAllowProfiler;
    private static boolean sIsCached;

    public static void recordAppStart(long time) {
        mMsgMap.put(KEY_APP_START, time);
    }

    public static void recordFirstFrameRendered(long time, long threadId) {
        if (!sIsAllowProfiler) {
            Log.d(TAG, "recordFirstFrameRendered: not allow profiler");
            return;
        }
        sExecutor.execute(() -> {
            if (mMsgMap.containsKey(KEY_APP_START)) {
                writeToFile(getContent("first frame rendered", getCost(time, KEY_APP_START), threadId));
            }
        });
    }

    public static boolean profilerIsEnabled() {
        return sIsAllowProfiler;
    }

    public static void profilerRecord(String msg, long threadId) {
        sExecutor.execute(() -> writeToFile(getContent("record", msg, threadId)));
    }

    public static void profilerSaveProfilerData(String data) {
        sExecutor.execute(() -> writeFlameDataToFile(data));
    }

    public static void profilerTimeEnd(String msg) {
        sExecutor.execute(() -> writeToFile(msg));
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
            File logFile = new File(Runtime.getInstance().getContext().getExternalFilesDir(null), FILE_NAME_TIMELINE);
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
            File logFile = new File(Runtime.getInstance().getContext().getExternalFilesDir(null), FILE_NAME_FLAME);
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
        sExecutor.execute(() -> {
            SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
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
}
