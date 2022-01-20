/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.skeleton;

public interface SkeletonProvider {
    String NAME = "skeleton_provider";
    String HIDE_SOURCE_NATIVE = "native";
    String HIDE_SOURCE_CP = "cp";

    boolean isSkeletonEnable(String packageName);
}
