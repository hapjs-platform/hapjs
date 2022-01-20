/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime.resource;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;

public interface ResourceManager {
    @Nullable
    Uri getResource(String resourcePath);

    @Nullable
    Uri getResource(String resourcePath, String page);

    long size(Context context);
}
