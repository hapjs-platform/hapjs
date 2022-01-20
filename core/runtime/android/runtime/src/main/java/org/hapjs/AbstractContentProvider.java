/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import java.io.FileNotFoundException;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.runtime.Runtime;

/**
 * instead of {@link android.content.ContentProvider}
 */
public abstract class AbstractContentProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        if (context != null) {
            context = context.getApplicationContext() == null ? new ContextProxy(context) : context;
            Runtime.getInstance().onPreCreate(context);
            Runtime.getInstance().onCreate(context);
        }
        // otherwise wait for application context create
    }

    @Override
    public final Cursor query(
            Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        checkApplicationOnCreate();
        return doQuery(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public final String getType(Uri uri) {
        checkApplicationOnCreate();
        return doGetType(uri);
    }

    @Override
    public final Uri insert(Uri uri, ContentValues values) {
        checkApplicationOnCreate();
        return doInsert(uri, values);
    }

    @Override
    public final int delete(Uri uri, String selection, String[] selectionArgs) {
        checkApplicationOnCreate();
        return doDelete(uri, selection, selectionArgs);
    }

    @Override
    public final int update(Uri uri, ContentValues values, String selection,
                            String[] selectionArgs) {
        checkApplicationOnCreate();
        return doUpdate(uri, values, selection, selectionArgs);
    }

    @Override
    public final Bundle call(String method, String arg, Bundle extras) {
        checkApplicationOnCreate();
        return doCall(method, arg, extras);
    }

    @Override
    public final ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        checkApplicationOnCreate();
        return doOpenFile(uri, mode);
    }

    @Override
    public final AssetFileDescriptor openAssetFile(Uri uri, String mode)
            throws FileNotFoundException {
        checkApplicationOnCreate();
        return doOpenAssetFile(uri, mode);
    }

    protected void checkApplicationOnCreate() {
        // 同一个进程时不做处理，避免application初始化时调用provider出现死锁
        if (Binder.getCallingPid() == Process.myPid()) {
            return;
        }
        Runtime.getInstance().waitUntilCreated();
        RuntimeLogManager.getDefault()
                .logExternalCall(getContext(), Binder.getCallingUid(), getClass());
    }

    public Cursor doQuery(
            Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    public String doGetType(Uri uri) {
        return null;
    }

    public Uri doInsert(Uri uri, ContentValues values) {
        return null;
    }

    public int doDelete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    public int doUpdate(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    public Bundle doCall(String method, String arg, Bundle extras) {
        return super.call(method, arg, extras);
    }

    public ParcelFileDescriptor doOpenFile(Uri uri, String mode) throws FileNotFoundException {
        return super.openFile(uri, mode);
    }

    public AssetFileDescriptor doOpenAssetFile(Uri uri, String mode) throws FileNotFoundException {
        return super.openAssetFile(uri, mode);
    }

    public class ContextProxy extends ContextWrapper {

        public ContextProxy(Context base) {
            super(base);
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }
    }
}
