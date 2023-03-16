/* Copyright (c) 2022-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.screenshot;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScreenshotObserver {
    private static final String TAG = "ScreenshotObserver";

    private static final String SCREENSHOT_DIR = "截屏";

    private static final String[] MEDIA_PROJECTIONS = {
            MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.DATE_TAKEN,
    };

    private String[] mKeyWords = {
            "screenshot", "screen_shot", "screen-shot", "screen shot",
            "screencapture", "screen_capture", "screen-capture", "screen capture",
            "screencap", "screen_cap", "screen-cap", "screen cap", "截屏", "截图"
    };

    private final List<String> sHasCallbackPaths = new ArrayList<String>();
    private Context mContext;
    private Set<OnScreenShotListener> mListeners = new HashSet<>();
    private long mStartListenTime;

    private MediaContentObserver mInternalObserver;
    private MediaContentObserver mExternalObserver;
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());

    private ScreenshotFileObserver mFileObserver;

    private ScreenshotObserver(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("The context must not be null.");
        }
        mContext = context;
    }

    public static ScreenshotObserver newInstance(Context context) {
        assertInMainThread();
        return new ScreenshotObserver(context);
    }

    public void setKeyWords(String[] keyWords) {
        mKeyWords = keyWords;
    }

    public void startListen() {
        Log.i(TAG, "startListen");
        assertInMainThread();

        sHasCallbackPaths.clear();
        mStartListenTime = System.currentTimeMillis();

        mInternalObserver = new MediaContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, mUiHandler);
        mExternalObserver = new MediaContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mUiHandler);

        mContext.getContentResolver().registerContentObserver(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                mInternalObserver
        );
        mContext.getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                mExternalObserver
        );

        // vivo android 5部分机器的ContentObserver失效, 使用FileObserver辅助监视
        // 如果ContentObserver生效, 连续的相同检测会被去重, 不会触发多次回调
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            File dcimDir = new File(Environment.getExternalStorageDirectory() +
                    File.separator + SCREENSHOT_DIR + File.separator);
            if (dcimDir.exists()) {
                mFileObserver = new ScreenshotFileObserver(dcimDir.getPath());
                mFileObserver.startWatching();
            }
        }
    }

    public void stopListen() {
        Log.i(TAG, "stopListen");
        assertInMainThread();

        if (mInternalObserver != null) {
            try {
                mContext.getContentResolver().unregisterContentObserver(mInternalObserver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mInternalObserver = null;
        }
        if (mExternalObserver != null) {
            try {
                mContext.getContentResolver().unregisterContentObserver(mExternalObserver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mExternalObserver = null;
        }

        if (mFileObserver != null) {
            mFileObserver.stopWatching();
            mFileObserver = null;
        }

        mStartListenTime = 0;
        sHasCallbackPaths.clear();
        mListeners.clear();
    }

    private void handleMediaContentChange(Uri contentUri) {
        Cursor cursor = null;
        try {
            //数据改变时查询数据库中最后加入的一条数据
            if (Build.VERSION.SDK_INT >= 30) {
                final Bundle bundle = new Bundle();
                bundle.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, new String[]{MediaStore.Images.ImageColumns.DATE_ADDED});
                bundle.putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING);
                bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, 4);
                cursor = mContext.getContentResolver().query(contentUri, MEDIA_PROJECTIONS, bundle, null);
            } else {
                cursor = mContext.getContentResolver().query(
                        contentUri,
                        MEDIA_PROJECTIONS,
                        null,
                        null,
                        MediaStore.Images.ImageColumns.DATE_ADDED + " desc limit 4"
                );
            }

            if (cursor == null) {
                Log.i(TAG, "ScreenSize cursor null");
                return;
            }
            if (!cursor.moveToFirst()) {
                Log.i(TAG, "ScreenSize not move first");
                return;
            }

            boolean found = false;
            do {
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                int dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN);

                String data = cursor.getString(dataIndex);
                long dateTaken = cursor.getLong(dateTakenIndex);

                if (checkScreenShot(data, dateTaken)) {
                    if (!mListeners.isEmpty() && !checkCallback(data)) {
                        found = true;
                        break;
                    }
                } else {
                    Log.i(TAG, data + " is not current ScreenShot");
                }
            } while (cursor.moveToNext());

            if (found) {
                for (OnScreenShotListener listener : mListeners) {
                    listener.onShot();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean checkScreenShot(String data, long dateTaken) {
        // 如果加入数据库的时间在开始监听之前, 或者与当前时间相差大于10秒, 则认为当前没有截屏
        if (dateTaken < mStartListenTime || (System.currentTimeMillis() - dateTaken) > 10 * 1000) {
            return false;
        }

        if (TextUtils.isEmpty(data)) {
            return false;
        }
        data = data.toLowerCase();

        for (String keyWork : mKeyWords) {
            if (data.contains(keyWork)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断路径是否已回调过
     * 会有截屏一次会发出多次内容改变的通知的情况
     * 删除图片也发通知
     */
    private boolean checkCallback(String imagePath) {
        if (sHasCallbackPaths.contains(imagePath)) {
            return true;
        }
        // 缓存15~20条记录
        if (sHasCallbackPaths.size() >= 20) {
            for (int i = 0; i < 5; i++) {
                sHasCallbackPaths.remove(0);
            }
        }
        sHasCallbackPaths.add(imagePath);
        return false;
    }

    public void addListener(OnScreenShotListener listener) {
        if (listener != null) {
            mListeners.add(listener);
        }
    }

    public void removeListener(OnScreenShotListener listener) {
        mListeners.remove(listener);
    }

    public interface OnScreenShotListener {
        void onShot();
    }

    private static void assertInMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            String methodMsg = null;
            if (elements != null && elements.length >= 4) {
                methodMsg = elements[3].toString();
            }
            throw new IllegalStateException("Call the method must be in main thread: " + methodMsg);
        }
    }

    private class MediaContentObserver extends ContentObserver {

        private Uri mContentUri;

        public MediaContentObserver(Uri contentUri, Handler handler) {
            super(handler);
            mContentUri = contentUri;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.i(TAG, "MediaContentObserver onChange ");
            handleMediaContentChange(mContentUri);
        }
    }

    private class ScreenshotFileObserver extends FileObserver {
        private String mPath;

        public ScreenshotFileObserver(String path) {
            super(path);
            mPath = path;
        }

        @Override
        public void onEvent(int event, String path) {
            Log.i(TAG, "FileObserver onEvent ");
            if (event == FileObserver.CREATE) {
                path = mPath + File.separator + path;
                if (checkScreenShot(path, System.currentTimeMillis())) {
                    if (!mListeners.isEmpty() && !checkCallback(path)) {
                        for (OnScreenShotListener listener : mListeners) {
                            listener.onShot();
                        }
                        Log.d(TAG, path + " detect by FileObserver");
                    }
                } else {
                    Log.d(TAG, path + " not current screenshot by FileObserver");
                }
            }
        }
    }
}
