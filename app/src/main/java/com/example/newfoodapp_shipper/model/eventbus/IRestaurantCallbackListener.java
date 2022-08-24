package com.example.newfoodapp_shipper.model.eventbus;

import com.example.newfoodapp_shipper.model.RestaurantModel;

import java.util.List;

public interface IRestaurantCallbackListener {


    void onRestaurantLoadSuccess(List<RestaurantModel> restaurantModelList);
    void onRestaurantLoadFailed(String message);




}
