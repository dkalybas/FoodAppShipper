package com.example.newfoodapp_shipper.callback;

import com.example.newfoodapp_shipper.model.ShippingOrderModel;

import java.util.List;

public interface IShippingOrderCallbsckListener {

    void onShippingOrderLoadSuccess(List<ShippingOrderModel> shippingOrderModelList);
    void onShippingOrderLoadFailed(String message);


}
