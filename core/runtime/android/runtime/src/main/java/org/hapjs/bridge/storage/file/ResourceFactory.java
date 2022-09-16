/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage.file;

import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import org.hapjs.bridge.ApplicationContext;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.utils.FileHelper;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.logging.RuntimeLogManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ResourceFactory implements IResourceFactory {
    private static final String TAG = "ResourceFactory";

    private ApplicationContext mApplicationContext;

    private File mCacheRootFile;
    private File mFilesRootFile;
    private File mMassRootFile;
    private File mPackageRootFile;
    private String mCacheRootCanonicalPath;
    private String mFilesRootCanonicalPath;
    private String mMassRootCanonicalPath;
    private String mPackageRootCanonicalPath;
    private String mInternalDirCanonicalPath;
    private String mExternalDirCanonicalPath;
    private boolean mInitialized;
    private boolean mAppSpecificDirInitialized;
    private Map<String, Resource> mTmpResMap;

    public ResourceFactory(ApplicationContext applicationContext) {
        mApplicationContext = applicationContext;
        mTmpResMap = new HashMap<>();
    }

    public Resource create(String internalUri) {
        initialize();

        internalUri = InternalUriUtils.getValidUri(internalUri);

        Resource resource = mTmpResMap.get(internalUri);
        if (resource != null) {
            return resource;
        }

        File file;
        if (internalUri.startsWith(InternalUriUtils.PACKAGE_PREFIX)) {
            Cache cache =
                    CacheStorage.getInstance(mApplicationContext.getContext())
                            .getCache(mApplicationContext.getPackage());
            file = cache.getResourceFile(internalUri);
            return new ArchiveResource(mApplicationContext, internalUri, file);
        } else if (internalUri.startsWith(InternalUriUtils.CACHE_PREFIX)) {
            String subPath = internalUri.substring(InternalUriUtils.CACHE_PREFIX.length());
            file = new File(mCacheRootFile, subPath);
            return new FileResource(mApplicationContext, internalUri, mCacheRootFile, file);
        } else if (internalUri.startsWith(InternalUriUtils.FILES_PREFIX)) {
            String subPath = internalUri.substring(InternalUriUtils.FILES_PREFIX.length());
            file = new File(mFilesRootFile, subPath);
            return new FileResource(mApplicationContext, internalUri, mFilesRootFile, file);
        } else if (mMassRootFile != null && internalUri.startsWith(InternalUriUtils.MASS_PREFIX)) {
            String subPath = internalUri.substring(InternalUriUtils.MASS_PREFIX.length());
            file = new File(mMassRootFile, subPath);
            return new FileResource(mApplicationContext, internalUri, mMassRootFile, file);
        }
        Log.e(TAG, "getUnderlyingFile failed for internalUri: " + internalUri);
        return null;
    }

    public Resource create(File underlyingFile) {
        initialize();

        String canonicalPath;
        try {
            canonicalPath = underlyingFile.getCanonicalPath();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        String internalUri;
        if (canonicalPath.startsWith(mPackageRootCanonicalPath)) {
            internalUri =
                    InternalUriUtils.PACKAGE_PREFIX
                            + canonicalPath.substring(mPackageRootCanonicalPath.length());
            return new ArchiveResource(mApplicationContext, internalUri, underlyingFile);
        } else if (!TextUtils.isEmpty(mCacheRootCanonicalPath)
                && canonicalPath.startsWith(mCacheRootCanonicalPath)) {
            internalUri =
                    InternalUriUtils.CACHE_PREFIX
                            + canonicalPath.substring(mCacheRootCanonicalPath.length());
            return new FileResource(mApplicationContext, internalUri, mCacheRootFile,
                    underlyingFile);
        } else if (!TextUtils.isEmpty(mFilesRootCanonicalPath)
                && canonicalPath.startsWith(mFilesRootCanonicalPath)) {
            internalUri =
                    InternalUriUtils.FILES_PREFIX
                            + canonicalPath.substring(mFilesRootCanonicalPath.length());
            return new FileResource(mApplicationContext, internalUri, mFilesRootFile,
                    underlyingFile);
        } else if (!TextUtils.isEmpty(mMassRootCanonicalPath)
                && canonicalPath.startsWith(mMassRootCanonicalPath)) {
            internalUri =
                    InternalUriUtils.MASS_PREFIX
                            + canonicalPath.substring(mMassRootCanonicalPath.length());
            return new FileResource(mApplicationContext, internalUri, mMassRootFile,
                    underlyingFile);
        } else {
            if (!isLegalAccessFile(canonicalPath)) {
                RuntimeLogManager.getDefault().recordIllegalAccessFile(mApplicationContext.getPackage(), canonicalPath);
                return null;
            }
            String fileName = underlyingFile.getName();
            internalUri = createTmpInternalUri(fileName);
            FileTmpResource tmpResource = new FileTmpResource(internalUri, underlyingFile);
            mTmpResMap.put(internalUri, tmpResource);
            return tmpResource;
        }
    }

    public Resource create(Uri underlyingUri, boolean needFileName) {
        if (underlyingUri == null) {
            return null;
        }
        initialize();
        if (UriUtils.isFileUri(underlyingUri)) {
            return create(new File(underlyingUri.getPath()));
        } else if (UriUtils.isContentUri(underlyingUri)) {
            String fileName = null;
            String filePath = FileHelper.getFileFromContentUri(mApplicationContext.getContext(), underlyingUri);
            if (needFileName) {
                if (TextUtils.isEmpty(filePath)) {
                    fileName =
                            FileHelper.getDisplayNameFromContentUri(
                                    mApplicationContext.getContext(), underlyingUri);
                } else {
                    fileName = new File(filePath).getName();
                }
            }
            String contentUri = underlyingUri.toString();
            if (!isLegalAccessFile(filePath)) {
                RuntimeLogManager.getDefault().recordIllegalAccessFile(mApplicationContext.getPackage(), contentUri);
                return null;
            }
            if (TextUtils.isEmpty(filePath)) {
                // 通过埋点跟踪是否有获取不到contentUri对应filePath，但这个contentUri是非法访问的情况
                RuntimeLogManager.getDefault().recordIllegalAccessFile(mApplicationContext.getPackage(), "empty filePath , " + contentUri);
            }
            String internalUri = createTmpInternalUri(fileName);
            UriTmpResource resource = new UriTmpResource(internalUri, underlyingUri);
            mTmpResMap.put(internalUri, resource);
            return resource;
        }
        Log.v(TAG, "getInternalUri failed for uri: " + underlyingUri.toString());
        return null;
    }

    public Resource create(ParcelFileDescriptor parcelFileDescriptor) {
        String internalUri = createTmpInternalUri(null);
        FDTmpResource fdTmpResource = new FDTmpResource(internalUri, parcelFileDescriptor);
        mTmpResMap.put(internalUri, fdTmpResource);
        return fdTmpResource;
    }

    private String createTmpInternalUri(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            fileName = UUID.randomUUID().toString();
        }
        return InternalUriUtils.TEMP_PREFIX + UUID.randomUUID().toString() + File.separator
                + fileName;
    }

    private void initialize() {
        if (!mInitialized) {
            try {
                mCacheRootFile = mApplicationContext.getCacheDir();
                mFilesRootFile = mApplicationContext.getFilesDir();
                mMassRootFile = mApplicationContext.getMassDir();
                CacheStorage cacheStorage =
                        CacheStorage.getInstance(mApplicationContext.getContext());
                String pkg = mApplicationContext.getPackage();
                mPackageRootFile = cacheStorage.getCache(pkg).getResourceFile("/");
                mCacheRootCanonicalPath =
                        mCacheRootFile == null ? "" : mCacheRootFile.getCanonicalPath() + "/";
                mFilesRootCanonicalPath =
                        mFilesRootFile == null ? "" : mFilesRootFile.getCanonicalPath() + "/";
                mMassRootCanonicalPath =
                        mMassRootFile == null ? "" : mMassRootFile.getCanonicalPath() + "/";
                mPackageRootCanonicalPath = mPackageRootFile.getCanonicalPath() + "/";
                mInitialized = true;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            // get app-specific directories path
            if (!mAppSpecificDirInitialized) {
                try {
                    File appInternalCacheDir = mApplicationContext.getContext().getCacheDir();
                    File appInternalDir = null;
                    if (appInternalCacheDir != null) {
                        appInternalDir = appInternalCacheDir.getParentFile();
                    }
                    mInternalDirCanonicalPath = appInternalDir == null ?
                            "" : appInternalDir.getCanonicalPath() + "/";

                    File appExternalCacheDir = mApplicationContext.getContext().getExternalCacheDir();
                    File appExternalDir = null;
                    if (appExternalCacheDir != null) {
                        appExternalDir = appExternalCacheDir.getParentFile();
                    }
                    mExternalDirCanonicalPath = appExternalDir == null ?
                            "" : appExternalDir.getCanonicalPath() + "/";

                    mAppSpecificDirInitialized = true;
                } catch (Exception e) {
                    Log.e(TAG, "initialize App-Specific directories fail ", e);
                }
            }
        }
    }

    /**
     * if it is legal access
     *
     * @param filePath absolute path of file
     * @return true if it is legal access
     */

    public boolean isLegalAccessFile(String filePath) {
        // 合法情况1：文件路径为空
        if (TextUtils.isEmpty(filePath)) {
            return true;
        }

        // 合法情况2：文件不在【应用】专属目录下
        if (!isSubWithinParent(mInternalDirCanonicalPath, filePath) && !isSubWithinParent(mExternalDirCanonicalPath, filePath)) {
            return true;
        }

        // 合法情况3：文件在【快应用】专属目录下
        if (isSubWithinParent(mPackageRootCanonicalPath, filePath) || isSubWithinParent(mCacheRootCanonicalPath, filePath) ||
                isSubWithinParent(mFilesRootCanonicalPath, filePath) || isSubWithinParent(mMassRootCanonicalPath, filePath)) {
            return true;
        }

        return false;
    }

    /**
     * if sub is whitin parent
     *
     * @param parentPath
     * @param subPath
     * @return true if sub is whitin parent
     */
    public static boolean isSubWithinParent(String parentPath, String subPath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Path parent = FileSystems.getDefault().getPath(parentPath);
                Path sub = FileSystems.getDefault().getPath(subPath);
                if (!Files.exists(parent) || !Files.isDirectory(parent)) {
                    return false;
                }
                while (null != sub) {
                    if (Files.exists(sub) && Files.isSameFile(parent, sub)) {
                        return true;
                    }
                    sub = sub.getParent();
                }
            } catch (Exception e) {
                Log.e(TAG, "Files.isSameFile fail ", e);
            }
        } else {
            return subPath.startsWith(parentPath);
        }
        return false;
    }

}
