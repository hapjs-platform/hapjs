/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.resident;

import static org.hapjs.common.resident.ResidentManager.ResidentChangeListener.RESIDENT_TYPE_IMPORTANT;
import static org.hapjs.common.resident.ResidentManager.ResidentChangeListener.RESIDENT_TYPE_NONE;
import static org.hapjs.common.resident.ResidentManager.ResidentChangeListener.RESIDENT_TYPE_NORMAL;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.MetaDataSet;
import org.hapjs.bridge.permission.HapCustomPermissions;
import org.hapjs.model.AppInfo;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.AppJsThread;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.runtime.HapEngine;

public class ResidentManager {

    public static final String ACTION_CLOSE = "resident.close";
    private static final String TAG = "ResidentManager";
    /**
     * After running in the background, if there is no corresponding interface call for a period of
     * time(20s), the background operation will be automatically terminated.
     */
    private static final int TIME_OUT_NORMAL_FEATURE = 20 * 1000;
    private static final int MESSAGE_STOP_RUNNING_IN_BACKGROUND = 1;
    private static final int MESSAGE_RUN_IN_FOREGROUND = 2;
    private static final int MESSAGE_RUN_IN_BACKGROUND = 3;
    private static final int MESSAGE_REGISTER_FEATURE = 4;
    private static final int MESSAGE_UNREGISTER_FEATURE = 5;
    private static final int MESSAGE_START_RESIDENT = 6;
    private static final int MESSAGE_STOP_RESIDENT = 7;
    private static final int MESSAGE_DESTROY = 8;
    private static final int MESSAGE_DESTROY_IMMEDIATELY = 9;
    private static final int MESSAGE_RUN_A_SHORT_TIME = 10;
    private Map<String, FeatureExtension> mResidentFeaturesMap = new ConcurrentHashMap<>();
    private ResidentService.ResidentBinder mResidentBinder;
    private Context mContext;
    private AppInfo mAppInfo;
    private AppJsThread mJsThread;
    private ServiceConnection mServiceConnection;
    private ResidentHandler mResidentHandler;
    private ResidentChangeListener mDbUpdateListener;
    private volatile boolean mIsInForeground = true;
    private String mNotiDesc;
    private BroadcastReceiver mReceiver;
    private Set<String> mStopActions = new HashSet<>();
    private volatile boolean mIsCardMode = true;
    private boolean mResidentModuleFlag = false;

    public ResidentManager() {
        mResidentHandler = new ResidentHandler(Looper.getMainLooper());
    }


    public void init(Context context, AppInfo appInfo, AppJsThread jsThread) {
        if (null != context) {
            this.mContext = context.getApplicationContext();
        }
        this.mAppInfo = appInfo;
        this.mJsThread = jsThread;
        this.mIsInForeground = true;
        this.mIsCardMode = HapEngine.getInstance(mAppInfo.getPackage()).isCardMode();
    }

    public void setUpdateDbListener(ResidentChangeListener listener) {
        this.mDbUpdateListener = listener;
    }

    /**
     * Quick apps are not allowed to use features which unregistered in manifest.json when running in
     * the background.
     *
     * @param featureName
     * @return
     */
    public boolean isAllowToInvoke(String featureName, String action) {
        if (mIsCardMode) {
            return true;
        }
        if (!mIsInForeground) {
            // In White List
            if (MetaDataSet.getInstance().isInResidentWhiteSet(featureName)) {
                return true;
            }

            // Registed in manifest.json
            if (mAppInfo.getConfigInfo().isBackgroundFeature(featureName)) {
                return true;
            }

            // In Background Method List
            if (MetaDataSet.getInstance().isInMethodResidentWhiteSet(featureName + "_" + action)) {
                return true;
            }
            Log.w(TAG, "Feature is not allowed to use in background: " + featureName);
            return false;
        }
        return true;
    }

