/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage.file;

import java.io.IOException;
import java.io.OutputStream;

public abstract class TmpResource extends Resource {

    TmpResource(String internalUri) {
        super(internalUri);
    }

    @Override
    public final OutputStream openOutputStream(long position, boolean append) throws IOException {
        return null;
    }

    @Override
    public final boolean delete() throws IOException {
        throw new IOException("Can not delete a tmp resource.");
    }

    @Override
    public final boolean canWrite() {
        return false;
    }
}
