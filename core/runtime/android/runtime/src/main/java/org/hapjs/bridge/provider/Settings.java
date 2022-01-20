/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.provider;

public interface Settings {
    boolean getBoolean(String name, boolean def);

    float getFloat(String name, float def);

    int getInt(String name, int def);

    long getLong(String name, long def);

    String getString(String name, String def);

    boolean putBoolean(String name, boolean value);

    boolean putFloat(String name, float value);

    boolean putInt(String name, int value);

    boolean putLong(String name, long value);

    boolean putString(String name, String value);
}
