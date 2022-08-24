
package com.example.newfoodapp_shipper.ui.home;

import com.example.newfoodapp_shipper.Common.Common;
import com.example.newfoodapp_shipper.callback.IShippingOrderCallbsckListener;
import com.example.newfoodapp_shipper.model.ShippingOrderModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel implements IShippingOrderCallbsckListener {

    private MutableLiveData<List<ShippingOrderModel>> shippingOrderMutableData;
    private MutableLiveData<String> messageError;

    private IShippingOrderCallbsckListener listener;

    public HomeViewModel() {
        shippingOrderMutableData = new MutableLiveData<>();
        messageError = new MutableLiveData<>();
        listener = this ;

    }

    public MutableLiveData<String> getMessageError() {
        return messageError;
    }

    public MutableLiveData<List<ShippingOrderModel>> getShippingOrderMutableData(String shipperPhone) {
        loadOrderByShipper(shipperPhone);
        return shippingOrderMutableData;

    }

    private void loadOrderByShipper(String shipperPhone) {
        List<ShippingOrderModel> tempList = new ArrayList<>();


        Query  orderRef = FirebaseDatabase.getInstance().
                getReference(Common.RESTAURANT_REF)
                .child(Common.currentRestaurant.getUid())
                .child(Common.SHIPPING_ORDER_REF)
                .orderByChild("shipperPhone")
                .equalTo(Common.currentShipperUser.getPhone());




        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot orderSnapshot :snapshot.getChildren()){

                    ShippingOrderModel shippingOrderModel = orderSnapshot.getValue(ShippingOrderModel.class);
                    tempList.add(shippingOrderModel);
                }
                listener.onShippingOrderLoadSuccess(tempList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    public void onShippingOrderLoadSuccess(List<ShippingOrderModel> shippingOrderModelList) {
        shippingOrderMutableData.setValue(shippingOrderModelList);
    }

    @Override
    public void onShippingOrderLoadFailed(String message) {

        messageError.setValue(message);
    }
}