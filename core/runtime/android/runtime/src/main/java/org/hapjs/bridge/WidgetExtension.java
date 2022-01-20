/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

public abstract class WidgetExtension extends AbstractExtension {
    @Override
    public ExtensionMetaData getMetaData() {
        return MetaDataSet.getInstance().getWidgetMetaData(getName());
    }
}
