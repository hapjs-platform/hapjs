/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.module;

import android.util.Log;
import android.view.View;
import androidx.viewpager.widget.ViewPager;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hapjs.bridge.Extension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.ModuleExtensionAnnotation;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.common.utils.IntegerUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.constants.Attributes;
import org.hapjs.model.AppInfo;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.render.vdom.VElement;
import org.json.JSONException;
import org.json.JSONObject;

@ModuleExtensionAnnotation(
        name = ModelModule.NAME,
        actions = {
                @ActionAnnotation(name = ModelModule.ACTION_GET_COMPUTED_ATTR, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ModelModule.ACTION_GET_COMPUTED_STYLE, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ModelModule.ACTION_GET_RECT, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = ModelModule.ACTION_GET_COMPONENT, mode = Extension.Mode.SYNC)
        })
public class ModelModule extends ModuleExtension {
    protected static final String NAME = "system.model";
    protected static final String ACTION_GET_COMPUTED_ATTR = "getComputedAttr";
    protected static final String ACTION_GET_COMPUTED_STYLE = "getComputedStyle";
    protected static final String ACTION_GET_RECT = "getBoundingRect";
    protected static final String ACTION_GET_COMPONENT = "getComponent";
    private static final String TAG = "ModelModule";
    private static final String RESULT_WIDTH = "width";
    private static final String RESULT_HEIGHT = "height";
    private static final String RESULT_TOP = "top";
    private static final String RESULT_LEFT = "left";
    private static final String RESULT_RIGHT = "right";
    private static final String RESULT_BOTTOM = "bottom";

    private RootView mRootView;

    @Override
    public void attach(RootView rootView, PageManager pageManager, AppInfo appInfo) {
        mRootView = rootView;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        SerializeObject params = request.getSerializeParams();
        int refId;

        switch (action) {
            case ACTION_GET_RECT:
                refId = params.optInt("ref");
                return getBoundingRect(refId);
            case ACTION_GET_COMPUTED_ATTR:
                refId = params.optInt("ref");
                return getComputedAttr(refId);
            case ACTION_GET_COMPUTED_STYLE:
                refId = params.optInt("ref");
                return getComputedStyle(refId);
            case ACTION_GET_COMPONENT:
                refId = params.optInt("ref");
                return getComponent(refId);
            default:
                return Response.NO_ACTION;
        }
    }

    /**
     * 根据refId获取组件实例
     *
     * @param refId
     * @return
     */
    private Component getComponentByRef(int refId) {
        try {
            VDocument vDoc = mRootView.getDocument();
            VElement vEle = vDoc.getElementById(refId);
            Component vComp = vEle.getComponent();
            return vComp;
        } catch (Exception e) {
            Log.w(TAG, "getComponentByRef by refId: " + refId, e);
            return null;
        }
    }

    /**
     * 获取相对于window的位置
     *
     * @param refId
     * @return
     */
    public Response getBoundingRect(int refId) {
        JSONObject ret = new JSONObject();

        try {
            Component vComp = getComponentByRef(refId);

            View view = vComp.getHostView();

            ret.put(RESULT_WIDTH, view.getWidth());
            ret.put(RESULT_HEIGHT, view.getHeight());

            int[] outLocation = new int[2];
            view.getLocationInWindow(outLocation);
            ret.put(RESULT_LEFT, outLocation[0]);
            ret.put(RESULT_RIGHT, outLocation[0] + view.getWidth());
            ret.put(RESULT_TOP, outLocation[1]);
            ret.put(RESULT_BOTTOM, outLocation[1] + view.getHeight());

            Log.d(
                    TAG,
                    "getBoundingRect by refId: " + refId + "; class: " + vComp.getClass()
                            + "; JSON: " + ret);
        } catch (Exception e) {
            Log.e(TAG, "getBoundingRect by refId: " + refId, e);
            e.printStackTrace();
            ret = null;
        }

        return new Response(ret);
    }

