/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */

package org.hapjs.render.jsruntime.serialize;

import java.nio.ByteBuffer;

public class TypedArrayProxy {
    private int type;
    private ByteBuffer buffer;

    public TypedArrayProxy(int type, ByteBuffer buffer) {
        this.type = type;
        this.buffer = buffer;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
