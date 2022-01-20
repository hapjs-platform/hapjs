/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.executors.Future;
import org.hapjs.common.utils.FileUtils;

public class ZipExtractor {
    private static final String TAG = "ZipExtractor";
    protected ZipInputStream mStream;
    private OnFileSavedListener mOnFileSavedListener;
    private boolean mAutoClose;

    public ZipExtractor(ZipInputStream stream) {
        mAutoClose = true;
        this.mStream = stream;
    }

    public ZipExtractor(ZipInputStream stream, boolean autoClose) {
        mStream = stream;
        mAutoClose = autoClose;
    }

    public static ZipExtractor create(File file) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        return new ZipExtractor(new ZipInputStream(new BufferedInputStream(fis)));
    }

    protected SaveFileTask generateSaveFileTask(byte[] content, File file) {
        return new SaveFileTask(content, file);
    }

    public void extract(File outDir) throws IOException, CacheException {
        long unzipStart = System.currentTimeMillis();
        ZipEntry ze;
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            while ((ze = mStream.getNextEntry()) != null) {
                String name = ze.getName();

                // fix ZipperDown issue
                if (!TextUtils.isEmpty(name) && name.contains("../")) {
                    continue;
                }

                Log.d(TAG, "extract: filename=" + name);
                if (ze.isDirectory()) {
                    File dir = new File(outDir, name);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                } else {
                    File file = new File(outDir, ze.getName());
                    File dir = file.getParentFile();
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    ByteArrayOutputStream out = readFile(mStream, ze.getName(), ze.getSize());
                    byte[] content = out.toByteArray();
                    SaveFileTask saveFileTask = generateSaveFileTask(content, file);
                    Future<Boolean> future = Executors.io().submit(saveFileTask);
                    futures.add(future);
                    out.close();
                }
                mStream.closeEntry();
            }
            if (futures != null) {
                // blocking wait all task finish
                for (Future<Boolean> future : futures) {
                    future.get();
                }
            }
            Log.d(TAG, "extract finish cost time: " + (System.currentTimeMillis() - unzipStart));
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "extract: ", e);
            throw new IOException("Fail to save file", e);
        } finally {
            if (mAutoClose) {
                FileUtils.closeQuietly(mStream);
            }
        }
    }

    protected ByteArrayOutputStream readFile(InputStream stream, String fileName, long size)
            throws IOException {
        CustomByteArrayOutputStream out;
        if (size > (long) Integer.MAX_VALUE) {
            throw new IOException("entry size too large");
        } else if (size < 0) {
            // sometimes size = -1, but can read normally
            out = new CustomByteArrayOutputStream();
        } else {
            out = new CustomByteArrayOutputStream((int) size);
        }
        byte[] byteBuff = new byte[1024];
        int bytesRead = 0;
        while ((bytesRead = stream.read(byteBuff)) != -1) {
            out.write(byteBuff, 0, bytesRead);
        }
        return out;
    }

    public void setOnFileSavedListener(OnFileSavedListener onFileSavedListener) {
        mOnFileSavedListener = onFileSavedListener;
    }

    public void cancel() throws CacheException {
        FileUtils.closeQuietly(mStream);
    }

    public interface OnFileSavedListener {
        void onFileSaved(File file);
    }

    protected class SaveFileTask implements Callable<Boolean> {

        byte[] mContent;
        File mFile;
        String mTmpFileSuffix = "";

        public SaveFileTask(byte[] content, File file) {
            mContent = content;
            mFile = file;
        }

        public SaveFileTask(byte[] content, File file, String tmpFileSuffix) {
            this(content, file);
            mTmpFileSuffix = tmpFileSuffix;
        }

        @Override
        public Boolean call() throws Exception {
            saveFile(mFile);
            Log.d(TAG, "extract success: filename=" + mFile.getName()
                    + Thread.currentThread().getName());
            return true;
        }

        private void saveFile(File file) throws IOException {
            saveFileInner(file);
            if (mOnFileSavedListener != null) {
                mOnFileSavedListener.onFileSaved(file);
            }
        }

        private void saveFileInner(File file) throws IOException {
            File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp" + mTmpFileSuffix);
            if (!FileUtils.saveToFile(mContent, tmpFile)) {
                throw new IOException("Fail to save tmpFile");
            }
            if (file.exists()) {
                file.delete();
            }
            if (!tmpFile.renameTo(file)) {
                if (file.exists()) {
                    // skip shared file, such as manifest.json
                    FileUtils.rmRF(tmpFile);
                } else {
                    throw new IOException(
                            "Fail to rename file:" + tmpFile.toString() + " to " + file);
                }
            }
        }
    }

    private class CustomByteArrayOutputStream extends ByteArrayOutputStream {

        public CustomByteArrayOutputStream() {
            super();
        }

        public CustomByteArrayOutputStream(int size) {
            super(size);
        }

        public byte[] getBuf() {
            return buf;
        }
    }
}
