/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import java.util.Map;
import java.util.Set;

public interface OnDomDataChangeListener {

    void onAttrsChange(Component component, Map<String, Object> attrs);

    void onStylesChange(Component component, Map<String, ? extends Object> attrs);

    void onEventsChange(Component component, Set<String> events, boolean add);
}
