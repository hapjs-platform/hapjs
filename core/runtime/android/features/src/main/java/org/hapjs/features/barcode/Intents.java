/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hapjs.features.barcode;

/**
 * This class provides the constants to use when sending an Intent to Barcode Scanner. These strings
 * are effectively API and cannot be changed.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class Intents {

    private Intents() {
    }

    public static final class Scan {

        /**
         * Send this intent to open the Barcodes app in scanning mode, find a barcode, and return the
         * results.
         */
        public static final String ACTION = "com.google.zxing.client.android.SCAN";

        /**
         * If a barcode is found, Barcodes returns {@link android.app.Activity#RESULT_OK} to {@link
         * android.app.Activity#onActivityResult(int, int, android.content.Intent)} of the app which
         * requested the scan via {@link
         * android.app.Activity#startActivityForResult(android.content.Intent, int)} The barcodes
         * contents can be retrieved with {@link android.content.Intent#getStringExtra(String)}. If the
         * user presses Back, the result code will be {@link android.app.Activity#RESULT_CANCELED}.
         */
        public static final String RESULT = "SCAN_RESULT";

        public static final String RESULT_TYPE = "RESULT_TYPE";

        public static final String RESULT_TYPE_TEXT = "1";

        private Scan() {
        }
    }
}
