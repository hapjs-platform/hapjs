/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

import android.app.Activity;
import android.content.Context;
import org.hapjs.card.api.debug.CardDebugController;
import org.hapjs.card.api.debug.CardDebugService;

public interface CardService {
    void init(Context context, String platform);

    int getPlatformVersion();

    CardInfo getCardInfo(String uri);

    AppInfo getAppInfo(String pkg);

    Card createCard(Activity activity);

    Card createCard(Activity activity, String uri);

    Inset createInset(Activity activity);

    Inset createInset(Activity activity, String uri);

    boolean grantPermissions(String uri);

    void download(String pkg, int versionCode, DownloadListener listener);

    void install(String pkg, String fileUri, InstallListener listener);

    void install(String pkg, int versionCode, InstallListener listener);

    @Deprecated
    //deprecated above version 1040
    CardDebugService getCardDebugService();

    //added in version 1040
    CardDebugController getCardDebugController();

    void setTheme(Context context, String theme);

    void setLogListener(LogListener listener);

    void setConfig(CardConfig config);

    void setRuntimeErrorListener(RuntimeErrorListener listener);

    void uninstall(String pkg, UninstallListener listener);

    void getAllApps(GetAllAppsListener listener);
}
