/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static boolean rmdirs(File dir) {
        dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    rmdirs(f);
                } else {
                    f.delete();
                }
                return false;
            }
        });
        return dir.delete();
    }

    public static boolean mkdirs(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                if (!dir.exists()) {
                    return false;
                }
            }
        } else if (!dir.isDirectory()) {
            return false;
        }
        return true;
    }

    /**
     * Copy a file from srcFile to destFile.
     *
     * @return true if succeed, false if fail
     */
    public static boolean copyFile(File srcFile, File destFile) {
        boolean result = false;
        try {
            InputStream in = new FileInputStream(srcFile);
            try {
                result = saveToFile(in, destFile);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Fail to copyFile, srcFile=" + srcFile + ", destFile=" + destFile, e);
            result = false;
        }
        return result;
    }

    /**
     * Copy data from a source stream to {@code destFile}.
     *
     * @return true if succeed, false if failed.
     */
    public static boolean saveToFile(InputStream inputStream, File destFile) {
        try {
            if (destFile.exists()) {
                if (!destFile.delete()) {
                    return false;
                }
            }
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                    // ignore this exception
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Fail to saveToFile, destFile=" + destFile, e);
            return false;
        }
    }

    public static boolean saveToFile(byte[] data, File destFile) {
        if (data == null) {
            return false;
        }
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(destFile);
            output.write(data);
            return true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Fail to saveToFile, destFile=" + destFile, e);
        } catch (IOException e) {
            Log.e(TAG, "Fail to saveToFile, destFile=" + destFile, e);
        } finally {
            closeQuietly(output);
        }
        return false;
    }

    public static boolean saveToFile(String inputText, File destFile) {
        try {
            return saveToFile(inputText.getBytes("UTF-8"), destFile);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Fail to saveToFile, destFile=" + destFile, e);
            return false;
        }
    }

    /**
     * Return file extension with the leading '.'
     */
    public static String getFileExtension(File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            return name.substring(lastDotIndex);
        } else {
            return "";
        }
    }

    /**
     * Returns the contents of 'path' as a string. The contents are assumed to be UTF-8.
     */
    public static String readFileAsString(String path) throws IOException {
        return new String(readFileAsBytes(path), Charset.forName("UTF-8"));
    }

    /**
     * Returns the contents of 'path' as a string. The contents are assumed to be UTF-8.
     */
    public static String readStreamAsString(InputStream input, boolean autoClose) throws IOException {
        return new String(readStreamAsBytes(input, 0, autoClose), Charset.forName("UTF-8"));
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.e(TAG, "Fail to closeQuietly", e);
            }
        }
    }

    public static byte[] readFileAsBytes(String path) throws IOException {
        FileInputStream f = null;
        try {
            f = new FileInputStream(path);
            return readStreamAsBytes(f, (int) (new File(path).length()), true);
        } finally {
            closeQuietly(f);
        }
    }

    public static byte[] readStreamAsBytes(InputStream input, int length, boolean autoClose) throws IOException {
        try {
            ByteArrayOutputStream stream;
            if (length > 0) {
                stream = new ByteArrayOutputStream(length);
            } else {
                stream = new ByteArrayOutputStream();
            }
            byte[] buffer = new byte[8192];
            while (true) {
                int byteCount = input.read(buffer);
                if (byteCount == -1) {
                    return stream.toByteArray();
                }
                stream.write(buffer, 0, byteCount);
            }
        } finally {
            if (autoClose) {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException e) {
                    // ignore this exception
                }
            }
        }
    }

    public static long getDiskUsage(File f) {
        if (f == null || !f.exists()) {
            return 0L;
        }
        if (f.isDirectory()) {
            final long[] size = new long[] {0L};
            f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File child) {
                    size[0] += getDiskUsage(child);
                    return false;
                }
            });
            return size[0];
        } else {
            return f.length();
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    public static String getFilePathByUri(Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        ;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else if ("home".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/documents/" + split[1];
                }
                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {  // DownloadsProvider
                final String id = DocumentsContract.getDocumentId(uri);
                if (TextUtils.isEmpty(id)) {
                    return null;
                }
                if (id.startsWith("raw:")) {
                    return id.substring(4);
                }
                String[] contentUriPrefixesToTry = new String[] {
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads",
                        "content://downloads/all_downloads"
                };
                for (String contentUriPrefix : contentUriPrefixesToTry) {
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix),
                            Long.valueOf(id));
                    String path = getDataColumn(context, contentUri, null, null);
                    if (path != null) {
                        return path;
                    }
                }
                String path = getDataColumn(context, uri, null, null);
                if (path != null) {
                    return path;
                }
                return null;
            } else if (isMediaDocument(uri)) {  // MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type.toLowerCase(Locale.ENGLISH))) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type.toLowerCase(Locale.ENGLISH))) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type.toLowerCase(Locale.ENGLISH))) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else {
                    contentUri = MediaStore.Files.getContentUri("external");
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) { // MediaStore (and general)

            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }

            //第三方QQ文件管理器
            if (isQQMediaDocument(uri)) {
                String path = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + uri.getPath().replace("/QQBrowser", "");
                if (path != null) {
                    return path;
                }
            }
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) { // File
            return uri.getPath();
        }
        return null;
    }

    /**
     * *
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            Log.e(TAG, "failed to get the value of the data column for Uri: " + uri, e);
        } finally {
            closeQuietly(cursor);
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * 使用第三方qq文件管理器打开
     *
     * @param uri
     * @return
     */
    public static boolean isQQMediaDocument(Uri uri) {
        return "com.tencent.mtt.fileprovider".equals(uri.getAuthority());
    }
}
