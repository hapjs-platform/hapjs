/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import java.util.Map;

public class ComponentAction implements RenderAction {

    public String component;
    public int ref;
    public String method;
    public Map<String, Object> args;

    @Override
    public String toString() {
        return "component:" + component + ", ref:" + ref + ", method:" + method + ", args:" + args;
    }
}
