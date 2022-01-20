/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hapjs.common.executors.AbsTask;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.net.NetworkReportManager;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.ResourceConfig;
import org.hapjs.runtime.RuntimeActivity;

public class FontFileManager {

    private static final String TAG = "FontFileManager";
    private static final String FONT_DIR = "fonts/";

    private Map<Uri, List<FontFilePrepareCallback>> mTasks;
    private OkHttpClient mOkHttpClient;

    private FontFileManager() {
    }

    public static FontFileManager getInstance() {
        return FontDownloadManagerHolder.INSTANCE;
    }

    public void enqueueTask(
            Context context, AppInfo appInfo, Uri uri, FontFilePrepareCallback callback) {
        ensureDownloadTasks();
        if (mTasks.containsKey(uri)) {
            List<FontFilePrepareCallback> list = mTasks.get(uri);
            if (list != null) {
                list.add(callback);
            }
            mTasks.put(uri, list);
        } else {
            List<FontFilePrepareCallback> callbacks = new ArrayList<>();
            callbacks.add(callback);
            mTasks.put(uri, callbacks);
            if (TextUtils.equals(uri.getScheme(), "http")
                    || TextUtils.equals(uri.getScheme(), "https")) {
                downloadFont(context, appInfo, uri);
            } else if (TextUtils.equals(uri.getScheme(), "content")) {
                copyFromContentUri(context, appInfo, uri);
            }
        }
    }

    private void ensureDownloadTasks() {
        if (mTasks == null) {
            mTasks = new HashMap<>();
        }
    }

    private File generateFontFileTemp(Context context, AppInfo appInfo, Uri fontUri) {
        File fontFile = getOrCreateFontFile(context, appInfo, fontUri);
        if (fontFile == null) {
            return null;
        }
        return new File(fontFile.getPath() + ".tmp");
    }

    /**
     * Generate the font file path according to the font download uri.
     *
     * @param context
     * @param fontUri
     * @return
     */
    public File getOrCreateFontFile(Context context, AppInfo appInfo, Uri fontUri) {
        String pkg = appInfo.getPackage();
        File fontFileDir;
        if (!ResourceConfig.getInstance().isLoadFromLocal()) {
            // 卡片/插页模式下文件存储在宿主app的缓存目录下
            fontFileDir =
                    new File(context.getCacheDir(),
                            pkg + File.separator + appInfo.getVersionCode());
        } else {
            if (!(context instanceof RuntimeActivity)) {
                Log.e(TAG, "context is not an instance of RuntimeActivity");
                return null;
            }
            final RuntimeActivity act = (RuntimeActivity) context;
            File cacheDir =
                    act.getHybridView().getHybridManager().getApplicationContext().getCacheDir();
            fontFileDir = new File(cacheDir, FONT_DIR);
        }

        if (!fontFileDir.exists() && !fontFileDir.mkdirs()) {
            Log.e(TAG, "error: can not create font file directory");
            return null;
        }
        String fileName = generateFontFileName(fontUri);
        return new File(fontFileDir, fileName);
    }

