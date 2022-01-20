/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileHelper {
    private static final String TAG = "FileHelper";
    private static final Pattern URI_PATTERN = Pattern.compile("[\\w]+://[^\"':|*?<>\\\\]+");

    private FileHelper() {
    }

    public static String getFileFromContentUri(Context context, Uri uri) {
        String path = null;
        if (uri == null || TextUtils.isEmpty(uri.toString())) {
            return null;
        }
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    path = Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                // DownloadsProvider
                final String id = DocumentsContract.getDocumentId(uri);
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:", "");
                }
                try {
                    long docId = Long.parseLong(id);
                    final Uri contentUri =
                            ContentUris.withAppendedId(
                                    Uri.parse("content://downloads/public_downloads"), docId);
                    path = getDataColumn(context, contentUri, null, null);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "get docId fail with id=" + id, e);
                }
            } else if (isMediaDocument(uri)) {
                // MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                if (contentUri != null) {
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[] {split[1]};
                    path = getDataColumn(context, contentUri, selection, selectionArgs);
                } else {
                    Log.e(TAG, "getFileFromContentUri: contentUri is null");
                }
            } else {
                path = getDataColumn(context, uri, null, null);
            }
        } else if ("file".equals(uri.getScheme())) {
            path = uri.getPath();
        } else {
            path = getDataColumn(context, uri, null, null);
        }
        return path;
    }

    public static String getDataColumn(
            Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = MediaStore.MediaColumns.DATA;
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver()
                    .query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            Log.e(TAG, "getDataColumn fail", e);
        } finally {
            FileUtils.closeQuietly(cursor);
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isUriHasFile(Context context, Uri uri) {
        if (uri == null || TextUtils.isEmpty(uri.toString())) {
            return false;
        }
        boolean exist = false;
        ParcelFileDescriptor pFd = null;
        try {
            String path = getFileFromContentUri(context, uri);
            if (!TextUtils.isEmpty(path)) {
                File file = new File(path);
                if (file.exists()) {
                    exist = true;
                }
            } else {
                pFd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pFd != null) {
                    exist = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "check isUriHasFile fail", e);
        } finally {
            FileUtils.closeQuietly(pFd);
        }
        return exist;
    }

    public static String getFileHashFromInputStream(InputStream inputStream, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[1024];
            int numRead = -1;
            while ((numRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, numRead);
            }
            return StringUtils.byte2HexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "getFileHashFromInputStream", e);
        } catch (IOException e) {
            Log.e(TAG, "getFileHashFromInputStream", e);
        } finally {
            FileUtils.closeQuietly(inputStream);
        }
        return null;
    }

    public static String getDisplayNameFromContentUri(Context context, Uri uri) {
        Cursor c = null;
        try {
            c =
                    context
                            .getContentResolver()
                            .query(uri, new String[] {MediaStore.MediaColumns.DISPLAY_NAME}, null,
                                    null, null);
            if (c != null && c.moveToFirst()) {
                return c.getString(0);
            }
        } catch (Exception e) {
            Log.w(TAG, "getDisplayNameFromContentUri", e);
        } finally {
            FileUtils.closeQuietly(c);
        }
        return null;
    }

    public static Pair<Long, Long> getFileInfoFromContentUri(Context context, Uri uri) {
        String filePath = getFileFromContentUri(context, uri);
        if (!TextUtils.isEmpty(filePath)) {
            File file = new File(filePath);
            if (file.isFile()) {
                return new Pair<>(file.length(), file.lastModified());
            }
        }

        Cursor c = null;
        try {
            c =
                    context
                            .getContentResolver()
                            .query(
                                    uri,
                                    new String[] {
                                            MediaStore.MediaColumns.SIZE,
                                            MediaStore.MediaColumns.DATE_MODIFIED
                                    },
                                    null,
                                    null,
                                    null);
            if (c != null && c.moveToFirst()) {
                int sizeIndex = c.getColumnIndex(MediaStore.MediaColumns.SIZE);
                int dateModifiedIndex = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);
                long size = sizeIndex == -1 ? -1 : c.getLong(sizeIndex);
                long dateModified = dateModifiedIndex == -1 ? -1 : c.getLong(dateModifiedIndex);
                return new Pair<>(size, dateModified);
            }
        } catch (Exception e) {
            Log.w(TAG, "getFileInfoFromContentUri", e);
        } finally {
            FileUtils.closeQuietly(c);
        }
        return null;
    }

    public static boolean isValidUri(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return false;
        }
        if (uri.contains("..")) {
            return false;
        }
        if (uri.startsWith("/")) {
            return true;
        }
        Matcher matcher = URI_PATTERN.matcher(uri);
        return matcher.matches();
    }

    public static String getValidUri(String uri) {
        if (!isValidUri(uri)) {
            if (uri.startsWith("./")) {
                return uri.substring(1);
            } else {
                return "/" + uri;
            }
        }

        return uri;
    }

    public static File generateAvailableFile(String fileName, File dir) throws IOException {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }
        int dotIndex = fileName.indexOf('.');
        if (dotIndex == -1) {
            dotIndex = fileName.length();
        }

        String suffix = fileName.substring(dotIndex);
        String prefix = fileName.substring(0, dotIndex);
        if (prefix.length() > 100) {
            prefix = prefix.substring(prefix.length() - 100);
        }

        File file = new File(dir, prefix + suffix);

        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            if (!file.exists() && file.createNewFile()) {
                break;
            }
            file = new File(dir, String.format(Locale.ROOT, "%s-%d%s", prefix, i, suffix));
        }
        if (!file.exists()) {
            return null;
        }
        return file;
    }
}
