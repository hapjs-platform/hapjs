/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.net;

import android.net.Uri;
import okhttp3.MediaType;

public class FormFile {
    public static final String KEY_FILENAME = "filename";
    public static final String KEY_FILE_URI = "uri";
    public static final String KEY_FILE_TYPE = "type";
    public static final String KEY_FORM_NAME = "name";

    public static final String DEFAULT_FORM_NAME = "file";

    public String formName;
    public String fileName;
    public Uri uri;
    public MediaType mediaType;

    public FormFile(String formName, String fileName, Uri uri, MediaType mediaType) {
        this.formName = formName;
        this.fileName = fileName;
        this.uri = uri;
        this.mediaType = mediaType;
    }
}
