/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage.file;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.common.utils.FileUtils;

public class FileResource extends Resource {
    private static final String TAG = "FileResource";
    protected File mFile;
    private ApplicationContext mApplicationContext;
    private File mRootDir;

    public FileResource(ApplicationContext applicationContext, String uri, File rootDir,
                        File file) {
        super(uri);
        mApplicationContext = applicationContext;
        mRootDir = rootDir;
        mFile = file;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        if (mFile.isDirectory()) {
            throw new IOException("Fail to open input stream, " + toUri() + " is a directory.");
        }
        return new FileInputStream(mFile);
    }

    @Override
    public OutputStream openOutputStream(long position, boolean append) throws IOException {
        if (append) {
            if (!mFile.exists()) {
                throw new IOException("file does not exists");
            }
            position = mFile.length();
        }

        if (mFile.isDirectory()) {
            throw new IOException("Fail to open output stream, " + toUri() + " is a directory.");
        }
        if (!mFile.getParentFile().exists()) {
            FileUtils.mkdirs(mFile.getParentFile());
        }
        if (position < 0) {
            if (mFile.exists() && !mFile.delete()) {
                return null;
            }
            position = 0;
        }

        RandomAccessFile randomAccessFile = new RandomAccessFile(mFile, "rw");
        randomAccessFile.seek(position);
        return new FileOutputStream(randomAccessFile.getFD()) {
            @Override
            public void close() throws IOException {
                super.close();
                FileUtils.closeQuietly(randomAccessFile);
            }
        };
    }

    @Override
    public ResourceInfo get() {
        return ResourceInfo.create(toUri(), mFile);
    }

    @Override
    public ResourceInfo get(boolean recursive) {
        return ResourceInfo.create(toUri(), mFile, recursive, mApplicationContext);
    }

    @Override
    public boolean delete() throws IOException {
        if (mFile.isDirectory()) {
            throw new IOException("Can not delete a directory.");
        }
        boolean result = mFile.delete();

        // 逐级向上删除空文件夹
        try {
            String typeDir = mRootDir.getCanonicalPath();
            File parent = mFile;
            String parentPath;
            do {
                parent = parent.getParentFile();
                parentPath = parent.getCanonicalPath();
            } while (!parentPath.equals(typeDir) && parent.delete());
        } catch (IOException e) {
            Log.w(TAG, "getCanonicalPath failed", e);
        }

        return result;
    }

    @Override
    public List<ResourceInfo> list() {
        List<ResourceInfo> resourceInfos = null;
        if (mFile.isDirectory()) {
            File[] files = mFile.listFiles();
            if (files != null) {
                resourceInfos = new ArrayList<>();
                for (File file : files) {
                    String internalUri = mApplicationContext.getInternalUri(file);
                    resourceInfos.add(ResourceInfo.create(internalUri, file));
                }
            }
        }
        return resourceInfos;
    }

    @Override
    public boolean mkDir(boolean recursive) throws IOException {
        String reason;

        if (mFile.exists()) {
            reason = "uri " + mUri + " exists";
            throw new IOException(reason);
        }

        if (!recursive) {
            if (!mFile.getParentFile().exists()) {
                reason = "the parent directory is not exists";
                throw new IOException(reason);
            }
        }

        return FileUtils.mkdirs(mFile);
    }

    @Override
    public boolean rmDir(boolean recursive) throws IOException {
        String reason;

        if (!mFile.exists()) {
            reason = "directory does not exists";
            throw new IOException(reason);
        }

        if (!mFile.isDirectory()) {
            reason = "uri " + mUri + " is not a directory";
            throw new IOException(reason);
        }

        if (!recursive) {
            boolean success = mFile.delete();
            if (!success) {
                reason = "delete directory fail";
                throw new IOException(reason);
            }
            return success;
        }

        return FileUtils.rmRF(mFile);
    }

    @Override
    public boolean access() {
        return mFile.exists();
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public File getUnderlyingFile() {
        return mFile;
    }

    @Override
    public Uri getUnderlyingUri() {
        return Uri.fromFile(mFile);
    }

    @Override
    public ParcelFileDescriptor getParcelFileDescriptor() throws IOException {
        if (mFile.isDirectory()) {
            throw new IOException(
                    "Fail to get parcel file descriptor, " + toUri() + " is a directory");
        }
        return ParcelFileDescriptor.open(mFile, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
