/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api.debug;

import android.content.Context;
import android.content.Intent;

public interface CardDebugController {

    String ACTION_DEBUG_CARD = "org.hapjs.intent.action.DEBUG_CARD";
    String ACTION_CARD_DEBUG_RESULT = "org.hapjs.intent.action.CARD_DEBUG_RESULT";
    String PERMISSION_DEBUG_CARD = "org.hapjs.permission.DEBUG_CARD";

    int MSG_IS_SUPPORT = 1;
    int MSG_LAUNCH_CARD = 2;
    int MSG_DEBUG_CARD = 3;

    String EXTRA_CARD_URL = "url";
    String EXTRA_SERVER = "server";
    String EXTRA_RESULT = "result";
    String EXTRA_ERROR_CODE = "errorCode";
    String EXTRA_SHOULD_RELOAD = "shouldReload";
    String EXTRA_USE_ADB = "useADB";
    String EXTRA_SERIAL_NUMBER = "serialNumber";
    String EXTRA_PLATFORM_VERSION_CODE = "platformVersionCode";
    String EXTRA_WAIT_DEVTOOLS = "waitDevTools";
    String EXTRA_MESSAGE_CODE = "messageCode";
    String EXTRA_IS_SUPPORTED = "isSupported";
    String EXTRA_FROM = "from";
    String EXTRA_ARCHIVE_HOST = "archiveHost";
    String EXTRA_RUNTIME_HOST = "runtimeHost";

    int CODE_UNKNOWN_ERROR = 0;
    int CODE_UNKNOWN_MESSAGE = 1;
    int CODE_UNSUPPORT_DEBUG = 2;
    int CODE_UNSUPPORT_CARD = 3;

    void handleDebugMessage(Context context, Intent intent);

    void setCardDebugHost(CardDebugHost host);
}
