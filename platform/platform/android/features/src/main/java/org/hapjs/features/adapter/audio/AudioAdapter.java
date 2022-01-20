/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.adapter.audio;

import android.content.ComponentName;
import android.content.Context;
import org.hapjs.bridge.annotation.InheritedAnnotation;
import org.hapjs.common.resident.ResidentManager;
import org.hapjs.common.utils.ProcessUtils;
import org.hapjs.features.adapter.audio.service.AudioService;
import org.hapjs.features.audio.Audio;
import org.hapjs.features.audio.AudioProxy;
import org.hapjs.features.audio.AudioProxyImpl;

@InheritedAnnotation
public class AudioAdapter extends Audio implements AudioProxy.ServiceInfoCallback {

    @Override
    protected AudioProxy createAudioProxy(
            Context context, String appId, ResidentManager residentManager) {
        return new AudioProxyImpl(context, appId, this, this, residentManager);
    }

    @Override
    public ComponentName getServiceComponentName(Context context) {
        String serviceName = AudioService.class.getName();
        int serviceId = getCurrentLauncherId(context);
        if (serviceId >= 0 && serviceId <= 4) {
            serviceName =
                    new StringBuilder(serviceName).append("$AudioService").append(serviceId)
                            .toString();
        }
        return new ComponentName(context, serviceName);
    }

    private int getCurrentLauncherId(Context context) {
        return getLauncherId(context, ProcessUtils.getCurrentProcessName());
    }

    private int getLauncherId(Context context, String processName) {
        String packageName = context.getPackageName();
        String processPrefix = packageName + ":Launcher";
        if (processName.startsWith(processPrefix)) {
            return Integer.parseInt(processName.substring(processPrefix.length()));
        } else {
            return -1;
        }
    }
}
