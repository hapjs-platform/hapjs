/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import java.util.ArrayList;
import java.util.List;
import org.hapjs.component.utils.map.SharedMap;
import org.hapjs.component.view.state.State;
import org.hapjs.render.css.value.CSSValueFactory;
import org.hapjs.render.css.value.CSSValues;

public class RecyclerDataTemplate {
    private static final String EMPTY_STRING = "";
    private static final CSSValues EMPTY_STYLE = CSSValueFactory.createCSSValues(State.NORMAL, "");
    private final Class mComponentType;
    private List<RecyclerDataTemplate> mChildren = new ArrayList<>();
    private SharedMap<String, CSSValues> mStyleSharedMap = new SharedMap<>(EMPTY_STYLE);
    private SharedMap<String, Object> mAttrSharedMap = new SharedMap<String, Object>(EMPTY_STRING);
    private SharedMap<String, Boolean> mEventSharedMap = new SharedMap<>(false);
    // 组件复用时, 相同 type 的 list-item dom树中同一位置可能出现不同类型的组件, 这里用链表处理以做兼容
    private RecyclerDataTemplate mNextTypeTemplate;

    public RecyclerDataTemplate(RecyclerDataItem item) {
        mComponentType = item.getComponentClass();
    }

    /**
     * 将 container 的 children 中的 RecyclerDataItem 和 container 对应模板中的 mChildren 子模板一一匹配.
     * 由于子模板是依次顺序创建的，因此 mChildren 子模板的 size 要保证至少和 i+1 对齐. todo: 如果 container 的 children
     * 元素非顺序添加，可能出现模板匹配乱序的情况，但目前并不影响样式表现.
     */
    void attachAllChildRecyclerItem(Container.RecyclerItem container) {
        for (int i = 0; i < container.getChildren().size(); i++) {
            RecyclerDataItem child = container.getChildren().get(i);

            if (child.getAttachedTemplate() == null) {
                attachToChildTemplate(i, child);
            } else if (i == mChildren.size()) {
                // 此处说明 template 非空，且 size<i+1，即 size 和 container.getChildren() 数目不一致
                // 为保证至少 size==i+1，mChildren 也需要同步增加元素，以防数组访问越界
                mChildren.add(child.getAttachedTemplate());
            }
        }
    }

    private void attachToChildTemplate(int index, RecyclerDataItem item) {
        // 必须顺序的添加, 保证dom结构一致
        if (index == mChildren.size()) {
            mChildren.add(new RecyclerDataTemplate(item));
        }
        item.attachToTemplate(mChildren.get(index));
    }

    void attach(RecyclerDataItem item) {
        getMatchTypeTemplate(item).internalAttach(item);
    }

    void detach(RecyclerDataItem item) {
        getMatchTypeTemplate(item).internalDetach(item);
    }

    private RecyclerDataTemplate getMatchTypeTemplate(RecyclerDataItem item) {
        if (mComponentType == item.getComponentClass()) {
            return this;
        }

        if (mNextTypeTemplate == null) {
            mNextTypeTemplate = new RecyclerDataTemplate(item);
        }

        return mNextTypeTemplate.getMatchTypeTemplate(item);
    }

    private void internalAttach(RecyclerDataItem item) {
        filterItem(item);
    }

    private void internalDetach(RecyclerDataItem item) {
        unFilterItem(item);
    }

    private void filterItem(RecyclerDataItem item) {
        item.getStyleDomData().setSharedMap(mStyleSharedMap);
        item.getAttrsDomData().setSharedMap(mAttrSharedMap);
        item.getEventCombinedMap().setSharedMap(mEventSharedMap);
    }

    private void unFilterItem(RecyclerDataItem item) {
        item.getStyleDomData().removeSharedMap();
        item.getAttrsDomData().removeSharedMap();
        item.getEventCombinedMap().removeSharedMap();
    }
}
