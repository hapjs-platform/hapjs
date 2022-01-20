/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.keyevent;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import org.hapjs.render.RootView;

public class KeyEventManager {

    private static final String TAG = "KeyEventManager";
    // 此常量用于区分系统产生的还是我们自己注入的，系统的source取值在InputDevice.java里有注释说明
    private static final int SOURCE_INJECT = 0x10000;
    private static int MAX_INTER_TIME = 100;
    private final Object mLock;
    private long mLastKeyEventTime = 0;

    private SparseArray<KeyEvent> mCacheKeyEventSparse;

    /**
     * key -->pageId value --> Set of hashcode
     */
    private SparseArray<Set<Integer>> mCacheHashCodes;

    private SparseArray<WeakReference<View>> mCacheHostViewSparse;

    private KeyEventManager() {
        mLock = new Object();
        mCacheKeyEventSparse = new SparseArray<>();
        mCacheHashCodes = new SparseArray<>();
        mCacheHostViewSparse = new SparseArray<>();
    }

    public static KeyEventManager getInstance() {
        return KeyEventManager.Holder.INSTANCE;
    }

    public static boolean isConfirmKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                return true;
            default:
                return false;
        }
    }

    public static boolean isInjectKeyEvent(KeyEvent keyEvent) {
        return keyEvent.getSource() == SOURCE_INJECT;
    }

    public void put(int hashcode, KeyEvent keyEvent, int pageId, View hostView) {
        synchronized (mLock) {
            mCacheHostViewSparse.put(hashcode, new WeakReference<>(hostView));
            mCacheKeyEventSparse.put(hashcode, keyEvent);
            Set<Integer> set = mCacheHashCodes.get(pageId);
            if (set == null) {
                set = new HashSet<>();
                mCacheHashCodes.put(pageId, set);
            }
            set.add(hashcode);
        }
    }

    public void remove(int hashcode) {
        synchronized (mLock) {
            mCacheKeyEventSparse.remove(hashcode);
            mCacheHostViewSparse.remove(hashcode);
        }
    }

    public boolean contains(int hashcode) {
        synchronized (mLock) {
            return mCacheKeyEventSparse.get(hashcode) != null;
        }
    }

    public KeyEvent get(int hashcode) {
        synchronized (mLock) {
            return mCacheKeyEventSparse.get(hashcode);
        }
    }

    public void clear() {
        synchronized (mLock) {
            mCacheKeyEventSparse.clear();
            mCacheHostViewSparse.clear();
        }
    }

    // clear all keyEvent of page
    public void clear(int pageId) {
        Set<Integer> set;
        synchronized (mLock) {
            set = mCacheHashCodes.get(pageId);
            if (set == null || set.size() < 1) {
                return;
            }
            for (Integer hashcode : set) {
                mCacheHashCodes.remove(hashcode);
            }
        }
        set.clear();
    }

    public boolean onDispatchKeyEvent(KeyEvent event) {
        if (event == null) {
            return false;
        }
        if (isInjectKeyEvent(event)) {
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN
                && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN
                || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP)) {
            long time = SystemClock.elapsedRealtime();
            if (time - mLastKeyEventTime < MAX_INTER_TIME) {
                Log.i(TAG, "Ignore multiple key events in a short time");
                return true;
            }
            mLastKeyEventTime = SystemClock.elapsedRealtime();
        }
        return false;
    }

    public void injectKeyEvent(boolean consumed, RootView rootView, int hashcode) {
        if (rootView == null) {
            return;
        }

        if (!consumed) {
            rootView.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            KeyEvent cacheEvent;
                            synchronized (mLock) {
                                cacheEvent = mCacheKeyEventSparse.get(hashcode);
                            }
                            // always happen in monkey test
                            // exit page before js invoke
                            if (cacheEvent == null) {
                                return;
                            }
                            Context context = rootView.getContext();
                            if (!(context instanceof Activity)) {
                                return;
                            }
                            Activity activity = (Activity) context;
                            if (activity.isDestroyed() || activity.isFinishing()) {
                                return;
                            }
                            if (!handleInputEventDefault(mCacheHostViewSparse.get(hashcode),
                                    cacheEvent)) {
                                cacheEvent.setSource(SOURCE_INJECT);
                                activity.getWindow().injectInputEvent(cacheEvent);
                            }
                        }
                    });
        } else {
            synchronized (mLock) {
                mCacheKeyEventSparse.remove(hashcode);
            }
        }
    }

    private boolean handleInputEventDefault(WeakReference<View> hostView, KeyEvent cacheEvent) {
        boolean handled = false;
        if (hostView == null || hostView.get() == null) {
            return false;
        }
        View targetView = hostView.get();
        if (targetView instanceof EditText) {
            EditText editText = (EditText) targetView;
            int keyCode = cacheEvent.getKeyCode();
            if (isConfirmKey(keyCode)) {
                InputMethodManager imm =
                        (InputMethodManager)
                                targetView.getContext()
                                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(targetView, InputMethodManager.SHOW_IMPLICIT);
                }
                handled = true;
            } else {
                if (TextUtils.isEmpty(editText.getText())) {
                    return false;
                }

                if (editText.getSelectionStart() == 0) {
                    handled |= (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT);
                } else if (editText.getSelectionStart() == editText.getText().length()) {
                    handled |= (keyCode == KeyEvent.KEYCODE_DPAD_LEFT);
                } else {
                    handled =
                            (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
                                    || (keyCode == KeyEvent.KEYCODE_DPAD_LEFT);
                }
            }
        }
        return handled;
    }

    private static class Holder {
        static final KeyEventManager INSTANCE = new KeyEventManager();
    }
}