    /**
     * 获取最终的组件属性
     *
     * @param refId
     * @return
     * @throws JSONException
     */
    public Response getComputedAttr(int refId) throws JSONException {
        JSONObject ret = new JSONObject();

        try {
            Component vComp = getComponentByRef(refId);

            Set<String> keyList = vComp.getAttrsDomData().keySet();

            for (String name : keyList) {
                Object value = vComp.retrieveAttr(name);
                ret.put(name, convertValue(value));
            }

            Log.d(
                    TAG,
                    "getComputedAttr by refId: " + refId + ", class: " + vComp.getClass()
                            + ", JSON: " + ret);
        } catch (Exception e) {
            Log.e(TAG, "getComputedAttr by refId: " + refId, e);
            ret = null;
        }

        return new Response(ret);
    }

    /**
     * 获取最终渲染的样式
     *
     * @param refId
     * @return
     * @throws JSONException
     */
    public Response getComputedStyle(int refId) throws JSONException {
        JSONObject ret = new JSONObject();

        try {
            Component vComp = getComponentByRef(refId);

            Set<String> nameList = new HashSet<>();
            nameList.addAll(vComp.getStyleDomData().keySet());
            // 添加常用样式
            nameList.add(Attributes.Style.PADDING);
            nameList.add(Attributes.Style.PADDING_TOP);
            nameList.add(Attributes.Style.PADDING_RIGHT);
            nameList.add(Attributes.Style.PADDING_BOTTOM);
            nameList.add(Attributes.Style.PADDING_LEFT);

            nameList.add(Attributes.Style.MARGIN);
            nameList.add(Attributes.Style.MARGIN_TOP);
            nameList.add(Attributes.Style.MARGIN_RIGHT);
            nameList.add(Attributes.Style.MARGIN_BOTTOM);
            nameList.add(Attributes.Style.MARGIN_LEFT);

            nameList.add(Attributes.Style.BORDER_WIDTH);
            nameList.add(Attributes.Style.BORDER_TOP_WIDTH);
            nameList.add(Attributes.Style.BORDER_RIGHT_WIDTH);
            nameList.add(Attributes.Style.BORDER_BOTTOM_WIDTH);
            nameList.add(Attributes.Style.BORDER_LEFT_WIDTH);

            nameList.add(Attributes.Style.BORDER_COLOR);
            nameList.add(Attributes.Style.BORDER_TOP_COLOR);
            nameList.add(Attributes.Style.BORDER_RIGHT_COLOR);
            nameList.add(Attributes.Style.BORDER_BOTTOM_COLOR);
            nameList.add(Attributes.Style.BORDER_LEFT_COLOR);

            nameList.add(Attributes.Style.BORDER_STYLE);

            // Text等文本组件
            nameList.add(Attributes.Style.LINES);
            nameList.add(Attributes.Style.LINE_HEIGHT);
            nameList.add(Attributes.Style.COLOR);
            nameList.add(Attributes.Style.FONT_SIZE);
            nameList.add(Attributes.Style.FONT_STYLE);
            nameList.add(Attributes.Style.FONT_WEIGHT);
            nameList.add(Attributes.Style.TEXT_DECORATION);
            nameList.add(Attributes.Style.TEXT_ALIGN);
            nameList.add(Attributes.Style.VALUE);
            nameList.add(Attributes.Style.CONTENT);
            nameList.add(Attributes.Style.TEXT_OVERFLOW);

            for (String name : nameList) {
                Object value = vComp.retrieveAttr(name);
                ret.put(name, convertValue(value));
            }

            // 内容
            ret.put("innerText", getInnerText(vComp));

            Log.d(
                    TAG,
                    "getComputedStyle by refId: "
                            + refId
                            + ", class: "
                            + vComp.getClass()
                            + ", JSON: "
                            + ret);
        } catch (Exception e) {
            Log.e(TAG, "getComputedStyle by refId: " + refId, e);
            ret = null;
        }

        return new Response(ret);
    }

