/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.bluetooth.data;

import java.util.UUID;

public class BleConst {

    public static final UUID DESCRIPTOR_NOTIFY =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int CODE_SUCCESS = 0;

    public static final int CODE_NOT_INIT = 10000;

    public static final int CODE_NOT_AVAILABLE = 10001;

    public static final int CODE_NO_DEVICE = 10002;

    public static final int CODE_CONNECTION_FAIL = 10003;

    public static final int CODE_NO_SERVICE = 10004;

    public static final int CODE_NO_CHARACTERISTIC = 10005;

    public static final int CODE_NO_CONNECTION = 10006;

    public static final int CODE_PROPERTY_NOT_SUPPORT = 10007;

    public static final int CODE_SYSTEM_ERROR = 10008;

    public static final int CODE_SYSTEM_NOT_SUPPORT = 10009;

    public static final int CODE_LOCATION_NOT_TURNED = 10010;

    public static final String MSG_NOT_INIT = "not init";

    public static final String MSG_NOT_AVAILABLE = "not available";

    public static final String MSG_NO_DEVICE = "no device";

    public static final String MSG_CONNECTION_FAIL = "connection fail";

    public static final String MSG_NO_SERVICE = "no service";

    public static final String MSG_NO_CHARACTERISTIC = "no characteristic";

    public static final String MSG_NO_CONNECTION = "no connection";

    public static final String MSG_PROPERTY_NOT_SUPPORT = "property not support";

    public static final String MSG_SYSTEM_ERROR = "system error";

    public static final String MSG_SYSTEM_NOT_SUPPORT = "system not support";

    public static final String MSG_LOCATION_NOT_TURNED = "location not turned on";

    public static final ScanOperateResult SCAN_SUCCESS =
            new ScanOperateResult(BleConst.CODE_SUCCESS, "SUCCESS");
}
