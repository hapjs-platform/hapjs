/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

import android.view.View;

public interface Card {
    View getView();

    void load();

    void load(String url);

    /* @cardData: Json string, some download data, like downloadUrl,versioncode
     * @param: some params ,host app need send to card*/
    void load(String cardData, String param);

    String getUri();

    void setVisible(boolean visible);

    void setAutoDestroy(boolean autoDestroy);

    void destroy();

    boolean isDestroyed();

    void setMessageCallback(CardMessageCallback callback);

    void sendMessage(int code, String data);

    void fold(boolean fold);

    void setLifecycleCallback(CardLifecycleCallback callback);

    void setRenderListener(IRenderListener listener);

    void onShow();

    void onHide();

    void changeVisibilityManually(boolean enable);

    void setPackageListener(PackageListener listener);
}
