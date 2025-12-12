/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */

package org.hapjs.render.jsruntime;

import android.os.Handler;
import android.os.ParcelFileDescriptor;
import org.hapjs.common.utils.ProcessUtils;
import org.hapjs.render.action.RenderActionManager;
import org.hapjs.runtime.Runtime;
import org.hapjs.runtime.sandbox.AppChannelReceiver;
import org.hapjs.runtime.sandbox.AppChannelSender;
import org.hapjs.runtime.sandbox.SandboxChannelReceiver;
import org.hapjs.runtime.sandbox.SandboxChannelSender;
import org.hapjs.runtime.sandbox.SandboxConfigs;

public class SandboxProviderImpl implements SandboxProvider {
    @Override
    public boolean isSandboxEnabled() {
        return false;
    }

    @Override
    public boolean isDebugLogEnabled() {
        return ProcessUtils.isSandboxProcess(Runtime.getInstance().getContext()) && SandboxConfigs.isDebugLogEnabled();
    }

    @Override
    public SandboxChannelSender createSandboxChannelSender(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, Handler handler) {
        return new SandboxChannelSender(readSide, writeSide, handler);
    }

    @Override
    public SandboxChannelReceiver createSandboxChannelReceiver(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide,
                                                               SandboxJsThread jsThread) {
        return new SandboxChannelReceiver(readSide, writeSide, jsThread);
    }

    @Override
    public AppChannelSender createAppChannelSender(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, Handler handler) {
        return new AppChannelSender(readSide, writeSide, handler);
    }

    @Override
    public AppChannelReceiver createAppChannelReceiver(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, IJavaNative javaNative) {
        return new AppChannelReceiver(readSide, writeSide, javaNative);
    }

    @Override
    public JsEngineImpl createEngineImpl(JsContext jsContext, V8InspectorNative.InspectorNativeCallback inspectorNativeCallback,
                                         JsEngineImpl.V8ExceptionHandler v8ExceptionHandler, JsEngineImpl.FrameCallback frameCallback) {
        return new JsEngineImpl(jsContext, inspectorNativeCallback, v8ExceptionHandler, frameCallback);
    }

    @Override
    public JavaNativeImpl createNativeImpl(RenderActionManager renderActionManager, JsEngineImpl.FrameCallback frameCallback) {
        return new JavaNativeImpl(renderActionManager, frameCallback);
    }
}
