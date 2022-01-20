/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage.file;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;

public interface IResourceFactory {
    Resource create(String internalUri);

    Resource create(File underlyingFile);

    Resource create(Uri underlyingUri, boolean needFileName);

    Resource create(ParcelFileDescriptor parcelFileDescriptor);
}
