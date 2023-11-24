/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debug;

import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.runtime.PermissionChecker;

public abstract class DebugHandler extends Handler {
    private static final String TAG = "DebugHandler";

    // Message must been removed before handleMessage() finished
    // for it will be recycled after that time
    private Map<Message, Integer> mMessageUidMap;
    private Set<Integer> mGrantedUIDs;
    private Context mContext;

    public DebugHandler(Context context, Looper looper) {
        super(looper);
        mContext = context;
        if (Build.VERSION.SDK_INT < 21) {
            mMessageUidMap = new ConcurrentHashMap<>();
        }
        mGrantedUIDs = new HashSet<>();
    }

    @Override
    public void handleMessage(Message msg) {
        int uid = getCallingUid(msg);
        if (uid < 0) {
            Log.e(TAG, "Fail to get calling uid");
            return;
        }

        RuntimeLogManager.getDefault().logExternalCall(mContext, uid, getClass());
//        if (!verifySignature(uid)) {
//            Log.e(TAG, "Received ungranted request");
//            return;
//        }

        onHandleMessage(msg);
    }

    public abstract void onHandleMessage(Message message);

    @Override
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        // sendMessageAtTime() is called earlier than handleMessage()
        // save uid here for later usage in handleMessage()
        if (Build.VERSION.SDK_INT < 21) {
            mMessageUidMap.put(msg, Binder.getCallingUid());
        }
        return super.sendMessageAtTime(msg, uptimeMillis);
    }

    private int getCallingUid(Message msg) {
        if (Build.VERSION.SDK_INT >= 21) {
            return msg.sendingUid;
        } else {
            // Remove msg from map to keep map small
            Integer uid = mMessageUidMap.remove(msg);
            if (uid != null) {
                return uid.intValue();
            } else {
                return -1;
            }
        }
    }

    private boolean verifySignature(int uid) {
        if (mGrantedUIDs.contains(uid)) {
            return true;
        }

        boolean matched = PermissionChecker.verify(mContext, uid);
        if (matched) {
            mGrantedUIDs.add(uid);
        }
        return matched;
    }
}
