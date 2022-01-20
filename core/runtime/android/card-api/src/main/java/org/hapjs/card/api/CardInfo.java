/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

import android.net.Uri;
import java.util.Collection;
import java.util.Map;

public interface CardInfo {
    String getTitle();

    String getDescription();

    int getMinPlatformVersion();

    Collection<String> getPermissionDescriptions();

    Uri getIcon();

    Map<String, Map<String, AppDependency>> getDependencies();
}
