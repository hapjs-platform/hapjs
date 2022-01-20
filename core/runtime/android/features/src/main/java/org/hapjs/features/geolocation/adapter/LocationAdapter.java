/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.geolocation.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import org.hapjs.features.R;
import org.hapjs.widgets.map.model.LocationInfo;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.MyViewHolder> {
    private Context context;
    private List<LocationInfo> datas;
    private OnItemClickListener clickListener;
    private int selectItemIndex;

    public LocationAdapter(Context context, List<LocationInfo> datas) {
        this.datas = datas;
        this.context = context;
        selectItemIndex = 0;
    }

    public void setSelectSearchItemIndex(int selectItemIndex) {
        this.selectItemIndex = selectItemIndex;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(context)
                        .inflate(R.layout.choose_location_poi_item, parent, false);
        return new MyViewHolder(view);
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
            holder.tvPoiName.setText(info.name);
        } else {
            holder.tvPoiAddress.setVisibility(View.VISIBLE);
            holder.tvPoiName.setVisibility(View.VISIBLE);
            holder.tvPoiAddress.setText(info.address);
            holder.tvPoiName.setText(info.name);
        }
        if (selectItemIndex == position) {
            holder.imgCurPoint.setImageResource(R.mipmap.icon_radio_selected);
        } else {
            holder.imgCurPoint.setImageDrawable(null);
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
        private ImageView imgCurPoint;

        public MyViewHolder(View itemView) {
            super(itemView);
            tvPoiName = (TextView) itemView.findViewById(R.id.tv_poi_name);
            tvPoiAddress = (TextView) itemView.findViewById(R.id.tv_poi_address);
            imgCurPoint = (ImageView) itemView.findViewById(R.id.img_choose_poi);
        }
    }
}