    public void postRegisterFeature(FeatureExtension feature) {
        if (mIsCardMode) {
            return;
        }
        if (!mAppInfo.getConfigInfo().isBackgroundFeature(feature.getName())) {
            return;
        }

        mResidentHandler.removeMessages(MESSAGE_STOP_RUNNING_IN_BACKGROUND);

        Message msg = new Message();
        msg.what = MESSAGE_REGISTER_FEATURE;
        msg.obj = feature;
        mResidentHandler.sendMessage(msg);
    }

    private void registerFeature(FeatureExtension feature) {
        mResidentFeaturesMap.put(feature.getName(), feature);
        if (!mIsInForeground) {
            runInBackground();
        }
    }

    /**
     * Background mode, and no feature calls need to be run in the background, then perform automatic
     * termination timing.
     *
     * @param feature
     */
    public void postUnregisterFeature(FeatureExtension feature) {
        if (mIsCardMode) {
            return;
        }
        if (!mAppInfo.getConfigInfo().isBackgroundFeature(feature.getName())) {
            return;
        }
        Message msg = new Message();
        msg.what = MESSAGE_UNREGISTER_FEATURE;
        msg.obj = feature;
        mResidentHandler.sendMessage(msg);
    }

    private void unregisterFeature(FeatureExtension feature) {
        mResidentFeaturesMap.remove(feature.getName());
        if (!mIsInForeground && mResidentFeaturesMap.size() == 0) {
            mResidentHandler.sendEmptyMessageDelayed(
                    MESSAGE_STOP_RUNNING_IN_BACKGROUND, TIME_OUT_NORMAL_FEATURE);
        }
    }

    private void updateNotification(String notiDesc) {
        mNotiDesc = notiDesc;
        if (null != mResidentBinder) {
            mResidentBinder.updateNotificationDesc(mAppInfo, mNotiDesc);
        }
    }

    public void postStartResident(String notiDesc) {
        if (mIsCardMode) {
            return;
        }
        Message msg = new Message();
        msg.what = MESSAGE_START_RESIDENT;
        msg.obj = notiDesc;
        mResidentHandler.sendMessage(msg);
    }

    private void startResident(String notiDesc) {
        mResidentModuleFlag = true;
        if (!mIsInForeground) {
            runInBackground();
        }
        updateNotification(notiDesc);
    }

    public void postStopResident() {
        if (mIsCardMode) {
            return;
        }
        mResidentHandler.sendEmptyMessage(MESSAGE_STOP_RESIDENT);
    }

    private void stopResident() {
        mResidentModuleFlag = false;
        if (!mIsInForeground) {
            stopRunInBackground();
        }
    }

