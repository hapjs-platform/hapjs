/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector.reflect;

public class Field {
    static {
        System.loadLibrary("inspector");
    }

    private long mFieldId;

    public Field(String className, String name, String signature) {
        this(className, name, signature, false);
    }

    public Field(String className, String name, String signature, boolean isStatic) {
        mFieldId = nativeGetField(className, name, signature, isStatic);
    }

    private static native long nativeGetField(
            String className, String name, String signature, boolean isStatic);

    private static native Object nativeGetObject(long fieldId, Object thiz);

    private static native void nativeSetObject(long fieldId, Object thiz, Object value);

    private static native int nativeGetInt(long fieldId, Object thiz);

    private static native void nativeSetInt(long fieldId, Object thiz, int value);

    private static native long nativeGetLong(long fieldId, Object thiz);

    private static native void nativeSetLong(long fieldId, Object thiz, long value);

    private static native double nativeGetDouble(long fieldId, Object thiz);

    private static native void nativeSetDouble(long fieldId, Object thiz, double value);

    private static native float nativeGetFloat(long fieldId, Object thiz);

    private static native void nativeSetFloat(long fieldId, Object thiz, float value);

    private static native char nativeGetChar(long fieldId, Object thiz);

    private static native void nativeSetChar(long fieldId, Object thiz, char value);

    private static native byte nativeGetByte(long fieldId, Object thiz);

    private static native void nativeSetByte(long fieldId, Object thiz, byte value);

    private static native short nativeGetShort(long fieldId, Object thiz);

    private static native void nativeSetShort(long fieldId, Object thiz, short value);

    private static native boolean nativeGetBoolean(long fieldId, Object thiz);

    private static native void nativeSetBoolean(long fieldId, Object thiz, boolean value);

    public Object get(Object thiz) {
        return nativeGetObject(mFieldId, thiz);
    }

    public void set(Object thiz, Object value) {
        nativeSetObject(mFieldId, thiz, value);
    }

    public int getInt(Object thiz) {
        return nativeGetInt(mFieldId, thiz);
    }

    public void setInt(Object thiz, int value) {
        nativeSetInt(mFieldId, thiz, value);
    }

    public long getLong(Object thiz) {
        return nativeGetLong(mFieldId, thiz);
    }

    public void setLong(Object thiz, long value) {
        nativeSetLong(mFieldId, thiz, value);
    }

    public double getDouble(Object thiz) {
        return nativeGetDouble(mFieldId, thiz);
    }

    public void setDouble(Object thiz, double value) {
        nativeSetDouble(mFieldId, thiz, value);
    }

    public float getFloat(Object thiz) {
        return nativeGetFloat(mFieldId, thiz);
    }

    public void setFloat(Object thiz, float value) {
        nativeSetFloat(mFieldId, thiz, value);
    }

    public char getChar(Object thiz) {
        return nativeGetChar(mFieldId, thiz);
    }

    public void setChar(Object thiz, char value) {
        nativeSetChar(mFieldId, thiz, value);
    }

    public byte getByte(Object thiz) {
        return nativeGetByte(mFieldId, thiz);
    }

    public void setByte(Object thiz, byte value) {
        nativeSetByte(mFieldId, thiz, value);
    }

    public short getShort(Object thiz) {
        return nativeGetShort(mFieldId, thiz);
    }

    public void setShort(Object thiz, short value) {
        nativeSetShort(mFieldId, thiz, value);
    }

    public boolean getBoolean(Object thiz) {
        return nativeGetBoolean(mFieldId, thiz);
    }

    public void setBoolean(Object thiz, boolean value) {
        nativeSetBoolean(mFieldId, thiz, value);
    }
}
