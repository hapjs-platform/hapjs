/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.permission;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.hapjs.bridge.AbstractExtension;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.ReflectUtils;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.runtime.Checkable;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.R;

public class RuntimePermissionManager implements PermissionManager {
    private static final Map<String, Integer> sPermissionDescriptions = new HashMap<>();
    private static final String TAG = "RuntimePermissionMgr";

    private static final String CLASS_NAME_ACTIVITY = "android.app.Activity";
    private static final String METHOD_NAME_IS_RESUMED = "isResumed";
    private static final RuntimePermissionManager sInstance = new RuntimePermissionManager();
    private static final int SEMAPHORE_COUNT = 1;

    static {
        sPermissionDescriptions.put(
                Manifest.permission.ACCESS_FINE_LOCATION, R.string.permission_desc_location);
        sPermissionDescriptions.put(
                Manifest.permission.RECORD_AUDIO, R.string.permission_desc_record_audio);
        sPermissionDescriptions.put(
                Manifest.permission.READ_PHONE_STATE, R.string.permission_desc_read_phone_state);
    }

    private RuntimePermissionProvider mProvider;
    private Semaphore mSemaphore;
    private LifecycleListener mLifecycleListener;

    private RuntimePermissionManager() {
        mProvider = ProviderManager.getDefault().getProvider(RuntimePermissionProvider.NAME);
        mSemaphore = new Semaphore(SEMAPHORE_COUNT);
    }

    public static void addPermissionDescription(String permission, int descResId) {
        sPermissionDescriptions.put(permission, descResId);
    }

    public static RuntimePermissionManager getDefault() {
        return sInstance;
    }

