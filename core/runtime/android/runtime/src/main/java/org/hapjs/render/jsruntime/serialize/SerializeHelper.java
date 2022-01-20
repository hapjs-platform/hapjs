/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.serialize;

import android.util.Log;
import java.text.DecimalFormat;

public class SerializeHelper {
    private static final String TAG = "SerializeHelper";

    public static Boolean toBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            String stringValue = (String) value;
            if ("true".equalsIgnoreCase(stringValue)) {
                return true;
            } else if ("false".equalsIgnoreCase(stringValue)) {
                return false;
            }
        }
        return defaultValue;
    }

    public static Double toDouble(Object value, double defaultValue) {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.valueOf((String) value);
            } catch (NumberFormatException ignored) {
                Log.e(TAG, "to double error", ignored);
            }
        }
        return defaultValue;
    }

    public static Integer toInteger(Object value, int defaultValue) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return (int) Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                Log.e(TAG, "to integer error", ignored);
            }
        }
        return defaultValue;
    }

    public static Long toLong(Object value, long defaultValue) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return (long) Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                Log.e(TAG, "to long error", ignored);
            }
        }
        return defaultValue;
    }

    public static String toString(Object value, String defaultValue) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Double || value instanceof Float) {
            DecimalFormat df = new DecimalFormat();
            df.setGroupingUsed(false);
            // java double to String largest lenght is 17,so setMaximumFractionDigits limit 17
            df.setMaximumFractionDigits(17);
            return df.format(value);
        } else if (value != null) {
            return String.valueOf(value);
        }
        return defaultValue;
    }
}
