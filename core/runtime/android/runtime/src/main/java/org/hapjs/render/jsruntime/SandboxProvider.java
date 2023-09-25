/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */

package org.hapjs.render.jsruntime;

import android.os.Handler;
import android.os.ParcelFileDescriptor;
import org.hapjs.render.action.RenderActionManager;
import org.hapjs.runtime.sandbox.AppChannelReceiver;
import org.hapjs.runtime.sandbox.AppChannelSender;
import org.hapjs.runtime.sandbox.SandboxChannelReceiver;
import org.hapjs.runtime.sandbox.SandboxChannelSender;

public interface SandboxProvider {
    String NAME = "SandboxProvider";
    boolean isSandboxEnabled();
    boolean isDebugLogEnabled();
    SandboxChannelSender createSandboxChannelSender(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, Handler handler);
    SandboxChannelReceiver createSandboxChannelReceiver(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, SandboxJsThread jsThread);
    AppChannelSender createAppChannelSender(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, Handler handler);
    AppChannelReceiver createAppChannelReceiver(ParcelFileDescriptor readSide, ParcelFileDescriptor writeSide, IJavaNative javaNative);
    JsEngineImpl createEngineImpl(JsContext jsContext, V8InspectorNative.InspectorNativeCallback inspectorNativeCallback,
                                  JsEngineImpl.V8ExceptionHandler v8ExceptionHandler, JsEngineImpl.FrameCallback frameCallback);
    JavaNativeImpl createNativeImpl(RenderActionManager renderActionManager, JsEngineImpl.FrameCallback frameCallback);
}
