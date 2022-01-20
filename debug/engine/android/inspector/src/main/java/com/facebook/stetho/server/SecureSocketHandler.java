/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.server;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Credentials;
import android.util.Log;
import com.facebook.stetho.common.LogUtil;
import java.io.IOException;
import org.hapjs.debug.log.DebuggerLogUtil;

public abstract class SecureSocketHandler implements SocketHandler {
    private final Context mContext;

    public SecureSocketHandler(Context context) {
        mContext = context;
    }

    private static void enforcePermission(Context context, AbsSocket peer)
            throws IOException, PeerAuthorizationException {
        Credentials credentials = peer.getPeerCredentials();

        if (credentials == null) {
            return;
        }

        int uid = credentials.getUid();
        int pid = credentials.getPid();

        if (LogUtil.isLoggable(Log.VERBOSE)) {
            LogUtil.v("Got request from uid=%d, pid=%d", uid, pid);
        }

        String requiredPermission = Manifest.permission.DUMP;
        int checkResult = context.checkPermission(requiredPermission, pid, uid);
        if (checkResult != PackageManager.PERMISSION_GRANTED) {
            throw new PeerAuthorizationException(
                    "Peer pid=" + pid + ", uid=" + uid + " does not have " + requiredPermission);
        }
    }

    @Override
    public final void onAccepted(AbsSocket socket) throws IOException {
        try {
            DebuggerLogUtil.logBreadcrumb("SecureSocketHandler.onAccepted");
            enforcePermission(mContext, socket);
            onSecured(socket);
        } catch (PeerAuthorizationException e) {
            LogUtil.e("Unauthorized request: " + e.getMessage());
            DebuggerLogUtil.logBreadcrumb("authorization fail");
            DebuggerLogUtil.logException(e);
        }
    }

    protected abstract void onSecured(AbsSocket socket) throws IOException;
}
