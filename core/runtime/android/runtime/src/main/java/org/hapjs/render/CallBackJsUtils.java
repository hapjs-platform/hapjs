/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.render;

import android.text.TextUtils;
import android.util.Log;

import org.hapjs.component.view.state.State;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.render.vdom.VElement;
import org.json.JSONObject;


/**
 * 节点渲染回调:
 * 1、节点挂载、移除、更新时, 根据前端需要添加节点渲染回调
 * <p>
 * 页面渲染完成\更新完成回调：
 * 1、针对 createFinish、updateFinish指令, 根据前端需要添加渲染完成\更新完成回调
 */
public class CallBackJsUtils {
    private static String TAG = "CallBackJsUtils";
    private static CallBackJsUtils mInstance;

    public static String TYPE_NODE_MOUNTED = "nodeMounted";
    public static String TYPE_NODE_UPDATE = "nodeUpdate";
    public static String TYPE_NODE_DESTROY = "nodeDestroy";
    public static String TYPE_PAGE_CREATE_FINISH = "createFinish";
    public static String TYPE_PAGE_UPDATE_FINISH = "updateFinish";

    public static String TYPE_VALUE_ATTR = "attr";
    public static String TYPE_VALUE_STYLE = "style";

    //节点回调,回传参数
    private String key = "";
    private String oldValue = "";
    private String newValue = "";
    private String valueType = "";

    private static class InnerCallBackJsUtils{
        private static final CallBackJsUtils instance = new CallBackJsUtils();
    }

    public static CallBackJsUtils getInstance() {
        return InnerCallBackJsUtils.instance;
    }

    private CallBackJsUtils() {
    }


    /**
     * 客户端通过 processRenderHooks 将回调事件给前端
     */
    public void callBackJs(JsThread jsThread, int pageId, String hookType, int ref) {
        try {
            JSONObject argsObject = new JSONObject();
            if (ref != 0) {
                argsObject.put("ref", ref);
            }
            if (!TextUtils.isEmpty(key)) {
                argsObject.put("key", key);
            }

            if (!TextUtils.isEmpty(oldValue)) {
                argsObject.put("oldValue", oldValue);
            }

            if (!TextUtils.isEmpty(newValue)) {
                argsObject.put("newValue", newValue);
            }

            if(!TextUtils.isEmpty(valueType)){
                argsObject.put("valueType", valueType);
            }

            jsThread.postExecuteScript("processRenderHooks("
                    + pageId + ","
                    + "\"" + hookType + "\","
                    + argsObject.toString() + ");");

            resetParams();
        } catch (Exception e) {
            Log.e(TAG, "invoke js callback fail!", e);
        }
    }


    /**
     * 获取节点渲染回调需要回传参数
     * 修改前的值: oldValue
     * 修改后的值：newValue
     * 属性名：key
     * 属性类型: valueType
     *
     * @param ele
     * @param action
     */
    public void getCallBackJsParams(VElement ele, VDomChangeAction action) {
        if (ele.getComponent() != null && ele.getComponent().getHook() != null && ele.getComponent().getHook().contains("update")) {
            try {
                if (action.styles.size() == 1) {
                    for (String keyStr : action.styles.keySet()) {
                        this.key = keyStr;
                        this.newValue = action.styles.get(key).get(State.NORMAL).toString();
                        this.oldValue = ele.getComponentDataHolder().getStyleDomData().get(key).get(State.NORMAL).toString();
                        this.valueType = TYPE_VALUE_STYLE;
                    }
                } else if (action.attributes.size() == 1) {
                    for (String keyStr : action.attributes.keySet()) {
                        this.key = keyStr;
                        this.oldValue = ele.getComponentDataHolder().getAttrsDomData().get(key).toString();
                        this.newValue = action.attributes.get(key).toString();
                        this.valueType = TYPE_VALUE_ATTR;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "update style callback fail");
            }
        }
    }


    public void resetParams() {
        key = "";
        oldValue = "";
        newValue = "";
        valueType = "";
    }

}
