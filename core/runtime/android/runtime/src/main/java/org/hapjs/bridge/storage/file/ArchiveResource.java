/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage.file;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.bridge.ApplicationContext;

public class ArchiveResource extends Resource {
    private File mFile;
    private ApplicationContext mApplicationContext;

    public ArchiveResource(ApplicationContext applicationContext, String internalUri, File file) {
        super(internalUri);
        mFile = file;
        mApplicationContext = applicationContext;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new FileInputStream(mFile);
    }

    @Override
    public OutputStream openOutputStream(long position, boolean append) throws IOException {
        return null;
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
    public boolean access() {
        return mFile.exists();
    }

    @Override
    public boolean delete() throws IOException {
        throw new IOException("can not delete archive resource");
    }

    @Override
    public boolean canWrite() {
        return false;
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
