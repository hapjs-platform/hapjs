/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import java.util.concurrent.Executor;

public class ExtensionManagerHelper {
    public static ExtensionManager.AsyncInvocation newAsyncInvocation(
            ExtensionManager extensionManager,
            FeatureExtension feature,
            Request request,
            Executor executor) {
        return extensionManager.new AsyncInvocation(feature, request, executor);
    }
}
