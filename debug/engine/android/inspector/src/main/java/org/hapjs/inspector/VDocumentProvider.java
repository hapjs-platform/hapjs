/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.ThreadBound;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.ComputedStyleAccumulator;
import com.facebook.stetho.inspector.elements.Descriptor;
import com.facebook.stetho.inspector.elements.DescriptorMap;
import com.facebook.stetho.inspector.elements.DocumentProvider;
import com.facebook.stetho.inspector.elements.DocumentProviderListener;
import com.facebook.stetho.inspector.elements.NodeDescriptor;
import com.facebook.stetho.inspector.elements.NodeType;
import com.facebook.stetho.inspector.elements.StyleAccumulator;
import com.facebook.stetho.inspector.elements.StyleRuleNameAccumulator;
import com.facebook.stetho.inspector.elements.android.ActivityTracker;
import com.facebook.stetho.inspector.elements.android.HighlightableDescriptor;
import com.facebook.stetho.inspector.helper.ThreadBoundProxy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.hapjs.bridge.HybridView;
import org.hapjs.render.Page;
import org.hapjs.render.RootView;
import org.hapjs.render.VDomChangeAction;
import org.hapjs.render.css.CSSInlineStyleRule;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.runtime.RuntimeActivity;
import org.hapjs.runtime.inspect.InspectorVElementType;

