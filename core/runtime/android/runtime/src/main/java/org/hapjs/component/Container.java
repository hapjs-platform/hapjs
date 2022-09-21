/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.annotation.NonNull;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaOverflow;
import com.facebook.yoga.YogaWrap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hapjs.analyzer.model.NoticeMessage;
import org.hapjs.analyzer.tools.AnalyzerHelper;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.render.Page;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.R;

public abstract class Container<T extends View> extends Component<T> {
    private static final String TAG = "Container";
    protected final List<Component> mChildren = new ArrayList<>();
    protected List<Component> mFixedChildren;
    protected List<Component> mFloatingChildren;
    protected boolean mClipChildren = true;

    public Container(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    public ViewGroup getInnerView() {
        if (mHost instanceof ViewGroup) {
            return (ViewGroup) mHost;
        }
        return null;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.FLEX_DIRECTION:
                String flexDirectionStr = Attributes.getString(attribute, "row");
                setFlexDirection(flexDirectionStr);
                return true;
            case Attributes.Style.JUSTIFY_CONTENT:
                String justifyContentStr = Attributes.getString(attribute, "flex-start");
                setJustifyContent(justifyContentStr);
                return true;
            case Attributes.Style.ALIGN_ITEMS:
                String alignItemsStr = Attributes.getString(attribute, "stretch");
                setAlignItems(alignItemsStr);
                return true;
            case Attributes.Style.FLEX_WRAP:
                String flexWrapStr = Attributes.getString(attribute, "nowrap");
                setFlexWrap(flexWrapStr);
                return true;
            case Attributes.Style.ALIGN_CONTENT:
                String alignContentStr = Attributes.getString(attribute, "stretch");
                setAlignContent(alignContentStr);
                return true;
            case Attributes.Style.DESCENDANT_FOCUSABILITY:
                String desFocusability =
                        Attributes
                                .getString(attribute, Attributes.DescendantFocusabilityType.AFTER);
                setDescendantFocusability(desFocusability);
                return true;
            case Attributes.Style.OVERFLOW:
                String overflowValue =
                        Attributes.getString(attribute, Attributes.OverflowType.HIDDEN);
                setOverflow(overflowValue);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    private void setDescendantFocusability(@NonNull String desFocusability) {
        ViewGroup hostView = getInnerView();
        if (hostView == null) {
            return;
        }
        int flag;
        switch (desFocusability) {
            case Attributes.DescendantFocusabilityType.BEFORE: {
                flag = ViewGroup.FOCUS_BEFORE_DESCENDANTS;
                break;
            }
            case Attributes.DescendantFocusabilityType.AFTER: {
                flag = ViewGroup.FOCUS_AFTER_DESCENDANTS;
                break;
            }
            case Attributes.DescendantFocusabilityType.BLOCK: {
                flag = ViewGroup.FOCUS_BLOCK_DESCENDANTS;
                break;
            }
            default: {
                flag = ViewGroup.FOCUS_AFTER_DESCENDANTS;
            }
        }
        hostView.setDescendantFocusability(flag);
    }

    private void setOverflow(String overflowValue) {
        if (TextUtils.isEmpty(overflowValue)) {
            return;
        }
        ViewParent parent = mHost.getParent();
        if (parent == null || parent.getParent() == null) {
            return;
        }
        ViewGroup parentView = (ViewGroup) parent.getParent();
        boolean visible = Attributes.OverflowType.VISIBLE.equals(overflowValue);

        parentView.setClipChildren(!visible);
        parentView.setClipToPadding(!visible);

        if (parentView instanceof YogaLayout) {
            YogaLayout yogaLayout = (YogaLayout) parentView;
            yogaLayout.getYogaNode()
                    .setOverflow(visible ? YogaOverflow.VISIBLE : YogaOverflow.HIDDEN);
        }
    }

    public void setFlexDirection(String flexDirectionStr) {
        if (TextUtils.isEmpty(flexDirectionStr) || !isYogaLayout()) {
            return;
        }
        YogaFlexDirection flexDirection = YogaFlexDirection.ROW;
        if ("column".equals(flexDirectionStr)) {
            flexDirection = YogaFlexDirection.COLUMN;
        } else if ("row-reverse".equals(flexDirectionStr)) {
            flexDirection = YogaFlexDirection.ROW_REVERSE;
        } else if ("column-reverse".equals(flexDirectionStr)) {
            flexDirection = YogaFlexDirection.COLUMN_REVERSE;
        }
        if (getInnerView() instanceof YogaLayout) {
            YogaNode yogaNode = ((YogaLayout) getInnerView()).getYogaNode();
            if (yogaNode != null) {
                yogaNode.setFlexDirection(flexDirection);
            } else {
                Log.e(TAG, "setFlexDirection: yogaNode from getInnerView() is null");
            }
        } else {
            YogaNode yogaNode = ((YogaLayout) mHost).getYogaNode();
            if (yogaNode != null) {
                yogaNode.setFlexDirection(flexDirection);
            } else {
                Log.e(TAG, "setFlexDirection: yogaNode from mHost is null");
            }
        }
    }

    public void setJustifyContent(String justifyContentStr) {
        if (TextUtils.isEmpty(justifyContentStr) || !isYogaLayout()) {
            return;
        }

        YogaJustify justifyContent = YogaJustify.FLEX_START;
        if ("flex-end".equals(justifyContentStr)) {
            justifyContent = YogaJustify.FLEX_END;
        } else if ("center".equals(justifyContentStr)) {
            justifyContent = YogaJustify.CENTER;
        } else if ("space-between".equals(justifyContentStr)) {
            justifyContent = YogaJustify.SPACE_BETWEEN;
        } else if ("space-around".equals(justifyContentStr)) {
            justifyContent = YogaJustify.SPACE_AROUND;
        }
        if (getInnerView() instanceof YogaLayout) {
            YogaNode yogaNode = ((YogaLayout) getInnerView()).getYogaNode();
            if (yogaNode != null) {
                yogaNode.setJustifyContent(justifyContent);
            } else {
                Log.e(TAG, "setJustifyContent: yogaNode from getInnerView() is null");
            }
        } else {
            YogaNode yogaNode = ((YogaLayout) mHost).getYogaNode();
            if (yogaNode != null) {
                yogaNode.setJustifyContent(justifyContent);
            } else {
                Log.e(TAG, "setJustifyContent: yogaNode from mHost is null");
            }
        }
    }

    public void setAlignItems(String alignItemsStr) {
        if (TextUtils.isEmpty(alignItemsStr) || !isYogaLayout()) {
            return;
        }

        YogaAlign alignItems = YogaAlign.STRETCH;
        if ("flex-start".equals(alignItemsStr)) {
            alignItems = YogaAlign.FLEX_START;
        } else if ("flex-end".equals(alignItemsStr)) {
            alignItems = YogaAlign.FLEX_END;
        } else if ("center".equals(alignItemsStr)) {
            alignItems = YogaAlign.CENTER;
        }
        if (getInnerView() instanceof YogaLayout) {
            YogaNode yogaNode = ((YogaLayout) getInnerView()).getYogaNode();
            if (yogaNode != null) {
                yogaNode.setAlignItems(alignItems);
            } else {
                Log.e(TAG, "setAlignItems: yogaNode from getInnerView() is null");
            }
        } else {
            YogaNode yogaNode = ((YogaLayout) mHost).getYogaNode();
            if (yogaNode != null) {
                yogaNode.setAlignItems(alignItems);
            } else {
                Log.e(TAG, "setAlignItems: yogaNode from mHost is null");
            }
        }
    }

    public void setFlexWrap(String flexWrapStr) {
        if (TextUtils.isEmpty(flexWrapStr) || !isYogaLayout()) {
            return;
        }

        YogaWrap flexWrap = YogaWrap.NO_WRAP;
        if ("nowrap".equals(flexWrapStr)) {
            flexWrap = YogaWrap.NO_WRAP;
        } else if ("wrap".equals(flexWrapStr)) {
            flexWrap = YogaWrap.WRAP;
        } else if ("wrap-reverse".equals(flexWrapStr)) {
            flexWrap = YogaWrap.WRAP_REVERSE;
        }
        if (getInnerView() instanceof YogaLayout) {
            YogaNode yogaNode = ((YogaLayout) getInnerView()).getYogaNode();
            if (yogaNode != null) {
                yogaNode.setWrap(flexWrap);
            } else {
                Log.e(TAG, "setFlexWrap: yogaNode from getInnerView() is null");
            }
        } else {
            YogaNode yogaNode = ((YogaLayout) mHost).getYogaNode();
            if (yogaNode != null) {
                yogaNode.setWrap(flexWrap);
            } else {
                Log.e(TAG, "setFlexWrap: yogaNode from mHost is null");
            }
        }
    }

    public void setAlignContent(String alignContentStr) {
        if (TextUtils.isEmpty(alignContentStr) || !isYogaLayout()) {
            return;
        }

        YogaAlign alignContent = YogaAlign.STRETCH;
        if ("stretch".equals(alignContentStr)) {
            alignContent = YogaAlign.STRETCH;
        } else if ("flex-start".equals(alignContentStr)) {
            alignContent = YogaAlign.FLEX_START;
        } else if ("flex-end".equals(alignContentStr)) {
            alignContent = YogaAlign.FLEX_END;
        } else if ("center".equals(alignContentStr)) {
            alignContent = YogaAlign.CENTER;
        } else if ("space-between".equals(alignContentStr)) {
            alignContent = YogaAlign.SPACE_BETWEEN;
        } else if ("space-around".equals(alignContentStr)) {
            alignContent = YogaAlign.SPACE_AROUND;
        }

        if (getInnerView() instanceof YogaLayout) {
            ((YogaLayout) getInnerView()).getYogaNode().setAlignContent(alignContent);
        } else {
            ((YogaLayout) mHost).getYogaNode().setAlignContent(alignContent);
        }
    }

    public void addChild(Component child) {
        addChild(child, -1);
    }

    public void addChild(Component child, int index) {
        if (child == null) {
            throw new IllegalArgumentException("Cannot add a null child component to Container");
        }
        final int childrenCount = getChildCount();

        if (index < 0 || index > childrenCount) {
            index = childrenCount;
        }

        mChildren.add(index, child);
        for (OnDomTreeChangeListener listener : mDomTreeChangeListeners) {
            listener.onDomTreeChange(child, true);
        }

        if (child instanceof Floating) {
            addFloatingChild(child);
            return;
        }
        if (child.mPosition != null && child.mPosition.isFixed()) {
            addFixedChild(child);
            return;
        }

        // move index forward because fixed or floating children may exist.
        index = offsetIndex(index);

        addView(child.getHostView(), index);

        if (isDisabled()) {
            // when parent is disabled, child must set disabled true
            child.setDisabled(true);
        }
    }

    public int offsetIndex(int index) {
        int fixedSize = (mFixedChildren == null) ? 0 : mFixedChildren.size();
        int floatingSize = (mFloatingChildren == null) ? 0 : mFloatingChildren.size();
        if (fixedSize == 0 && floatingSize == 0) {
            return index;
        }
        if (index == getChildCount()) {
            return (index - fixedSize - floatingSize);
        }

        int offset = 0;
        if (fixedSize > 0) {
            for (Component child : mFixedChildren) {
                int fixedIndex = mChildren.indexOf(child);
                if (index > fixedIndex) {
                    offset++;
                }
            }
        }
        if (floatingSize > 0) {
            for (Component child : mFloatingChildren) {
                int floatingIndex = mChildren.indexOf(child);
                if (index > floatingIndex) {
                    offset++;
                }
            }
        }

        return (index - offset);
    }

    public int getChildCount() {
        return mChildren.size();
    }

    public void addView(View childView, int index) {
        ViewGroup viewGroup = getInnerView();

        if (viewGroup == null || childView == null) {
            return;
        }

        if (viewGroup instanceof YogaLayout) {
            YogaLayout yogaLayout = (YogaLayout) viewGroup;
            ViewGroup.LayoutParams lp = childView.getLayoutParams();
            if (lp == null) {
                lp =
                        new YogaLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                childView.setLayoutParams(lp);
            }
            yogaLayout.addView(childView, index, lp);
        } else {
            viewGroup.addView(childView, index);
        }

        if (childView instanceof ComponentHost) {
            Component component = ((ComponentHost) childView).getComponent();
            component.onHostViewAttached(viewGroup);
        }
    }

    public Component getChildAt(int index) {
        if (index < 0 || index >= getChildCount()) {
            return null;
        }

        return mChildren.get(index);
    }

    public View getChildViewAt(int index) {
        if (index < 0 || index >= getChildCount()) {
            return null;
        }
        ViewGroup innerView = getInnerView();
        if (innerView == null) {
            return null;
        }
        return innerView.getChildAt(index);
    }

    public void removeChild(Component child) {
        removeChildInternal(child);
    }

    protected int removeChildInternal(Component child) {
        final int index = mChildren.indexOf(child);
        if (index < 0) {
            return index;
        }

        removeFloatingComponent(child);
        removeFixedComponent(child);
        mChildren.remove(child);
        for (OnDomTreeChangeListener listener : mDomTreeChangeListeners) {
            listener.onDomTreeChange(child, false);
        }
        removeView(child.getHostView());
        child.destroy();
        return index;
    }

    public void removeView(View child) {
        ViewGroup viewGroup = getInnerView();
        if (viewGroup != null) {
            viewGroup.removeView(child);
        }
    }

    public boolean isYogaLayout() {
        return super.isYogaLayout() || (getInnerView() instanceof YogaLayout);
    }

    @Override
    public void destroy() {
        for (Component c : mChildren) {
            c.destroy();
        }
        super.destroy();
    }

    @Override
    protected Component findComponentTraversal(String id) {
        if (id.equals(mId)) {
            return this;
        }

        for (int i = 0; i < mChildren.size(); i++) {
            Component component = mChildren.get(i);
            component = component.findComponentById(id);
            if (component != null) {
                return component;
            }
        }

        return null;
    }

    protected void removeFixedComponent(Component component) {
        if (component instanceof Container) {
            Container container = (Container) component;
            for (int i = 0; i < container.getChildCount(); i++) {
                Component child = container.getChildAt(i);
                container.removeFixedComponent(child);
            }
        }

        removeFixedView(component);
    }

    private void removeFixedView(Component component) {
        View hostView = component.getHostView();
        if (hostView != null && component.mPosition != null && component.mPosition.isFixed()) {
            ViewGroup parent = (ViewGroup) hostView.getParent();
            if (parent != null) {
                parent.removeView(hostView);
                mFixedChildren.remove(component);
            }
        }
    }

    protected void removeFloatingComponent(Component component) {
        if (component instanceof Floating) {
            mFloatingChildren.remove(component);
            ((Floating) component).dismiss();
        }
    }

    public void addFloatingChild(Component child) {
        if (mFloatingChildren == null) {
            mFloatingChildren = new ArrayList<>();
        }
        mFloatingChildren.add(child);
    }

    public void removeFloatingChild(Component child) {
        if (mFloatingChildren == null || mFloatingChildren.isEmpty()) {
            return;
        }
        mFloatingChildren.remove(child);
    }

    public void addFixedChild(Component child) {
        if (mFixedChildren == null) {
            mFixedChildren = new ArrayList<>();
        }
        mFixedChildren.add(child);
    }

    public void removeFixedChild(Component child) {
        if (mFixedChildren == null || mFixedChildren.isEmpty()) {
            return;
        }
        mFixedChildren.remove(child);
    }

    public List<Component> getChildren() {
        return mChildren;
    }

    public void setClipChildren(boolean clipChildren) {
        mClipChildren = clipChildren;
        if (mHost != null) {
            ViewGroup parentView = (ViewGroup) mHost;
            setClipChildrenInternal(parentView, clipChildren);
        }
    }

    protected void setClipChildrenInternal(ViewGroup parent, boolean clipChildren) {
        if (parent != null) {
            parent.setClipToPadding(clipChildren);
            parent.setClipChildren(clipChildren);
            if (parent instanceof YogaLayout) {
                YogaLayout yogaLayout = (YogaLayout) parent;
                yogaLayout
                        .getYogaNode()
                        .setOverflow(clipChildren ? YogaOverflow.HIDDEN : YogaOverflow.VISIBLE);
            }
        }
    }

    @Override
    public void setDisabled(boolean disabled) {
        super.setDisabled(disabled);
        for (Component child : mChildren) {
            if (child != null) {
                child.setDisabled(disabled);
            }
        }
    }

    public static class RecyclerItem extends Component.RecyclerItem {
        private RecyclerItemList mChildren = new RecyclerItemList();

        public RecyclerItem(int ref, ComponentCreator componentCreator) {
            super(ref, componentCreator);
        }

        @Override
        protected void dispatchDetachFromTemplate() {
            detachFromTemplate();
            if (isRecycler()) {
                return;
            }
            for (RecyclerDataItem childItem : mChildren) {
                childItem.dispatchDetachFromTemplate();
            }
        }

        @Override
        public void dispatchBindComponent(Component recycle) {
            bindComponent(recycle);
            if (isRecycler()) {
                return;
            }

            Container container = (Container) recycle;

            // attach child's same data to template
            if (getAttachedTemplate() != null) {
                getAttachedTemplate().attachAllChildRecyclerItem(this);
            }

            int i = 0;
            boolean differentDom = false;
            for (; i < mChildren.size(); i++) {
                RecyclerDataItem childItem = mChildren.get(i);

                // list-item 内部使用 if/else/for 等, 可能会导致: 相同 type 的 list-item 有不同 dom 结构
                // 这里通过 add/remove 和 RecyclerTemplate内部的链表做了兼容处理, 但是会影响性能
                // 最好的方式是让开发者确保: 相同 type 的 list-item 有一样的 dom 结构

                // check child's type
                Component child = container.getChildAt(i);
                if (child != null && !childItem.isComponentClassMatch(child.getClass())) {
                    Log.w("Recycler", "please use different list-item type with different dom");
                    container.removeChild(child);
                    child.destroy();
                    child = null;
                    differentDom = true;
                }
                if (child == null) {
                    child = childItem.createRecycleComponent(container);
                    childItem.dispatchBindComponent(child);
                    container.addChild(child, i);
                } else {
                    childItem.dispatchBindComponent(child);
                }
            }

            // situation1: "for" attr: different size in list-item
            // situation2: "if" attr: VElement has been removed
            while (i < container.getChildCount()) {
                Log.w("Recycler", "please use different list-item type with different dom");
                Component child = container.getChildAt(i);
                if (child != null) {
                    container.removeChild(child);
                    child.destroy();
                }
            }
            handleAnalyzerPanelDetect(container, differentDom);
        }

        private void handleAnalyzerPanelDetect(Container container, boolean differentDom) {
            if (AnalyzerHelper.getInstance().isInAnalyzerMode() && differentDom) {
                Page currentPage = AnalyzerHelper.getInstance().getCurrentPage();
                if (currentPage != null) {
                    RecyclerItem parent = getParent();
                    while (parent != null) {
                        ComponentCreator componentCreator = parent.getComponentCreator();
                        if (componentCreator != null && TextUtils.equals(componentCreator.getClazz().getSimpleName(), "List")) {
                            NoticeMessage warn = NoticeMessage.warn(currentPage.getName(), container.mContext.getString(R.string.analyzer_irregular_listitem_type_warning, currentPage.getName()));
                            warn.setAction(new NoticeMessage.UIAction.Builder().pageId(currentPage.getPageId()).addComponentId(parent.getRef()).build());
                            AnalyzerHelper.getInstance().notice(warn);
                            break;
                        }
                        parent = parent.getParent();
                    }
                }
            }
        }

        @Override
        public void dispatchUnbindComponent() {
            unbindComponent();
            if (isRecycler()) {
                return;
            }

            for (RecyclerDataItem childItem : mChildren) {
                childItem.dispatchUnbindComponent();
            }
        }

        @Override
        protected void setUseTemplate(boolean use) {
            super.setUseTemplate(use);
            if (isRecycler()) {
                return;
            }
            for (RecyclerDataItem childItem : mChildren) {
                childItem.setUseTemplate(use);
            }
        }

        public RecyclerItemList getChildren() {
            return mChildren;
        }

        public void setChildrenHolder(List<? extends Holder> childrenHolders) {
            mChildren.setRecyclerItemHolders(childrenHolders);
        }

        public void onChildAdded(RecyclerDataItem child, int index) {
            child.assignParent(this);

            if (isUseWithTemplate()) {
                requestBindTemplate();
                return;
            }

            if (!isRecycler() && getBoundComponent() != null) {
                // component
                Container container = (Container) getBoundComponent();
                Component childComponent = child.createRecycleComponent(container);
                child.bindComponent(childComponent);
                container.addChild(childComponent, index);
            }

            if (!isRecycler() && getTwinComponents() != null) {
                Set<Component> twinComponents = getTwinComponents();
                for (Component twinParent : twinComponents) {
                    if (twinParent instanceof Container) {
                        Component childComponent =
                                child.createRecycleComponent((Container) twinParent);
                        Component boundComponent = child.getBoundComponent();
                        child.bindComponent(childComponent);
                        if (boundComponent != null) {
                            child.bindComponent(boundComponent);
                        } else {
                            child.unbindComponent();
                        }
                        child.addTwinComponent(childComponent);
                        ((Container) twinParent).addChild(childComponent, index);
                    }
                }
            }
        }

        public void onChildRemoved(RecyclerDataItem child, int index) {
            child.assignParent(null);

            if (isUseWithTemplate()) {
                requestBindTemplate();
                return;
            }

            if (!isRecycler() && getBoundComponent() != null && child.getBoundComponent() != null) {
                Container container = (Container) getBoundComponent();
                Component childComponent = child.getBoundComponent();
                container.removeChild(childComponent);
            }

            if (!isRecycler() && child.getTwinComponents() != null) {
                Set<Component> twinComponents = child.getTwinComponents();
                for (Component childComponent : twinComponents) {
                    Container parent = childComponent.getParent();
                    if (parent != null) {
                        parent.removeChild(childComponent);
                    }
                }
            }
        }

        @Override
        public void addTwinComponent(Component component) {
            super.addTwinComponent(component);
            if (isRecycler()) {
                return;
            }
            for (RecyclerDataItem childItem : mChildren) {
                if (childItem != null && childItem.getBoundComponent() != null) {
                    childItem.addTwinComponent(childItem.getBoundComponent());
                }
            }
        }

        @Override
        public void removeTwinComponent(Component component) {
            super.removeTwinComponent(component);
            if (component == null || isRecycler()) {
                return;
            }
            Container container = (Container) component;
            int index = 0;
            for (RecyclerDataItem childItem : mChildren) {
                Component child = container.getChildAt(index);
                childItem.removeTwinComponent(child);
                index++;
            }
        }

        @Override
        public void removeAllTwinComponent() {
            super.removeAllTwinComponent();
            for (RecyclerDataItem childItem : mChildren) {
                if (childItem != null) {
                    childItem.removeAllTwinComponent();
                }
            }
        }

        @Override
        public int identity() {
            int h = super.identity();
            int size = mChildren.size();

            for (int i = 0; i < size; i++) {
                RecyclerDataItem child = mChildren.get(i);
                h = h * 31 + child.identity();
            }
            return h;
        }
    }
}
