/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ExtensionBridge {
    private static final String TAG = "ExtensionBridge";
    protected ConcurrentHashMap<String, AbstractExtension> mExtensions = new ConcurrentHashMap<>();
    private ClassLoader mLoader;

    public ExtensionBridge(ClassLoader loader) {
        mLoader = loader;
    }

    protected abstract ExtensionMetaData getExtensionMetaData(String module);

    public AbstractExtension getExtension(String name) {
        AbstractExtension extension = mExtensions.get(name);
        if (extension == null) {
            ExtensionMetaData extensionMetaData = getExtensionMetaData(name);
            if (extensionMetaData != null) {
                extension = createExtension(mLoader, extensionMetaData);
                if (extension == null) {
                    throw new RuntimeException("Fail to init extension: " + name);
                }
                AbstractExtension oldExtension = mExtensions.putIfAbsent(name, extension);
                // Fail to put extension, use the old value
                if (oldExtension != null) {
                    extension = oldExtension;
                }
            }
        }
        return extension;
    }

    protected AbstractExtension createExtension(
            ClassLoader classLoader, ExtensionMetaData extensionMetaData) {
        try {
            @SuppressWarnings("unchecked")
            Class<AbstractExtension> hfc =
                    (Class<AbstractExtension>) classLoader.loadClass(extensionMetaData.getModule());
            AbstractExtension extension = hfc.newInstance();
            return extension;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "extension not found: " + extensionMetaData.getName(), e);
        } catch (InstantiationException e) {
            Log.e(TAG, "extension cannot be instantiated: " + extensionMetaData.getName(), e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "extension cannot be accessed: " + extensionMetaData.getName(), e);
        }
        return null;
    }
}
