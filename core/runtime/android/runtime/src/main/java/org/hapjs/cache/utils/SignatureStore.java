/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.hapjs.common.utils.FileUtils;

public class SignatureStore {
    public static byte[] load(File file) throws IOException {
        return FileUtils.readFileAsBytes(file.getAbsolutePath());
    }

    public static byte[] load(InputStream inputStream) throws IOException {
        return FileUtils.readStreamAsBytes(inputStream, 0, true);
    }

    public static boolean save(byte[] signature, File file) {
        return FileUtils.saveToFile(signature, file);
    }

    public static boolean exist(File file) {
        return file.exists() && file.length() > 0;
    }

    public static boolean match(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }
}
