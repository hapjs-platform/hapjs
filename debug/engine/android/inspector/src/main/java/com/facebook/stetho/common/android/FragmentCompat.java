/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.common.android;

import android.app.Activity;
import android.os.Build;
import com.facebook.stetho.common.ReflectionUtil;
import java.lang.reflect.Field;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Compatibility abstraction which allows us to generalize access to both the support library's
 * fragments and the built-in framework version. Note: both versions can be live at the same time in
 * a single application and even on a single object instance.
 *
 * <p>Type safety is enforced via generics internal to the implementation but treated as opaque from
 * the outside.
 *
 * @param <T> FRAGMENT
 * @param <S> DIALOG_FRAGMENT
 * @param <U> FRAGMENT_MANAGER
 * @param <V> FRAGMENT_ACTIVITY
 */
@NotThreadSafe
public abstract class FragmentCompat<
        T, S, U, V extends Activity> {
    private static final boolean sHasSupportFragment;
    private static FragmentCompat sFrameworkInstance;
    private static FragmentCompat sSupportInstance;

    static {
        sHasSupportFragment =
                ReflectionUtil.tryGetClassForName("android.support.v4.app.Fragment") != null;
    }

    FragmentCompat() {
    }

    @Nullable
    public static FragmentCompat getFrameworkInstance() {
        if (sFrameworkInstance == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            sFrameworkInstance = new FragmentCompatFramework();
        }
        return sFrameworkInstance;
    }

    @Nullable
    public static FragmentCompat getSupportLibInstance() {
        if (sSupportInstance == null && sHasSupportFragment) {
            sSupportInstance = new FragmentCompatSupportLib();
        }
        return sSupportInstance;
    }

    public abstract Class<T> getFragmentClass();

    public abstract Class<S> getDialogFragmentClass();

    public abstract Class<V> getFragmentActivityClass();

    public abstract FragmentAccessor<T, U> forFragment();

    public abstract DialogFragmentAccessor<S, T, U> forDialogFragment();

    public abstract FragmentManagerAccessor<U, T> forFragmentManager();

    public abstract FragmentActivityAccessor<V, U> forFragmentActivity();

    static class FragmentManagerAccessorViaReflection<M, F>
            implements FragmentManagerAccessor<M, F> {
        @Nullable
        private Field mFieldMAdded;

        @SuppressWarnings("unchecked")
        @Nullable
        @Override
        public List<F> getAddedFragments(M fragmentManager) {
            // This field is actually sitting on FragmentManagerImpl, which derives from FragmentManager.
            if (mFieldMAdded == null) {
                Field fieldMAdded =
                        ReflectionUtil.tryGetDeclaredField(fragmentManager.getClass(), "mAdded");

                if (fieldMAdded != null) {
                    fieldMAdded.setAccessible(true);
                    mFieldMAdded = fieldMAdded;
                }
            }

            return (mFieldMAdded != null)
                    ? (List<F>) ReflectionUtil.getFieldValue(mFieldMAdded, fragmentManager)
                    : null;
        }
    }
}
