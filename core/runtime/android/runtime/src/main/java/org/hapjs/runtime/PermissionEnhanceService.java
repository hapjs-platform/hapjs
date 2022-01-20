/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.logging.RuntimeLogManager;

public abstract class PermissionEnhanceService extends Service {
    private static final String TAG = "PermissionEnhanceSv";
    protected final PermissionEnhanceHandler mHandler;
    protected final Messenger mMessenger;
    protected HandlerThread mHandlerThread;

    public PermissionEnhanceService() {
        this(false);
    }

    public PermissionEnhanceService(boolean runOnWorkerThread) {
        if (runOnWorkerThread) {
            mHandlerThread = new HandlerThread("PermissionEnhanceThread");
            mHandlerThread.start();
            mHandler = new PermissionEnhanceHandler(mHandlerThread.getLooper());
        } else {
            mHandler = new PermissionEnhanceHandler();
        }
        mMessenger = new Messenger(mHandler);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
    }

    protected abstract void onInvokeAccepted(Message msg);

    protected abstract void onInvokeRejected(Message msg);

    public class PermissionEnhanceHandler extends Handler {

        private Map<Message, Integer> mMessageUidMap;

        public PermissionEnhanceHandler() {
            this(Looper.myLooper());
        }

        public PermissionEnhanceHandler(Looper looper) {
            super(looper);
            if (Build.VERSION.SDK_INT < 21) {
                mMessageUidMap = new ConcurrentHashMap<>();
            }
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

        @Override
        public void handleMessage(Message msg) {
            int callingUid = getCallingUid(msg);
            if (callingUid < 0) {
                Log.e(TAG, "Fail to get calling uid");
                return;
            }

            RuntimeLogManager.getDefault()
                    .logExternalCall(
                            getApplicationContext(), callingUid,
                            PermissionEnhanceService.this.getClass());
            if (PermissionChecker.verify(PermissionEnhanceService.this, callingUid)) {
                onInvokeAccepted(msg);
            } else {
                onInvokeRejected(msg);
            }
        }

        @Override
        public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
            // sendMessageAtTime() is called earlier than handleMessage()
            // save uid here for later usage in handleMessage()
            if (Build.VERSION.SDK_INT < 21) {
                mMessageUidMap.put(msg, Binder.getCallingUid());
            }
            return super.sendMessageAtTime(msg, uptimeMillis);
        }
    }
}
