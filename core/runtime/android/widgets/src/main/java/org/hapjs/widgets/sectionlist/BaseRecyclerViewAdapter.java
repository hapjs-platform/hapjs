/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.sectionlist;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BaseRecyclerViewAdapter<D, V extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<V> {

    private List<D> mDatas;
    private RecyclerView mRecyclerView;

    public void bindRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        recyclerView.setAdapter(this);
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @NonNull
    @Override
    public V onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        V holder = createHolder(parent, viewType);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull V holder, int position) {
        bindHolder(holder, getDatas().get(position), position);
    }

    @Override
    public int getItemCount() {
        return getDatas().size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public D getItem(int position) {
        return getDatas().get(position);
    }

    public int getItemPosition(D item) {
        return getDatas().indexOf(item);
    }

    @NonNull
    protected abstract V createHolder(@NonNull ViewGroup parent, int viewType);

    protected abstract void bindHolder(@NonNull V holder, D data, int position);

    public List<D> getDatas() {
        if (mDatas == null) {
            mDatas = new ArrayList<>();
        }
        return mDatas;
    }

    public void setDatas(List<D> datas) {
        getDatas().clear();
        if (datas != null && !datas.isEmpty()) {
            getDatas().addAll(datas);
        }

        if (mRecyclerView != null && mRecyclerView.isComputingLayout()) {
            mRecyclerView.post(this::notifyDataSetChanged);
            return;
        }
        notifyDataSetChanged();
    }

    public void clear() {
        if (mRecyclerView.isComputingLayout()) {
            mRecyclerView.post(
                    () -> {
                        getDatas().clear();
                        notifyDataSetChanged();
                    });
        } else {
            getDatas().clear();
            notifyDataSetChanged();
        }
    }

    public void addItem(D item) {
        addItem(getDatas().size(), item);
    }

    public void addItem(int position, D item) {
        if (position < 0 || position > getDatas().size()) {
            return;
        }

        if (mRecyclerView.isComputingLayout()) {
            int finalPosition = position;
            mRecyclerView.post(
                    () -> {
                        getDatas().add(finalPosition, item);
                        mRecyclerView.post(() -> notifyItemInserted(finalPosition));
                    });
        } else {
            getDatas().add(position, item);
            notifyItemInserted(position);
        }
    }

    public void addItems(List<D> items) {
        addItems(getDatas().size(), items);
    }

    public void addItems(int position, List<D> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        if (position < 0 || position > getDatas().size()) {
            return;
        }

        if (mRecyclerView.isComputingLayout()) {
            int finalPosition = position;
            mRecyclerView.post(
                    () -> {
                        getDatas().addAll(finalPosition, items);
                        notifyItemRangeInserted(finalPosition, items.size());
                    });
        } else {
            getDatas().addAll(position, items);
            notifyItemRangeInserted(position, items.size());
        }
    }

    public void removeItem(D item) {
        int position = getItemPosition(item);
        if (position > 0) {
            removeItem(position);
        }
    }

    public void removeItem(int position) {
        if (position < 0 || position >= getDatas().size()) {
            return;
        }

        if (mRecyclerView.isComputingLayout()) {
            mRecyclerView.post(
                    () -> {
                        getDatas().remove(position);
                        notifyItemRemoved(position);
                    });
        } else {
            getDatas().remove(position);
            notifyItemRemoved(position);
        }
    }

    public void removeItems(List<D> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        // 如果删除连续的位置，可以局部进行刷新
        boolean continuouslyContain = true;
        D first = items.get(0);
        D last = items.get(items.size() - 1);
        List<D> datas = getDatas();
        int firstIndex = datas.indexOf(first);
        int lastIndex = datas.indexOf(last);
        if (firstIndex >= 0 && lastIndex > firstIndex) {
            if ((lastIndex - firstIndex + 1) != items.size()) {
                continuouslyContain = false;
            } else {
                int l = 0;
                int r = items.size() - 1;
                for (int start = firstIndex, end = lastIndex; end >= start;
                        start++, l++, end--, r--) {
                    if (Objects.equals(datas.get(start), items.get(l))
                            && Objects.equals(datas.get(end), items.get(r))) {
                        continue;
                    }
                    continuouslyContain = false;
                    break;
                }
            }
        } else {
            continuouslyContain = false;
        }

        if (mRecyclerView.isComputingLayout()) {
            boolean finalContinuouslyContain = continuouslyContain;
            mRecyclerView.post(
                    () -> {
                        getDatas().removeAll(items);
                        if (finalContinuouslyContain) {
                            notifyItemRangeRemoved(firstIndex, items.size());
                        } else {
                            notifyDataSetChanged();
                        }
                    });
        } else {
            getDatas().removeAll(items);
            if (continuouslyContain) {
                notifyItemRangeRemoved(firstIndex, items.size());
            } else {
                notifyDataSetChanged();
            }
        }
    }

    public void setItem(int position, D item) {
        if (position < 0 || position >= getDatas().size()) {
            return;
        }

        if (mRecyclerView.isComputingLayout()) {
            mRecyclerView.post(
                    () -> {
                        getDatas().set(position, item);
                        notifyItemChanged(position);
                    });
        } else {
            getDatas().set(position, item);
            notifyItemChanged(position);
        }
    }

    public void post(Runnable runnable) {
        mRecyclerView.post(runnable);
    }
}
