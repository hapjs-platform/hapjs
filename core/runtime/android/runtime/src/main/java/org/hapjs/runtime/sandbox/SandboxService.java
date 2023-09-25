/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.hapjs.analyzer.model.LogData;
import org.hapjs.analyzer.monitors.AbsLogDumper;
import org.hapjs.common.executors.Executors;
import org.hapjs.logging.LogProvider;
import org.hapjs.render.jsruntime.SandboxJsThread;
import org.hapjs.runtime.ProviderManager;

public class SandboxService extends Service {
    private static final String TAG = "SandboxService";

    private ILogListener mLogListener;
    private Dumper mDumper;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ISandbox.Stub() {
            @Override
            public void init(Map configs) {
                SandboxConfigs.setConfigs(configs);
            }

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public ParcelFileDescriptor[] createChannel(ParcelFileDescriptor[] readSides) throws RemoteException {
                try {
                    ParcelFileDescriptor[] positiveDescriptors = ParcelFileDescriptor.createReliablePipe();
                    ParcelFileDescriptor[] passiveDescriptors = ParcelFileDescriptor.createReliablePipe();
                    new SandboxJsThread(SandboxService.this,
                            new ParcelFileDescriptor[] {readSides[1], positiveDescriptors[1]},
                            new ParcelFileDescriptor[] {readSides[0], passiveDescriptors[1]});
                    return new ParcelFileDescriptor[] {
                            positiveDescriptors[0], passiveDescriptors[0]
                    };
                } catch (IOException e) {
                    throw new RemoteException(e.getMessage());
                }
            }

            @Override
            public void setLogProvider(ILogProvider logProviderImpl) {
                LogProvider logProvider = ProviderManager.getDefault().getProvider(LogProvider.NAME);
                if (logProvider instanceof SandboxLogProviderImpl) {
                    ((SandboxLogProviderImpl) logProvider).setLogProvider(logProviderImpl);
                }

                try {
                    logProviderImpl.asBinder().linkToDeath(() -> {
                        Log.e(TAG, "app process has died. kill sandbox process as well.");
                        Process.killProcess(Process.myPid());
                    }, 0);
                } catch (RemoteException e) {
                    Log.e(TAG, "failed to linkToDeath", e);
                }
            }

            @Override
            public void setLogListener(ILogListener listener) {
                mLogListener = listener;
                if (listener != null) {
                    if (mDumper == null) {
                        mDumper = new Dumper();
                        Executors.io().execute(mDumper);
                    }
                } else {
                    if (mDumper != null) {
                        mDumper.close();
                        mDumper = null;
                    }
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogProvider logProvider = ProviderManager.getDefault().getProvider(LogProvider.NAME);
        if (logProvider instanceof SandboxLogProviderImpl) {
            ((SandboxLogProviderImpl) logProvider).setLogProvider(null);
        }

        mLogListener = null;
        Dumper dumper = mDumper;
        mDumper = null;
        if (dumper != null) {
            dumper.close();
        }
    }

    public static class Sandbox0 extends SandboxService {}
    public static class Sandbox1 extends SandboxService {}
    public static class Sandbox2 extends SandboxService {}
    public static class Sandbox3 extends SandboxService {}
    public static class Sandbox4 extends SandboxService {}

    private class Dumper extends AbsLogDumper {
        @Override
        protected void doDumpLog(List<LogData> logs) {
            ILogListener logListener = mLogListener;
            if (logListener != null) {
                try {
                    logListener.onLog(logs);
                } catch (RemoteException e) {
                    Log.e(TAG, "failed to onLog", e);
                }
            }
        }
    }
}