    @Override
    public void requestPermissions(
            final HybridManager hybridManager,
            final String[] permissions,
            final PermissionCallbackAdapter adapter,
            final AbstractExtension.PermissionPromptStrategy strategy) {
        final PermissionCallbackAdapter callback = new RuntimePermissionCallbackWrapper(adapter);

        try {
            mSemaphore.acquire();
        } catch (InterruptedException e) {
            Log.d(TAG, "RuntimePermissionManager InterruptedException : ", e);
        }

        if (mLifecycleListener == null) {
            mLifecycleListener =
                    new LifecycleListener() {
                        @Override
                        public void onDestroy() {
                            releaseSemaphore(hybridManager);
                        }
                    };
            hybridManager.addLifecycleListener(mLifecycleListener);
        }

        if (strategy == AbstractExtension.PermissionPromptStrategy.EVERY_TIME) {
            Activity activity = hybridManager.getActivity();
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            showPermissionPrompt(hybridManager, permissions, callback, 0);
                        }
                    });
            return;
        }

        if (mProvider == null) {
            callback.onPermissionAccept(hybridManager, null, false);
            return;
        }

        Activity activity = hybridManager.getActivity();
        String pkg = hybridManager.getApplicationContext().getPackage();
        final int[] permissionsMode = mProvider.checkPermissions(pkg, permissions);
        if (hasForbiddenPermission(activity, permissionsMode, permissions)) {
            callback.onPermissionReject(hybridManager, null);
            return;
        }
        final String[] ungrantedPermissions =
                filterUngrantedPermissions(permissions, permissionsMode);
        if (ungrantedPermissions == null || ungrantedPermissions.length == 0) {
            callback.onPermissionAccept(hybridManager, null, false);
            return;
        }

        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        showPermissionPrompt(hybridManager, ungrantedPermissions, callback, 0);
                    }
                });
    }

    public boolean isRequestPermissionInProcessing() {
        return mSemaphore.availablePermits() < SEMAPHORE_COUNT;
    }

    private void releaseSemaphore(HybridManager hybridManager) {
        if (!mSemaphore.hasQueuedThreads() && mProvider != null) {
            mProvider.clearRejectPermissionCache();
        }
        if (null != mLifecycleListener) {
            hybridManager.removeLifecycleListener(mLifecycleListener);
            mLifecycleListener = null;
        }
        mSemaphore.release();
    }

    public void grantPermissions(String pkg, String[] permissions) {
        if (mProvider == null || permissions == null || permissions.length == 0) {
            return;
        }
        mProvider.grantPermissions(pkg, permissions);
    }

    public String getPermissionDescription(Context context, String permission) {
        String permissionDesc;
        Integer resId = sPermissionDescriptions.get(permission);
        if (resId == null) {
            permissionDesc = getDefaultPermissionDescription(context, permission);
        } else {
            permissionDesc = context.getString(resId);
        }
        return permissionDesc;
    }

    private String[] filterUngrantedPermissions(String[] permissions, int[] permissionsMode) {
        List<String> ungrantedPermissions = new ArrayList<>();
        for (int i = 0; i < permissionsMode.length; i++) {
            if (permissionsMode[i] != RuntimePermissionProvider.MODE_ACCEPT) {
                ungrantedPermissions.add(permissions[i]);
            }
        }
        if (ungrantedPermissions.isEmpty()) {
            return null;
        } else {
            return ungrantedPermissions.toArray(new String[ungrantedPermissions.size()]);
        }
    }

    private void showPermissionPrompt(
            final HybridManager hybridManager,
            final String[] permissions,
            final PermissionCallbackAdapter callback,
            final int index) {
        Activity activity = hybridManager.getActivity();
        if (activity.isFinishing() || activity.isDestroyed()) {
            callback.onPermissionReject(hybridManager, null);
            return;
        }

        ApplicationContext applicationContext = hybridManager.getApplicationContext();
        String name = applicationContext.getName();
        String message = getPermissionMessage(activity, name, permissions[index]);
        final String pkg = applicationContext.getPackage();
        final String permission = permissions[index];
        DialogInterface.OnClickListener listener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            RuntimeLogManager.getDefault()
                                    .logPermissionPrompt(pkg, permission, true, true);
                            if (index < permissions.length - 1) {
                                showPermissionPrompt(hybridManager, permissions, callback,
                                        index + 1);
                            } else {
                                callback.onPermissionAccept(hybridManager, permissions, true);
                            }
                        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                            boolean forbidden = false;
                            if (dialog instanceof Checkable) {
                                forbidden = ((Checkable) dialog).isChecked();
                            } else if (dialog instanceof Dialog) {
                                View checkBox =
                                        ((Dialog) dialog).findViewById(android.R.id.checkbox);
                                if (checkBox instanceof CheckBox) {
                                    forbidden = ((CheckBox) checkBox).isChecked();
                                }
                            }
                            RuntimeLogManager.getDefault()
                                    .logPermissionPrompt(pkg, permission, false, forbidden);
                            mProvider.rejectPermissions(pkg, new String[] {permission}, forbidden);
                            String[] grantedPermissions = Arrays.copyOf(permissions, index);
                            callback.onPermissionReject(hybridManager, grantedPermissions);
                        }
                    }
                };

        boolean enableCheckBox =
                (mProvider.getPermissionFlag(pkg, permission)
                        & RuntimePermissionProvider.FLAG_SHOW_FORBIDDEN)
                        == RuntimePermissionProvider.FLAG_SHOW_FORBIDDEN;
        Dialog dialog =
                mProvider.createPermissionDialog(activity, message, listener, enableCheckBox);
        if (isResumed(hybridManager.getActivity()) || hybridManager.getHapEngine().isCardMode()) {
            DarkThemeUtil.disableForceDark(dialog);
            if (!mProvider.isHidePermissionDialog(hybridManager.getActivity(), dialog)) {
                dialog.show();
            }
        }

        final Dialog finalDialog = dialog;
        final LifecycleListener lifecycleListener =
                new LifecycleListener() {

                    @Override
                    public void onResume() {
                        super.onResume();
                        if (null != finalDialog && !finalDialog.isShowing()) {
                            DarkThemeUtil.disableForceDark(finalDialog);
                            if (!mProvider.isHidePermissionDialog(hybridManager.getActivity(),
                                    finalDialog)) {
                                finalDialog.show();
                            }
                        }
                    }

                    @Override
                    public void onDestroy() {
                        finalDialog.dismiss();
                    }
                };
        hybridManager.addLifecycleListener(lifecycleListener);
        dialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        hybridManager.removeLifecycleListener(lifecycleListener);
                    }
                });
    }

    private boolean isResumed(Activity activity) {
        Object obj =
                ReflectUtils.invokeMethod(
                        CLASS_NAME_ACTIVITY, activity, METHOD_NAME_IS_RESUMED, null, null);
        if (null != obj) {
            return (boolean) obj;
        }
        // 默认放行
        return true;
    }

    private String getPermissionMessage(Context context, String appName, String permission) {
        String permissionDesc = getPermissionDescription(context, permission);
        return context.getString(R.string.permission_dialog_message, appName, permissionDesc);
    }

    private String getDefaultPermissionDescription(Context context, String permission) {
        if (HapCustomPermissions.isHapPermission(permission)) {
            return HapCustomPermissions.getHapPermissionDesc(permission, context);
        }
        PackageManager pm = context.getPackageManager();
        try {
            PermissionInfo pi = pm.getPermissionInfo(permission, 0);
            CharSequence desc = pi.loadLabel(pm);
            if (desc != null) {
                return desc.toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        return permission;
    }

    private boolean hasForbiddenPermission(Activity activity, int[] modes, String[] permissions) {
        if (modes == null) {
            return false;
        }
        for (int i = 0; i < modes.length; i++) {
            int result = modes[i];
            if (result == RuntimePermissionProvider.MODE_REJECT) {
                mProvider.onPermissionForbidden(activity, permissions[i]);
                return true;
            }
        }
        return false;
    }

    private class RuntimePermissionCallbackWrapper implements PermissionCallbackAdapter {
        PermissionCallbackAdapter callback;

        public RuntimePermissionCallbackWrapper(PermissionCallbackAdapter callback) {
            this.callback = callback;
        }

        @Override
        public void onPermissionAccept(
                final HybridManager hybridManager,
                final String[] grantedPermissions,
                final boolean userGranted) {
            if (ThreadUtils.isInMainThread()) {
                Executors.computation()
                        .execute(
                                () -> {
                                    callback.onPermissionAccept(hybridManager, grantedPermissions,
                                            userGranted);
                                    releaseSemaphore(hybridManager);
                                });
            } else {
                callback.onPermissionAccept(hybridManager, grantedPermissions, userGranted);
                releaseSemaphore(hybridManager);
            }
        }

        @Override
        public void onPermissionReject(
                final HybridManager hybridManager, final String[] grantedPermissions) {
            if (ThreadUtils.isInMainThread()) {
                Executors.computation()
                        .execute(
                                () -> {
                                    callback.onPermissionReject(hybridManager, grantedPermissions);
                                    releaseSemaphore(hybridManager);
                                });
            } else {
                callback.onPermissionReject(hybridManager, grantedPermissions);
                releaseSemaphore(hybridManager);
            }
        }
    }
}
