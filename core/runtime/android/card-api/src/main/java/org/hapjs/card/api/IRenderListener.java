/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

public interface IRenderListener {
    void onRenderSuccess();

    /*
     * use onRenderFailed instead.
     */
    @Deprecated
    void onRenderException(int errorCode, String message);

    boolean onRenderFailed(int errorCode, String message);

    boolean onRenderProgress();

    class ErrorCode {
        public static final int ERROR_UNKNOWN = 1000;
        public static final int ERROR_INITIAL = 1001;
        public static final int ERROR_URL = 1002;
        public static final int ERROR_FILE_NOT_FOUND = 1003;
        public static final int ERROR_INSTALL_FAILED = 1004;
        public static final int ERROR_PAGE_NOT_FOUND = 1005;
        public static final int ERROR_INCOMPATIBLE = 1006;
        public static final int ERROR_INSPECTOR_UNREADY = 1007;

        private ErrorCode() {
        }
    }
}
