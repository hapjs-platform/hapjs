/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import android.net.Uri;
import org.hapjs.runtime.Runtime;
import org.hapjs.runtime.resource.CacheProviderContracts;

public class DefaultFileNotFoundHandler implements Cache.FileNotFoundHandler {

    @Override
    public Uri handleFileNotFound(String pkg, String resourcePath) {
        Context context = Runtime.getInstance().getContext();
        return CacheProviderContracts.getResource(context.getPackageName(), pkg, resourcePath);
    }
}
