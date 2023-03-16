/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hapjs.common.executors.Executors;

public class MediaUtils {
    private static final String TAG = "MediaUtils";
    private static final String DIRECTORY_SHARE = "share_image";

    public static byte[] getImageThumbData(Context context, Uri uri, int maxSize) {
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                Log.e(TAG, "getImageThumbData: openInputStream result is null uri = " + uri);
                return null;
            }
            int length = in.available();
            if (length <= maxSize) {
                return FileUtils.readStreamAsBytes(in, in.available(), true);
            }
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int i = 5; i > 0; i--) {
                out.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
                byte[] result = out.toByteArray();
                if (result.length <= maxSize) {
                    return result;
                }

                float zoom = (float) Math.sqrt((float) result.length / maxSize);
                int w = (int) (bitmap.getWidth() / zoom);
                int h = (int) (bitmap.getHeight() / zoom);
                bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
            }
        } catch (Exception e) {
            Log.w(TAG, "getImageThumbData", e);
        } finally {
            FileUtils.closeQuietly(in);
        }
        return null;
    }

    public static Uri createExternalStorageUri(Context context, String pkg, Uri uri) {
        if (uri == null) {
            return null;
        }

        Uri destUri = null;
        String filePath = FileHelper.getFileFromContentUri(context, uri);
        File file = !TextUtils.isEmpty(filePath) ? new File(filePath) : null;
        if (isExternalStorageFile(file)) {
            destUri = uri;
        } else {
            destUri = copyFile2ExternalStorage(context, pkg, uri);
        }

        if (destUri != null) {
            return destUri;
        }
        return uri;
    }

    public static Uri copyFile2ExternalStorage(Context context, String pkg, Uri srcUri) {
        File dir = getImageTempDir(context, pkg);
        if (dir == null) {
            return null;
        }

        InputStream input = null;
        try {
            File dstFile = createRandomTempFile(srcUri, dir);
            input = context.getContentResolver().openInputStream(srcUri);
            if (FileUtils.saveToFile(input, dstFile)) {
                return Uri.fromFile(dstFile);
            }
        } catch (Exception e) {
            Log.w(TAG, "copy file failed!", e);
        } finally {
            FileUtils.closeQuietly(input);
        }
        return null;
    }

    public static File getImageTempDir(Context context, String pkg) {
        if (isExternalStorageWritable()) {
            try {
                File dir = new File(getAppMediaTempDir(context, pkg), DIRECTORY_SHARE);
                if (FileUtils.mkdirs(dir)) {
                    return dir;
                }
            } catch (Exception e) {
                Log.e(TAG, "get image temp dir error", e);
            }
        }
        return null;
    }

    private static File getMediaTempDir(Context context) {
        return new File(context.getExternalCacheDir(), "temp");
    }

    private static File getAppMediaTempDir(Context context, String pkg) {
        File dir = new File(getMediaTempDir(context), pkg);
        FileUtils.mkdirs(dir);
        return dir;
    }

    private static File getQQShareFileProDir(Context context) {
        File dir = new File(context.getExternalFilesDir(null), "Images/tmp");
        FileUtils.mkdirs(dir);
        return dir;
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private static File createRandomTempFile(Uri srcUri, File directory) throws IOException {
        String suffix = MimeTypeMap.getFileExtensionFromUrl(srcUri.toString());
        suffix = TextUtils.isEmpty(suffix) ? "" : "." + suffix;
        File file = File.createTempFile(UUID.randomUUID().toString(), suffix, directory);
        return file;
    }

    private static boolean isExternalStorageFile(File file) {
        if (file == null || !isExternalStorageWritable()) {
            return false;
        }
        String path = file.getAbsolutePath();
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        if (TextUtils.isEmpty(path) || externalStorageDirectory == null) {
            return false;
        }
        String externalStoragePath = externalStorageDirectory.getAbsolutePath();
        if (TextUtils.isEmpty(externalStoragePath)) {
            return false;
        }
        if (path.startsWith(externalStoragePath)) {
            return true;
        }
        return false;
    }

    public static Uri getMediaContentUri(Context context, String pkg, String fileType, Uri uri) {
        Uri externalStorageUri = createExternalStorageUri(context, pkg, uri);
        String filePath = FileHelper.getFileFromContentUri(context, externalStorageUri);
        if (TextUtils.isEmpty(filePath)) {
            return uri;
        }

        fileType = TextUtils.isEmpty(fileType) ? getFileType(filePath) : fileType;
        Uri baseUri = getMediaBaseUri(fileType);
        Uri mediaUri = null;

        Cursor cursor = null;
        try {
            cursor =
                    context
                            .getContentResolver()
                            .query(
                                    baseUri,
                                    new String[] {MediaStore.MediaColumns._ID},
                                    MediaStore.MediaColumns.DATA + "=? ",
                                    new String[] {filePath},
                                    null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                    mediaUri = Uri.withAppendedPath(baseUri, String.valueOf(id));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getMediaContentUri: ", e);
        } finally {
            FileUtils.closeQuietly(cursor);
        }

        if (mediaUri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, filePath);
            mediaUri = context.getContentResolver().insert(baseUri, values);
        }
        return mediaUri;
    }

    private static Uri getMediaBaseUri(String fileType) {
        if (!TextUtils.isEmpty(fileType)) {
            if (fileType.contains("video/")) {
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if (fileType.contains("image/")) {
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if (fileType.contains("audio/")) {
                return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }
        }
        return MediaStore.Files.getContentUri("external");
    }

    private static String getFileType(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        try {
            return fileNameMap.getContentTypeFor(URLEncoder.encode(name, "UTF-8"));
        } catch (Exception e) {
            Log.w(TAG, "getContentType error", e);
        }
        return null;
    }

    public static void clearExpiredTempFile(final Context context, final String pkg) {
        final long timeStamp = System.currentTimeMillis();
        Executors.io()
                .execute(
                        () -> {
                            try {
                                final List<File> files = new ArrayList<>();
                                FileUtils.rmRF(
                                        getAppMediaTempDir(context, pkg),
                                        new FileFilter() {
                                            @Override
                                            public boolean accept(File file) {
                                                if (file.isFile()
                                                        && file.lastModified() < timeStamp) {
                                                    files.add(file);
                                                    return true;
                                                }
                                                return false;
                                            }
                                        });
                                for (File file : files) {
                                    removeMediaContentUri(context, file);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "clearExpiredTempFile", e);
                            }
                        });
    }

    private static void removeMediaContentUri(Context context, File file) {
        if (context == null || file == null) {
            return;
        }
        String fileType = getFileType(file.getName());
        Uri baseUri = getMediaBaseUri(fileType);
        context
                .getContentResolver()
                .delete(baseUri, MediaStore.MediaColumns.DATA + "=? ",
                        new String[] {file.getPath()});
    }
}
