/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.runtime.adapter;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.model.TabBarInfo;
import org.hapjs.runtime.R;

import java.util.ArrayList;
import java.util.List;

public class TabBarItemAdapter extends RecyclerView.Adapter<TabBarItemAdapter.TabBarHolder> {
    private final String TAG = "TabBarItemAdapter";
    private int textColor;
    private int textSelectedColor;
    private Context mContext;
    private List<TabBarInfo> itemDatas = new ArrayList<TabBarInfo>();
    private double realWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
    private int itemPadding = 0;
    private static final int DEFAULT_MENUBAR_CONTAINER_PADDING_FIVE = 7;
    private double HEAD_TAIL_ITEM_MARGIN = 0;

    public TabBarItemAdapter(Context outContext, List<TabBarInfo> datas, int textColor, int textSelectedColor) {
        mContext = outContext;
        itemDatas.clear();
        if (null != datas) {
            itemDatas.addAll(datas);
        }
        this.textColor = textColor;
        this.textSelectedColor = textSelectedColor;
    }

    private void initItemWidth(int size) {
        Context context = mContext;
        if (!(context instanceof Activity)) {
            Log.e(TAG, "initItemWidth error: mContext is not an instance of Activity.");
            return;
        }
        double CONTAINER_PADDING_FIVE = (DEFAULT_MENUBAR_CONTAINER_PADDING_FIVE *
                context.getResources().getDisplayMetrics().density);
        Activity activity = (Activity) context;
        double width = DisplayUtil.getScreenWidth(activity);
        if (size > 0) {
            realWidth = (width - 2 * CONTAINER_PADDING_FIVE) / size;
        } else {
            Log.w(TAG, "initItemWidth error size is 0.");
        }
        HEAD_TAIL_ITEM_MARGIN = CONTAINER_PADDING_FIVE;
    }

    public class TabBarHolder extends RecyclerView.ViewHolder {

        public TextView textView;
        public ImageView imageView;

        public TabBarHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.tabbar_item_image);
            textView = (TextView) itemView.findViewById(R.id.tabbar_item_tv);
        }
    }

    public void updateData(List<TabBarInfo> data, int maxSize) {
        itemDatas.clear();
        if (null != data) {
            itemDatas.addAll(data);
        }
        initItemWidth(maxSize);
        notifyDataSetChanged();
    }

    private ItemClickListner itemClickListner;

    public void setItemClickListner(ItemClickListner clickListner) {
        itemClickListner = clickListner;
    }

    @Override
    public TabBarHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View parentView = LayoutInflater.from(mContext).inflate(R.layout.tabbar_item_view, parent, false);
        int tmpRealWidth = (int) realWidth;
        if (tmpRealWidth != ViewGroup.LayoutParams.WRAP_CONTENT) {
            parentView.setLayoutParams(new LinearLayout.LayoutParams(tmpRealWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        return new TabBarHolder(parentView);
    }

    @Override
    public void onBindViewHolder(TabBarHolder holder, int position) {
        if (position < itemDatas.size()) {
            TabBarInfo tabBarItemData = null;
            tabBarItemData = itemDatas.get(position);
            if (null != tabBarItemData) {
                holder.textView.setText(tabBarItemData.getTabBarText());
                Uri uri = tabBarItemData.getTabBarIconUri();
                Uri selectedUri = tabBarItemData.getTabBarSelectedIconUri();
                if (tabBarItemData.isSelected()) {
                    if (null != selectedUri) {
                        holder.imageView.setImageURI(selectedUri);
                    }
                    holder.textView.setTextColor(textSelectedColor);
                } else {
                    if (null != uri) {
                        holder.imageView.setImageURI(uri);
                    }
                    holder.textView.setTextColor(textColor);
                }
            }
        }

        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        if (layoutParams instanceof RecyclerView.LayoutParams) {
            if (position == 0) {
                ((RecyclerView.LayoutParams) layoutParams).leftMargin = (int) HEAD_TAIL_ITEM_MARGIN;
                ((RecyclerView.LayoutParams) layoutParams).rightMargin = 0;
            } else if (position == (itemDatas.size() - 1)) {
                ((RecyclerView.LayoutParams) layoutParams).leftMargin = itemPadding;
                ((RecyclerView.LayoutParams) layoutParams).rightMargin = (int) HEAD_TAIL_ITEM_MARGIN;
            } else {
                ((RecyclerView.LayoutParams) layoutParams).leftMargin = itemPadding;
                ((RecyclerView.LayoutParams) layoutParams).rightMargin = 0;
            }

        } else if (layoutParams instanceof LinearLayout.LayoutParams) {
            if (position == 0) {
                ((LinearLayout.LayoutParams) layoutParams).leftMargin = (int) HEAD_TAIL_ITEM_MARGIN;
                ((LinearLayout.LayoutParams) layoutParams).rightMargin = 0;
            } else if (position == (itemDatas.size() - 1)) {
                ((LinearLayout.LayoutParams) layoutParams).leftMargin = itemPadding;
                ((LinearLayout.LayoutParams) layoutParams).rightMargin = (int) HEAD_TAIL_ITEM_MARGIN;
            } else {
                ((LinearLayout.LayoutParams) layoutParams).leftMargin = itemPadding;
                ((LinearLayout.LayoutParams) layoutParams).rightMargin = 0;
            }
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != itemDatas) {
                    int allSize = itemDatas.size();
                    TabBarInfo tabBarInfo = null;
                    for (int i = 0; i < allSize; i++) {
                        tabBarInfo = itemDatas.get(i);
                        if (null != tabBarInfo && tabBarInfo.isSelected()) {
                            tabBarInfo.setSelected(false);
                            notifyItemChanged(i);
                        }
                    }
                }
                if (null != itemClickListner) {
                    if (null != itemDatas && position < itemDatas.size()) {
                        TabBarInfo tabBarInfo = itemDatas.get(position);
                        if (null != tabBarInfo) {
                            tabBarInfo.setSelected(true);
                            notifyItemChanged(position);
                        }
                    }
                    itemClickListner.onItemClick(position, holder.itemView);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemDatas.size();
    }

    public List<TabBarInfo> getItemDatas() {
        return itemDatas;
    }

    public interface ItemClickListner {
        void onItemClick(int position, View itemView);
    }
}
