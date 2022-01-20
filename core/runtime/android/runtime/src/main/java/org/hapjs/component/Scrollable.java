/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

public interface Scrollable {

    void bindAppearEvent(Component component);

    void bindDisappearEvent(Component component);

    void unbindAppearEvent(Component component);

    void unbindDisappearEvent(Component component);

    void processAppearanceEvent();

    void processAppearanceOneEvent(Component component);

    void addSubScrollable(Scrollable subScrollable);

    void removeSubScrollable(Scrollable subScrollable);
}
