/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import android.os.RemoteException;
import android.util.Log;
import java.util.Map;
import org.hapjs.common.executors.Executor;
import org.hapjs.common.executors.Executors;
import org.hapjs.logging.LogProvider;

public class SandboxLogProviderImpl implements LogProvider {
    private static final String TAG = "SandboxLogProviderImpl";

    private ILogProvider mLogProviderImpl;
    private Executor mExecutor = Executors.createSingleThreadExecutor();

    public void setLogProvider(ILogProvider logProviderImpl) {
        mLogProviderImpl = logProviderImpl;
    }

    @Override
    public void logCountEvent(String appPackage, String category, String key) {
        mExecutor.execute(() -> doLogCountEvent(appPackage, category, key));
    }

    protected void doLogCountEvent(String appPackage, String category, String key) {
        ILogProvider logProviderImpl = mLogProviderImpl;
        if (logProviderImpl == null) {
            return;
        }

        try {
            logProviderImpl.logCountEvent(appPackage, category, key);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to stat", e);
        }
    }

    @Override
    public void logCountEvent(String appPackage, String category, String key, Map params) {
        mExecutor.execute(() -> doLogCountEvent(appPackage, category, key, params));
    }

    protected void doLogCountEvent(String appPackage, String category, String key, Map params) {
        ILogProvider logProviderImpl = mLogProviderImpl;
        if (logProviderImpl == null) {
            return;
        }

        try {
            logProviderImpl.logCountEventWithParams(appPackage, category, key, params);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to stat", e);
        }
    }

    @Override
    public void logCalculateEvent(String appPackage, String category, String key, long value) {
        mExecutor.execute(() -> doLogCalculateEvent(appPackage, category, key, value));
    }

    private void doLogCalculateEvent(String appPackage, String category, String key, long value) {
        ILogProvider logProviderImpl = mLogProviderImpl;
        if (logProviderImpl == null) {
            return;
        }

        try {
            logProviderImpl.logCalculateEvent(appPackage, category, key, value);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to stat", e);
        }
    }

    @Override
    public void logCalculateEvent(String appPackage, String category, String key, long value, Map params) {
        mExecutor.execute(() -> doLogCalculateEvent(appPackage, category, key, value, params));
    }

    private void doLogCalculateEvent(String appPackage, String category, String key, long value, Map params) {
        ILogProvider logProviderImpl = mLogProviderImpl;
        if (logProviderImpl == null) {
            return;
        }

        try {
            logProviderImpl.logCalculateEventWithParams(appPackage, category, key, value, params);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to stat", e);
        }
    }

    @Override
    public void logNumericPropertyEvent(String appPackage, String category, String key, long value) {
        mExecutor.execute(() -> doLogNumericPropertyEvent(appPackage, category, key, value));
    }

    private void doLogNumericPropertyEvent(String appPackage, String category, String key, long value) {
        ILogProvider logProviderImpl = mLogProviderImpl;
        if (logProviderImpl == null) {
            return;
        }

        try {
            logProviderImpl.logNumericPropertyEvent(appPackage, category, key, value);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to stat", e);
        }
    }

    @Override
    public void logNumericPropertyEvent(String appPackage, String category, String key, long value, Map params) {
        mExecutor.execute(() -> doLogNumericPropertyEvent(appPackage, category, key, value, params));
    }

    private void doLogNumericPropertyEvent(String appPackage, String category, String key, long value, Map params) {
        ILogProvider logProviderImpl = mLogProviderImpl;
        if (logProviderImpl == null) {
            return;
        }

        try {
            logProviderImpl.logNumericPropertyEventWithParams(appPackage, category, key, value, params);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to stat", e);
        }
    }

    @Override
    public void logStringPropertyEvent(String appPackage, String category, String key, String value) {
        mExecutor.execute(() -> doLogStringPropertyEvent(appPackage, category, key, value));
    }

    private void doLogStringPropertyEvent(String appPackage, String category, String key, String value) {
        ILogProvider logProviderImpl = mLogProviderImpl;
        if (logProviderImpl == null) {
            return;
        }

        try {
            logProviderImpl.logStringPropertyEvent(appPackage, category, key, value);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to stat", e);
        }
    }

    @Override
    public void logStringPropertyEvent(String appPackage, String category, String key, String value, Map params) {
        mExecutor.execute(() -> doLogStringPropertyEvent(appPackage, category, key, value, params));
    }

    private void doLogStringPropertyEvent(String appPackage, String category, String key, String value, Map params) {
        ILogProvider logProviderImpl = mLogProviderImpl;
        if (logProviderImpl == null) {
            return;
        }

        try {
            logProviderImpl.logStringPropertyEventWithParams(appPackage, category, key, value, params);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to stat", e);
        }
    }
}
