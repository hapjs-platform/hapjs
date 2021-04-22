/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.permission;

import android.util.Log;
import org.hapjs.bridge.AbstractExtension;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.Response;
import org.hapjs.common.executors.Executors;
import org.hapjs.runtime.ProviderManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

public class HapPermissionManager {
    private static final String TAG = "HapPermissionManager";
    private static final HapPermissionManager sInstance = new HapPermissionManager();
    private static RuntimePermissionProvider sPermissionProvider =
            ProviderManager.getDefault().getProvider(RuntimePermissionProvider.NAME);

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
                        new PermissionCallbackAdapterImpl(callback, permissions) {
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
                                                                                        callback, permissions),
                                                                                strategy));
                                    } catch (RejectedExecutionException ex) {
                                        Log.d(TAG, "reject task because : " + ex.getMessage());
                                        callback.onPermissionReject(Response.CODE_TOO_MANY_REQUEST, false);
                                    }
                                }
                            }
                        },
                        strategy);
    }

    private static class PermissionCallbackAdapterImpl implements PermissionCallbackAdapter {
        private PermissionCallback callback;
        private String[] requestPermissions;

        public PermissionCallbackAdapterImpl(PermissionCallback callback, String[] requestPermissions) {
            this.callback = callback;
            this.requestPermissions = requestPermissions;
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
            callback.onPermissionReject(Response.CODE_USER_DENIED,
                    hasRejectPermission(hybridManager.getHapEngine().getPackage(), grantedPermissions));
        }

        private void grantPermissions(HybridManager hybridManager, String[] permissions) {
            String pkg = hybridManager.getApplicationContext().getPackage();
            RuntimePermissionManager.getDefault().grantPermissions(pkg, permissions);
        }

        private boolean hasRejectPermission(String pkg, String[] grantedPermissions){
            boolean dontDisturb = false;
            String[] permissions = filterRefusedPermissions(requestPermissions, grantedPermissions);
            if (permissions != null) {
                int[] mode = sPermissionProvider.getPermissionsMode(pkg, permissions);
                for (int m : mode) {
                    if (m == RuntimePermissionProvider.MODE_REJECT) {
                        dontDisturb = true;
                        break;
                    }
                }
            }
            return dontDisturb;
        }

        private String[] filterRefusedPermissions(String[] requestPermissions, String[] grantedPermissions) {
            if (requestPermissions == null || requestPermissions.length == 0) return null;
            if (grantedPermissions == null || grantedPermissions.length == 0) {
                return requestPermissions;
            }
            List<String> refusedPermissions = new ArrayList<>();
            HashSet<String> grantedPermissionSet = new HashSet<>();
            Collections.addAll(grantedPermissionSet, grantedPermissions);

            for (String requestPermission : requestPermissions) {
                if (!grantedPermissionSet.contains(requestPermission)) {
                    refusedPermissions.add(requestPermission);
                }
            }

            return refusedPermissions.toArray(new String[]{});
        }
    }
}