    /**
     * 获取组件的节点信息
     *
     * @param refId
     * @return
     */
    public Response getComponent(int refId) {
        JSONObject ret = new JSONObject();

        try {
            Component vComp = getComponentByRef(refId);

            // 补充type
            ret.put("type", "");
            ret.put("ref", refId);

            // 属性
            JSONObject vAttr = new JSONObject();
            Map<String, Object> vAttrMap = vComp.getAttrsDomData();
            for (String key : vAttrMap.keySet()) {
                vAttr.put(key, vAttrMap.get(key).toString());
            }
            ret.put("attr", vAttr);

            // 样式
            JSONObject vStyle = new JSONObject();
            Set<String> keyList = vComp.getStyleDomData().keySet();
            for (String key : keyList) {
                Object val = vComp.getCurStateStyle(key, null);
                vStyle.put(key, convertValue(val));
            }
            ret.put("style", vStyle);

            Log.d(
                    TAG,
                    "getComponent by refId: " + refId + ", class: " + vComp.getClass()
                            + ", JSON: " + ret);
        } catch (Exception e) {
            Log.e(TAG, "getComponent by refId: " + refId, e);
            ret = null;
        }

        return new Response(ret);
    }

    public Object convertValue(Object value) {
        try {
            if (value == null) {
                return "";
            } else if (value instanceof String) {
                return value;
            } else if (value instanceof Integer) {
                Integer iValue = (Integer) value;
                if (iValue.equals(IntegerUtil.UNDEFINED)) {
                    return "";
                } else {
                    return iValue;
                }
            } else if (value instanceof Float) {
                Float iValue = (Float) value;
                if (iValue.equals(FloatUtil.UNDEFINED)) {
                    return "";
                } else {
                    return iValue;
                }
            } else if (value.getClass().isArray()) {
                // 数组用空格间隔
                StringBuilder sb = new StringBuilder();
                if ("int".equals(value.getClass().getComponentType().toString())) {
                    int[] listInt = (int[]) value;
                    for (Object v : listInt) {
                        sb.append(convertValue(v) + " ");
                    }
                } else if ("float".equals(value.getClass().getComponentType().toString())) {
                    float[] listInt = (float[]) value;
                    for (Object v : listInt) {
                        sb.append(convertValue(v) + " ");
                    }
                } else if (String.class == value.getClass().getComponentType()) {
                    String[] listInt = (String[]) value;
                    for (Object v : listInt) {
                        sb.append(convertValue(v) + " ");
                    }
                }
                return sb.toString().trim();
            } else {
                return value;
            }
        } catch (Exception e) {
            Log.e(TAG, "convertValue exception ", e);
        }
        return "";
    }

    /**
     * 获取节点的文本内容
     *
     * @param comp
     * @return
     */
    public String getInnerText(Component comp) {
        if (comp == null) {
            return null;
        }

        Object value = comp.retrieveAttr(Attributes.Style.VALUE);

        if (value != null) {
            return value.toString().trim();
        } else if (comp instanceof Container) {
            Container containerComponent = (Container) comp;

            if (containerComponent.getHostView() instanceof ViewPager) {
                // TabContent
                ViewPager tabContentView = (ViewPager) containerComponent.getHostView();
                return getInnerText(containerComponent.getChildAt(tabContentView.getCurrentItem()));
            }

            StringBuilder sb = null;
            for (int i = 0; i < containerComponent.getChildCount(); i++) {
                Component childComp = containerComponent.getChildAt(i);
                String childCompValue = getInnerText(childComp);
                if (childCompValue != null) {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }
                    sb.append(childCompValue);
                }
            }

            if (sb == null) {
                return null;
            } else {
                return sb.toString();
            }
        } else {
            return null;
        }
    }
}
