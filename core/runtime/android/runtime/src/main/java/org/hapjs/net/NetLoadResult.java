/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.net;

public class NetLoadResult<T> {
    public static final int INVALID = -1;
    public static final int SUCCESS = 0;
    // 网络异常，与服务器未连接上
    public static final int ERR_NETWORK = -2;
    // 服务器异常，http code 非200 或者 result为false
    public static final int ERR_SERVER = -3;
    // 解析异常，服务器数据有问题或者本地数据解析有问题
    public static final int ERR_IO = -5;
    // 数据解密异常
    public static final int ERR_DECRYPT = -6;

    private int mResultCode = INVALID;
    private Exception mException;
    private T mT = null;
    private String mOriginData;

    public NetLoadResult() {
    }

    public int getResultCode() {
        return mResultCode;
    }

    public void setResultCode(int resultCode) {
        mResultCode = resultCode;
    }

    public Exception getException() {
        return mException;
    }

    public void setException(Exception exception) {
        mException = exception;
    }

    public T getData() {
        return mT;
    }

    public void setData(T t) {
        mT = t;
    }

    public String getOriginData() {
        return mOriginData;
    }

    public void setOriginData(String originData) {
        mOriginData = originData;
    }
}
