/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.media;

public interface CompareOperator {
    int MEDIA_OP_MORE = 0;
    int MEDIA_OP_LESS = 1;
    int MEDIA_OP_MORE_EQUAL = 2;
    int MEDIA_OP_LESS_EQUAL = 3;
    int MEDIA_OP_EQUAL = 4;

    boolean compare(Object value);
}
