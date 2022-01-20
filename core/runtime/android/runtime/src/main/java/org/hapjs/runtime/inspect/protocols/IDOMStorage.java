/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime.inspect.protocols;

import java.util.Map;

public interface IDOMStorage {

    Map<String, String> entries();

    String getItem(String key);

    void setItem(String key, String value);

    void removeItem(String key);

    void clear();
}
