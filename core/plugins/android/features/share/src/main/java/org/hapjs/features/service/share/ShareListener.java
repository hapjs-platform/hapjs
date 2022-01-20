/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.share;

public interface ShareListener {
    void onStart(Platform media);

    void onResult(Platform media);

    void onError(Platform media, String message);

    void onCancel(Platform media);
}