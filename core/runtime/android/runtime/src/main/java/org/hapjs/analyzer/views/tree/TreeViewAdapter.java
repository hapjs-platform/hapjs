/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.views.tree;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.runtime.R;

import java.util.List;
import java.util.Objects;

public class TreeViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_GROUP = 1;
    private static final int TYPE_ITEM = 2;
    private static final int SELECT_COLOR = 0x99456FFF;
    private static final int NORMAL_COLOR = Color.TRANSPARENT;
    private RecyclerView mRecyclerView;
    private List<TreeNode<NodeItemInfo>> mNodes;
    private OnNodeItemClickListener mOnNodeItemClickListener;
    private TreeNode mSelectNode;

    public interface OnNodeItemClickListener {
        void onNodeItemClick(RecyclerView.ViewHolder holder, int position, TreeNode<NodeItemInfo> node);
    }

    void setOnNodeItemClickListener(OnNodeItemClickListener onNodeItemClickListener) {
        mOnNodeItemClickListener = onNodeItemClickListener;
    }

    void bindRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        recyclerView.setAdapter(this);
    }

    public void setNodes(List<TreeNode<NodeItemInfo>> nodes) {
        mSelectNode = null;
        mNodes = nodes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        if (viewType == TYPE_ITEM) {
            TreeViewHolder itemViewHolder = TreeViewHolder.createViewHolder(parent.getContext(), parent);
            itemViewHolder.itemView.setOnClickListener(v -> {
                int position = itemViewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    onItemContentClick(itemViewHolder, position, mNodes.get(position));
                }
            });
            holder = itemViewHolder;
        } else {
            GroupTreeViewHolder groupViewHolder = GroupTreeViewHolder.createViewHolder(parent.getContext(), parent);
            groupViewHolder.mArrowView.setOnClickListener(v -> {
                int position = groupViewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    onItemArrowClick(groupViewHolder, position, mNodes.get(position));
                }
            });

            groupViewHolder.mContentTextView.setOnClickListener(v -> {
                int position = groupViewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    onItemContentClick(groupViewHolder, position, mNodes.get(position));
                }
            });
            holder = groupViewHolder;
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TreeNode<NodeItemInfo> node = mNodes.get(position);
        if (holder instanceof TreeViewHolder) {
            TreeViewHolder itemHolder = (TreeViewHolder) holder;
            onBindTreeItemHolder(itemHolder, node);
        } else {
            onBindTreeGroupHolder((GroupTreeViewHolder) holder, node);
        }
        if (Objects.equals(node, mSelectNode)) {
            holder.itemView.setBackgroundColor(SELECT_COLOR);
        } else {
            holder.itemView.setBackgroundColor(NORMAL_COLOR);
        }
    }

    @Override
    public int getItemViewType(int position) {
        TreeNode node = mNodes.get(position);
        if (node.children == null || node.children.isEmpty()) {
            return TYPE_ITEM;
        }
        return TYPE_GROUP;
    }

    @Override
    public int getItemCount() {
        return mNodes == null ? 0 : mNodes.size();
    }

    private void onBindTreeItemHolder(TreeViewHolder holder, TreeNode<NodeItemInfo> node) {
        View itemView = holder.itemView;
        NodeItemInfo info = node.data;
        holder.setContent(info.title);
        int level = node.getLevel();
        int margin = DisplayUtil.dip2Pixel(itemView.getContext(), level * 5 + 22);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
        if (params == null) {
            params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemView.setLayoutParams(params);
        }
        params.leftMargin = margin;
        itemView.requestLayout();
    }

    private void onBindTreeGroupHolder(GroupTreeViewHolder holder, TreeNode<NodeItemInfo> node) {
        View itemView = holder.itemView;
        NodeItemInfo info = node.data;
        if (node.expanded) {
            holder.expandArrow();
        } else {
            holder.foldArrow();
        }
        holder.setContent(info.title);
        int level = node.getLevel();
        int margin = DisplayUtil.dip2Pixel(itemView.getContext(), level * 5);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
        if (params == null) {
            params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemView.setLayoutParams(params);
        }
        params.leftMargin = margin;
        itemView.requestLayout();
    }

    private void onItemContentClick(RecyclerView.ViewHolder holder, int position, TreeNode<NodeItemInfo> node) {
        if (mOnNodeItemClickListener != null) {
            mOnNodeItemClickListener.onNodeItemClick(holder, position, node);
            TreeNode preSelectNode = mSelectNode;
            mSelectNode = node;
            holder.itemView.setBackgroundColor(SELECT_COLOR);
            if (preSelectNode != null) {
                int index = mNodes.indexOf(preSelectNode);
                if (index >= 0) {
                    RecyclerView.ViewHolder preSelectHolder = mRecyclerView.findViewHolderForAdapterPosition(index);
                    if (preSelectHolder != null) {
                        preSelectHolder.itemView.setBackgroundColor(NORMAL_COLOR);
                    }
                }
            }
        }
    }

    private void onItemArrowClick(GroupTreeViewHolder holder, int position, TreeNode<NodeItemInfo> node) {
        resolveDisplay(position, holder, node);
    }


    private void resolveDisplay(int position, GroupTreeViewHolder holder, TreeNode<NodeItemInfo> node) {
        if (node.expanded) {
            List<TreeNode<NodeItemInfo>> children = node.getTotalChildren();
            if (children != null && children.size() > 0) {
                mNodes.removeAll(children);
                notifyDataSetChanged();
            }
            holder.foldArrow();
        } else {
            List<TreeNode<NodeItemInfo>> children = node.getTotalChildren();
            if (children != null && children.size() > 0) {
                mNodes.addAll(position + 1, children);
                notifyItemRangeInserted(position + 1, children.size());
            }
            holder.expandArrow();
        }
        node.expanded = !node.expanded;
    }

    static class TreeViewHolder extends RecyclerView.ViewHolder {

        TextView mContentTextView;

        TreeViewHolder(@NonNull View itemView) {
            super(itemView);
            mContentTextView = itemView.findViewById(R.id.analyzer_tree_item_content);
        }

        public void setContent(String content) {
            mContentTextView.setText(content);
        }

        public static TreeViewHolder createViewHolder(Context context, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.layout_analyzer_tree_item, parent, false);
            return new TreeViewHolder(view);
        }
    }

    static class GroupTreeViewHolder extends RecyclerView.ViewHolder {

        ImageView mArrowView;
        TextView mContentTextView;

        GroupTreeViewHolder(@NonNull View itemView) {
            super(itemView);
            mArrowView = itemView.findViewById(R.id.analyzer_tree_group_arrow);
            mContentTextView = itemView.findViewById(R.id.analyzer_tree_group_content);
        }

        public void setContent(String content) {
            mContentTextView.setText(content);
        }

        void expandArrow() {
            mArrowView.setImageResource(R.drawable.analyzer_arrow_down);
        }

        void foldArrow() {
            mArrowView.setImageResource(R.drawable.analyzer_arrow_right);
        }

        static GroupTreeViewHolder createViewHolder(Context context, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.layout_analyzer_tree_group, parent, false);
            return new GroupTreeViewHolder(view);
        }
    }
}
