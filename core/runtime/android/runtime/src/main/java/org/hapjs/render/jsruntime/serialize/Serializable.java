/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

public interface Serializable {
    int TYPE_JSON = 0;
    int TYPE_V8 = 1;

    int getType();
}
