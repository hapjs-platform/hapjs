/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.nfc;

import org.hapjs.bridge.Response;

public class NFCConstants {

    // 成功
    public static final int CODE_SUCCESS = 0;

    public static final int CODE_SERVICE_UNAVAILABLE = Response.CODE_SERVICE_UNAVAILABLE;
    public static final String DESC_SERVICE_UNAVAILABLE = "no such instance";

    // 设备不支持NFC
    public static final int CODE_NOT_SUPPORT_NFC = 10000;
    public static final String DESC_NOT_SUPPORT_NFC = "not support nfc";

    // 系统NFC开关未打开
    public static final int CODE_NOT_OPEN_NFC = 10001;
    public static final String DESC_NOT_OPEN_NFC = "not open nfc";

    // 未知错误
    public static final int CODE_UNKNOWN_ERROR = 10010;
    public static final String DESC_UNKNOWN_ERROR = "unknown error";

    // 参数无效
    public static final int CODE_INVALID_PARAMETER = 10011;
    public static final String DESC_INVALID_PARAMETER = "invalid parameter";

    // 将参数解析为NdefMessage失败
    public static final int CODE_PARSE_NDEF_MESSAGE_FAILED = 10012;
    public static final String DESC_PARSE_NDEF_MESSAGE_FAILED = "parse ndef nessage failed";

    // 未扫描到NFC标签
    public static final int CODE_NO_DISCOVERED_TAG = 10013;
    public static final String DESC_NO_DISCOVERED_TAG = "no discovered tag";

    // 连接失败
    public static final int CODE_CONNECT_FAILED = 10014;
    public static final String DESC_CONNECT_FAILED = "connect failed";

    // 相关读写操作失败
    public static final int CODE_SYSTEM_INTERNAL_ERROR = 10015;
    public static final String DESC_SYSTEM_INTERNAL_ERROR = "system internal error";

    // 未连接
    public static final int CODE_TECH_HAS_NOT_CONNECTED = 10016;
    public static final String DESC_TECH_HAS_NOT_CONNECTED = "tech has not connected";

    // 当前标签技术不支持该功能
    public static final int CODE_FUNCTION_NOT_SUPPORT = 10017;
    public static final String DESC_FUNCTION_NOT_SUPPORT = "function not support";

    // 容量不够
    public static final int CODE_INSUFFICIENT_STORAGE_CAPACITY = 10018;
    public static final String DESC_INSUFFICIENT_STORAGE_CAPACITY = "insufficient storage capacity";


}
