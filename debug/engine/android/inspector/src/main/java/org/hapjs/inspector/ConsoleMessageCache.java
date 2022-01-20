/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

public class ConsoleMessageCache {
    private static final int SIZE_SHIFT = 20; // 1MB
    private static final int SIZE_MASK = (1 << SIZE_SHIFT) - 1;
    private static final int MAX_BUFFER = (1 << SIZE_SHIFT);
    private byte[] mBuffer = new byte[MAX_BUFFER];
    private int mLast = 0;
    private int mTop = 0;

    public ConsoleMessageCache() {
    }

    private void push(byte b) {
        mBuffer[mLast & SIZE_MASK] = b;
        mLast++;
    }

    public void clear() {
        mTop = 0;
        mLast = 0;
    }

    public void append(int level, String msg) {
        int oldLast = mLast;

        try {
            byte[] bytes = msg.getBytes("UTF-8");
            int length = ((bytes.length + 2) & 0xffff); // level is one size
            length -= 2;

            int len = (mLast + length) - MAX_BUFFER;
            if (len > 0) {
                System.arraycopy(bytes, 0, mBuffer, mLast, length - len);
                mLast = 0;
            } else {
                len = length;
            }
            System.arraycopy(bytes, length - len, mBuffer, mLast, len);
            mLast += len;
            push((byte) ((level + 1) & 0xff));
            push((byte) 0); // end

            if (oldLast > mLast || oldLast < mTop) {
                mTop = (mLast + 1) & SIZE_MASK;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getLength(int top) {
        int s = top;
        while (top != mLast && mBuffer[top] != 0) {
            top = (top + 1) & SIZE_MASK;
        }
        if (top == s) {
            return 0;
        }
        return ((top + 1 + MAX_BUFFER) - s) & SIZE_MASK;
    }

    public long begin() {

        int top = mTop;
        int len = 0;

        while (len <= 2 && top != mLast) {
            top = (top + len) & SIZE_MASK;
            len = getLength(top);
            if (len == 0) {
                break;
            }
        }

        return top | (((long) len) << 32);
    }

    public long next(long info) {
        int top = (int) (info & 0xffffffff);
        int len = (int) (info >> 32);

        top = (top + len) & SIZE_MASK;
        if (top == mLast) {
            return (long) top;
        }

        len = getLength(top);

        return top | (((long) len) << 32);
    }

    public boolean isEnd(long info) {
        int top = (int) (info & 0xffffffff);
        int len = (int) (info >> 32);
        return (top == mLast) || (len == 0);
    }

    public int getLevel(long info) {
        int top = (int) (info & 0xffffffff);
        int len = (int) (info >> 32);

        return mBuffer[(top + len - 2) & SIZE_MASK] - 1;
    }

    public String getMessage(long info) {
        int top = (int) (info & 0xffffffff);
        int len = (int) (info >> 32);
        try {
            return new String(mBuffer, top, len - 2, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
