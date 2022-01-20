/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import java.util.ArrayList;
import java.util.List;

public class RenderActionPackage {

    public static final int TYPE_DEFAULT = 0;
    public static final int TYPE_PRE_CREATE_BODY = 1;

    public final int pageId;
    public final int type;

    public List<RenderAction> renderActionList = new ArrayList<>();

    public RenderActionPackage(int pageId) {
        this(pageId, TYPE_DEFAULT);
    }

    public RenderActionPackage(int pageId, int type) {
        this.pageId = pageId;
        this.type = type;
    }
}
