/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import android.content.Context;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.hapjs.cache.utils.PackageUtils;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.model.SubpackageInfo;

public class PackageInstallerFactory {
    public static PackageInstaller createInstaller(
            Context context,
            String pkg,
            int versionCode,
            long size,
            SubpackageInfo info,
            InputStream stream,
            boolean isUpdate,
            InstallFlag installFlag)
            throws CacheException {
        TeeInputStream teeStream = null;
        ZipInputStream zipStream = null;
        ZipInputStream zipStream2 = null;
        ProgressInputStream progressStream = null;
        try {
            String subpackageName = info == null ? "" : info.getName();
            File tmpArchiveFile = File.createTempFile(pkg, "rpk", context.getCacheDir());
            progressStream = new ProgressInputStream(stream);
            teeStream = new TeeInputStream(progressStream, tmpArchiveFile);
            zipStream = new ZipInputStream(new BufferedInputStream(teeStream));
            if (isSupportStream(zipStream)) {
                File tmpCertFile =
                        File.createTempFile(pkg + subpackageName, "CERT", context.getCacheDir());
                FileUtils.saveToFile(zipStream, tmpCertFile);
                StreamSignature streamSignature = StreamSignature.parse(pkg, tmpCertFile);
                StreamZipExtractor extractor =
                        new StreamZipExtractor(
                                zipStream,
                                teeStream,
                                progressStream,
                                tmpArchiveFile,
                                streamSignature,
                                subpackageName);
                return new StreamPackageInstaller(
                        context,
                        pkg,
                        versionCode,
                        size,
                        isUpdate,
                        info,
                        tmpArchiveFile,
                        extractor,
                        streamSignature,
                        installFlag);
            } else {
                if (info != null) {
                    teeStream.flush();
                    zipStream2 =
                            new ZipInputStream(
                                    new BufferedInputStream(
                                            new HybridInputStream(tmpArchiveFile, teeStream)));
                    TeeZipExtractor extractor =
                            new TeeZipExtractor(zipStream2, teeStream, progressStream,
                                    tmpArchiveFile);
                    return new FileSrpkInstaller(
                            context,
                            pkg,
                            versionCode,
                            size,
                            isUpdate,
                            info,
                            tmpArchiveFile,
                            extractor,
                            installFlag);
                } else {
                    teeStream.skipFully();
                    FileUtils.closeQuietly(teeStream, zipStream, progressStream);
                    File archiveFile = Cache.getArchiveFile(context, pkg);
                    tmpArchiveFile.renameTo(archiveFile);
                    return new FilePackageInstaller(context, pkg, archiveFile, isUpdate);
                }
            }
        } catch (IOException e) {
            FileUtils.closeQuietly(teeStream, zipStream, progressStream, zipStream2);
            throw new CacheException(CacheErrorCode.PACKAGE_READ_FAILED, "Fail to read stream", e);
        }
    }

    /**
     * RPK
     *
     * <p>├── META-INF │ ├── CERT ├── manifest.json ├── ...
     */
    private static boolean isSupportStream(ZipInputStream zis) throws IOException {
        ZipEntry ze = zis.getNextEntry();
        String filename = PackageUtils.FILENAME_META_INFO + "/";
        if (ze != null && filename.equals(ze.getName())) {
            zis.closeEntry();
            ze = zis.getNextEntry();
            filename = PackageUtils.FILENAME_META_INFO + "/" + PackageUtils.FILENAME_CERT;
            if (ze != null && filename.equals(ze.getName())) {
                return true;
            }
        }
        return false;
    }
}
