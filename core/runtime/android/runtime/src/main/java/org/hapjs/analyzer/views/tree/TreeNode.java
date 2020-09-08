/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.views.tree;

import java.util.ArrayList;
import java.util.List;

public class TreeNode<T> {
    public TreeNode parent;
    public List<TreeNode<T>> children;
    public boolean expanded;
    public T data;

    public TreeNode(T data) {
        this.data = data;
        expanded = true;
    }

    public TreeNode(List<TreeNode<T>> children, T data) {
        this.children = children;
        this.data = data;
        expanded = true;
    }

    public boolean isGroup() {
        return children != null && children.size() > 0;
    }

    public void addChild(TreeNode<T> child) {
        if (child == null) {
            return;
        }

        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
        child.parent = this;
    }

    public int getLevel() {
        int level = 0;
        TreeNode node = this;
        while (node.parent != null) {
            node = node.parent;
            level++;
        }
        return level;
    }

    public TreeNode getRootNode() {
        TreeNode root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        return root;
    }

    public boolean isRootNode() {
        return parent == null;
    }

    public List<TreeNode<T>> getTotalChildren() {
        if (!isGroup()) {
            return new ArrayList<>(0);
        }

        List<TreeNode<T>> nodes = new ArrayList<>();
        for (TreeNode<T> child : children) {
            nodes.add(child);
            nodes.addAll(child.getTotalChildren());
        }
        return nodes;
    }
}
