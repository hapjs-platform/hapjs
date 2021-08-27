/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

public interface FitWidescreenProvider {
    String NAME = "FitWidthScreenProvider";

    String getFitMode(String packageName, String fitMode);
}
