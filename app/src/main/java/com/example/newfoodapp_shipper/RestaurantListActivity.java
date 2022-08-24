 package com.example.newfoodapp_shipper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.paperdb.Paper;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.Toast;

import com.example.newfoodapp_shipper.Common.Common;
import com.example.newfoodapp_shipper.adapter.MyRestaurantAdapter;
import com.example.newfoodapp_shipper.model.RestaurantModel;
import com.example.newfoodapp_shipper.model.ShipperUserModel;
import com.example.newfoodapp_shipper.model.eventbus.IRestaurantCallbackListener;
import com.example.newfoodapp_shipper.model.eventbus.RestaurantSelectEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

 public class RestaurantListActivity extends AppCompatActivity implements IRestaurantCallbackListener {


    @BindView(R.id.recycler_restaurant)
    RecyclerView recycler_restaurant;
    AlertDialog dialog;
    LayoutAnimationController layoutAnimationController;
    MyRestaurantAdapter adapter ;

    DatabaseReference serverRef;
    IRestaurantCallbackListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_list);

        initViews();
        loadAllRestaurant();

    }

     private void loadAllRestaurant() {

        dialog.show();
        List<RestaurantModel> restaurantModels = new ArrayList<>();
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF);

        restaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){

                    for (DataSnapshot restaurantSnapShot : snapshot.getChildren()){

                        RestaurantModel restaurantModel = restaurantSnapShot.getValue(RestaurantModel.class);
                        restaurantModel.setUid(restaurantSnapShot.getKey());
                        restaurantModels.add(restaurantModel);

                    }
                    if(restaurantModels.size() > 0)
                        listener.onRestaurantLoadSuccess(restaurantModels);
                    else
                        listener.onRestaurantLoadFailed("Restaurant List  empty ");

                } else {

                    listener.onRestaurantLoadFailed("Restaurant List not Found !!!");

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onRestaurantLoadFailed(error.getMessage());
            }
        });
     }

     private void initViews() {
         ButterKnife.bind(this);
         listener = this;

        dialog= new AlertDialog.Builder(this).setCancelable(false).setMessage("Please wait...")
                .create();
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(this,R.anim.layout_slide_from_left);
         LinearLayoutManager layoutManager = new LinearLayoutManager(this);
         layoutManager.setOrientation(RecyclerView.VERTICAL);
         recycler_restaurant.setLayoutManager(layoutManager);
         recycler_restaurant.addItemDecoration(new DividerItemDecoration(this,layoutManager.getOrientation()));

     }


     @Override
     public void onRestaurantLoadSuccess(List<RestaurantModel> restaurantModelList) {
        dialog.dismiss();
        adapter = new MyRestaurantAdapter(this,restaurantModelList);
        recycler_restaurant.setAdapter(adapter);
        recycler_restaurant.setLayoutAnimation(layoutAnimationController);


     }

     @Override
     public void onRestaurantLoadFailed(String message) {

         Toast.makeText(this,message,Toast.LENGTH_SHORT).show();

     }


     //Event  Bus here


     @Override
     protected void onStart() {
         super.onStart();
         EventBus.getDefault().register(this);


     }


     @Override
     protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();

     }


     @Subscribe(sticky=true,threadMode = ThreadMode.MAIN)
     public void onRestaurantSelectedEvent(RestaurantSelectEvent event){

        if (event != null){

            String jsonEncode = new Gson().toJson(event.getRestaurantModel());
            Paper.init(this);
            Paper.book().write(Common.RESTAURANT_SAVE,jsonEncode);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null){

                checkServerUserFromFirebase(user,event.getRestaurantModel().getUid());

            }

        }

     }

     private void checkServerUserFromFirebase(FirebaseUser user, String uid) {

        dialog.show();

        serverRef = FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
                .child(uid)
                .child(Common.SHIPPER_REF);

        serverRef.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists())
                        {

                            ShipperUserModel userModel = snapshot.getValue(ShipperUserModel.class);
                        if (userModel.isActive()){

                            goToHomeActivity(userModel);

                        }else{
                            dialog.dismiss();
                            Toast.makeText(RestaurantListActivity.this,
                                    "you must be allowed from server app to do that ",Toast.LENGTH_SHORT).show();

                        }

                        }

                        else{

                            dialog.dismiss();
                            showRegisterDialog(user,uid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


     }

     private void showRegisterDialog(FirebaseUser user, String uid)  {


         androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
         builder.setTitle("Register");
         builder.setMessage("Please fill on these information \n and  you will be accepted by admin later");

         View itemView  = LayoutInflater.from(this).inflate(R.layout.layout_register,null);
         EditText my_name = (EditText)itemView.findViewById(R.id.myName);
         EditText my_phone = (EditText)itemView.findViewById(R.id.myPhone);

         //Setting data
         my_phone.setText(user.getPhoneNumber());
         builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                 .setPositiveButton("Register", (dialogInterface, which) -> {

                     if (TextUtils.isEmpty(my_name.getText().toString())){

                         Toast.makeText(RestaurantListActivity.this,"Please enter your name here ",Toast.LENGTH_SHORT).show();
                         return;

                     }

                     ShipperUserModel shipperUserModel = new ShipperUserModel();
                     shipperUserModel.setUid(user.getUid());
                     shipperUserModel.setName(my_name.getText().toString());
                     shipperUserModel.setPhone(my_phone.getText().toString());
                     shipperUserModel.setActive(false); // By default , if we want that true we do it manually on Firebase

                     dialog.show();

                     //Init ServerRef here ...
                     serverRef =FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
                             .child(uid)
                             .child(Common.SHIPPER_REF);

                     serverRef.child(shipperUserModel.getUid())
                             .setValue(shipperUserModel)
                             .addOnFailureListener(e -> {
                                 dialog.dismiss();
                                 Toast.makeText(RestaurantListActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                             }).addOnCompleteListener(task -> {
                         dialog.dismiss();
                         Toast.makeText(RestaurantListActivity.this,"Congrats you got Registered " +
                                 "Succesfully!! Admin will check and activate you asap",Toast.LENGTH_SHORT).show();
                         //goToHomeActivity(serverUserModel);
                     });


                 });

         builder.setView(itemView);

         androidx.appcompat.app.AlertDialog registerDialog = builder.create();
         registerDialog.show();

     }



     private void  goToHomeActivity(ShipperUserModel userModel) {

        dialog.dismiss();
        Common.currentShipperUser = userModel;
        startActivity(new Intent(this,HomeActivity.class));
        finish();
     }


 }