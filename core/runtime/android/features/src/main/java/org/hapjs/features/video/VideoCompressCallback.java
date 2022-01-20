/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.video;

public interface VideoCompressCallback {
    void notifyComplete(VideoCompressTask curVideoCompressTask, boolean isSuccess);

    void notifyAbort(VideoCompressTask curVideoCompressTask);
}
