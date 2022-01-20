/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

public class DigestOutputStream extends OutputStream {

    private MessageDigest mMessageDigest;

    public DigestOutputStream(MessageDigest messageDigest) {
        this.mMessageDigest = messageDigest;
    }

    @Override
    public void write(int b) throws IOException {
        mMessageDigest.update((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        mMessageDigest.update(b, off, len);
    }
}
