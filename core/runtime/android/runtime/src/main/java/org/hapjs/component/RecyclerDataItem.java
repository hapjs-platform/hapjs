/*
 * Copyright (c) 2021-present,  the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.content.Context;
import android.util.Log;
import androidx.collection.ArraySet;
import java.util.Map;
import java.util.Set;
import org.hapjs.bridge.Widget;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.utils.map.CombinedMap;
import org.hapjs.render.DebugUtils;
import org.hapjs.render.css.Node;
import org.hapjs.render.css.value.CSSValues;
import org.hapjs.runtime.HapEngine;

public abstract class RecyclerDataItem implements ComponentDataHolder {

    private static final String TAG = "RecyclerDataItem";
    private final int mRef;
    private final CombinedMap<String, CSSValues> mStyleDomData = new CombinedMap<>();
    private final CombinedMap<String, Object> mAttrsDomData = new CombinedMap<>();
    private final CombinedMap<String, Boolean> mEventDomData = new CombinedMap<>();
    protected final ComponentCreator mComponentCreator;
    protected Node mCssNode;
    private Container.RecyclerItem mParent;
    private Component mBoundRecycleComponent;

    private RecyclerDataTemplate mTemplate;

    private boolean mUseTemplate = false;
    private boolean mRequestBindTemplated = false;

    private Set<Component> mTwinComponents;

    public RecyclerDataItem(int ref, ComponentCreator componentCreator) {
        mRef = ref;
        mComponentCreator = componentCreator;
        mStyleDomData.setId(ref);
        mAttrsDomData.setId(ref);
        mEventDomData.setId(ref);
    }

    private boolean isComponentClassMatch(Object o) {
        return o != null && mComponentCreator.getClazz() == o.getClass();
    }

    public boolean isComponentClassMatch(Class c) {
        return c != null && mComponentCreator.getClazz() == c;
    }

    protected Class getComponentClass() {
        return mComponentCreator.getClazz();
    }

    ComponentCreator getComponentCreator() {
        return mComponentCreator;
    }

    public abstract Component createRecycleComponent(Container parent);

    public void attachToTemplate(RecyclerDataTemplate template) {
        if (mTemplate != null) {
            if (mTemplate != template) {
                throw new IllegalStateException("please detach first");
            } else {
                return;
            }
        }

        template.attach(this);
        mTemplate = template;
    }

    void detachFromTemplate() {
        if (mTemplate == null) {
            return;
        }
        mTemplate.detach(this);
        mTemplate = null;
    }

    protected RecyclerDataTemplate getAttachedTemplate() {
        return mTemplate;
    }

    public void bindComponent(Component recycle) {
        if (!isComponentClassMatch(recycle)) {
            throw new IllegalStateException("will not come here");
        }
        // 注意：必须在 onApplyDataToComponent 前更新组件绑定的数据，否则会回调改变原有的绑定数据
        recycle.setBoundRecyclerItem(this);
        if (DebugUtils.DBG) {
            Log.d(TAG, "bindComponent: node" + mCssNode + " component: " + recycle);
        }
        if (mCssNode != null) {
            recycle.setCssNode(mCssNode);
            mCssNode.setComponent(recycle);
        }
        // onApplyDataToComponent will createview
        onApplyDataToComponent(recycle);
        setBoundComponent(recycle);
        mRequestBindTemplated = false;
    }

    protected abstract void onApplyDataToComponent(Component recycle);

    public void unbindComponent() {
        if (mBoundRecycleComponent != null) {
            mBoundRecycleComponent.setBoundRecyclerItem(null);
            if (DebugUtils.DBG) {
                Log.d(TAG, "unbindComponent: node" + mCssNode + " component: "
                        + mBoundRecycleComponent);
            }
            if (mCssNode != null) {
                mCssNode.setComponent(null);
            }
            if (mBoundRecycleComponent != null) {
                mBoundRecycleComponent.setCssNode(null);
            }
            setBoundComponent(null);
        }
    }

    public <T extends Component> T getBoundComponent() {
        return (T) mBoundRecycleComponent;
    }

    public void setBoundComponent(Component recycle) {
        mBoundRecycleComponent = recycle;
    }

    protected void dispatchDetachFromTemplate() {
        detachFromTemplate();
    }

    public void dispatchBindComponent(Component recycle) {
        bindComponent(recycle);
    }

    public void dispatchUnbindComponent() {
        unbindComponent();
    }

    public void addTwinComponent(Component component) {
        if (component == null || component.getRef() != mRef) {
            return;
        }

        if (mTwinComponents == null) {
            mTwinComponents = new ArraySet<>();
        }
        mTwinComponents.add(component);
    }

    public void removeTwinComponent(Component component) {
        if (mTwinComponents == null) {
            return;
        }
        mTwinComponents.remove(component);
    }

    public void removeAllTwinComponent() {
        if (mTwinComponents == null) {
            return;
        }
        mTwinComponents.clear();
    }

    public Set<Component> getTwinComponents() {
        return mTwinComponents;
    }

    public boolean isTwinComponent(Component component) {
        return mTwinComponents != null && mTwinComponents.contains(component);
    }

    @Override
    public int getRef() {
        return mRef;
    }

    @Override
    public CombinedMap<String, Object> getAttrsDomData() {
        return mAttrsDomData;
    }

    @Override
    public CombinedMap<String, CSSValues> getStyleDomData() {
        return mStyleDomData;
    }

    @Override
    public Set<String> getDomEvents() {
        return mEventDomData.keySet();
    }

    CombinedMap<String, Boolean> getEventCombinedMap() {
        return mEventDomData;
    }

    @Override
    public void bindAttrs(Map attrs) {
        if (attrs == null || attrs.size() == 0) {
            return;
        }

        onDataChanged();
        mAttrsDomData.putAll(attrs);

        if (getBoundComponent() != null) {
            getBoundComponent().bindAttrs(attrs);
        }

        if (mTwinComponents != null) {
            for (Component component : mTwinComponents) {
                component.bindAttrs(attrs);
            }
        }
    }

    @Override
    public void bindStyles(Map<String, ? extends CSSValues> attrs) {
        if (attrs == null || attrs.size() == 0) {
            return;
        }

        onDataChanged();
        mStyleDomData.putAll(attrs);

        if (getBoundComponent() != null) {
            getBoundComponent().bindStyles(attrs);
        }

        if (mTwinComponents != null) {
            for (Component component : mTwinComponents) {
                component.bindStyles(attrs);
            }
        }
    }

    @Override
    public void bindEvents(Set events) {
        if (events == null || events.size() == 0) {
            return;
        }

        onDataChanged();

        for (Object key : events) {
            mEventDomData.put((String) key, true);
        }

        if (getBoundComponent() != null) {
            getBoundComponent().bindEvents(events);
        }

        if (mTwinComponents != null) {
            for (Component component : mTwinComponents) {
                component.bindEvents(events);
            }
        }
    }

    @Override
    public void removeEvents(Set<String> events) {
        if (events == null || events.size() == 0) {
            return;
        }

        onDataChanged();

        for (Object key : events) {
            mEventDomData.remove((String) key);
        }

        if (getBoundComponent() != null) {
            getBoundComponent().removeEvents(events);
        }

        if (mTwinComponents != null) {
            for (Component component : mTwinComponents) {
                component.removeEvents(events);
            }
        }
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        // do nothing
    }

    public void onDataChanged() {
    }

    protected void requestBindTemplate() {
        if (isUseWithTemplate() && getParent() != null && !mRequestBindTemplated) {
            getParent().requestBindTemplate();
            mRequestBindTemplated = true;
        }
    }

    void assignParent(Container.RecyclerItem parent) {
        mParent = parent;

        if (mParent != null) {
            if (mParent.isRecycler()) {
                if (mParent.isSupportTemplate()) {
                    setUseTemplate(true);
                } else {
                    setUseTemplate(false);
                }
            } else if (mParent.isUseWithTemplate() != isUseWithTemplate()) {
                setUseTemplate(mParent.isUseWithTemplate());
            }
        }
    }

    public boolean isSupportTemplate() {
        throw new IllegalStateException("this will be override");
    }

    boolean isRecycler() {
        return Recycler.class.isAssignableFrom(getComponentClass());
    }

    boolean isUseWithTemplate() {
        return mUseTemplate;
    }

    protected void setUseTemplate(boolean use) {
        mUseTemplate = use;
    }

    protected Container.RecyclerItem getParent() {
        return mParent;
    }

    public int identity() {
        return getComponentClass().hashCode();
    }

    public void destroy() {
        // mTemplate == null, so component will not be used by others
        if (getBoundComponent() != null && mTemplate == null) {
            getBoundComponent().destroy();
        }

        if (mTwinComponents != null) {
            for (Component component : mTwinComponents) {
                component.destroy();
            }
            mTwinComponents.clear();
        }

        detachFromTemplate();
    }

    public void setCssNode(Node cssNode) {
        mCssNode = cssNode;
    }

    public interface Holder {
        RecyclerDataItem getRecyclerItem();
    }

    public interface Creator {
        RecyclerDataItem createRecyclerItem(
                HapEngine hapEngine,
                Context context,
                String element,
                int ref,
                RenderEventCallback callback,
                Map<String, Object> componentInfo);
    }

    public static class ComponentCreator {
        private HapEngine mHapEngine;
        private Context mContext;
        private RenderEventCallback mCallback;
        private Widget mWidget;

        public Context getContext() {
            return mContext;
        }

        public ComponentCreator(
                HapEngine hapEngine, Context context, RenderEventCallback callback, Widget widget) {
            mHapEngine = hapEngine;
            mContext = context;
            mCallback = callback;
            mWidget = widget;
        }

        Component createComponent(Container parent, int ref) {
            return mWidget
                    .createComponent(mHapEngine, mContext, parent, ref, mCallback, null, null);
        }

        Class<? extends Component> getClazz() {
            return mWidget.getClazz();
        }
    }
}
