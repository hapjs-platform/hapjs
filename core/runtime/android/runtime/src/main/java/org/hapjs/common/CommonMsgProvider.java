/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common;

public interface CommonMsgProvider {

    String NAME = "CommonMsgProvider";

    void onCommonMsg(String key, Object... args);
}
