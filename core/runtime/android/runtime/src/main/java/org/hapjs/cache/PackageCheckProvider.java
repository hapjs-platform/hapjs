/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

public interface PackageCheckProvider {
    String NAME = "package_check";

    boolean hasAppJs(String path);

    boolean isValidCertificate(String pkg, byte[] certificate);
}
