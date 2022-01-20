/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage.file;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.cache.Cache;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.utils.FileHelper;
import org.hapjs.common.utils.UriUtils;

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
    private boolean mInitialized;
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
        if (UriUtils.isFileUri(underlyingUri)) {
            return create(new File(underlyingUri.getPath()));
        } else if (UriUtils.isContentUri(underlyingUri)) {
            String fileName = null;
            if (needFileName) {
                String filePath =
                        FileHelper.getFileFromContentUri(mApplicationContext.getContext(),
                                underlyingUri);
                if (TextUtils.isEmpty(filePath)) {
                    fileName =
                            FileHelper.getDisplayNameFromContentUri(
                                    mApplicationContext.getContext(), underlyingUri);
                } else {
                    fileName = new File(filePath).getName();
                }
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
        }
    }
}
