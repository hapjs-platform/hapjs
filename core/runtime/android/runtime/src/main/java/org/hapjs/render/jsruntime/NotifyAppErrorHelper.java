/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

public class NotifyAppErrorHelper {
    private static final String MESSAGE_TAG = "$INTERRUPTION$:";
    private static final String ESCAPED_MESSAGE_TAG = "\\$INTERRUPTION\\$:";

    public static boolean isExceptionFromOnError(String errMsg) {
        return hasTagInFirstLine(errMsg);
    }

    private static boolean hasTagInFirstLine(String text) {
        if (text != null) {
            int firstLineEnd = text.indexOf("\n");
            if (firstLineEnd == -1) {
                firstLineEnd = text.length();
            }
            int tagIndex = text.substring(0, firstLineEnd).indexOf(MESSAGE_TAG);
            if (tagIndex != -1) {
                return true;
            }
        }
        return false;
    }

    public static String removeMessageTag(String text) {
        if (hasTagInFirstLine(text)) {
            text = text.replaceFirst(ESCAPED_MESSAGE_TAG, "");
        }
        return text;
    }

    public static String generateScript(int appId, String rawMsg, String rawStack) {
        String escapedMsg = escapeExceptionInfo(removeMessageTag(rawMsg));
        String escapedStack = escapeExceptionInfo(removeMessageTag(rawStack));
        return "if(global.notifyAppError){"
                + "notifyAppError("
                + appId
                + ","
                + "{"
                + "message: '"
                + escapedMsg
                + "',"
                + "stack: '"
                + escapedStack
                + "'"
                + "});"
                + "}";
    }

    private static String escapeExceptionInfo(String text) {
        if (text != null) {
            // 必须escape换行符号, 否则js编译出错(抛V8ScriptCompilationException).
            text =
                    text.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "\\r")
                            .replace("\n", "\\n");
        }
        return text;
    }
}
