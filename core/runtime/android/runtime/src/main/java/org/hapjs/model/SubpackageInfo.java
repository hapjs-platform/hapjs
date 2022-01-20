/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.hapjs.logging.RuntimeLogManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SubpackageInfo {
    public static final String BASE_PKG_NAME = "base";
    private static final String TAG = "SubpackageInfo";
    private static final String KEY_NAME = "name";
    private static final String KEY_RESOURCE = "resource";
    private static final String KEY_STANDALONE = "standalone";
    private static final String KEY_SRC = "src";
    private static final String KEY_PAGES = "pages";
    private static final String KEY_SIZE = "size";
    // 兼容快游戏
    private static final String KEY_RESOURCE_ALIAS = "root";
    private static final String KEY_PAGE_NAME = "name";
    private static final String KEY_PAGE_PATH = "path";
    private static final String KEY_PAGE_ENTRY = "entry";
    /**
     * mName : base mSrc : http://cdn.server/base.srpk mStandalone : true mResource : * mPages :
     * [{"mName":"Index","mPath":"/Index","mEntry":true}]
     */
    private String mName;

    private String mSrc;
    private boolean mStandalone;
    private String mResource;
    private List<PageBean> mPages;
    private long mSize;

    private SubpackageInfo(String name, boolean standalone, String resource) {
        this(name, null, standalone, resource, null, 0);
    }

    private SubpackageInfo(
            String name,
            String src,
            boolean standalone,
            String resource,
            List<PageBean> pages,
            long size) {
        mName = name;
        mSrc = src;
        mStandalone = standalone;
        mResource = resource;
        mPages = pages;
        mSize = size;
    }

    public static SubpackageInfo getTargetSubpackageByPageOrPath(
            List<SubpackageInfo> subpackageInfos, String pageOrPath) {
        SubpackageInfo base = null;
        for (SubpackageInfo subpackageInfo : subpackageInfos) {
            if (subpackageInfo.contain(pageOrPath)) {
                return subpackageInfo;
            } else if (subpackageInfo.isBase()) {
                base = subpackageInfo;
            }
        }
        return base;
    }

    public static SubpackageInfo getTargetSubpackageBySubpackageName(
            List<SubpackageInfo> subpackageInfos, String subpackage) {
        for (SubpackageInfo subpackageInfo : subpackageInfos) {
            if (subpackageInfo.getName().equals(subpackage)) {
                return subpackageInfo;
            }
        }
        return null;
    }

    public static SubpackageInfo parse(String pkgName, JSONObject object) {
        try {
            String name = object.getString(KEY_NAME);
            String src = object.optString(KEY_SRC);
            boolean standalone = object.optBoolean(KEY_STANDALONE);
            long size = object.optLong(KEY_SIZE);
            String resource = object.optString(KEY_RESOURCE);
            if (TextUtils.isEmpty(resource)) {
                resource = object.optString(KEY_RESOURCE_ALIAS);
            }

            List<PageBean> pageBeans = new ArrayList<>();
            JSONArray pageArray = object.optJSONArray(KEY_PAGES);
            if (pageArray != null) {
                for (int i = 0; i < pageArray.length(); ++i) {
                    Object rawObject = pageArray.get(i);
                    if (rawObject instanceof JSONObject) {
                        pageBeans.add(PageBean.parse((JSONObject) (rawObject)));
                    } else {
                        Log.e(TAG, "pages can not contain " + rawObject);
                        RuntimeLogManager.getDefault().logSubpackageInfoError(pkgName);
                    }
                }
            }

            return new SubpackageInfo(name, src, standalone, resource, pageBeans, size);
        } catch (JSONException e) {
            throw new IllegalStateException("Illegal subpackage settings", e);
        }
    }

    public static List<SubpackageInfo> parseInfosFromManifest(
            String pkgName,
            JSONArray subpackageArray,
            Map<String, PageInfo> pageInfos,
            PageInfo entryPage) {
        if (subpackageArray == null || subpackageArray.length() == 0) {
            return null;
        }

        List<SubpackageInfo> list = new ArrayList<>();
        List<PageBean> pageBeans = toPageBeans(pageInfos, entryPage);
        for (int i = 0; i < subpackageArray.length(); i++) {
            JSONObject item = subpackageArray.optJSONObject(i);
            if (item != null) {
                SubpackageInfo info = SubpackageInfo.parse(pkgName, item);
                info.setPages(getPages(info.getResource(), pageBeans));
                list.add(info);
            }
        }
        SubpackageInfo main = new SubpackageInfo(SubpackageInfo.BASE_PKG_NAME, true, "");
        main.setPages(pageBeans);
        list.add(main);
        return list;
    }

    private static List<PageBean> toPageBeans(Map<String, PageInfo> pageInfos, PageInfo entryPage) {
        if (pageInfos == null) {
            return null;
        }

        List<PageBean> pageBeans = new ArrayList<>();
        for (Map.Entry<String, PageInfo> entry : pageInfos.entrySet()) {
            PageInfo pageInfo = entry.getValue();
            boolean isEntry = TextUtils.equals(pageInfo.getName(), entryPage.getName());
            pageBeans.add(new PageBean(pageInfo.getName(), pageInfo.getPath(), isEntry));
        }
        return pageBeans;
    }

    public List<PageBean> getPages() {
        return mPages;
    }

    private static List<PageBean> getPages(String resource, List<PageBean> allPageBeans) {
        if (allPageBeans == null) {
            return null;
        }

        List<PageBean> pageBeans = new ArrayList<>();
        Iterator<PageBean> itr = allPageBeans.iterator();
        while (itr.hasNext()) {
            PageBean pageBean = itr.next();
            if (pageBean.getName().startsWith(resource)) {
                pageBeans.add(pageBean);
                itr.remove();
            }
        }
        return pageBeans;
    }

    public static List<SubpackageInfo> parseInfosFromServer(String pkgName, String infoStr) {
        List<SubpackageInfo> list = new ArrayList<>();
        if (!TextUtils.isEmpty(infoStr)) {
            try {
                JSONArray subpackageArray = new JSONArray(infoStr);
                for (int i = 0; i < subpackageArray.length(); i++) {
                    Object item = subpackageArray.opt(i);
                    if (item instanceof JSONObject) {
                        SubpackageInfo info = SubpackageInfo.parse(pkgName, (JSONObject) item);
                        list.add(info);
                    } else {
                        Log.e(TAG, "subpackages can not contain " + item);
                        RuntimeLogManager.getDefault().logSubpackageInfoError(pkgName);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "failed to parse subpackageInfos", e);
            }
        }
        return list;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(KEY_NAME, mName);
            json.put(KEY_SRC, mSrc);
            json.put(KEY_STANDALONE, mStandalone);
            json.put(KEY_RESOURCE, mResource);
            json.put(KEY_SIZE, mSize);

            JSONArray pageArray = new JSONArray();
            if (mPages != null) {
                for (PageBean page : mPages) {
                    pageArray.put(page.toJson());
                }
            }
            json.put(KEY_PAGES, pageArray);
        } catch (JSONException e) {
            Log.e(TAG, "failed to toJson", e);
        }
        return json;
    }

    public static JSONArray toJson(List<SubpackageInfo> infos) {
        JSONArray jsonArray = new JSONArray();
        if (infos != null) {
            for (SubpackageInfo info : infos) {
                jsonArray.put(info.toJson());
            }
        }
        return jsonArray;
    }

    public boolean isBase() {
        return BASE_PKG_NAME.equals(mName);
    }

    public boolean containPage(String pageName) {
        if (TextUtils.isEmpty(pageName)) {
            return false;
        }

        if (mPages != null) {
            for (int i = 0; i < mPages.size(); i++) {
                PageBean page = mPages.get(i);
                if (page.getName().equals(pageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        if (mPages != null) {
            for (int i = 0; i < mPages.size(); i++) {
                PageBean page = mPages.get(i);
                if ("/".equals(path) && page.isEntry() || page.getPath().equals(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containResource(String resPath) {
        if (TextUtils.isEmpty(mResource) || TextUtils.isEmpty(resPath)) {
            return false;
        }

        // 快游戏支持将一个js文件指定为一个分包
        if (mResource.endsWith(".js")) {
            return resPath.equals(mResource);
        }

        String normalizedResource = mResource.endsWith("/") ? mResource : (mResource + "/");
        resPath = resPath.startsWith("/") ? resPath.substring(1) : resPath;
        return resPath.startsWith(normalizedResource) || resPath.equals(mResource);
    }

    public boolean contain(String pageOrPath) {
        return containPage(pageOrPath)
                || containResource(pageOrPath)
                || containPath(TextUtils.isEmpty(pageOrPath) ? "/" : pageOrPath);
    }

    public void setPages(List<PageBean> pages) {
        this.mPages = pages;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getSrc() {
        return mSrc;
    }

    public void setSrc(String src) {
        this.mSrc = src;
    }

    public boolean isStandalone() {
        return mStandalone;
    }

    public void setStandalone(boolean standalone) {
        this.mStandalone = standalone;
    }

    public String getResource() {
        return mResource;
    }

    public void setResource(String resource) {
        this.mResource = resource;
    }

    public long getSize() {
        return mSize;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public static class PageBean {
        /**
         * mName : Index mPath : /Index mEntry : true
         */
        private String mName;

        private String mPath;
        private boolean mEntry;

        public PageBean(String name, String path, boolean entry) {
            this.mName = name;
            this.mPath = path;
            this.mEntry = entry;
        }

        public static PageBean parse(JSONObject jsonObject) throws JSONException {
            String name = jsonObject.getString(KEY_PAGE_NAME);
            String path = jsonObject.optString(KEY_PAGE_PATH);
            boolean standalone = jsonObject.optBoolean(KEY_PAGE_ENTRY);
            return new PageBean(name, path, standalone);
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            this.mName = name;
        }

        public String getPath() {
            return TextUtils.isEmpty(mPath) ? "/" + mName : mPath;
        }

        public void setPath(String path) {
            this.mPath = path;
        }

        public boolean isEntry() {
            return mEntry;
        }

        public void setEntry(boolean entry) {
            this.mEntry = entry;
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put(KEY_PAGE_NAME, mName);
                json.put(KEY_PAGE_PATH, mPath);
                json.put(KEY_PAGE_ENTRY, mEntry);
            } catch (JSONException e) {
                Log.e(TAG, "failed to toJson", e);
            }
            return json;
        }
    }
}