public class VDocumentProvider extends ThreadBoundProxy
        implements DocumentProvider, Descriptor.Host {

    private static final String TAG = "VDocumentProvider";
    private static int sPageId;
    private final DescriptorMap mDescriptorMap;
    private final Rect mHighlightingBoundsRect = new Rect();
    private @Nullable DocumentProviderListener mListener;
    private VDocumentRoot mDocumentRoot;
    private Application mApplication;
    private ViewHighlighter mHighlighter;
    private WeakReference<RootView> mRootView;

    // TODO use weak reference as key
    // private static Map<Object, Integer> Document_PageId_Maps = new HashMap<Object, Integer>();

    public VDocumentProvider(Application application, ThreadBound enforcer) {
        super(enforcer);
        mApplication = application;
        mDocumentRoot = new VDocumentRoot();
        updateRoot();
        mDescriptorMap =
                new DescriptorMap()
                        .beginInit()
                        .registerDescriptor(InspectorVDocument.class, new VGroupDescriptor())
                        .registerDescriptor(InspectorVGroup.class, new VGroupDescriptor())
                        .registerDescriptor(InspectorVElement.class, new VElementDescriptor())
                        .registerDescriptor(VDocumentRoot.class, mDocumentRoot)
                        .setHost(this)
                        .endInit();

        mHighlighter = ViewHighlighter.newInstance();

        Holder.CURRENT = this; // save as current provider
    }

    public static int getPageId() {
        return sPageId;
    }

    private static void setPageId(int pageId) {
        VDocumentProvider.sPageId = pageId;
    }

    private static void addElmenet(
            InspectorVDocument doc, VDomChangeAction action, InspectorVGroup parent) {
        InspectorVElement current = null;
        if (action.inspectorVElementType == InspectorVElementType.VGROUP) {
            current = new InspectorVGroup(doc, action);
        } else if (action.inspectorVElementType == InspectorVElementType.VELEMENT) {
            current = new InspectorVElement(doc, action);
        } else {
            // Consistent behavior to rendering && logcat message
            current = new InspectorVElement(doc, action);
            Log.e("VDocumentProvider", "In addElmenet unkown InspectorVElementType",
                    new Exception());
        }
        parent.addChild(current, action.index);
        for (VDomChangeAction child : action.children) {
            addElmenet(doc, child, (InspectorVGroup) current);
        }
    }

    private static void removeElmenet(InspectorVDocument doc, VDomChangeAction action) {
        InspectorVElement current = doc.getElementById(action.vId);
        InspectorVGroup parent = (InspectorVGroup) (doc.getElementById(action.parentVId));
        if (parent == null && current != null) {
            parent = current.getParent();
        }
        if (parent != null && current != null) {
            parent.removeChild(current);
        }
    }

    public static VDocumentProvider getCurrent() {
        return Holder.CURRENT;
    }

    public void setListener(DocumentProviderListener listener) {
        verifyThreadAccess();
        mListener = listener;
    }

    public void dispose() {
        verifyThreadAccess();

        mHighlighter.clearHighlight();
        mListener = null;
    }

    @Nullable
    public Object getRootElement() {
        verifyThreadAccess();
        return mDocumentRoot;
    }

    @Nullable
    public NodeDescriptor getNodeDescriptor(@Nullable Object element) {
        try {
            verifyThreadAccess();
            return getDescriptor(element);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void highlightElement(Object element, int color) {
        verifyThreadAccess();

        final HighlightableDescriptor descriptor = getHighlightableDescriptor(element);

        if (descriptor == null) {
            mHighlighter.clearHighlight();
            return;
        }

        mHighlightingBoundsRect.setEmpty();
        final View highlightingView =
                descriptor.getViewAndBoundsForHighlighting(element, mHighlightingBoundsRect);

        if (highlightingView == null) {
            mHighlighter.clearHighlight();
            return;
        }

        mHighlighter.setHighlightedView(highlightingView, mHighlightingBoundsRect, color);
    }

    public void hideHighlight() {
        verifyThreadAccess();

        mHighlighter.clearHighlight();
    }

    public void setInspectModeEnabled(boolean enabled) {
    }

    public void setAttributesAsText(Object element, String text) {
    }

    @Override
    public void setAttributesAsText(Object element, String name, String text) {
        Descriptor descriptor = mDescriptorMap.get(element.getClass());
        if (descriptor != null) {
            descriptor.setAttributesAsText(element, name, text);
        }
    }

    @Override
    public void onAttributeModified(Object element, String name, String value) {
        if (mListener != null) {
            mListener.onAttributeModified(element, name, value);
        }
    }

    @Override
    public void onAttributeRemoved(Object element, String name) {
        if (mListener != null) {
            mListener.onAttributeRemoved(element, name);
        }
    }

    private RootView findRootView() {
        ActivityTracker tracker = ActivityTracker.get();
        List<WeakReference<Activity>> activities = tracker.getActivitiesView();
        for (int i = activities.size() - 1; i >= 0; --i) {
            Activity activity = activities.get(i).get();
            if (activity == null || !(activity instanceof RuntimeActivity)) {
                continue;
            }

            HybridView hybridView = ((RuntimeActivity) activity).getHybridView();
            if (hybridView != null) {
                View webview = hybridView.getWebView();
                if (webview instanceof RootView) {
                    return (RootView) webview;
                }
            }
        }
        return null;
    }

    private VDocument findVDocumentRoot() {
        RootView rootView = getRootView();
        if (rootView != null) {
            // get the Document
            VDocument vdoc = getVDocumentFromRootView(rootView);
            if (vdoc != null) {
                // Document_PageId_Maps.put(vdoc, mRootView.getCurrentPage().pageId);
                return vdoc;
            }
        }
        return null;
    }

    private VDocument getVDocumentFromRootView(RootView rootView) {
        return rootView.getDocument();
    }

    public Descriptor getDescriptor(Object element) {
        return (element == null) ? null : mDescriptorMap.get(element.getClass());
    }

    public HighlightableDescriptor getHighlightableDescriptor(@Nullable Object element) {
        if (element == null) {
            return null;
        }

        HighlightableDescriptor highlightableDescriptor = null;
        Class<?> theClass = element.getClass();
        Descriptor lastDescriptor = null;

        while (highlightableDescriptor == null && theClass != null) {
            Descriptor descriptor = mDescriptorMap.get(theClass);

            if (descriptor == null) {
                return null;
            }

            if (descriptor != lastDescriptor && descriptor instanceof HighlightableDescriptor) {
                highlightableDescriptor = (HighlightableDescriptor) descriptor;
            }

            lastDescriptor = descriptor;
            theClass = theClass.getSuperclass();
        }
        return highlightableDescriptor;
    }

    public void onPagePreChange(int oldIndex, int newIndex, Page oldPage, Page newPage) {
    }

    public void onPageChanged(int oldIndex, int newIndex, Page oldPage, Page newPage) {
        if (null != newPage) {
            setPageId(newPage.pageId);
        }
        updateRoot();
        if (mListener != null) {
            mListener.onPossiblyChanged();
        }
    }

    public void onPageRemoved(int index, Page page) {
        // VPageMap.removeVElementMap(page.pageId);
        VPageMap.removeDocument(page.pageId);
        // VDocument vDoc = findVDocumentRoot();
        // mDocumentRoot.setDocument(inspectorVDoc);
        if (mListener != null) {
            mListener.onPossiblyChanged();
        }
    }

    public void onAppliedChangeAction(Context context, JsThread jsThread, VDomChangeAction action) {
        if (action == null) {
            return;
        }

        InspectorVDocument inspectorVDoc = VPageMap.getDocument(action.pageId);
        if (inspectorVDoc == null) {
            inspectorVDoc = new InspectorVDocument(null);
            VPageMap.addDocument(action.pageId, inspectorVDoc);
        }
        switch (action.action) {
            case VDomChangeAction.ACTION_REMOVE:
                removeElmenet(inspectorVDoc, action);
                break;
            case VDomChangeAction.ACTION_MOVE: {
                InspectorVElement ele = inspectorVDoc.getElementById(action.vId);
                if (ele == null) {
                    Log.e(TAG, "ele is null," + action);
                    return;
                }
                InspectorVGroup parent = ele.getParent();
                InspectorVGroup newParent =
                        (InspectorVGroup) inspectorVDoc.getElementById(action.parentVId);
                int oldIndex = parent.getChildren().indexOf(ele);
                if (oldIndex == action.index && parent == newParent) {
                    return;
                }
                parent.removeChild(ele);
                if (newParent == null) {
                    Log.e(TAG, "newParent is null," + action);
                    return;
                }
                newParent.addChild(ele, action.index);
                break;
            }
            case VDomChangeAction.ACTION_ADD: {
                InspectorVGroup parent =
                        (InspectorVGroup) inspectorVDoc.getElementById(action.parentVId);
                if (parent == null) {
                    Log.e(TAG, "parent is null in addElement, action:," + action);
                    return;
                }
                addElmenet(inspectorVDoc, action, parent);
                break;
            }
            case VDomChangeAction.ACTION_UPDATE_STYLE:
                updateAttrs(action, inspectorVDoc);
                updateStyleRules(action, inspectorVDoc);
                break;
            case VDomChangeAction.ACTION_UPDATE_ATTRS:
                updateAttrs(action, inspectorVDoc);
                break;
            case VDomChangeAction.ACTION_PRE_CREATE_BODY: {
                InspectorVElement bodyEle = inspectorVDoc.getElementById(InspectorVElement.ID_BODY);
                if (bodyEle == null) {
                    createBody(inspectorVDoc, action);
                }
                break;
            }
            case VDomChangeAction.ACTION_CREATE_BODY: {
                InspectorVElement bodyEle = inspectorVDoc.getElementById(InspectorVElement.ID_BODY);
                if (bodyEle == null) {
                    bodyEle = createBody(inspectorVDoc, action);
                }
                addElmenet(inspectorVDoc, action, (InspectorVGroup) bodyEle);
                if (mListener != null) {
                    mListener.onPossiblyChanged();
                }
                break;
            }
            case VDomChangeAction.ACTION_UPDATE_FINISH:
            case VDomChangeAction.ACTION_CREATE_FINISH:
                if (mListener != null) {
                    mListener.onPossiblyChanged();
                }
                break;
            default:
                break;
        }
    }

    private void updateStyleRules(VDomChangeAction action, InspectorVDocument inspectorVDoc) {
        InspectorVElement ele = inspectorVDoc.getElementById(action.vId);
        if (ele == null) {
            Log.e("VDocumentProvider", "onAppliedChangeAction: element is null!");
            return;
        }
        ele.updateStyleRules(action);
        for (VDomChangeAction child : action.children) {
            updateStyleRules(child, inspectorVDoc);
        }
    }

    private InspectorVElement createBody(InspectorVDocument doc, VDomChangeAction action) {
        InspectorVElement bodyEle =
                new InspectorVGroup(doc, InspectorVElement.ID_BODY, InspectorVElement.TAG_BODY);
        bodyEle.getAttrsMap().putAll(action.attributes);
        bodyEle.setInlineCSSRule((CSSInlineStyleRule) action.inlineCSSRule);
        bodyEle.setMatchedCSSRuleList(action.matchedCSSRuleList);
        doc.addChild(bodyEle);
        return bodyEle;
    }

    private void updateAttrs(VDomChangeAction action, InspectorVDocument inspectorVDoc) {
        InspectorVElement ele = inspectorVDoc.getElementById(action.vId);
        if (ele == null) {
            Log.e("VDocumentProvider", "onAppliedChangeAction: element is null!");
            return;
        }
        ele.updateAttrs(action);

        if (mListener != null) {
            mListener.onInspectRequested(ele);
            Map<String, Object> attrs = action.attributes;
            for (String key : attrs.keySet()) {
                Object value = attrs.get(key);
                if (value != null) {
                    mListener.onAttributeModified(ele, key, value.toString());
                }
            }
        }
    }

    public RootView getRootView() {
        return mRootView != null ? mRootView.get() : null;
    }

    private void updateRoot() {
        View view = V8Inspector.getInstance().getRootView();
        if (view instanceof RootView) {
            mRootView = new WeakReference<>((RootView) view);
        } else {
            mRootView = new WeakReference<>(findRootView());
        }
        VDocument vdoc = findVDocumentRoot();
        InspectorVDocument inspectorVDoc = VPageMap.getDocument(sPageId);
        if (inspectorVDoc == null) {
            inspectorVDoc = new InspectorVDocument(vdoc);
            VPageMap.addDocument(sPageId, inspectorVDoc);
        } else {
            inspectorVDoc.setVDocument(vdoc);
        }
        mDocumentRoot.setDocument(inspectorVDoc);
    }

    private static class Holder {
        private static VDocumentProvider CURRENT;
    }

    private static class VElementDescriptor extends VElementDescriptorBase<InspectorVElement> {
    }

    private static class VGroupDescriptor extends VElementDescriptorBase<InspectorVGroup> {
        @Override
        public void getChildren(InspectorVGroup element, Accumulator<Object> children) {
            if (element == null || element.getChildren() == null) {
                return;
            }
            List<InspectorVElement> listElements = new ArrayList<>(element.getChildren());
            for (InspectorVElement child : listElements) {
                children.store(child);
            }
        }
    }

    public static final class VPageMap {
        private static final Map<Integer, InspectorVDocument> mDocumentMap = new ArrayMap<>();

        public static InspectorVDocument getDocument(int pageId) {
            return mDocumentMap.get(pageId);
        }

        private static void addDocument(int pageId, InspectorVDocument doc) {
            mDocumentMap.put(pageId, doc);
        }

        private static void removeDocument(int pageId) {
            mDocumentMap.remove(pageId);
        }
    }

    private static class VDocumentRoot extends Descriptor<VDocumentRoot> {
        private InspectorVDocument mDocument;

        public VDocumentRoot() {
        }

        public InspectorVDocument getDocument() {
            return mDocument;
        }

        public void setDocument(InspectorVDocument doc) {
            mDocument = doc;
        }

        @Override
        public String getNodeName(VDocumentRoot element) {
            return "root";
        }

        @Override
        public String getLocalName(VDocumentRoot element) {
            return getNodeName(element);
        }

        @Override
        public void getAttributes(VDocumentRoot element, AttributeAccumulator attributes) {
        }

        @Override
        public void setAttributesAsText(VDocumentRoot element, String text) {
        }

        @Override
        public void getStyleRuleNames(VDocumentRoot element, StyleRuleNameAccumulator accumulator) {
        }

        @Override
        public void getStyles(VDocumentRoot element, String ruleName,
                              StyleAccumulator accumulator) {
        }

        @Override
        public void setStyle(VDocumentRoot element, String ruleName, String name, String value) {
        }

        @Override
        public void getComputedStyles(VDocumentRoot element, ComputedStyleAccumulator styles) {
        }

        @Override
        public void hook(VDocumentRoot element) {
        }

        @Override
        public void unhook(VDocumentRoot element) {
        }

        @Override
        public NodeType getNodeType(VDocumentRoot element) {
            return NodeType.DOCUMENT_NODE;
        }

        @Override
        public String getNodeValue(VDocumentRoot element) {
            return null;
        }

        @Override
        public void getChildren(VDocumentRoot element, Accumulator<Object> children) {
            children.store(mDocument);
        }
    }
}
