/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage.file;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.hapjs.common.utils.FileUtils;

public abstract class Resource {
    private static final String TAG = "HapResource";

    protected final String mUri;

    public Resource(String uri) {
        this.mUri = uri;
    }

    public abstract InputStream openInputStream() throws IOException;

    public abstract OutputStream openOutputStream(long position, boolean append) throws IOException;

    public void moveTo(Resource dstRes) throws IOException {
        if (!canWrite()) {
            throw new IOException("moveTo failed, src is not writable.");
        }
        if (dstRes == null) {
            throw new IOException("moveTo failed, dstRes is null.");
        }
        if (!dstRes.canWrite()) {
            throw new IOException("moveTo failed, dst is not writable.");
        }
        File srcFile = getUnderlyingFile();
        File dstFile = dstRes.getUnderlyingFile();

        if (srcFile != null && dstFile != null) {
            if (srcFile.isDirectory()) {
                throw new IOException("moveTo failed, srcFile is dir.");
            }

            if (dstFile.isDirectory()) {
                throw new IOException("moveTo failed, dstFile is dir.");
            }

            if (!dstFile.getParentFile().exists()) {
                dstFile.getParentFile().mkdirs();
            }

            if (srcFile.renameTo(dstFile)) {
                return;
            }
            Log.v(TAG, "rename file failed.");
        }
        // fallback 到复制删除方式
        copyTo(dstRes);
        if (!delete()) {
            Log.i(TAG, "delete src res " + toUri() + " failed ");
        }
    }

    public void copyTo(Resource dstRes) throws IOException {
        if (dstRes == null) {
            throw new IOException("copyTo failed, dstRes can not be null.");
        }
        if (!dstRes.canWrite()) {
            throw new IOException("copyTo failed, dstRes can not be written.");
        }
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = openInputStream();
            if (inputStream == null) {
                throw new IOException("copyTo failed, open inputStream get null by " + toUri());
            }
            outputStream = dstRes.openOutputStream(-1, false);
            if (outputStream == null) {
                throw new IOException(
                        "copyTo failed, open outputStream get null by " + dstRes.toUri());
            }
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } finally {
            FileUtils.closeQuietly(inputStream);
            FileUtils.closeQuietly(outputStream);
        }
    }

    public abstract ResourceInfo get();

    public ResourceInfo get(boolean recursive) {
        return null;
    }

    public abstract boolean delete() throws IOException;

    public List<ResourceInfo> list() {
        return null;
    }

    public boolean mkDir(boolean recursive) throws IOException {
        return false;
    }

    public boolean rmDir(boolean recursive) throws IOException {
        return false;
    }

    public boolean access() {
        return false;
    }

    public abstract File getUnderlyingFile();

    public abstract Uri getUnderlyingUri();

    public abstract ParcelFileDescriptor getParcelFileDescriptor() throws IOException;

    public abstract boolean canWrite();

    public String toUri() {
        return mUri;
    }

    public String getName() {
        File underlyingFile = getUnderlyingFile();
        if (underlyingFile != null) {
            return underlyingFile.getName();
        }
        Uri underlyingUri = getUnderlyingUri();
        if (underlyingUri != null) {
            return underlyingUri.getLastPathSegment();
        }
        return Uri.parse(mUri).getLastPathSegment();
    }
}
