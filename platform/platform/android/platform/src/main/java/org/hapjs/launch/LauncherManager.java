/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.launch;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.ProcessUtils;
import org.hapjs.logging.RuntimeLogManager;

public class LauncherManager {
    private static final String TAG = "LauncherManager";
    private static List<LauncherClient> sLauncherClients = new ArrayList<>();

    public static void addClient(LauncherClient client) {
        sLauncherClients.add(client);
    }

    public static void removeClient(LauncherClient client) {
        sLauncherClients.remove(client);
    }

    public static void launch(Context context, Intent intent) {
        LauncherClient launcherClient = getLauncherClient(intent);
        if (launcherClient == null) {
            Log.w(TAG, "Fail to find responsible LauncherClient");
            return;
        }

        String pkg = launcherClient.getPackage(intent);
        if (pkg == null || pkg.isEmpty()) {
            Log.w(TAG, "Package can't be empty");
            return;
        }

        if (launcherClient.needLauncherId()) {
            Launcher.LauncherInfo launcherInfo = Launcher.select(context, pkg);
            if (launcherInfo == null) {
                throw new RuntimeException("Fail to select launcherInfo");
            }
            if (!launcherInfo.isAlive) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }
            intent.setClassName(context, launcherClient.getClassName(launcherInfo.id));
        } else {
            intent.setClassName(context, launcherClient.getClassName(-1));
        }
        launcherClient.launch(context, intent);
    }

    public static boolean active(Context context, String pkg) {
        return Launcher.active(context, pkg);
    }

    private static void inactive(Context context, String pkg) {
        Launcher.inactive(context, pkg);
    }

    public static void inactiveAsync(Context context, String pkg) {
        Executors.io()
                .execute(
                        () -> {
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskStart(pkg, "LauncherManager#inactive");
                            inactive(context, pkg);
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskEnd(pkg, "LauncherManager#inactive");
                        });
    }

    private static void updateResidentType(Context context, String pkg, int residentType) {
        Launcher.updateResidentType(context, pkg, residentType);
    }

    public static void updateResidentTypeAsync(Context context, String pkg, int residentType) {
        Executors.io()
                .execute(
                        () -> {
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskStart(pkg,
                                            "LauncherManager#updateResidentType");
                            updateResidentType(context, pkg, residentType);
                            RuntimeLogManager.getDefault()
                                    .logAsyncThreadTaskEnd(pkg,
                                            "LauncherManager#updateResidentType");
                        });
    }

    public static int getCurrentLauncherId(Context context) {
        return getLauncherId(context, ProcessUtils.getCurrentProcessName());
    }

    public static String getLauncherProcessName(Context context, int launcherId) {
        return context.getPackageName() + ":Launcher" + launcherId;
    }

    private static int getLauncherId(Context context, String processName) {
        String packageName = context.getPackageName();
        String processPrefix = packageName + ":Launcher";
        if (processName.startsWith(processPrefix)) {
            return Integer.parseInt(processName.substring(processPrefix.length()));
        } else {
            throw new IllegalStateException("Illegal process name: " + processName);
        }
    }

    public static LauncherClient getLauncherClient(Intent intent) {
        for (LauncherClient client : sLauncherClients) {
            if (client.respond(intent)) {
                return client;
            }
        }
        return null;
    }

    public interface LauncherClient {
        boolean respond(Intent intent);

        boolean needLauncherId();

        String getPackage(Intent intent);

        String getClassName(int launcherId);

        void launch(Context context, Intent intent);
    }
}
