package com.example.newfoodapp_shipper.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.newfoodapp_shipper.Common.Common;
import com.example.newfoodapp_shipper.R;
import com.example.newfoodapp_shipper.callback.IRecyclerClickListener;
import com.example.newfoodapp_shipper.model.RestaurantModel;
import com.example.newfoodapp_shipper.model.eventbus.RestaurantSelectEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyRestaurantAdapter extends RecyclerView.Adapter<MyRestaurantAdapter.MyViewHolder> {

    Context context ;
    List<RestaurantModel> restaurantModelList;

    public MyRestaurantAdapter(Context context, List<RestaurantModel> restaurantModelList) {
        this.context = context;
        this.restaurantModelList = restaurantModelList;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder((LayoutInflater.from(context).inflate(R.layout.layout_restaurant,parent,false))) ;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context)
                .load(restaurantModelList.get(position).getImageUrl())
                .into(holder.img_restaurant);
        holder.txt_restaurant_name.setText(new StringBuilder(restaurantModelList.get(position).getName()));
        holder.txt_restaurant_address.setText(new StringBuilder(restaurantModelList.get(position).getAddress()));




        //Events
        holder.setListener((view, pos) -> {
            // Code late

            Common.currentRestaurant = restaurantModelList.get(pos);
            EventBus.getDefault().postSticky(new RestaurantSelectEvent(restaurantModelList.get(pos)));

        });
    }

    @Override
    public int getItemCount() {
        return restaurantModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.txt_restaurant_name)
        TextView txt_restaurant_name;
        @BindView(R.id.txt_restaurant_address)
        TextView txt_restaurant_address;
        @BindView(R.id.img_restaurant)
        ImageView img_restaurant;
        Unbinder unbinder;

        IRecyclerClickListener listener;

        public void setListener(IRecyclerClickListener listener) {
            this.listener = listener;
        }

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this,itemView);
            itemView.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            listener.onItemClickListener(v,getAdapterPosition());
        }
    }

}
