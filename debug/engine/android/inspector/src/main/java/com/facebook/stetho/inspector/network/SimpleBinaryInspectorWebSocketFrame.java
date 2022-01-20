/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.network;

import java.io.UnsupportedEncodingException;

public class SimpleBinaryInspectorWebSocketFrame
        implements NetworkEventReporter.InspectorWebSocketFrame {
    private final String mRequestId;
    private final byte[] mPayload;

    public SimpleBinaryInspectorWebSocketFrame(String requestId, byte[] payload) {
        mRequestId = requestId;
        mPayload = payload;
    }

    @Override
    public String requestId() {
        return mRequestId;
    }

    @Override
    public int opcode() {
        return OPCODE_BINARY;
    }

    @Override
    public boolean mask() {
        return false;
    }

    @Override
    public String payloadData() {
        try {
            // LOL, yes this is really how Chrome does it too...
            return new String(mPayload, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
