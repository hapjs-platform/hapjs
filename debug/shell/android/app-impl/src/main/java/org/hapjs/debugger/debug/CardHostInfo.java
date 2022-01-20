/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.debug;

import android.text.TextUtils;
import android.util.Log;
import java.util.Objects;

public class CardHostInfo {
    private static final String TAG = "CardHostInfo";

    // 在通常的情形里, runtimeHost和archiveHost一样, 比如快应用调试器作为卡片宿主时, 两者都是调试器. 此外,
    // 卡片宿主可能被动态加载, 运行在另一个宿主里. 比如某些负一屏的实现里, 负一屏被动态加载, 运行在桌面进程里.
    // 这种情形下, runtimeHost是桌面, archiveHost是负一屏.
    public final String runtimeHost;
    public final String archiveHost;

    public CardHostInfo(String runtimeHost, String archiveHost) {
        this.runtimeHost = runtimeHost;
        this.archiveHost = archiveHost;
    }

    public static CardHostInfo fromString(String str) {
        if (!TextUtils.isEmpty(str)) {
            String[] segs = str.split(";");
            if (segs.length == 2) {
                return new CardHostInfo(segs[0], segs[1]);
            }
        }
        Log.w(TAG, "illegal archiveHost str: " + str);
        return null;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + Objects.hashCode(runtimeHost);
        hash = hash * 31 + Objects.hashCode(archiveHost);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CardHostInfo)) {
            return false;
        }

        return TextUtils.equals(runtimeHost, ((CardHostInfo) obj).runtimeHost)
                && TextUtils.equals(archiveHost, ((CardHostInfo) obj).archiveHost);
    }

    @Override
    public String toString() {
        return runtimeHost + ";" + archiveHost;
    }
}
