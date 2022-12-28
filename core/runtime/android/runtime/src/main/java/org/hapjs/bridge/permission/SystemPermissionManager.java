/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import org.hapjs.bridge.AbstractExtension;
import org.hapjs.bridge.Constants;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.LifecycleListener;

public class SystemPermissionManager implements PermissionManager {

    private static final String TAG = "SystemPermissionManager";

    private static final Map<String, String> PERMISSION_ALTERNATIVES = new HashMap<>();
    private static final AtomicInteger sRequestCodeGenerator =
            new AtomicInteger(Constants.FEATURE_PERMISSION_CODE_BASE);
    private static final SystemPermissionManager sInstance = new SystemPermissionManager();

    private Semaphore mSemaphore;

    private static final int SEMAPHORE_COUNT = 1;

    static {
        PERMISSION_ALTERNATIVES.put(
                Manifest.permission.READ_PHONE_STATE,
                "android.permission.READ_PRIVILEGED_PHONE_STATE");
    }

    public static SystemPermissionManager getDefault() {
        return sInstance;
    }

    private SystemPermissionManager() {
        mSemaphore = new Semaphore(SEMAPHORE_COUNT);
    }

    @Override
    public void requestPermissions(
            final HybridManager hybridManager,
            final String[] permissions,
            final PermissionCallbackAdapter callback,
            final AbstractExtension.PermissionPromptStrategy strategy) {
        String[] ungrantedPermissions = filterUngrantedPermissions(hybridManager, permissions);
        if (ungrantedPermissions == null || ungrantedPermissions.length == 0) {
            callback.onPermissionAccept(hybridManager, null, false);
            return;
        }

        try {
            mSemaphore.acquire();
        } catch (InterruptedException e) {
            Log.d(TAG, "SystemPermissionManager InterruptedException : ", e);
        }

        final int requestCode = sRequestCodeGenerator.incrementAndGet();
        ActivityCompat.requestPermissions(hybridManager.getActivity(), permissions, requestCode);
        hybridManager.addLifecycleListener(
                new LifecycleListener() {
                    @Override
                    public void onRequestPermissionsResult(
                            int code, String[] permissions, int[] grantResults) {
                        if (requestCode == code) {
                            mSemaphore.release();
                            hybridManager.removeLifecycleListener(this);
                            String[] grantedPermissions =
                                    filterGrantedPermissions(permissions, grantResults);
                            if (grantedPermissions != null
                                    && grantedPermissions.length == permissions.length) {
                                callback.onPermissionAccept(hybridManager, grantedPermissions,
                                        true);
                            } else {
                                callback.onPermissionReject(hybridManager, grantedPermissions);
                            }
                        }
                    }

                    @Override
                    public void onDestroy() {
                        super.onDestroy();
                        mSemaphore.release();
                        hybridManager.removeLifecycleListener(this);
                    }
                });
    }

    private String[] filterUngrantedPermissions(HybridManager hybridManager, String[] permissions) {
        Activity activity = hybridManager.getActivity();
        List<String> ungrantedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (!checkPermission(activity, permission)) {
                ungrantedPermissions.add(permission);
            }
        }
        if (ungrantedPermissions.isEmpty()) {
            return null;
        } else {
            return ungrantedPermissions.toArray(new String[ungrantedPermissions.size()]);
        }
    }

    private String[] filterGrantedPermissions(String[] permissions, int[] grantResults) {
        if (grantResults == null) {
            return null;
        }
        List<String> grantedPermissions = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            int result = grantResults[i];
            if (result == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(permissions[i]);
            }
        }
        if (grantedPermissions.isEmpty()) {
            return null;
        } else {
            return grantedPermissions.toArray(new String[grantedPermissions.size()]);
        }
    }

    private boolean checkPermission(Context context, String permission) {
        if (HapCustomPermissions.isHapPermission(permission)) {
            return true;
        }
        if (isPermissionGranted(context, permission)) {
            return true;
        }

        String permissionAlternative = PERMISSION_ALTERNATIVES.get(permission);
        if (permissionAlternative != null) {
            return isPermissionGranted(context, permissionAlternative);
        }

        return false;
    }

    private boolean isPermissionGranted(Context context, String permission) {
        int result = ActivityCompat.checkSelfPermission(context, permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isAllResultsGranted(int[] grantResults) {
        if (grantResults == null) {
            return false;
        }
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
