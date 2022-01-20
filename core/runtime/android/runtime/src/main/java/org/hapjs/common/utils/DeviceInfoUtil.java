/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.UUID;
import org.hapjs.bridge.provider.SystemSettings;

public class DeviceInfoUtil {
    private static final String TAG = "DeviceInfoUtil";
    private static final String ADID_KEY = "device_adid";
    private static final String ADID_FILE_LOCK = "adid_file_lock.lock";
    private static final Object LOCK = new Object();

    public static String getAdvertisingId(Context context) {
        String adId = SystemSettings.getInstance().getString(ADID_KEY, "");
        if (!TextUtils.isEmpty(adId)) {
            return adId;
        }
        synchronized (LOCK) {
            File file = new File(context.getFilesDir() + File.separator + ADID_FILE_LOCK);
            RandomAccessFile lockFile = null;
            FileLock fileLock = null;
            try {
                lockFile = new RandomAccessFile(file.getPath(), "rw");
                fileLock = lockFile.getChannel().lock();
                adId = SystemSettings.getInstance().getString(ADID_KEY, "");
                if (TextUtils.isEmpty(adId)) {
                    String id = UUID.randomUUID().toString();
                    boolean result = SystemSettings.getInstance().putString(ADID_KEY, id);
                    adId = result ? id : "";
                }
            } catch (IOException e) {
                Log.e(TAG, "Fail to sync", e);
            } finally {
                if (fileLock != null) {
                    try {
                        fileLock.release();
                    } catch (IOException e) {
                        Log.e(TAG, "Fail to release lock", e);
                    }
                }
                FileUtils.closeQuietly(lockFile);
            }
            return adId;
        }
    }
}