    public boolean needRunInBackground() {
        if (mIsCardMode) {
            return false;
        }
        if (!mResidentModuleFlag) {
            return false;
        }
        Set<String> registedFeatues = mAppInfo.getConfigInfo().getBackgroundFeatures();
        if (registedFeatues.size() > 0 && (mResidentFeaturesMap.size() > 0)) {
            for (String featureName : registedFeatues) {
                if (mResidentFeaturesMap.containsKey(featureName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void postRunInBackground() {
        if (mIsCardMode) {
            return;
        }
        mResidentHandler.removeMessages(MESSAGE_STOP_RUNNING_IN_BACKGROUND);
        mResidentHandler.sendEmptyMessage(MESSAGE_RUN_IN_BACKGROUND);
    }

    private void runInBackground() {
        if (!needRunInBackground()) {
            if (null != mJsThread) {
                mJsThread.block(RootView.BLOCK_JS_THREAD_DELAY_TIME);
            }
        } else {
            updateReceiver();
            if (null != mJsThread) {
                mJsThread.unblock();
            }
            mIsInForeground = false;

            if (hasNotificationShownByFeatures()) {
                Log.d(TAG, "some feature has shown notification.");
                if (mServiceConnection != null) {
                    unbindResidentService();
                }
            } else if (null == mServiceConnection) {
                mServiceConnection =
                        new ServiceConnection() {
                            @Override
                            public void onServiceConnected(ComponentName name, IBinder service) {
                                mResidentBinder = (ResidentService.ResidentBinder) service;
                                mResidentBinder.notifyUser(mAppInfo, mNotiDesc);
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName name) {
                                mResidentBinder = null;
                                mServiceConnection = null;
                            }
                        };
                if (null != mContext) {
                    mContext.bindService(
                            ResidentService.getIntent(mContext), mServiceConnection,
                            Context.BIND_AUTO_CREATE);
                } else {
                    Log.e(TAG, "Application Context is null.");
                }
            }
            updateDbInfo();
        }
    }

    private boolean hasNotificationShownByFeatures() {
        boolean hasNotification = false;
        for (FeatureExtension feature : mResidentFeaturesMap.values()) {
            hasNotification = feature.hasShownForegroundNotification();
            if (hasNotification) {
                break;
            }
        }
        return hasNotification;
    }

    public void postRunInForeground() {
        if (mIsCardMode) {
            return;
        }
        mResidentHandler.removeMessages(MESSAGE_STOP_RUNNING_IN_BACKGROUND);
        mResidentHandler.sendEmptyMessage(MESSAGE_RUN_IN_FOREGROUND);
    }

    private void runInForeground() {
        if (mJsThread != null) {
            mJsThread.unblock();
        }
        mIsInForeground = true;

        clearReceiverAndService();
        if (null != mContext && null != mAppInfo && null != mDbUpdateListener) {
            mDbUpdateListener.onResidentChange(mContext, mAppInfo.getPackage(), RESIDENT_TYPE_NONE);
        }
    }

    public void postDestroy(boolean immediately) {
        if (mIsCardMode) {
            return;
        }
        Message msg = new Message();
        msg.what = MESSAGE_DESTROY;
        msg.obj = Boolean.valueOf(immediately);
        mResidentHandler.sendMessage(msg);
    }

    private void destroy(boolean immediately) {
        if (mJsThread != null) {
            mJsThread.shutdown(immediately ? 0 : RootView.BLOCK_JS_THREAD_DELAY_TIME);
            mJsThread = null;
        }

        if (immediately) {
            destroyImmediately();
        } else {
            mResidentHandler.sendEmptyMessageDelayed(
                    MESSAGE_DESTROY_IMMEDIATELY, RootView.BLOCK_JS_THREAD_DELAY_TIME);
        }
    }

    private void destroyImmediately() {
        mResidentFeaturesMap.clear();
        clearReceiverAndService();
    }

    private void updateDbInfo() {
        if (null != mDbUpdateListener && null != mContext && null != mAppInfo) {
            Set<String> set = mResidentFeaturesMap.keySet();
            int residentType = RESIDENT_TYPE_NONE;
            if (null != set && set.size() > 0) {
                residentType = RESIDENT_TYPE_NORMAL;
                Iterator<String> iterator = set.iterator();
                while (iterator.hasNext()) {
                    if (MetaDataSet.getInstance().isInResidentImportantSet(iterator.next())) {
                        residentType = RESIDENT_TYPE_IMPORTANT;
                        break;
                    }
                }
            }
            mDbUpdateListener.onResidentChange(mContext, mAppInfo.getPackage(), residentType);
        } else {
            Log.e(TAG, "One of mDbUpdateListener, mContext, mAppInfo is null.");
        }
    }

    private void stopRunInBackground() {
        for (FeatureExtension feature : mResidentFeaturesMap.values()) {
            feature.onStopRunningInBackground();
        }
        if (null != mJsThread) {
            mJsThread.block(RootView.BLOCK_JS_THREAD_DELAY_TIME);
        }

        clearReceiverAndService();
    }

    private void clearReceiverAndService() {
        unregisterReceiver();
        unbindResidentService();
    }

    private void unbindResidentService() {
        if (null != mResidentBinder) {
            mResidentBinder.removeNotify(mAppInfo);
            mResidentBinder = null;
        }

        if (null != mServiceConnection) {
            if (null != mContext) {
                mContext.unbindService(mServiceConnection);
            } else {
                Log.e(TAG, "Application Context is null.");
            }
            mServiceConnection = null;
        }
    }

    private void unregisterReceiver() {
        if (null != mReceiver) {
            try {
                if (null != mContext) {
                    mContext.unregisterReceiver(mReceiver);
                } else {
                    Log.e(TAG, "Application Context is null.");
                }
            } catch (IllegalArgumentException e) {
                // ignore
            }
            mReceiver = null;
        }
        mStopActions.clear();
    }

    private void registerReceiver(Set<String> stopActions) {
        if (stopActions.isEmpty()) {
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        for (String action : stopActions) {
            intentFilter.addAction(action);
        }

        mReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (!TextUtils.isEmpty(action)) {
                            if (stopActions.contains(action)) {
                                stopRunInBackground();
                            }
                        }
                    }
                };
        if (null != mContext) {
            mContext.registerReceiver(
                    mReceiver,
                    intentFilter,
                    HapCustomPermissions.getHapPermissionReceiveBroadcast(mContext),
                    null);
        } else {
            Log.e(TAG, "Application Context is null.");
        }
    }

    private void updateReceiver() {
        Set<String> stopActions = new HashSet<>();
        for (FeatureExtension feature : mResidentFeaturesMap.values()) {
            String stopAction =
                    feature.hasShownForegroundNotification()
                            ? feature.getForegroundNotificationStopAction()
                            : null;
            stopAction = TextUtils.isEmpty(stopAction) ? ACTION_CLOSE : stopAction;
            stopAction = mAppInfo.getPackage() + "." + stopAction;
            stopActions.add(stopAction);
        }

        if (mReceiver == null
                || mStopActions.size() != stopActions.size()
                || !mStopActions.containsAll(stopActions)) {
            unregisterReceiver();
            registerReceiver(stopActions);
            mStopActions = stopActions;
        }
    }

    public void postRunAShortTime() {
        if (mIsCardMode) {
            return;
        }
        Message msg = new Message();
        msg.what = MESSAGE_RUN_A_SHORT_TIME;
        mResidentHandler.sendMessage(msg);
    }

    private void runAShortTime() {
        if (mJsThread != null && mJsThread.isBlocked()) {
            mJsThread.unblock();
            mJsThread.block(RootView.BLOCK_JS_THREAD_DELAY_TIME);
        }
    }

    /**
     * Updating resident information to the database for process selection needs to exclude processes
     * running in the background.
     */
    public interface ResidentChangeListener {

        int RESIDENT_TYPE_NONE = 0;
        int RESIDENT_TYPE_NORMAL = 1;
        int RESIDENT_TYPE_IMPORTANT = 2;

        void onResidentChange(Context context, String packageName, @ResidentType int residentType);

        @IntDef(value = {RESIDENT_TYPE_NONE, RESIDENT_TYPE_NORMAL, RESIDENT_TYPE_IMPORTANT})
        @Retention(RetentionPolicy.SOURCE)
        @interface ResidentType {
        }
    }

    private class ResidentHandler extends Handler {

        public ResidentHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (null == mContext) {
                Log.w(TAG, "Null of mContext.");
                return;
            }
            switch (msg.what) {
                case MESSAGE_RUN_IN_FOREGROUND:
                    runInForeground();
                    break;
                case MESSAGE_RUN_IN_BACKGROUND:
                    runInBackground();
                    break;
                case MESSAGE_DESTROY:
                    destroy((boolean) msg.obj);
                    break;
                case MESSAGE_DESTROY_IMMEDIATELY:
                    destroyImmediately();
                    break;
                case MESSAGE_REGISTER_FEATURE:
                    registerFeature((FeatureExtension) msg.obj);
                    break;
                case MESSAGE_UNREGISTER_FEATURE:
                    unregisterFeature((FeatureExtension) msg.obj);
                    break;
                case MESSAGE_START_RESIDENT:
                    startResident((String) msg.obj);
                    break;
                case MESSAGE_STOP_RESIDENT:
                    stopResident();
                    break;
                case MESSAGE_STOP_RUNNING_IN_BACKGROUND:
                    stopRunInBackground();
                    break;
                case MESSAGE_RUN_A_SHORT_TIME:
                    runAShortTime();
                    break;
                default:
                    break;
            }
        }
    }
}
