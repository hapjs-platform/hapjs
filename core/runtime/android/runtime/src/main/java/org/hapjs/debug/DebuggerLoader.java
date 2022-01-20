/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debug;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.debug.log.DebuggerLogUtil;

public class DebuggerLoader {
    private static final String TAG = "DebuggerLoader";

    private static Params sParams;

    public static void resetDebugger(boolean platform) {
        if (platform) {
            DebuggerLogUtil.logMessage("ENGINE_LOCAL_SERVER_CLOSED");
        } else {
            DebuggerLogUtil.logMessage("ENGINE_SERVER_CLOSED_FROM_REMOTE");
        }
        stopDebugger(platform);
        sParams = null;
    }

    public static void attachDebugger(Context context, int platformVersionCode, Params params) {
        if (sParams == null || !sParams.equals(params)) {
            DebuggerLogUtil.init(context.getApplicationContext(), params.mTraceId);
            DebuggerLogUtil.logMessage("ENGINE_START_DEBUG");
            sParams = params;
            initDebugger(context, params);
        }
    }

    private static void initDebugger(Context context, Params params) {
        DebuggerLogUtil.logBreadcrumb("V8Inspector init");
        try {
            Class<?> v8InspectorClass = Class.forName("org.hapjs.inspector.V8Inspector");
            Method getInstanceMethod = v8InspectorClass.getMethod("getInstance");
            Object v8InspectorInstance = getInstanceMethod.invoke(null);
            Method initMethod;
            try {
                initMethod = v8InspectorClass.getMethod("init", Context.class, Map.class);
                initMethod.invoke(v8InspectorInstance, context, params.map());
            } catch (NoSuchMethodException e) {
                // fallback to old version V8Inspector.init(context, server) method
                initMethod = v8InspectorClass.getMethod("init", Context.class, String.class);
                initMethod.invoke(v8InspectorInstance, context, params.getDebugServer());
            }
            Log.i(TAG, "Success to initialize debugger");
            DebuggerLogUtil.logMessage("ENGINE_INIT_V8_INSPECTOR");
        } catch (Exception e) {
            Log.e(TAG, "Fail to initialize debugger", e);
            DebuggerLogUtil.logBreadcrumb("Fail to initialize debugger");
            DebuggerLogUtil.logException(e);
        }
    }

    private static void stopDebugger(boolean platform) {
        try {
            Class<?> v8InspectorClass = Class.forName("org.hapjs.inspector.V8Inspector");
            Method getInstanceMethod = v8InspectorClass.getMethod("getInstance");
            Object v8InspectorInstance = getInstanceMethod.invoke(null);
            Method initMethod;
            try {
                initMethod = v8InspectorClass.getMethod("stop", Boolean.class);
                initMethod.invoke(v8InspectorInstance, platform);
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "could not found stop method");
            }
            Log.i(TAG, "Success to stop debugger");
            DebuggerLogUtil.logMessage("ENGINE_V8INSPECTOR_STOP");
        } catch (Exception e) {
            Log.e(TAG, "Fail to stop debugger", e);
            DebuggerLogUtil.logBreadcrumb("V8Inspector stop exception");
            DebuggerLogUtil.logException(e);
            DebuggerLogUtil.stop();
        }
    }

    public static class Params {
        public static final String PARAM_KEY_DEBUG_SERVER = "DEBUG_SERVER";
        public static final String PARAM_KEY_DEBUG_PACKAGE = "DEBUG_PACKAGE";
        public static final String PARAM_KEY_DEBUG_USE_ADB = "DEBUG_USE_ADB";
        public static final String PARAM_KEY_DEBUG_SERIAL_NUMBER = "DEBUG_SERIAL_NUMBER";
        public static final String PARAM_KEY_DEBUG_TARGET = "DEBUG_TARGET";
        // Possible value: skeleton
        public static final String PARAM_KEY_DEBUG_TRACE_ID = "DEBUG_TRACE_ID";
        private String mDebugServer;
        private String mDebugPackage;
        private String mSerialNumber;
        private boolean mUseADB;
        private String mDebugTarget;
        private String mTraceId;

        Params(
                String debugServer,
                String debugPackage,
                String serialNumber,
                boolean useADB,
                String debugTarget,
                String traceId) {
            mDebugServer = debugServer;
            mDebugPackage = debugPackage;
            mSerialNumber = serialNumber;
            mUseADB = useADB;
            mDebugTarget = debugTarget;
            mTraceId = traceId;
        }

        public Map<String, Object> map() {
            Map<String, Object> params = new HashMap<>();
            params.put(PARAM_KEY_DEBUG_SERVER, mDebugServer);
            params.put(PARAM_KEY_DEBUG_PACKAGE, mDebugPackage);
            params.put(PARAM_KEY_DEBUG_USE_ADB, mUseADB);
            params.put(PARAM_KEY_DEBUG_SERIAL_NUMBER, mSerialNumber);
            params.put(PARAM_KEY_DEBUG_TARGET, mDebugTarget);
            params.put(PARAM_KEY_DEBUG_TRACE_ID, mTraceId);
            return params;
        }

        public String getDebugServer() {
            return mDebugServer;
        }

        public String getDebugPackage() {
            return mDebugPackage;
        }

        public String getSerialNumber() {
            return mSerialNumber;
        }

        public boolean isUseADB() {
            return mUseADB;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Params)) {
                return false;
            }

            Params params = (Params) obj;
            return mUseADB == params.mUseADB
                    && TextUtils.equals(mDebugServer, params.mDebugServer)
                    && TextUtils.equals(mDebugPackage, params.mDebugPackage)
                    && TextUtils.equals(mSerialNumber, params.mSerialNumber);
        }

        @Override
        public int hashCode() {
            int result = mDebugPackage.hashCode();
            result = 31 * result + (mUseADB ? 1231 : 1237);
            result = 31 * result + mDebugServer.hashCode();
            result = 31 * result + mSerialNumber.hashCode();
            return result;
        }

        public static class Builder {
            private String debugServer;
            private String debugPackage;
            private String serialNumber;
            private boolean useADB;
            private String debugTarget;
            private String traceId;

            public Builder debugServer(String debugServer) {
                this.debugServer = debugServer;
                return this;
            }

            public Builder debugPackage(String debugPackage) {
                this.debugPackage = debugPackage;
                return this;
            }

            public Builder serialNumber(String serialNumber) {
                this.serialNumber = serialNumber;
                return this;
            }

            public Builder useADB(boolean useADB) {
                this.useADB = useADB;
                return this;
            }

            public Builder debugTarget(String debugTarget) {
                this.debugTarget = debugTarget;
                return this;
            }

            public Builder traceId(String traceId) {
                this.traceId = traceId;
                return this;
            }

            public Params build() {
                return new Params(debugServer, debugPackage, serialNumber, useADB, debugTarget,
                        traceId);
            }
        }
    }
}
