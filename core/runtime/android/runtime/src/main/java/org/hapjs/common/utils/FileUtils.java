/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Locale;

public class FileUtils {
    private static final String TAG = "FileUtils";

    /**
     * Force to remove file or directory recursively
     *
     * @param f the file or directory to remove
     * @return true if remove success
     */
    public static boolean rmRF(File f) {
        if (null == f) {
            return true;
        }
        if (!f.exists()) {
            return true;
        }
        if (f.isDirectory()) {
            f.listFiles(
                    new FileFilter() {
                        @Override
                        public boolean accept(File child) {
                            rmRF(child);
                            return false;
                        }
                    });
        }
        return f.delete();
    }

    public static boolean rmRF(File f, final FileFilter fileFilter) {
        if (!f.exists()) {
            return true;
        }
        if (f.isDirectory()) {
            f.listFiles(
                    new FileFilter() {
                        @Override
                        public boolean accept(File child) {
                            rmRF(child, fileFilter);
                            return false;
                        }
                    });
        }
        if (fileFilter.accept(f)) {
            return f.delete();
        }
        return false;
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

    public static boolean copyRF(File src, File dst, boolean force) {
        if (src.isDirectory()) {
            if (!mkdirs(dst)) {
                Log.e(TAG, "failed to mkdirs " + dst.getAbsolutePath());
                return false;
            }

            boolean result = true;
            File[] srcFiles = src.listFiles();
            if (srcFiles != null) {
                for (File srcFile : srcFiles) {
                    result = copyRF(srcFile, new File(dst, srcFile.getName()), force) && result;
                    if (!result && !force) {
                        break;
                    }
                }
            }
            return result;
        } else if (src.isFile()) {
            return copyFile(src, dst);
        } else {
            Log.e(TAG, "illegal src: " + src.getAbsolutePath());
            return false;
        }
    }

    public static boolean saveToFile(ParcelFileDescriptor fd, File destFile) {
        if (fd == null || destFile == null) {
            return false;
        }
        return saveToFile(new FileInputStream(fd.getFileDescriptor()), destFile);
    }

    /**
     * Copy data from a source stream to {@code destFile}.
     *
     * @return true if succeed, false if failed.
     */
    public static boolean saveToFile(InputStream inputStream, File destFile) {
        if (inputStream == null || destFile == null) {
            return false;
        }
        FileOutputStream out = null;
        try {
            if (destFile.exists()) {
                if (!destFile.delete()) {
                    return false;
                }
            }
            out = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) >= 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            out.getFD().sync();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Fail to saveToFile, destFile=" + destFile, e);
        } finally {
            FileUtils.closeQuietly(out);
        }
        return false;
    }

    public static boolean saveToFile(byte[] data, File destFile) {
        return saveToFile(data, destFile, false);
    }

