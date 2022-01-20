/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.input.phone;

import android.text.TextUtils;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hapjs.runtime.ProviderManager;

public class PhoneManager {
    private static final String REGEX =
            "^((13[0-9])|(14[5,7])|(15[0-3,5-9])|(17[0,3,5-8])|(18[0-9])|166|198|199|(147))\\d{8}$";
    private PhoneStorageProvider mPhoneStorage;

    private PhoneManager() {
        PhoneStorageProvider phoneStorage =
                ProviderManager.getDefault().getProvider(PhoneStorageProvider.NAME);
        this.mPhoneStorage = phoneStorage != null ? phoneStorage : new DefaultPhoneStorage();
    }

    public static PhoneManager get() {
        return Holder.INSTANCE;
    }

    private boolean isValidNumber(String number) {
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        Pattern p = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(number);
        return m.matches();
    }

    public boolean saveNumber(String number) {
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        if (!isValidNumber(number)) {
            return false;
        }
        return mPhoneStorage.add(number);
    }

    public boolean deleteNumber(String number) {
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        if (!isValidNumber(number)) {
            return false;
        }
        return mPhoneStorage.delete(number);
    }

    public List<String> getSavedNumbers() {
        return mPhoneStorage.getAll();
    }

    private static final class Holder {
        static final PhoneManager INSTANCE = new PhoneManager();
    }
}
