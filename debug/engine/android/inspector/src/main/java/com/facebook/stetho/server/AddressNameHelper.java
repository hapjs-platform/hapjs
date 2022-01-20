/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.server;

import com.facebook.stetho.common.ProcessUtil;
import org.hapjs.inspector.V8Inspector;

public class AddressNameHelper {
    private static final String PREFIX = "hybrid_";

    public static String createCustomAddress(String suffix) {
        if (V8Inspector.getInstance().useLocalSocket()) {
            return PREFIX + ProcessUtil.getProcessName() + suffix;
        } else {
            String addr = V8Inspector.getInstance().getRemoteAddr();
            if (addr.length() <= 0) {
                return "socket://0";
            } else {
                return "socket://" + addr;
            }
        }
    }
}
