/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileSource implements Source {
    private File mFile;

    public FileSource(File file) {
        mFile = file;
    }

    @Override
    public InputStream open() throws IOException {
        return new FileInputStream(mFile);
    }
}
