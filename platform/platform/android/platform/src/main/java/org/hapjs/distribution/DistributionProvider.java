/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.distribution;

import java.io.InputStream;
import org.hapjs.cache.CacheErrorCode;
import org.hapjs.cache.CacheException;

public interface DistributionProvider {
    String NAME = "package";

    /**
     * @param subpackageName
     * @param destFile
     * @param distributionMeta
     * @return one of {@link CacheErrorCode}.
     */
    int fetch(AppDistributionMeta distributionMeta, String subpackageName, String destFile);

    /**
     * Get stream of rpk
     *
     * @param distributionMeta distributionMeta
     * @param subpackageName   subpackageName
     * @return the stream of rpk
     * @throws CacheException
     */
    InputStream fetch(AppDistributionMeta distributionMeta, String subpackageName)
            throws CacheException;

    boolean needUpdate(String packageName);

    PreviewInfo getPreviewInfo(String packageName) throws CacheException;

    ServerSettings getServerSettings(String packageName);

    boolean needSubpackageUpdate(String packageName, String subpackageName);

    AppDistributionMeta getAppDistributionMeta(String packageName, int versionCode)
            throws CacheException;

    int download(String pkg, int versionCode);
}
