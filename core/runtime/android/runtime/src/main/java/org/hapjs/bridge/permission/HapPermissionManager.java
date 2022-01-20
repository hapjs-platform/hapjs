/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.permission;

import android.util.Log;
import java.util.concurrent.RejectedExecutionException;
import org.hapjs.bridge.AbstractExtension;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.Response;
import org.hapjs.common.executors.Executors;

public class HapPermissionManager {
    private static final String TAG = "HapPermissionManager";
    private static final HapPermissionManager sInstance = new HapPermissionManager();

    public static HapPermissionManager getDefault() {
        return sInstance;
    }

    public static void addPermissionDescription(String permission, int descResId) {
        RuntimePermissionManager.addPermissionDescription(permission, descResId);
    }

    public void requestPermissions(
            HybridManager hybridManager, final String[] permissions,
            final PermissionCallback callback) {
        requestPermissions(
                hybridManager,
                permissions,
                callback,
                AbstractExtension.PermissionPromptStrategy.FIRST_TIME);
    }

    public void requestPermissions(
            HybridManager hybridManager,
            final String[] permissions,
            final PermissionCallback callback,
            final AbstractExtension.PermissionPromptStrategy strategy) {
        SystemPermissionManager.getDefault()
                .requestPermissions(
                        hybridManager,
                        permissions,
                        new PermissionCallbackAdapterImpl(callback) {
                            @Override
                            public void onPermissionAccept(
                                    final HybridManager hybridManager,
                                    String[] grantedPermissions,
                                    boolean userGranted) {
                                if (userGranted) {
                                    super.onPermissionAccept(hybridManager, grantedPermissions,
                                            userGranted);
                                } else {
                                    // Execute in pool to avoid blocking in UI thread
                                    try {
                                        Executors.io()
                                                .execute(
                                                        () ->
                                                                RuntimePermissionManager
                                                                        .getDefault()
                                                                        .requestPermissions(
                                                                                hybridManager,
                                                                                permissions,
                                                                                new PermissionCallbackAdapterImpl(
                                                                                        callback),
                                                                                strategy));
                                    } catch (RejectedExecutionException ex) {
                                        Log.d(TAG, "reject task because : " + ex.getMessage());
                                        callback.onPermissionReject(Response.CODE_TOO_MANY_REQUEST);
                                    }
                                }
                            }
                        },
                        strategy);
    }

    private static class PermissionCallbackAdapterImpl implements PermissionCallbackAdapter {
        private PermissionCallback callback;

        public PermissionCallbackAdapterImpl(PermissionCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onPermissionAccept(
                HybridManager hybridManager, String[] grantedPermissions, boolean userGranted) {
            if (userGranted) {
                grantPermissions(hybridManager, grantedPermissions);
            }
            callback.onPermissionAccept();
        }

        @Override
        public void onPermissionReject(HybridManager hybridManager, String[] grantedPermissions) {
            grantPermissions(hybridManager, grantedPermissions);
            callback.onPermissionReject(Response.CODE_USER_DENIED);
        }

        private void grantPermissions(HybridManager hybridManager, String[] permissions) {
            String pkg = hybridManager.getApplicationContext().getPackage();
            RuntimePermissionManager.getDefault().grantPermissions(pkg, permissions);
        }
    }
}
