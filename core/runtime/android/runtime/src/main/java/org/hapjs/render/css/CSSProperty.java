/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

// For inspector "style"
public interface CSSProperty {

    String UNDEFINED = "";

    String getNameWithState();

    String getNameWithoutState();

    Object getValue();

    String getState();

    // For inspector

    /**
     * @return inspector "name"
     */
    public String getInspectorName();

    /**
     * @return inspector "value"
     */
    public String getValueText(); // TODO rename to "getInspectorValue"

    /**
     * web 调试器可以选择禁用某一条样式
     *
     * @return inspector "disabled"
     */
    public boolean getDisabled();
}