    public static boolean saveToFile(byte[] data, File destFile, boolean append) {
        if (data == null) {
            return false;
        }
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(destFile, append);
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

    public static boolean saveToFile(ByteBuffer byteBuffer, long position, File destFile) {
        if (null == byteBuffer) {
            return false;
        }
        FileChannel channel = null;
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(destFile, "rw");
            channel = file.getChannel();
            channel.write(byteBuffer, position);
            return true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Fail to saveToFile, destFile=" + destFile, e);
        } catch (IOException e) {
            Log.e(TAG, "Fail to saveToFile, destFile=" + destFile, e);
        } finally {
            closeQuietly(channel);
            closeQuietly(file);
        }
        return false;
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

    public static String getFileExtensionFromFileName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            return fileName.substring(lastDotIndex);
        } else {
            return "";
        }
    }

    public static String getFileNameWithoutExtension(File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            return name.substring(0, lastDotIndex);
        } else {
            return name;
        }
    }

    /**
     * Return file name without suffix.
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (!TextUtils.isEmpty(fileName)) {
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex > -1) {
                return fileName.substring(0, lastDotIndex);
            }
        }
        return fileName;
    }


    public static String readFileAsString(File file) throws IOException {
        return new String(readFileAsBytes(file), Charset.forName("UTF-8"));
    }

    /**
     * Returns the contents of 'path' as a string. The contents are assumed to be UTF-8.
     */
    public static String readFileAsString(String path) throws IOException {
        return readFileAsString(path, "UTF-8");
    }

    /**
     * Returns the contents of 'path' as a string.
     */
    public static String readFileAsString(String path, String encoding) throws IOException {
        return new String(readFileAsBytes(path), Charset.forName(encoding));
    }

    public static String readUriAsString(Context context, Uri uri) throws IOException {
        InputStream is = context.getContentResolver().openInputStream(uri);
        return readStreamAsString(is, "UTF-8", true);
    }

    /**
     * Returns the contents of 'path' as a string. The contents are assumed to be UTF-8.
     */
    public static String readStreamAsString(InputStream input, boolean autoClose)
            throws IOException {
        return readStreamAsString(input, "UTF-8", autoClose);
    }

    /**
     * Returns the contents of 'path' as a string.
     */
    public static String readStreamAsString(InputStream input, String encoding, boolean autoClose)
            throws IOException {
        return new String(readStreamAsBytes(input, 0, autoClose), Charset.forName(encoding));
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

    public static void closeQuietly(Closeable closeable1, Closeable closeable2) {
        closeQuietly(closeable1);
        closeQuietly(closeable2);
    }

    public static void closeQuietly(Closeable... closeables) {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Fail to closeQuietly", e);
                    }
                }
            }
        }
    }

    public static byte[] readFileAsBytes(String path) throws IOException {
        return readFileAsBytes(new File(path));
    }

    public static byte[] readFileAsBytes(File file) throws IOException {
        FileInputStream f = null;
        try {
            f = new FileInputStream(file);
            return readStreamAsBytes(f, (int) (file.length()), true);
        } finally {
            closeQuietly(f);
        }
    }

    public static ByteBuffer readStreamAsBuffer(
            InputStream input, int position, int length, boolean autoClose) throws IOException {
        byte[] bytes = readStreamAsBytes(input, position, length, autoClose);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes, 0, bytes.length);
        return byteBuffer;
    }

    public static byte[] readStreamAsBytes(InputStream input, int length, boolean autoClose)
            throws IOException {
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

    public static byte[] readStreamAsBytes(
            InputStream input, int position, int length, boolean autoClose) throws IOException {
        try {
            if (position < 0 || length < 0) {
                throw new IndexOutOfBoundsException();
            }
            long skipped = input.skip(position);
            if (skipped < 0) {
                throw new IOException(TAG + ": readStreamAsBuffer: unexpected EOF");
            }
            int bufferMaxSize = 8192;
            int size = Math.min(bufferMaxSize, length);

            int remaining = length;
            byte[] bytes = new byte[size];
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            while (remaining > 0) {
                int byteCount = input.read(bytes, 0, Math.min(size, remaining));
                if (byteCount < 0) {
                    break;
                }
                stream.write(bytes, 0, byteCount);
                remaining -= byteCount;
            }
            return stream.toByteArray();
        } finally {
            if (autoClose) {
                closeQuietly(input);
            }
        }
    }

    public static long getDiskUsage(File f) {
        if (f == null || !f.exists()) {
            return 0L;
        }
        if (f.isDirectory()) {
            final long[] size = new long[] {0L};
            f.listFiles(
                    new FileFilter() {
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
     * Format file size to string end with unit "B","KB","MB" and "GB"(e.g, 16.82 MB)
     *
     * @param size The real file size
     * @return formatted file size string
     */
    public static String formatFileSize(long size) {
        float value = size;
        String unit = "B";
        if (value > 1024) {
            value /= 1024;
            unit = "KB";
        }
        if (value > 1024) {
            value /= 1024;
            unit = "MB";
        }
        if (value > 1024) {
            value /= 1024;
            unit = "GB";
        }
        return String.format(Locale.getDefault(), "%.2f %s", value, unit);
    }

    public static boolean isSupportedAudioType(String fileName) {
        if (TextUtils.isEmpty(fileName) || !fileName.contains(".")) {
            return false;
        }
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        switch (extension.toLowerCase()) {
            case "mp3":
            case "ogg":
            case "oga":
            case "flac":
            case "wav":
            case "m4a":
            case "amr":
            case "awb":
            case "wma":
            case "aac":
            case "mka":
            case "mid":
            case "midi":
            case "smf":
            case "imy":
                return true;
            default:
                break;
        }
        return false;
    }

    public static boolean isFileExist(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        return new File(filePath).exists();
    }
}