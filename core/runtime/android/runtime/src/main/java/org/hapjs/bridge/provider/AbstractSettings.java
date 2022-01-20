/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.provider;

public abstract class AbstractSettings implements Settings {
    @Override
    public boolean getBoolean(String name, boolean def) {
        String value = getValue(name);
        if (value == null) {
            return def;
        } else {
            return Boolean.parseBoolean(value);
        }
    }

    @Override
    public float getFloat(String name, float def) {
        String value = getValue(name);
        if (value == null) {
            return def;
        } else {
            return Float.parseFloat(value);
        }
    }

    @Override
    public int getInt(String name, int def) {
        String value = getValue(name);
        if (value == null) {
            return def;
        } else {
            return Integer.parseInt(value);
        }
    }

    @Override
    public long getLong(String name, long def) {
        String value = getValue(name);
        if (value == null) {
            return def;
        } else {
            return Long.parseLong(value);
        }
    }

    @Override
    public String getString(String name, String def) {
        String value = getValue(name);
        if (value == null) {
            value = def;
        }
        return value;
    }

    @Override
    public boolean putBoolean(String name, boolean value) {
        return putValue(name, String.valueOf(value));
    }

    @Override
    public boolean putFloat(String name, float value) {
        return putValue(name, String.valueOf(value));
    }

    @Override
    public boolean putInt(String name, int value) {
        return putValue(name, String.valueOf(value));
    }

    @Override
    public boolean putLong(String name, long value) {
        return putValue(name, String.valueOf(value));
    }

    @Override
    public boolean putString(String name, String value) {
        return putValue(name, value);
    }

    protected abstract String getValue(String name);

    protected abstract boolean putValue(String name, String value);
}
