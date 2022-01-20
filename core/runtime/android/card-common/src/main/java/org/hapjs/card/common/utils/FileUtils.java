/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class FileUtils {
    private static final String TAG = "FileUtils";

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
                if (input == null) {
                    return stream.toByteArray();
                }
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
}
