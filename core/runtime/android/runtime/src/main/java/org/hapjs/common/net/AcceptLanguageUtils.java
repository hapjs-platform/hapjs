/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.net;

import android.text.TextUtils;
import android.util.Log;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.runtime.ConfigurationManager;

public class AcceptLanguageUtils {

    private static final String TAG = "AcceptLanguageUtils";

    private static Map<String, String> sAcceptLanguages = new ConcurrentHashMap<>();

    public static String getAcceptLanguage() {
        return getAcceptLanguage(ConfigurationManager.getInstance().getCurrentLocale());
    }

    public static String getAcceptLanguage(Locale locale) {
        if (locale == null) {
            return "";
        }

        String lang = locale.getLanguage();
        String countryOrRegion = locale.getCountry();
        if (TextUtils.isEmpty(lang)) {
            return "";
        }

        String exactMatch = lang;
        if (!TextUtils.isEmpty(countryOrRegion)) {
            exactMatch = lang + "-" + countryOrRegion;
        }

        String acceptLanguage = sAcceptLanguages.get(exactMatch);
        if (TextUtils.isEmpty(acceptLanguage)) {
            String internationalLang = "en";
            String qFactor = ";q=";
            StringBuilder result =
                    TextUtils.isEmpty(countryOrRegion)
                            ? new StringBuilder()
                            : new StringBuilder(exactMatch).append(",");
            result.append(lang).append(qFactor).append(0.9);
            if (!internationalLang.equals(lang)) {
                result.append(",").append(internationalLang).append(qFactor).append(0.8);
            }
            acceptLanguage = result.toString();
            sAcceptLanguages.put(exactMatch, acceptLanguage);
            Log.i(TAG, "getAcceptLanguage: " + acceptLanguage);
        }
        return acceptLanguage;
    }
}
