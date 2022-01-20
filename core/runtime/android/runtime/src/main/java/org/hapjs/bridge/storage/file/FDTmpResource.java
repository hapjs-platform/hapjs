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

/**
 * 目前FD类型的resource不能支持Get操作,也不能支持写操作
 * 除了能够copy以外,其他什么干不了,目前只有原生应用与快应用通信的过程中,native向原生应用发送文件时会产生这种Resource
 */
public class FDTmpResource extends TmpResource {
    private ParcelFileDescriptor mFileDescriptor;

    public FDTmpResource(String internalUri, ParcelFileDescriptor fileDescriptor) {
        super(internalUri);
        mFileDescriptor = fileDescriptor;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new FileInputStream(mFileDescriptor.getFileDescriptor());
    }

    @Override
    public ResourceInfo get() {
        return null;
    }

    @Override
    public File getUnderlyingFile() {
        return null;
    }

    @Override
    public Uri getUnderlyingUri() {
        return null;
    }

    @Override
    public ParcelFileDescriptor getParcelFileDescriptor() throws IOException {
        return mFileDescriptor;
    }
}
