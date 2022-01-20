/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.data.internal;

import java.util.Map;

public interface IStorage {
    String get(String key);

    boolean set(String key, String value);

    Map<String, String> entries();

    String key(int index);

    int length();

    boolean delete(String key);

    boolean clear();
}
