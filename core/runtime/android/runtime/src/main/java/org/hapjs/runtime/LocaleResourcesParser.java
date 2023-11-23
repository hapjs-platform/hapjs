/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.text.TextUtils;
import android.util.Log;

import org.hapjs.bridge.ApplicationContext;
import org.hapjs.io.RpkSource;
import org.hapjs.io.TextReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class LocaleResourcesParser {

    private static final String TAG = "LocaleResourcesParser";

    private static final String KEY_MESSAGE = "message";
    private static final String KEY_DATE_TIME_FORMAT = "dateTimeFormat";
    private static final String KEY_NUMBER_FORMAT = "numberFormat";
    private static final String DIRECTORY_I18N = "/i18n";
    private static final String DEFAULT_LOCALE_NAME = "defaults";
    private static final String DASH_MARK = "-";
    private static final String SUFFIX_JSON = ".json";

    private static final String TEMPLATE_PATTERN_STRING = "\\$\\{(.+?)\\}";
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile(TEMPLATE_PATTERN_STRING);
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("^[0-9a-zA-Z\\[\\]\\-\\_]+$");

    private Map<String, List<String>> mLocaleNames = new ConcurrentHashMap<>();

    private LocaleResourcesParser() {
    }

    public static LocaleResourcesParser getInstance() {
        return Holder.INSTANCE;
    }

    private static Token getToken(String expression) {
        StringBuilder keySb = new StringBuilder();
        List<Integer> index = new ArrayList<>();
        char[] keyChars = expression.toCharArray();
        for (int i = 0; i < keyChars.length; i++) {
            char c = keyChars[i];
            if (c == '[') {
                index.add(Integer.parseInt(String.valueOf(keyChars[i + 1])));
                continue;
            }
            if (c == ']' || (c >= '0' && c <= '9')) {
                continue;
            }
            keySb.append(c);
        }
        String key = keySb.toString();
        return new Token(key, index);
    }

    private void config(ApplicationContext applicationContext, @Nullable String path) {
        String pkg = applicationContext.getPackage();
        HapEngine hapEngine = HapEngine.getInstance(pkg);
        if (mLocaleNames.size() > 0) {
            List<String> packageRes = mLocaleNames.get(pkg);
            if (packageRes != null && packageRes.size() > 0) {
                // already config
                return;
            }
        }
        String resourcePath = DIRECTORY_I18N;
        if (hapEngine.isCardMode() && !TextUtils.isEmpty(path)) {
            resourcePath = path + DIRECTORY_I18N;
        }
        List<String> packageRes = hapEngine.getResourceManager()
                .getFileNameList(applicationContext.getContext(), pkg, resourcePath);
        if (packageRes != null) {
            mLocaleNames.put(pkg, packageRes);
        } else {
            Log.d(TAG, "no i18n resources found, skip configuration ");
        }
    }

    public Map<String, JSONObject> resolveLocaleResources(String pkg, @Nullable String path, Locale locale) {
        ApplicationContext context = HapEngine.getInstance(pkg).getApplicationContext();
        config(context, path);
        Map<String, JSONObject> result = new LinkedHashMap<>(); // must be an ordered map
        List<String> localeFiles = mLocaleNames.get(pkg);
        if (localeFiles == null) {
            return result;
        }
        try {
            Collections.sort(localeFiles, String::compareToIgnoreCase);
            String language = locale.getLanguage();
            String countryOrRegion = locale.getCountry();
            String fullLocaleName = language + DASH_MARK + countryOrRegion;
            if (!TextUtils.isEmpty(language)) {
                // find resource that exactly match the given locale
                if (localeFiles.contains(fullLocaleName)) {
                    result.put(fullLocaleName,
                            new JSONObject(getLocaleContent(context, path, fullLocaleName)));
                }
                // find resource that using the given language
                if (localeFiles.contains(language)) {
                    result.put(language, new JSONObject(getLocaleContent(context, path, language)));
                }
                // find resource that using same language
                for (String localeName : localeFiles) {
                    if (localeName.startsWith(language + DASH_MARK)) {
                        result.put(localeName,
                                new JSONObject(getLocaleContent(context, path, localeName)));
                    }
                }
            }
            // find default resource
            if (localeFiles.contains(DEFAULT_LOCALE_NAME)) {
                result.put(
                        DEFAULT_LOCALE_NAME,
                        new JSONObject(getLocaleContent(context, path, DEFAULT_LOCALE_NAME)));
            }
        } catch (Exception e) {
            Log.e(TAG, "fail to config locales ", e);
        }
        Log.d(TAG, "resolveLocaleResources: " + result.keySet());
        return result;
    }

    private String getLocaleContent(ApplicationContext context, @Nullable String path, String localeName) {
        String resourcePath = DIRECTORY_I18N + "/" + localeName + SUFFIX_JSON;
        if (HapEngine.getInstance(context.getPackage()).isCardMode() && !TextUtils.isEmpty(path)) {
            resourcePath = path + resourcePath;
        }
        return TextReader.get()
                .read(
                        new RpkSource(
                                context.getContext(),
                                context.getPackage(),
                                resourcePath));
    }

    /**
     * return text that stand for the given content and locale, the content is like:
     * Page{message.titles[0]}
     */
    public String getText(String content, Map<String, JSONObject> resources) {
        if (resources == null || resources.size() == 0) {
            return content;
        }

        String[] nonTemplateStrings = getAllNonTemplateStrings(content);
        String[] templateStrings = getAllTemplateStrings(content, resources);

        if (nonTemplateStrings == null || templateStrings == null) {
            return content;
        }

        StringBuilder sb = new StringBuilder();
        if (nonTemplateStrings.length > 0) {
            for (int i = 0; i < nonTemplateStrings.length; i++) {
                String nonTemplateString = nonTemplateStrings[i];
                if (!TextUtils.isEmpty(nonTemplateString)) {
                    sb.append(nonTemplateString);
                }
                if (i < templateStrings.length) {
                    sb.append(templateStrings[i]);
                }
            }
        } else {
            for (String templateString : templateStrings) {
                if (!TextUtils.isEmpty(templateString)) {
                    sb.append(templateString);
                }
            }
        }
        return sb.toString();
    }

    public String getText(String pkg, Locale locale, String content) {
        Map<String, JSONObject> resources = resolveLocaleResources(pkg, null, locale);
        return getText(content, resources);
    }

    private String[] getAllNonTemplateStrings(String content) {
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        return content.split(TEMPLATE_PATTERN_STRING);
    }

    private String[] getAllTemplateStrings(String content, Map<String, JSONObject> resources) {
        if (TextUtils.isEmpty(content)) {
            return null;
        }

        List<String> templates = new ArrayList<>();
        List<String> result = new ArrayList<>();

        Matcher m = TEMPLATE_PATTERN.matcher(content);
        while (m.find()) {
            String s = m.group(1);
            templates.add(s);
        }
        for (String t : templates) {
            result.add(getTemplateString(t, resources));
        }
        return result.toArray(new String[0]);
    }

    /**
     * return a string that represent this expression
     *
     * @param expression the expression of this template string, like: message.hello[0].title
     * @param resources  the i18n json resources of a given locale
     * @return string that represent this expression according to the resources
     */
    private String getTemplateString(String expression, Map<String, JSONObject> resources) {
        if (TextUtils.isEmpty(expression)) {
            return expression;
        }

        String[] tokens = TextUtils.split(expression, "\\.");
        if (tokens == null || tokens.length == 0) {
            return expression;
        }

        if (!KEY_MESSAGE.equals(tokens[0])) {
            return expression;
        }

        String result = expression;
        int lastIndex = tokens.length - 1;
        for (JSONObject resource : resources.values()) {
            try {
                JSONObject obj = resource;
                for (int i = 0; i < tokens.length; i++) {
                    Token token = getToken(tokens[i]);
                    boolean isArray = token.index.size() > 0;
                    if (i != lastIndex) {
                        obj = isArray ? getJsonObjectFromArray(obj, token) :
                                obj.getJSONObject(token.key);
                    }
                    if (i == lastIndex) {
                        result =
                                isArray ? getStringFromArray(obj, token) : obj.getString(token.key);
                        if (result != null) {
                            return result;
                        }
                    }
                    if (obj == null) {
                        // 中间某一对象找不到直接break，因为这个资源不可能再有对应的字符串了
                        break;
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "getTemplateString: ", e);
            }
        }
        return result;
    }

    private JSONObject getJsonObjectFromArray(JSONObject object, Token token) throws JSONException {
        JSONArray array = object.getJSONArray(token.key);
        JSONObject result = null;
        for (int j = 0; j < token.index.size(); j++) {
            int index = token.index.get(j);
            if (j != token.index.size() - 1) {
                array = array.getJSONArray(index);
            } else {
                result = array.getJSONObject(index);
            }
        }
        return result;
    }

    private String getStringFromArray(JSONObject object, Token token) throws JSONException {
        JSONArray array = object.getJSONArray(token.key);
        String result = null;
        for (int j = 0; j < token.index.size(); j++) {
            int index = token.index.get(j);
            if (j != token.index.size() - 1) {
                array = array.getJSONArray(index);
            } else {
                result = array.getString(index);
            }
        }
        return result;
    }

    private static class Holder {
        static final LocaleResourcesParser INSTANCE = new LocaleResourcesParser();
    }

    static class Token {
        String key;
        List<Integer> index;

        Token(String key, List<Integer> index) {
            this.key = key;
            this.index = index;
        }
    }
}