    private String generateFontFileName(Uri fontUri) {
        if (fontUri == null) {
            return "";
        }
        try {
            KeySpec spec = new PBEKeySpec(fontUri.toString().toCharArray());
            SecretKeyFactory secretKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return new BigInteger(1, secretKey.generateSecret(spec).getEncoded()).toString();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "generate font file name error", e);
        }
        return URLUtil.guessFileName(fontUri.toString(), null, null);
    }

    private void renameFontFile(Context context, AppInfo appInfo, Uri downloadUri, File tempFile) {
        if (tempFile == null) {
            Log.e(TAG, "rename font file error: font temp file is null");
            return;
        }
        tempFile.renameTo(getOrCreateFontFile(context, appInfo, downloadUri));
    }

    private void downloadFont(final Context context, final AppInfo appInfo, final Uri downloadUri) {
        if (mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient();
        }
        NetworkReportManager.getInstance()
                .reportNetwork(NetworkReportManager.KEY_FONT_FILE_MANAGER, downloadUri.toString());

        Request request = new Request.Builder().url(downloadUri.toString()).build();

        final Handler mainHandler = new Handler(context.getMainLooper());
        mOkHttpClient
                .newCall(request)
                .enqueue(
                        new Callback() {
                            @Override
                            public void onFailure(Call call, final IOException e) {
                                mainHandler.post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                onFontFilePrepared(downloadUri, null);
                                                Log.e(TAG, "download font failed", e);
                                            }
                                        });
                            }

                            @Override
                            public void onResponse(Call call, Response response)
                                    throws IOException {
                                if (response.body() != null) {
                                    saveFontFile(context, appInfo, response.body().byteStream(),
                                            downloadUri);
                                }
                                response.close();

                                final File fontFile =
                                        getOrCreateFontFile(context, appInfo, downloadUri);
                                mainHandler.post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                if (fontFile != null) {
                                                    onFontFilePrepared(downloadUri,
                                                            Uri.fromFile(fontFile));
                                                } else {
                                                    onFontFilePrepared(downloadUri, null);
                                                }
                                            }
                                        });
                            }
                        });
    }

    private void saveFontFileTemp(InputStream is, File tempFile) {
        if (is == null) {
            Log.e(TAG, "save temp file failed: input stream is null");
            return;
        }
        if (tempFile == null) {
            Log.e(TAG, "save temp file failed: tempFile is null");
            return;
        }
        FileOutputStream fos = null;
        byte[] buff = new byte[2048];
        try {
            fos = new FileOutputStream(tempFile);
            int len;
            while ((len = is.read(buff)) != -1) {
                fos.write(buff, 0, len);
            }
            fos.flush();
        } catch (IOException e) {
            Log.e(TAG, "save font temp file error", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "save font temp file error", e);
                }
            }
            try {
                is.close();
            } catch (IOException e) {
                Log.e(TAG, "save font temp file error", e);
            }
        }
    }

    private void saveFontFile(
            Context context, AppInfo appInfo, InputStream inputStream, Uri fontUri) {
        File tempFile = generateFontFileTemp(context, appInfo, fontUri);
        saveFontFileTemp(inputStream, tempFile);
        renameFontFile(context, appInfo, fontUri, tempFile);
    }

    private void copyFromContentUri(final Context context, final AppInfo appInfo,
                                    final Uri fontUri) {
        Executors.io()
                .execute(
                        new AbsTask<Void>() {
                            @Override
                            protected Void doInBackground() {
                                try {
                                    InputStream is =
                                            context.getContentResolver().openInputStream(fontUri);
                                    saveFontFile(context, appInfo, is, fontUri);
                                } catch (FileNotFoundException e) {
                                    Log.e(TAG, "doInBackground: ", e);
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void object) {
                                final File fontFile =
                                        getOrCreateFontFile(context, appInfo, fontUri);
                                if (fontFile == null) {
                                    onFontFilePrepared(fontUri, null);
                                } else {
                                    onFontFilePrepared(fontUri, Uri.fromFile(fontFile));
                                }
                            }
                        });
    }

    private void onFontFilePrepared(Uri downloadUri, Uri fileUri) {
        if (downloadUri == null || mTasks == null || mTasks.isEmpty()) {
            return;
        }
        List<FontFilePrepareCallback> callbacks = mTasks.get(downloadUri);
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }
        for (FontFilePrepareCallback callback : callbacks) {
            callback.onFontFilePrepared(fileUri);
        }
        mTasks.remove(downloadUri);
    }

    public interface FontFilePrepareCallback {
        void onFontFilePrepared(Uri fileUri);
    }

    private static class FontDownloadManagerHolder {
        private static FontFileManager INSTANCE = new FontFileManager();
    }
}
