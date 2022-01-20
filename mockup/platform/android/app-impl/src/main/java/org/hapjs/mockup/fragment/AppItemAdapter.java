/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.mockup.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.hapjs.LauncherActivity;
import org.hapjs.logging.Source;
import org.hapjs.mockup.app.AppItem;
import org.hapjs.mockup.app.impl.R;

public class AppItemAdapter extends RecyclerView.Adapter<AppItemAdapter.ViewHolder> {

    private final List<AppItem> mValues;

    public AppItemAdapter(Collection<AppItem> items) {
        mValues = new ArrayList<>(items);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fragment_appitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mName.setText(holder.mItem.getName());
        if (holder.mItem.getState() == AppItem.STATE_UPDATE_AVAILABLE) {
            holder.mLaunch.setText(R.string.btn_update);
        } else {
            holder.mLaunch.setText(R.string.btn_launch);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mName;
        public final Button mLaunch;
        public AppItem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mName = (TextView) view.findViewById(R.id.name);
            mLaunch = (Button) view.findViewById(R.id.launch);
            mLaunch.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Source src = new Source();
                            src.setPackageName(mView.getContext().getPackageName());
                            src.setType(Source.TYPE_OTHER);
                            LauncherActivity
                                    .launch(mView.getContext(), mItem.getPackageName(), "/", src);
                        }
                    });
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mName.getText() + "'";
        }
    }
}
