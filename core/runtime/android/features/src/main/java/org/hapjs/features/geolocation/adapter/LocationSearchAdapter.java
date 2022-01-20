/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.geolocation.adapter;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.features.R;
import org.hapjs.widgets.map.model.LocationInfo;

public class LocationSearchAdapter
        extends RecyclerView.Adapter<LocationSearchAdapter.MyViewHolder> {
    private Context context;
    private List<LocationInfo> datas = new ArrayList<>();
    private OnItemClickListener clickListener;
    private String keyWord;

    public LocationSearchAdapter(Context context, List<LocationInfo> datas) {
        this.datas = datas;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(context)
                        .inflate(R.layout.choose_location_poi_search_item, parent, false);
        return new MyViewHolder(view);
    }

    public void setKeyWord(String key) {
        keyWord = key;
    }

    public SpannableString getSpannableString(String content) {
        if (TextUtils.isEmpty(keyWord)) {
            return new SpannableString(content);
        } else {
            SpannableString s = new SpannableString(content);
            Pattern p = Pattern.compile(keyWord);
            Matcher m = p.matcher(s);

            while (m.find()) {
                int start = m.start();
                int end = m.end();
                s.setSpan(
                        new ForegroundColorSpan(ColorUtil.getColor("#0086FF")),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return s;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final LocationInfo info = datas.get(position);
        if (TextUtils.isEmpty(info.name)) {
            holder.tvPoiAddress.setVisibility(View.GONE);
            holder.tvPoiName.setVisibility(View.VISIBLE);
            holder.tvPoiAddress.setText("");
            holder.tvPoiName.setText(info.address);
        } else if (TextUtils.isEmpty(info.address)) {
            holder.tvPoiAddress.setVisibility(View.GONE);
            holder.tvPoiName.setVisibility(View.VISIBLE);
            holder.tvPoiAddress.setText("");
            holder.tvPoiName.setText(getSpannableString(info.name));
        } else {
            holder.tvPoiAddress.setVisibility(View.VISIBLE);
            holder.tvPoiName.setVisibility(View.VISIBLE);
            holder.tvPoiAddress.setText(info.address);
            holder.tvPoiName.setText(getSpannableString(info.name));
        }

        holder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (clickListener != null) {
                            clickListener.onItemClicked(holder.getAdapterPosition(), info);
                        }
                    }
                });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    public void setClickListener(OnItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface OnItemClickListener {
        void onItemClicked(int position, LocationInfo info);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPoiName;
        private TextView tvPoiAddress;

        public MyViewHolder(View itemView) {
            super(itemView);
            tvPoiName = (TextView) itemView.findViewById(R.id.tv_poi_name);
            tvPoiAddress = (TextView) itemView.findViewById(R.id.tv_poi_address);
        }
    }
}
