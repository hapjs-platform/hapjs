/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector.reflect;

public class Method {
    static {
        System.loadLibrary("inspector");
    }

    private long mMethod;

    public Method(String className, String name, String signature) {
        this(className, name, signature, false);
    }

    public Method(String className, String name, String signature, boolean isStatic) {
        mMethod = nativeGetMethod(className, name, signature, isStatic);
    }

    private static native long nativeGetMethod(
            String className, String methodName, String signature, boolean isStatic);

    private static native Object nativeInvokeObject(long method, Object receiver, Object[] args);

    private static native int nativeInvokeInt(long method, Object receiver, Object[] args);

    private static native void nativeInvokeVoid(long method, Object receiver, Object[] args);

    private static native long nativeInvokeLong(long method, Object receiver, Object[] args);

    private static native double nativeInvokeDouble(long method, Object receiver, Object[] args);

    private static native float nativeInvokeFloat(long method, Object receiver, Object[] args);

    private static native char nativeInvokeChar(long method, Object receiver, Object[] args);

    private static native byte nativeInvokeByte(long method, Object receiver, Object[] args);

    private static native short nativeInvokeShort(long method, Object receiver, Object[] args);

    private static native boolean nativeInvokeBoolean(long method, Object receiver, Object[] args);

    public Object invokeObject(Object receiver, Object... args) throws IllegalArgumentException {
        return nativeInvokeObject(mMethod, receiver, args);
    }

    public void invokeVoid(Object receiver, Object... args) throws IllegalArgumentException {
        nativeInvokeVoid(mMethod, receiver, args);
    }

    public int invokeInt(Object receiver, Object... args) throws IllegalArgumentException {
        return nativeInvokeInt(mMethod, receiver, args);
    }

    public long invokeLong(Object receiver, Object... args) throws IllegalArgumentException {
        return nativeInvokeLong(mMethod, receiver, args);
    }

    public double invokeDouble(Object receiver, Object... args) throws IllegalArgumentException {
        return nativeInvokeDouble(mMethod, receiver, args);
    }

    public float invokeFloat(Object receiver, Object... args) throws IllegalArgumentException {
        return nativeInvokeFloat(mMethod, receiver, args);
    }

    public char invokeChar(Object receiver, Object... args) throws IllegalArgumentException {
        return nativeInvokeChar(mMethod, receiver, args);
    }

    public byte invokeByte(Object receiver, Object... args) throws IllegalArgumentException {
        return nativeInvokeByte(mMethod, receiver, args);
    }

    public short invokeShort(Object receiver, Object... args) throws IllegalArgumentException {
        return nativeInvokeShort(mMethod, receiver, args);
    }
}
