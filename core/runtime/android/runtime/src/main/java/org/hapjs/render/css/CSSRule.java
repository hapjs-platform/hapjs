/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

public abstract class CSSRule {
    static final short UNKNOWN_RULE = 0;
    static final short STYLE_RULE = 1;
    static final short FONT_FACE_RULE = 2;
    static final short KEYFRAME_RULE = 3;
    static final short MEDIA_RULE = 4;

    public abstract int getType();
}
