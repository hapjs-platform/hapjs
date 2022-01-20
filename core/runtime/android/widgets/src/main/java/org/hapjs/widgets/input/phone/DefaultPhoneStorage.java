/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.input.phone;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hapjs.bridge.provider.SystemSettings;

class DefaultPhoneStorage implements PhoneStorageProvider {

    private static final String KEY_PHONE_NUMBER_HISTORY = "PhoneNumber.History";
    private static final String INTERVAL = ",";
    private static final int MAX_COUNT = 3;

    @Override
    public boolean add(String number) {
        String value = number;
        String oldValue = SystemSettings.getInstance().getString(KEY_PHONE_NUMBER_HISTORY, null);
        if (!TextUtils.isEmpty(oldValue)) {
            String[] phones = oldValue.split(INTERVAL);
            List<String> list = new ArrayList<>(Arrays.asList(phones));
            list.remove(number);
            list.add(0, number);
            StringBuilder sb = new StringBuilder();
            int count = Math.min(list.size(), MAX_COUNT);
            for (int i = 0; i < count; i++) {
                sb.append(list.get(i)).append(INTERVAL);
            }
            value = sb.toString();
        }
        return SystemSettings.getInstance().putString(KEY_PHONE_NUMBER_HISTORY, value);
    }

    @Override
    public boolean delete(String number) {
        String oldValue = SystemSettings.getInstance().getString(KEY_PHONE_NUMBER_HISTORY, null);
        if (TextUtils.isEmpty(oldValue)) {
            return false;
        }
        String[] phones = oldValue.split(INTERVAL);
        StringBuilder sb = new StringBuilder();
        for (String phone : phones) {
            if (!phone.equals(number)) {
                sb.append(phone).append(INTERVAL);
            }
        }
        return SystemSettings.getInstance().putString(KEY_PHONE_NUMBER_HISTORY, sb.toString());
    }

    @Override
    public List<String> getAll() {
        String value = SystemSettings.getInstance().getString(KEY_PHONE_NUMBER_HISTORY, null);
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String[] phones = value.split(INTERVAL);
        return new ArrayList<>(Arrays.asList(phones));
    }
}
