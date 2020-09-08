/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.bridge;

public interface FeatureInvokeListener {
    void invoke(String name, String action, Object rawParams, String jsCallback, int instanceId);
}
