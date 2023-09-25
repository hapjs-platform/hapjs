/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */

package org.hapjs.render.jsruntime.serialize;

import java.nio.ByteBuffer;

public class TypedArrayProxy {
    private int type;
    private ByteBuffer buffer;
    private byte[] bytes;

    private TypedArrayProxy() {}

    public TypedArrayProxy(int type, byte[] bytes) {
        this.type = type;
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public ByteBuffer getBuffer() {
        if (buffer == null) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.rewind();
            buffer = byteBuffer;
        }
        return buffer;
    }
}
